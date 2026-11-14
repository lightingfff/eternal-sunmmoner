# Optional APK artifact retrieval repair

Issue111; base main `380e4e3ba4840e673e54966715ff12a49cbdfab4`.
Owner's run43488010036 built/signed/packaged/uploaded successfully, then failed the
`gh api .../actions/artifacts/14067424689/zip` retrieval with HTTP404. Live metadata
later shows that same ID exists and is unexpired. This does not establish whether the
original404 was consistency, access, service or another retrieval problem; it was not
a Kotlin/APK compilation failure. `actions: read` is already present and unchanged.

## Correct behavior

Keep the existing policy: Actions artifacts are an optional mirror; main releases use
the already verified files on the build runner. Retry eligible retrieval failures up
to3attempts, default2/4second backoff,30second request timeout. Stop immediately on429
rather than add rate-limit pressure. Exhausted404/known network/server unavailability
emits an explicit warning, `UNAVAILABLE` JSON/step output and job summary. It does not
claim remote verification passed. Main's release step can continue; PRs never release.

401/403, auth exit4, unknown CLI/configuration errors and invalid arguments remain fatal.
Any successfully fetched invalid archive, missing/duplicate/unexpected path, special
file type, bad CRC/size/hash/local-byte match or APK signature also remains fatal.
No blanket continue-on-error is added. Never print potentially sensitive command stderr.

The downloaded APK, portableZIP and checksum manifest must match the exact local runner
distribution, rather than only trusting a checksum shipped beside unknown remote bytes.
Validate the local reference before contacting GitHub; extract only the exact regular
three-file set into a temporary directory and clean it on exit. Reuse the existing
package verifier, then verify the downloaded APK using the real apksigner. Retry files
are truncated and removed, so partial/error bodies cannot masquerade as successful ZIPs.

## Actual verification

```sh
python3 -m unittest discover -s tools -p 'test_verify_uploaded_apk_artifact.py' -v
```

19offline tests pass: successful verification;404then success; exhausted404;401/403;
alternate CLI status format/no secret logging; timeout/server/known-network recovery;
exhaustedserver;429; malformed/HTMLZIP; missingmanifest; modifiedbytes; unexpected/traversal
paths; outerCRCcorruption; specialfilemodes; signerfailure; badlocalreference; invalidIDs/
repository; unknownconfiguration/auth-exit failures. Transport/signature subprocesses
are explicitly stubbed for these ZIP fixtures; they are not signed Android test binaries.
The workflow executes these tests before building and retains strict integrity failure.

Separately downloaded the actual originally failing artifact as a reference and ran
the helper against that same liveID using the available authenticated GitHub connection
and real Android SDK apksigner. See `live-verification.json` for actual result/attempts.
This local connection is distinct from GITHUB_TOKEN; exact-SHA runner behavior must be
read from the new PR's actual CI, not inferred from this successful authenticated check.

Independent read-only pre-review found two hardening gaps: unknown nonHTTP CLI failures
were too broad, and special Unix file modes were accepted. Both were corrected and
covered before commit. Final exact-SHA review/CI status are recorded on the PR.

No Android application ID/manifest/Gradle/signing/entry point, app code, game assets,
save or gameplay changed. No manual rerun/merge/mainpush/forcepush/release publication.
This changes CI retrieval failure handling, not device installation compatibility or
game performance. No physical-device claims. Owner alone merges after independent review.
