# P30 / Issue #43

Baseline main d9c330fe768a789e57ad634dc597ae1256e663fd. Owner authorized #43 in chat and issue. PR42 a10cb75 owns Sanctuary screen/components/pixel renderer; none will be changed. PR28 09c553d remains independent (open/dirty), no merging its work. Single agent.

Trace input -> engine order -> next fixed AI tick -> contact cancellation/movement -> Compose acknowledgement. First reproduce REGROUP during basic/skill contact before/after hit, ordinary/premium and emergency-healer priority. Check all other orders keep their semantics. Add real touch tests at 800x360 and 360x640, freeze scene simulation to distinguish event acknowledgement from the 250ms periodic HUD sampler, verify UI vs uncovered-world taps and preserve pan/pinch/skills. Screenshots use actual native Compose, not device evidence.

Allowed: battle screen, AI and targeted tests/docs; no Sanctuary renderer/shader/art/HUD, map, save or mission changes. Fix only demonstrated causes. Build/compile and selected regressions sequentially. Publish separate PR referencing #43, no merge/force push. Physical latency/FPS unmeasured without a device. #46 gets a source audit/contract proposal only until #43 is delivered; implementation, if undertaken, has its own branch/PR.
