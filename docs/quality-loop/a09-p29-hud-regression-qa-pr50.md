# A09 Independent Regression QA — PR #50 (P29 HUD / Master Hub)

- **Task**: ES-A09-001
- **Reviewer**: A09 Independent QA
- **PR reviewed**: https://github.com/lightingfff/eternal-sunmmoner/pull/50
- **Head SHA reviewed**: `8cc082fbbee5938dbfcfe159d2d2bba0d1645b1c`
- **Merge-base with main**: `f3553613df914f495b7f58de5849614204f21f41`
- **Main tip at review time**: `bdff6f9976dc4927e1e095a503bc339509c7a9aa`
- **Review date**: 2026-11-08
- **Method**: Read-only. Local `git diff` against merge-base; `gh` for PR/CI state; `python3` (PIL/numpy) for contrast math. **No Gradle/Robolectric run locally** (no JDK/Android SDK in sandbox) — CI is the only build/test authority.
- **Verdict**: **NOT READY FOR MERGE — R1 BLOCKER.**

---

## 1. Scope of review (per issue #60)

Verify PR #50's R1–R4 fixes against exact HEAD:
- source + tests,
- screenshots / visual evidence (criterion: no changes to old PNGs),
- severity-graded findings,
- no merge, no edits to PR #50 source.

PR #50 diff vs merge-base `f355361`: **16 files, +71/−5**. Source/test diff matches A01's audit in issue #52 (R1–R4 = 4 fixes + 2 new tests + 1 fixture change). The remaining 10 files are regenerated evidence PNGs (see §6).

---

## 2. Findings (severity-graded)

### R1 — Back does not dismiss Master Hub modal — **BLOCKER (P0)**

**Finding**: The new test `masterModalDismissedByAndroidBack` **FAILED on CI** at SHA `e43dea2` (P27B run 42450674309: 33 tests, 1 failed, 1 skipped; `master_hub_screen` still exists after back press). The PR's only commit after `e43dea2` is `8cc082f` ("P17-C: engine render evidence", automated) which touches **zero source/test files**.

**Evidence**:
- `git diff --stat e43dea2 refs/remotes/pr50` → only 10 PNG files, 0 insertions/0 deletions in code.
- `git diff e43dea2 refs/remotes/pr50 -- app/src/main/** app/src/test/**` → **empty**.
- `MasterHubScreen.kt` line 61 `androidx.activity.compose.BackHandler(onBack = onClose)` identical on `e43dea2` and head.
- Test file `P29HubHudTest.kt` byte-identical on `e43dea2` and head.

**Conclusion**: The R1 behavioral bug is **NOT fixed** on head `8cc082f`. The test that detects it is unchanged and still fails by deterministic identity of source. The PR added a test that exposes the bug, then did not fix the bug.

**Root-cause note (for the fix writer)**: Two `BackHandler`s are active when the modal is shown — `MasterHubScreen`'s innermost `BackHandler(onBack = onClose)` (enabled) and `SanctuaryLobbyScreen`'s `BackHandler(enabled = showMasterHub || …)` with branch `showMasterHub -> showMasterHub = false`. The legacy `androidx.activity.compose.BackHandler` registers an `OnBackPressedDispatcher` callback; in the Robolectric `runOnIdle { owner.onBackPressedDispatcher.onBackPressed() }` flow the callback is not being invoked (or `onClose` is not recomposed to `showMasterHub = false` before the assertion). Requires a real fix + re-run, not a comment.

**Required fix**: Make `masterModalDismissedByAndroidBack` pass on a fresh CI run, OR prove the failure was a flake with a repeat run. Current evidence says it is not fixed.

### R4 — HudGlass text contrast — **PASS (measured), with 4 notes**

**Measured contrast** (python3/PIL, WCAG relative luminance, alpha-blend over worst-case backdrops):

| Token | Backdrop | Contrast | ≥4.5:1? |
|---|---|---|---|
| `0xFFFFD700` (SummonerGold / dock Gold) | HudGlass(0xCC) over pure black | 12.82:1 | ✅ |
| `0xFFFFD700` | over dark amber (61,37,15) | 11.77:1 | ✅ |
| `0xFFFFD700` | over pure white | 6.37:1 | ✅ |
| `0xFFFFFF` (white text) | HudGlass(0xCC) over pure black | 17.98:1 | ✅ |
| dock labels, amber tier (`0xFFDDA15E`) | over dark amber | 6.35:1 | ✅ |
| dock labels, amber tier (`0xFFC9A227`) | over dark amber | 5.91:1 | ✅ |

All worst-case values pass 4.5:1. **R4 behavior is satisfied.**

**Notes (all LOW, test/comment hygiene — not blockers)**:
1. **Token-identity gap**: the committed test asserts `FantasySkin.SummonerGold` (0xFFFFD700) on `HudGlass`, but the actual navigation-dock gold is `FantasyManagement.Gold = Color(0xFFFFD700)` — a **different token with the same value**. `FantasySkin.Gold` remains `0xffc6b27f`. The test would not catch a future regression in the dock token.
2. **Comment drift**: test comment says `HudGlass (0xB3141D2A)` but committed value is `0xCC141D2A`.
3. **Glass opacity drift (MEDIUM)**: PR raised `HudGlass` alpha `0xB3` (70%) → `0xCC` (80%), contradicting the P29 comment "70% glass, let the world read through". The comment was not updated. This is more-opaque glass, an aesthetic drift from the documented P29 intent.
4. **Worst-case gap**: the test's "worst-case" backdrops are dark, but for translucent glass the true worst case for gold text is a **bright** backdrop (white). Measured white = 6.37:1 (still passes), but the test does not cover it.

### R2 — HUD fixture missing prologue flag — **PASS**

`sampleState()` now includes `storyFlags = listOf(ManhwaStoryData.FLAG_PROLOGUE_SEEN)`. Low-risk fixture addition; no behavior change.

### R3 — Collapsed nav lost a11y labels — **PASS (minimal test)**

Fix `contentDescription = if (!expanded) title else null` is correct. Test `collapsedNavHasAccessibleDestinationNames` covers only `nav_heroes` (single node) — adequate for the fix but not per-destination. Matches Codex R3 ask loosely.

---

## 3. CI state on head `8cc082f`

| Run | Workflow | Status | Conclusion |
|---|---|---|---|
| 42453097349 | Build Eternal Summoner APK | completed | **action_required** |
| 42453097332 | P27B matched Sanctuary render evidence | completed | **action_required** |
| 42453097314 | Lyra engine render evidence | completed | **action_required** |

**No green/red verdict on head.** All three runs require workflow approval (`ACTION_REQUIRED`) and were not executed. The only failure evidence (`masterModalDismissedByAndroidBack`) is on superseded SHA `e43dea2`.

---

## 4. PR staleness

PR #50 is **stale vs current main** `bdff6f9`: it is missing 832 lines of `docs/agents/**` added by PR #65. It is `MERGEABLE` (no conflicts) and not a draft. 12 commits, P29 fixes interleaved with automated P17-C evidence commits.

---

## 5. Visual evidence (issue #60 criterion: no changes to old PNGs)

- **P29 evidence PNGs unchanged**: `docs/visual-evidence/p29/hub/*.png` identical to main; last commit `a10cb75`. **Criterion satisfied for the P29 set.**
- The 10 churned PNGs are `p23/p26/p26c` engine-render evidence, re-landed by the automated "P17-C: engine render evidence (Robolectric native graphics)" bot commits. These are not part of the P29 review set and are expected churn from the bot.

---

## 6. Summary table

| ID | Area | Severity | Status |
|---|---|---|---|
| R1 | Back dismisses Master modal | **BLOCKER (P0)** | NOT FIXED — test fails, source unchanged since failure |
| R4 | HudGlass contrast | PASS | measured ≥4.5:1 all worst cases |
| R4 | Token identity | LOW | test measures SummonerGold, dock uses FantasyManagement.Gold |
| R4 | Comment drift | LOW | test comment 0xB3 vs committed 0xCC |
| R4 | Glass opacity drift | MEDIUM | 0xB3→0xCC contradicts "70% glass" comment |
| R4 | Worst-case gap | LOW | test omits bright backdrop (white) |
| R2 | prologue flag fixture | PASS | low-risk fixture add |
| R3 | collapsed nav a11y | PASS | fix correct, test minimal |
| Meta | CI verdict on head | BLOCKER (process) | all 3 runs ACTION_REQUIRED, no verdict |
| Meta | PR staleness | MEDIUM | missing #65 docs/agents/** |

---

## 7. Recommendation

**Do not merge PR #50 at head `8cc082f`.** R1 is a live behavioral bug with a failing test and unchanged source since the failure. Request a `[FIX_PLAN]` from the coordinator: one corrective writer for the R1 source (`SanctuaryLobbyScreen.kt` / `MasterHubScreen.kt`), re-run `masterModalDismissedByAndroidBack` on a fresh CI SHA, then repeat A09 + Codex + Argon review on the new SHA. R4 notes can be folded into the same fix round as comment/token hygiene.
