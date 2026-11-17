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

The exact release APK also passed a fresh PackageManager install on Android11/API30; raw evidence is in `api30/`. An earlier probe found the local emulator unavailable after a sibling VM shutdown; that was not an APK rejection.

The owner's reported rejection remains **UNRESOLVED**. These successful emulator installs narrow the investigation; they do not establish which installer condition produced the owner's error or prove it fixed. Exact-HEAD CI is reported separately in the Issue/PR handoff as it completes. An exploratory `am start -W` timed out under software emulation; that
is not counted as a passing launch or a measured phone performance result. The
later log shows a drawn activity after44seconds and an input ANR on this software
emulator. Both observations remain recorded limitations, not a successful launch
test or an explanation for the owner's installation rejection.

## Concrete verification gap corrected

Existing APK CI verifies signatures, ZIP integrity and hashes. It did not ask
Android PackageManager to install the distributed APK. Add a separate fresh-install
workflow on API30 and35 that reuses the artifact from the existing APK build
associated with the same PR head. The producer run ID/workflow/HEAD/conclusion
and downloaded bytes are checked; APK contents are never rewritten. The existing
producer's default PR checkout can be GitHub's synthetic merge with main; the
recorded producer metadata identifies that run rather than claiming a separately
rebuilt byte-for-byte HEAD APK.

The matrix checks both original and compatibility signing profiles on separate
fresh API30/API35 emulators. A missing/failed producer artifact blocks the test;
only diagnostic upload is optional. Compatibility APK upload requires successful
signing/payload checks and actual installation in that matrix job.

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

Eleven decision tests use explicitly simulated ADB output and cover invalid-package
failures even with exit0, misleading success text, missing post-install metadata,
timeouts, physical targets, unbooted emulators and preservation of an existing app.
Those tests are not installation evidence; the API35 raw PackageManager result is.

## Controlled signing-envelope compatibility experiment

The owner confirms downloading the exact provided APK link. AOSP installation does
not explain the Samsung rejection. A separately named `Eternal-Summoner-compat.apk`
therefore tests one variable: add JAR/v1 alongside v2, with the same existing
certificate. Absence of v1 is a **hypothesis**, not a proved cause or a fixed bug.
The original APK/release and application signing configuration remain untouched.

`tools/create_apk_compat.py` uses the existing debug key, never generates one and
refuses a missing key, an unexpected source certificate or a changed candidate
identity. CI restores the original cache without saving/replacing it and pins
the expected canonical9d17... release certificate. No keystore enters artifacts.
On failure a newly created candidate is removed, not distributed.

Checks compare every non-signature ZIP entry's bytes and compression and record
ordering/offset differences. Signing necessarily changes the ZIP/signing envelope;
this is not a byte-identical whole APK. Manifest/dex/resources/native code remain
byte-identical, preserving package, min/targetSDK, versions and launcher. Final
signed bytes pass16KiB ZIP alignment, normal v2 verification for API24..36 and
separate JAR/v1 verification using verifier API23 bounds. The latter only selects
a verifier code path; the app's minimum remains24. Android24+ still verifies v2.
The verifier's v1-only META-INF warnings do not authorize deleting those payloads;
they are retained and protected by v2.

Eight ZIP-payload/metadata fixture tests reject changed/deleted/extra/duplicated code or
metadata and altered compression. They are not cryptographic signing evidence.
Separately, real local SDK signing verified a local-keyb6ae... source/candidate
pair and preserved all1,082 non-signature entries and their compression/order;
1,054 entry offsets changed while final alignment passed. That local candidate
is **not** a user update for the canonical9d17... channel. A real wrong-key test
against release392 is also recorded: the helper rejects the mismatch and removes
its output. The actual downloadable CI candidate must independently match the
canonical release key, pass fresh installs and receive exact-SHA review.

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

## First CI result and verification-tool correction

On d5e5fc3, actual CI fresh installs of the original APK passed on API30 and35.
Both compatibility jobs failed closed before creating a candidate: the selected
runner verifier returned no fingerprint to the existing metadata parser. This was
not a changed-key finding: the recorded APK SHA was exactly the original release
SHA8417389a..., which the local SDK36.0.0 verified as the canonical9d17... signer.
No compatibility APK was uploaded from those failed jobs.

The probe and compatibility helper now explicitly select the producer's pinned
SDK build-tools36.0.0 rather than accepting an executable from PATH or a different
SDK version. CI installs that verification tool version and records the prior
verifier's public output for diagnosis. Missing signer metadata is classified as
a metadata/tool error before installation/signing, not proof of key rotation.
Two additional tests pin SDK selection and missing-metadata refusal;19 total
fixture tests pass. These changes affect verification tools only, not application
min/targetSDK or signing configuration. Final CI and canonical candidate results
are reported on the PR/issue for the reviewed SHA.
