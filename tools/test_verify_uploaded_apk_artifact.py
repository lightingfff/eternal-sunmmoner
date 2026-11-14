"""Transport/ZIP fixtures; signature subprocess is stubbed, real signing is checked in CI."""
from contextlib import redirect_stdout
import io
import os
import stat
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import zipfile

from package_apk import APK, PORTABLE, package
from verify_uploaded_apk_artifact import IntegrityError, RetrievalError, record, verify_uploaded


class ArtifactVerificationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.reference = self.root / "reference"
        self.reference.mkdir()
        source = self.root / "fixture.apk"
        with zipfile.ZipFile(source, "w") as apk:
            for name in ("AndroidManifest.xml", "classes.dex", "resources.arsc"):
                apk.writestr(name, b"not a signed Android binary; ZIP fixture only")
        with redirect_stdout(io.StringIO()):
            package(source, self.reference)
        self.payload = self.outer()
        self.downloads = 0
        self.signatures = 0

    def outer(self, changes=None, omit=(), extra=None, modes=None):
        out = io.BytesIO()
        with zipfile.ZipFile(out, "w", compression=zipfile.ZIP_STORED) as archive:
            for name in (APK, PORTABLE, "SHA256SUMS"):
                if name not in omit:
                    entry = zipfile.ZipInfo(name)
                    entry.create_system = 3
                    entry.external_attr = ((modes or {}).get(name, stat.S_IFREG) | 0o600) << 16
                    archive.writestr(entry, (changes or {}).get(name, (self.reference / name).read_bytes()))
            for name, content in (extra or {}).items():
                archive.writestr(name, content)
        return out.getvalue()

    def run_case(self, responses, payload=None, signer_ok=True):
        replies = iter(responses)
        def invoke(argv, **kwargs):
            if argv[0] == "gh":
                self.downloads += 1
                status = next(replies)
                if status == "timeout":
                    raise subprocess.TimeoutExpired(argv, 30)
                if status == "network":
                    return subprocess.CompletedProcess(argv, 1, stderr=b"dial tcp: network is unreachable")
                if status:
                    kwargs["stdout"].write(b"partial error body")
                    return subprocess.CompletedProcess(argv, 1,
                        stderr=f"gh: request failed (HTTP {status})".encode())
                kwargs["stdout"].write(self.payload if payload is None else payload)
                return subprocess.CompletedProcess(argv, 0, stderr=b"")
            self.signatures += 1
            return subprocess.CompletedProcess(argv, 0 if signer_ok else 1, stdout="", stderr="")
        with patch("verify_uploaded_apk_artifact.subprocess.run", side_effect=invoke), \
                patch("verify_uploaded_apk_artifact.time.sleep") as sleep:
            self.result = verify_uploaded("lightingfff/eternal-sunmmoner", "14067424689",
                self.reference, "fixture-apksigner", retry_delay=0)
            self.sleeps = sleep.call_count
        return self.result

    def test_success_requires_all_local_bytes_and_signature(self):
        self.assertEqual("verified", self.run_case([0])["status"])
        self.assertEqual((1, 1), (self.downloads, self.signatures))

    def test_404_then_success_retries_without_retaining_partial_error_body(self):
        result = self.run_case([404, 0])
        self.assertEqual(("verified", 2), (result["status"], result["attempts"]))
        self.assertEqual((2, 1, 1), (self.downloads, self.signatures, self.sleeps))

    def test_exhausted_404_is_explicitly_unavailable_not_verified(self):
        result = self.run_case([404, 404, 404])
        self.assertEqual(("unavailable", 3, 404), (result["status"], result["attempts"], result["http_status"]))
        self.assertEqual((3, 0, 2), (self.downloads, self.signatures, self.sleeps))
        output, summary = self.root / "outputs", self.root / "summary"
        with patch.dict(os.environ, {"GITHUB_OUTPUT": str(output), "GITHUB_STEP_SUMMARY": str(summary)}), \
                redirect_stdout(io.StringIO()) as capture:
            record(result, self.root / "result.json")
        self.assertIn("::warning::", capture.getvalue())
        self.assertIn("NOT verified", capture.getvalue())
        self.assertEqual("artifact_verification=unavailable\n", output.read_text())
        self.assertIn("UNAVAILABLE", summary.read_text())

    def test_401_and_403_are_fatal_configuration_errors(self):
        for status in (401, 403):
            with self.subTest(status=status), self.assertRaises(RetrievalError):
                self.run_case([status])
        self.assertEqual(0, self.signatures)

    def test_timeout_and_server_error_can_recover(self):
        self.assertEqual("verified", self.run_case(["timeout", 503, 0])["status"])
        self.assertEqual(3, self.downloads)

    def test_auth_status_without_parentheses_is_still_fatal_and_not_logged(self):
        response = subprocess.CompletedProcess([], 1, stderr=b"HTTP 403: forbidden fixture-secret")
        with patch("verify_uploaded_apk_artifact.subprocess.run", return_value=response), \
                redirect_stdout(io.StringIO()) as output, self.assertRaises(RetrievalError) as error:
            verify_uploaded("owner/repo", "9", self.reference, "fixture-apksigner")
        self.assertNotIn("fixture-secret", str(error.exception) + output.getvalue())

    def test_known_network_error_retries(self):
        self.assertEqual("verified", self.run_case(["network", 0])["status"])
        self.assertEqual(2, self.downloads)

    def test_unknown_cli_configuration_errors_and_auth_exit_are_fatal(self):
        for code, detail in ((1, b"error parsing configuration: yaml"), (1, b"unknown flag --invalid"),
                             (4, b"token required")):
            response = subprocess.CompletedProcess([], code, stderr=detail)
            with self.subTest(code=code, detail=detail), \
                    patch("verify_uploaded_apk_artifact.subprocess.run", return_value=response), \
                    patch("verify_uploaded_apk_artifact.time.sleep") as sleep, self.assertRaises(RetrievalError):
                verify_uploaded("owner/repo", "9", self.reference, "fixture-apksigner")
            sleep.assert_not_called()

    def test_exhausted_server_failure_stays_unavailable(self):
        self.assertEqual("unavailable", self.run_case([502, 503, 504])["status"])
        self.assertEqual(0, self.signatures)

    def test_rate_limit_stops_without_more_requests(self):
        result = self.run_case([429])
        self.assertEqual(("unavailable", 1, 0), (result["status"], self.downloads, self.sleeps))

    def test_html_or_nonzip_success_is_fatal_integrity_failure(self):
        with self.assertRaises(IntegrityError):
            self.run_case([0], payload=b"<html>Not an artifact</html>")
        self.assertEqual((1, 0), (self.downloads, self.signatures))

    def test_missing_checksum_manifest_is_not_ignored(self):
        with self.assertRaises(IntegrityError):
            self.run_case([0], payload=self.outer(omit=("SHA256SUMS",)))

    def test_same_size_modified_bytes_are_fatal(self):
        data = bytearray((self.reference / APK).read_bytes())
        data[-1] ^= 1
        with self.assertRaises(IntegrityError):
            self.run_case([0], payload=self.outer(changes={APK: bytes(data)}))
        self.assertEqual(0, self.signatures)

    def test_unexpected_and_traversal_paths_are_rejected(self):
        with self.assertRaises(IntegrityError):
            self.run_case([0], payload=self.outer(extra={"../escaped": b"bad"}))
        self.assertFalse((self.root.parent / "escaped").exists())

    def test_corrupt_outer_zip_crc_is_fatal(self):
        data = bytearray(self.payload)
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            info = archive.getinfo(APK)
            offset = info.header_offset + 30 + len(info.filename.encode()) + len(info.extra)
        data[offset + 10] ^= 1
        with self.assertRaises(IntegrityError):
            self.run_case([0], payload=bytes(data))

    def test_special_file_modes_are_fatal_even_when_bytes_match(self):
        for mode in (stat.S_IFIFO, stat.S_IFDIR, stat.S_IFLNK, stat.S_IFSOCK):
            with self.subTest(mode=mode), self.assertRaises(IntegrityError):
                self.run_case([0], payload=self.outer(modes={APK: mode}))

    def test_signature_failure_is_fatal_not_optional(self):
        with self.assertRaises(IntegrityError):
            self.run_case([0], signer_ok=False)
        self.assertEqual((1, 1), (self.downloads, self.signatures))

    def test_bad_local_reference_fails_before_network(self):
        (self.reference / "SHA256SUMS").unlink()
        with self.assertRaises(IntegrityError):
            self.run_case([0])
        self.assertEqual(0, self.downloads)

    def test_invalid_id_and_repository_fail_before_network(self):
        with patch("verify_uploaded_apk_artifact.subprocess.run") as transport:
            for repo, artifact in (("../wrong", "4"), ("owner/repo", ""), ("owner/repo", "0")):
                with self.subTest(repo=repo, artifact=artifact), self.assertRaises(RetrievalError):
                    verify_uploaded(repo, artifact, self.reference, "fixture-apksigner")
            transport.assert_not_called()


if __name__ == "__main__":
    unittest.main()
