"""Payload-preservation fixtures; real signing is checked separately with SDK tools."""
from pathlib import Path
import tempfile
import unittest
import warnings
import zipfile

from create_apk_compat import verify_payload


class CompatibilityPayloadTest(unittest.TestCase):
    def compare(self, entries, *, compression=zipfile.ZIP_STORED, duplicate=False):
        with tempfile.TemporaryDirectory() as tmp:
            source, target = Path(tmp) / "source.apk", Path(tmp) / "compat.apk"
            original = {"AndroidManifest.xml": b"manifest fixture", "classes.dex": b"dex fixture",
                        "META-INF/services/example": b"ordinary metadata"}
            with zipfile.ZipFile(source, "w") as archive:
                for name, data in original.items():
                    archive.writestr(name, data)
            with zipfile.ZipFile(target, "w", compression=compression) as archive:
                for name, data in entries.items():
                    archive.writestr(name, data)
                if duplicate:
                    with warnings.catch_warnings():
                        warnings.simplefilter("ignore", UserWarning)
                        archive.writestr("classes.dex", b"duplicate")
            return verify_payload(source, target)

    def entries(self):
        return {"AndroidManifest.xml": b"manifest fixture", "classes.dex": b"dex fixture",
                "META-INF/services/example": b"ordinary metadata", "META-INF/MANIFEST.MF": b"jar fixture",
                "META-INF/ANDROID.SF": b"signature fixture", "META-INF/ANDROID.RSA": b"certificate fixture"}

    def test_only_signature_entry_addition_is_accepted(self):
        result = self.compare(self.entries())
        self.assertEqual(3, result["non_signature_entries"])
        self.assertTrue(result["payload_bytes_preserved"])

    def test_changed_code_is_rejected(self):
        entries = self.entries(); entries["classes.dex"] = b"changed code"
        with self.assertRaises(ValueError): self.compare(entries)

    def test_missing_manifest_is_rejected(self):
        entries = self.entries(); del entries["AndroidManifest.xml"]
        with self.assertRaises(ValueError): self.compare(entries)

    def test_extra_non_signature_entry_is_rejected(self):
        entries = self.entries(); entries["new-payload"] = b"unexpected"
        with self.assertRaises(ValueError): self.compare(entries)

    def test_regular_meta_inf_payload_cannot_be_changed(self):
        entries = self.entries(); entries["META-INF/services/example"] = b"changed service"
        with self.assertRaises(ValueError): self.compare(entries)

    def test_changed_compression_is_rejected(self):
        with self.assertRaises(ValueError): self.compare(self.entries(), compression=zipfile.ZIP_DEFLATED)

    def test_duplicate_zip_entry_is_rejected(self):
        with self.assertRaises(ValueError): self.compare(self.entries(), duplicate=True)


if __name__ == "__main__":
    unittest.main()
