# Main Tower merge repair

Task: Issue107, continuing the owner's request to apply the learned skills to a working game.
Base: `2e160f48b9903c331b3655aea4277dab765bdba8`, after external merges of PR106 and PR50.
The PR body records exact tested/reviewed HEAD and CI; this file cannot include its own hash.
No prior agent branch is rewritten, and no merge/main/force push is performed by this writer.

## Reproduced defects and correction

Main APK CI run43380823143 failed its Build APK step. Local compilation on that exact main
reproduced duplicate `tappedHero` declarations508/536 and four unresolved hero mana references
in LostCourierEncounter372. Removing only the second inspection block preserves the existing
nearer-hero/enemy arbitration and Arena's emergency hazard ground tap.

The scout reward referenced `HeroCombatUnit.mana/maxMana`, which do not exist. Heroes use
skill cooldowns. The one-shot scout shortcut now reduces cooldown by25% of maximum with a
matching Vietnamese message. It leaves manual command locks intact and clamps at zero.
No fake mana model, new serialized state or economy formula is introduced.

Independent read-only review also identified two concrete same-merge regressions:

- Courier support could reduce Kraag HP to0 without setting `dead` or clearing the target.
  A narrow engine-owned fixed-damage operation now shares the normal enemy-death cleanup.
  The authored stone remains12damage; it does not acquire random crit/counter modifiers.
- The new pinned dodge/move control bypassed global battle-input ownership and immediate
  acknowledgment. Its handler now matches the existing guarded command controls for
  intro, inactive scene, retreat confirmation and settlement.

The first broader actual-route run also caught a newly merged pacing regression: the
southern-medicine/forced-east supplies route failed because the courier accelerated while
the party was diverted to supplies. Brisk66px/s now requires no aggroed threats, nearby
protection and PROTECT_ALLY, outside supply navigation. Unprotected/manual diversion keeps
the original44px/s; no added timer, HP inflation or hidden teleport. Both the dedicated
brisk/diversion fixture and untouched full supply route are required to pass.

Merged Arena tests called nonexistent mana/feedback-history APIs and private setters
(`eliteTelegraph`, `activeOrder`). They now use the current cooldown/feedback APIs and
public command entry point without widening production visibility. Gate fixtures isolate
Kraag from unrelated scouts, arrange the real attack range and respect the .22s contact
marker. These fixtures are explicitly distinct from untouched full starter-party routes.
The final support-hit regression checks death/target cleanup then remaining extraction.

## Scope and verification

Only TowerCombatEngine, LostCourierEncounter, TowerBattleScreen, the merged Arena reforge
test, a new pinned-input UI test, existing CI test selection and this evidence change.
Sanctuary/Master HUD, shared renderer/shaders/camera, existing sprites, Android identity/
signing/configuration, save schema/economy and PR96 files remain untouched.
Preserved Arena features include brisk safe-road courier travel, ground-touch dodge,
Kraag counter window/feedback, courier support, equipment comparison/direct equip and
triage guidance. PR106's field supplies/ordinary settlement/retry remain intact.

Existing JDK21/Gradle9.3.1/SDK36/36.1 are used. A temporary Sign-In resource matches the
existing CI setup and is excluded from commit; no Android configuration/key changes.
Initial focused repaired fixtures/control/full-route tests passed; broader final suite,
APK and actual test counts are recorded in `local-verification.json` and on the PR.
CI now explicitly selects the reforge and pinned-input cases, instead of merely compiling
the merged tests while never executing their behavioral assertions.

UI evidence uses actual production Compose/Robolectric SDK36 native graphics, not a phone.
Pinned control is disabled during intro/inactive scene/retreat, acknowledges a live
command before the250ms HUD poll, and re-enables correctly after the modal closes.
Complete actual starter-party route tests continue to verify physical rescue/extraction,
field-reward consumption, persistent HP, restart and duplicate settlement.

No physical device is attached/verified here. Phone installation, latency, GPU/FPS/thermal
and human replay/fun acceptance remain separate from compilation and mechanical tests.
Independent exact-SHA Codex review plus Argon/A09 review are requested; no writer self-approval.
