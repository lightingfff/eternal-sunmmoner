# P1.5A verification note

Scope: original Kaelen (Knight), existing FIELD_GOBLIN_RAIDER, existing NW smithy.
Atlas PNGs, frame durations, directions, pivots, hashes and provenance live in
`app/src/main/assets/premium_slice/manifest.json`. Authoring uses offline Pillow;
there is no runtime art generator or network dependency. The optional world-graphics/
Scenario skill was unavailable; the existing Canvas pipeline was used.

Preservation baseline: commit `5c27f4a`, world 3200 × 2760, margin 120;
armory (995,655), hero quarters (825,790), restaurant (1105,925).
ObliqueCamera ground Y remains 0.72; upright height and nearest-neighbor filtering remain.
No map/catalog/collision, HeroRecord, save schema, resource costs or character IDs changed.
Kaelen's 64 × 96 frames use pivot (32,90), scale 1.08, approximately 92 world-unit
visible height, close to the old 50px × 1.85 sprite. Other Knights retain their art/attacks.
The Knight shockwave retains its shield, damage, range and cooldown; damage is now
delivered at its 0.30s release. Basic hero/raider contact is 0.22s. Retreat cancels windup.

Native Android Canvas evidence (Robolectric, **not device screenshots or recordings**):
`visual-evidence/p15a/{before,after}/` contains matched noon/clear scenes with one
hero, cameras (1070,800,1), (950,1000,1.8), (995,560,1.3), (1530,1220,0.45).
GIFs show renderer frames from real simulation steps; `combat_events.csv` records
HP and shield changes at contact. Idle, walk and defeat also use real engine steps.
The canopy capture checks partial occlusion. Before images come from the untouched
baseline commit in an isolated worktree, with the same capture harness and coordinates.

Source evidence of bottlenecks: first access eagerly constructed every legacy actor
sheet; the lobby rebuilt/sorted wrappers every frame; heroActors concatenated lists
on every read; shadow traversal resolved offscreen sprites. These now use per-key
loading, a reusable depth merge, scene/roster caches and conservative pre-load culling.
The full premium atlas source is temporary; extracted frames and silhouettes are cached.
Tests cover cache identity, moving-actor order, scene invalidation and offscreen loads.
No physical-device FPS/frame-time/jank/memory/GC baseline or improvement was measured:
`adb devices -l` returned no devices; emulator/system images and KVM were unavailable.

Build/check: source `/workspace/environment-setup/activate.sh`, then
`gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest` with filters for
PremiumSliceCombatTest, PremiumSliceEvidenceTest, SanctuaryEnvironmentArtTest,
LivingWorldTest, GuestSavePersistenceRobolectricTest and LayoutResponsivenessRobolectricTest.
Collect: `python3 tools/collect_p15a_evidence.py --baseline /path/to/baseline/captures`.
On a phone, install both commits in turn, use the same save, roster, noon/clear state,
camera and 60s pan/zoom route; reset/read `adb shell dumpsys gfxinfo
com.aistudio.eternalsummoner.kvrpzm` and `dumpsys meminfo` each time. Use Perfetto
for long frames/GC. No performance target is marked achieved without that comparison.

Art acceptance is partial: original detailed sprites and smithy are integrated, but
commercial-reference craft quality, physical-phone readability/performance and audio
are not established. Next task: profile this exact slice on a real Android phone.
