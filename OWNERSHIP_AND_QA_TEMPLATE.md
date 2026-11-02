# Mission ticket + integration handoff template

## Branch / baseline
- base SHA:
- active environment-art PR status:
- working tree clean? (yes/no; if no, never overwrite):
- rollback plan:

## Agent assignments
| Agent | Purpose | Allowed file paths | Forbidden/shared paths | Expected API/interface | Tests | Status |
| --- | --- | --- | --- | --- | --- | --- |
| gameplay_architect | combat / tactical commands | DISCOVER FIRST | pixel renderer, sanctuary art, map catalog unless approved | TBD | TBD | audit |
| tower_encounter_builder | rescue mission data / persistence | DISCOVER FIRST | overlapping gameplay files, shared catalog unless approved | TBD | TBD | audit |
| android_performance_auditor | read-only benchmarks | NONE | all writable paths | report | static/probe audit | audit |
| integration_reviewer | read-only final gate | NONE | all writable paths | blockers | diff review | deferred |

## Acceptance checklist
- [ ] Guest → Sanctuary → Tower1 entrance works
- [ ] No map placement/collision regression
- [ ] Gameplay objectives genuinely update
- [ ] Three Master command effects tested (focus, regroup, retreat)
- [ ] Distinct enemy behaviors tested
- [ ] Rescue success + partial/failure with retry tested
- [ ] Save/load persistent reward or story consequence tested
- [ ] Existing Hero/character animations intact
- [ ] Android assembleDebug + targeted tests results recorded
- [ ] Real engine render viewed; no invisible/missing assets
- [ ] Device FPS, p95, thermal either measured with method or explicitly NOT TESTED
- [ ] PR only, no automatic merge

## Evidence / known blockers
Record URL and commit for every CI result, exact root paths for test artifacts, results and defects. No invented success flags.
