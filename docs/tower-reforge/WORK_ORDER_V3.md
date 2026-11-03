# Tower Reforge V3 — single agent

Baseline: PR28 branch mission01/floor1-rescue at ba7677a; gameplay at 01e98ce; main f0dc0de. Remote refs verified this session; REST API forbidden. User clarified “cả hai”: implement V3, then merge when validated. No subagents, force push or direct main push.

C0 first: integrate main, preserve both diagnostic histories, remove obsolete Android SDK setup dependency, retain P25 validator and run baseline APK, rescue, save/retry/UI, native render and asset gates sequentially. One writer owns workflow/diagnostic/report changes. Upstream P23–P26 code/assets are integrated without redesign. Protect camera .72, Sanctuary, guest/save, PR33/P27 scopes.

Only after C0 is validated: C1 telemetry/route compression; C2 agency/monster roles; C3 active skills/ultimate; C4 encounter/map and honest A/B/C simulation; C5 native UI/render/regressions. Changes must produce real command effects, four consequential decisions, distinct outcomes, no filler; human duration and physical-device performance remain unvalidated without measurements. City follows accepted Floor1. Re-run affected checks after fixes, at most three correction cycles per issue.

Baseline quality is FAIL: 317–318 sim seconds, 221 courier travel, ~12 chapter combat seconds; this is not 5–10 minutes of meaningful play. Do not claim completion/merge readiness from build alone. Record exact commands/results in MISSION01D_REPORT_VI.md. Git commits preserve rollback points; do not discard user work.
