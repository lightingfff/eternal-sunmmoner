"""Decision tests with explicitly simulated ADB responses, not installation evidence."""
import json
from pathlib import Path
import subprocess
import tempfile
import unittest

from check_apk_installability import ACTIVITY, PACKAGE, probe


class InstallProbeTest(unittest.TestCase):
    def run_case(self, install=None, present=False, serial="emulator-5580", boot="1", qemu="1",
                 after_path=True, dumpsys_error=False):
        calls = []
        installed = False

        def runner(argv, **kwargs):
            nonlocal installed
            calls.append(argv)
            args = argv[3:]
            code, out, err = 0, "", ""
            if args == ["shell", "getprop", "ro.kernel.qemu"] or args == ["shell", "getprop", "ro.boot.qemu"]:
                out = qemu
            elif args == ["shell", "getprop", "sys.boot_completed"]:
                out = boot
            elif args == ["shell", "pm", "path", PACKAGE]:
                out = "package:/data/app/fixture/base.apk\n" if present or installed and after_path else ""
            elif args[0] == "install":
                installed = True
                if isinstance(install, Exception):
                    raise install
                code, out, err = install or (0, "Performing Push Install\nSuccess\n", "")
            elif args[:2] == ["shell", "dumpsys"] and dumpsys_error:
                code, err = 1, "fixture metadata failure"
            return subprocess.CompletedProcess(argv, code, out, err)

        with tempfile.TemporaryDirectory() as tmp:
            metadata = {"application_id": PACKAGE, "launchable_activity": ACTIVITY}
            report = probe(Path("adb"), serial, Path("candidate.apk"), metadata, Path(tmp), runner=runner)
            self.assertEqual(report, json.loads((Path(tmp) / "report.json").read_text()))
        return report, calls

    def test_only_success_plus_actual_package_path_can_pass(self):
        report, calls = self.run_case()
        self.assertTrue(report["installation_verified"])
        self.assertEqual("PASS", report["status"])
        self.assertIn(["adb", "-s", "emulator-5580", "install", "--no-streaming", "candidate.apk"], calls)

    def test_installer_failure_with_exit_zero_is_still_rejected(self):
        report, _ = self.run_case((0, "Failure [INSTALL_FAILED_INVALID_APK: fixture]\n", ""))
        self.assertFalse(report["installation_verified"])
        self.assertEqual("INSTALL_REJECTED", report["status"])
        self.assertIn("INSTALL_FAILED_INVALID_APK", report["error"])

    def test_nonzero_install_cannot_pass_even_with_success_text(self):
        report, _ = self.run_case((1, "Success\n", "transport failed"))
        self.assertFalse(report["installation_verified"])

    def test_mixed_success_and_failure_text_cannot_pass(self):
        report, _ = self.run_case((0, "Success\nFailure [INSTALL_FAILED_INVALID_APK]\n", ""))
        self.assertFalse(report["installation_verified"])

    def test_failed_package_metadata_does_not_leave_pass_status(self):
        report, _ = self.run_case(dumpsys_error=True)
        self.assertEqual("ERROR", report["status"])
        self.assertFalse(report["installation_verified"])

    def test_missing_package_after_success_cannot_pass(self):
        report, _ = self.run_case(after_path=False)
        self.assertEqual("ERROR", report["status"])
        self.assertFalse(report["installation_verified"])

    def test_existing_install_is_preserved_and_no_install_attempted(self):
        report, calls = self.run_case(present=True)
        self.assertEqual("BLOCKED_NOT_FRESH", report["status"])
        self.assertFalse(any("install" in command or "uninstall" in command for command in calls))

    def test_physical_device_serial_is_refused_before_any_adb_command(self):
        report, calls = self.run_case(serial="physical-device")
        self.assertFalse(report["installation_verified"])
        self.assertEqual([], calls)

    def test_nonemulator_and_unbooted_targets_cannot_install(self):
        for kwargs in ({"qemu": "0"}, {"boot": ""}):
            report, calls = self.run_case(**kwargs)
            self.assertFalse(report["installation_verified"])
            self.assertFalse(any("install" in command for command in calls))

    def test_timeout_is_an_error_not_a_passing_install(self):
        report, _ = self.run_case(subprocess.TimeoutExpired("adb install", 300))
        self.assertFalse(report["installation_verified"])
        self.assertEqual("ERROR", report["status"])


if __name__ == "__main__":
    unittest.main()
