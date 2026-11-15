# APK fresh-install investigation — Issue #121

The owner reports Samsung Package Installer rejects Eternal Summoner as an invalid
package even after removing the old app. The screenshot shows the correct app
label/icon. This is treated as an installation failure; it is not dismissed as an
update-key mismatch or device fault. The original CRC/signature audit was not
sufficient to establish installability.

## Actual APK and observed result

Downloaded the public release `apk-392-507973cbceb258013bb585e261d38d3f38714966`:

- APK: `Eternal-Summoner-debug.apk`, 47,235,814 bytes.
- SHA256: `8417389a8df36f9f05547f802123ce10e52fa0da1517572856b471bcedb8df9a`.
- Package `com.aistudio.eternalsummoner.kvrpzm`; launcher `com.example.MainActivity`;
  min SDK24 / target36 / versionCode1, unchanged.
- Single v2 signing certificate SHA256
  `9d179477ce35520d6af58f29864c94d60ae4c8ca283e84541cd08192534a8b2b`.

On **2026-11-15 (Asia/Saigon)**, installed those exact bytes on a newly created
Android15/API35 AOSP ATD x86_64 emulator with an empty app state. This used a real
Android PackageManager, not Robolectric or a mocked installer:

```text
adb -s emulator-5580 install --no-streaming Eternal-Summoner-debug.apk
Performing Push Install
Success
```

No `-t`, reinstall/uninstall, signing alteration, package-name change or SDK
downgrade was used. PackageManager then returned the installed `base.apk` path
and `dumpsys package` confirmed installed=true. Raw commands, APK identity,
device fingerprint, prior absence and installation output are retained in the
`api35/` evidence. The emulator runs in software because this Cloud host has no
`/dev/kvm`; it is not a Samsung/physical-phone measurement.

The owner's reported rejection remains **UNRESOLVED**. This successful emulator
install narrows the investigation; it does not establish which bytes/installer
condition produced the owner's error or prove the problem fixed. A second API30
check and exact-HEAD CI are reported separately in the Issue/PR handoff as they
complete. An exploratory `am start -W` timed out under software emulation; that
is not counted as a passing launch or a measured phone performance result.

## Concrete verification gap corrected

Existing APK CI verifies signatures, ZIP integrity and hashes. It did not ask
Android PackageManager to install the distributed APK. Add a separate fresh-install
workflow on API30 and35 that reuses the artifact from the existing APK build
associated with the same PR head. The producer run ID/workflow/HEAD/conclusion
and downloaded bytes are checked; APK contents are never rewritten. The existing
producer's default PR checkout can be GitHub's synthetic merge with main; the
recorded producer metadata identifies that run rather than claiming a separately
rebuilt byte-for-byte HEAD APK.

The new workflow does not modify the publishing workflow or its signing/cache
configuration. It is a PR check for review, not yet a dependency that blocks the
existing main release-publishing step. PR #112 remains an independent artifact
retrieval change.

`tools/check_apk_installability.py`:

- Requires an explicitly named, confirmed emulator with completed boot and no
  existing project package. Physical devices and existing installs are untouched.
- Validates real APK signature/metadata using the existing audit helper.
- Installs using `adb install --no-streaming`, without the test-only bypass.
- Requires exit0, an exact Success line, no Failure marker and an installed package
  path before claiming PASS; saves installer output/package metadata or failure
  logcat and a JSON command journal.
- Never uninstalls, clears data, changes signing/identity or retries by suppressing
  a failing installation.

Ten decision tests use explicitly simulated ADB output and cover invalid-package
failures even with exit0, misleading success text, missing post-install metadata,
timeouts, physical targets, unbooted emulators and preservation of an existing app.
Those tests are not installation evidence; the API35 raw PackageManager result is.

## Commands

```sh
python3 -m unittest discover -s tools -p test_check_apk_installability.py -v
python3 tools/check_apk_installability.py \
  --apk /workspace/apk-invalid-investigation/release-392/Eternal-Summoner-debug.apk \
  --serial emulator-5580 --sdk-root /workspace/toolchains/android-sdk \
  --output /workspace/apk-invalid-investigation/install-release392-api35 --timeout 600
```

Use a fresh task emulator; the probe intentionally refuses to remove an existing
installation. Tooling SDK/AVDs live under `/workspace/apk-install-sdk` and
`/workspace/apk-invalid-investigation`, leaving the project's compile/signing
configuration untouched. Do not rerun a successful fresh-install probe on the same
AVD and call the expected BLOCKED_NOT_FRESH result an APK failure.

## Independent static audit and remaining limits

Actual read-only Codex auditor `/root/android_conflict_auditor` independently checked
the release APK. No proven package rejection cause was found: manifest flags and
resource references parse,203 compiled resource XMLs parse, all139 resource type
chunks are conventional, all18 DEX pass header/checksum/limit checks with no
duplicate class definitions, ZIP/native/resource alignment is valid. Signatures
verify for API24..36. These static results are not a physical install test.

No application, game/save/renderer/map, Gradle/Manifest, Android version, entrypoint,
signing key/configuration or existing workflow was changed. This PR supplies the
missing installation check and real evidence; it does not claim to repair an
unreproduced Samsung rejection. Exact-SHA independent review and owner-only merge
remain required. Physical installation and the failing file's identity/rejection
code remain unverified.
