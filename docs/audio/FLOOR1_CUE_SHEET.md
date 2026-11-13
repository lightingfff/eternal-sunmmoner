# Floor 1 — Verdant Frontier Cue Sheet

**TASK:** ES-A12-001  
**ROLE:** A12 Audio & SFX Designer  
**FLOOR:** Floor 01 — "Cánh Đồng Khởi Nguyên" (Verdant Frontier)  
**BIOME:** `FIRST_FIELD_MEADOW`  
**ENEMIES:** Goblin Trinh Sát, Goblin Xạ Tiễn, Sói Hoang Gió Lốc, Thủ Lĩnh Kraag (Boss)  
**DURATION TARGET:** 5-10 minutes gameplay (per AGENTS.md design goal)

---

## Timeline Overview

```
[FLOOR ENTRY] ──► [SCOUTING PHASE] ──► [COURIER ENCOUNTER*] ──► [EVENT WAVE TRIGGER] ──► [BOSS FIGHT] ──► [VICTORY/DEFEAT] ──► [RESULTS]
     │                    │                       │                         │                      │                 │
   BGM: Gate         BGM: Meadow            BGM: Tension              BGM: Boss Stinger      BGM: Boss Loop    BGM: Victory
   SFX: Transition   SFX: Ambient           SFX: Courier cues         SFX: War Horn          SFX: Combat       SFX: Result
                                        (*optional Lost Courier encounter)
```

---

## Cue Sheet — Chronological Order

### Phase 0: Floor Entry & Intro Dialogue

| # | Cue ID | Trigger | Description | Duration | Loop | Priority |
|---|---|---|---|---|---|---|
| 01 | `BGM_FLOOR_ENTRY` | `MissionStage.INTRO_DIALOGUE` start | Short orchestral sting — "entering the frontier" mood. Dark horn + soft strings. | 4-6s | No | HIGH |
| 02 | `SFX_TOWER_DESCENT` | Tower Gate portal activation | Stone grinding + magical whoosh descending pitch. | 1.5s | No | HIGH |
| 03 | `SFX_INTRO_DIALOGUE_BG` | Dialogue overlay begins | Very soft ambient bed — wind + distant birds. Fades under dialogue text. | Loop | Yes | LOW |

### Phase 1: Scouting / Main Combat Loop

| # | Cue ID | Trigger | Description | Duration | Loop | Priority |
|---|---|---|---|---|---|---|
| 04 | `BGM_MEADOW_COMBAT` | `MissionStage.IN_PROGRESS` | Mid-tempo dark-fantasy battle loop. Bodhrán drum pulse + low strings + occasional horn call. Loop point must be seamless. | 90-120s | Yes (seamless) | HIGH |
| 05 | `AMB_MEADOW_WIND` | Floor ambient layer start | Gentle wind through tall grass, occasional bird calls. Low volume bed under BGM. | 60s | Yes (seamless) | MEDIUM |
| 06 | `SFX_GRASS_RUSTLE` | Master/hero movement on meadow tiles | Soft grass footstep variation. Random-pitch (±5%) to avoid mechanical repetition. | 0.3s | No (per-step) | LOW |
| 07 | `SFX_ENEMY_AGGRO_GOBLIN` | `FIELD_GOBLIN_RAIDER`/`FIELD_GOBLIN_ARCHER` enters aggro range (340f/370f) | Short guttural cry + weapon clang. Volume scales with camera distance. | 0.8s | No | MEDIUM |
| 08 | `SFX_ENEMY_AGGRO_WOLF` | `DIRE_WOLF` enters aggro range (380f) | Wolf howl — medium length, chilling. Volume scales with camera distance. | 1.2s | No | MEDIUM |
| 09 | `SFX_MELEE_HIT_LIGHT` | `ContactAttackTimeline.advance()` returns hit=true (normal attack, non-skill) | Quick metallic impact — blade on flesh/armor. | 0.3s | No | HIGH |
| 10 | `SFX_MELEE_HIT_HEAVY` | Contact hit on boss enemy (Kraag) | Deeper, resonant impact with slight reverb. | 0.5s | No | HIGH |
| 11 | `SFX_ARROW_RELEASE` | `FIELD_GOBLIN_ARCHER` ranged attack | Bow twang + arrow whistle (pitch Doppler if possible based on direction). | 0.6s | No | HIGH |
| 12 | `SFX_WOLF_BITE` | `DIRE_WOLF` melee contact | Animalistic snap/bite — wet, aggressive. | 0.3s | No | HIGH |
| 13 | `SFX_ENEMY_DEATH_GOBLIN` | Enemy HP reaches 0 (goblin types) | Collapse thud + weapon clatter (randomize: dagger/spear). | 0.8s | No | MEDIUM |
| 14 | `SFX_ENEMY_DEATH_WOLF` | `DIRE_WOLF` HP reaches 0 | Whimper + body thud. | 0.7s | No | MEDIUM |
| 15 | `SFX_HERO_HIT` | `applyDamageToHero()` — hero takes damage | Impact grunt + armor clang. Variation based on hero class (optional: per-class voice). | 0.4s | No | HIGH |
| 16 | `SFX_MASTER_HIT` | `applyDamageToMaster()` — master takes damage | Concerning impact — more urgent than hero hit. Slight screen shake audio (low rumble). | 0.5s | No | HIGH |
| 17 | `SFX_HERO_SKILL` | `triggerHeroManualSkill()` activated | Powerful magical/physical skill SFX — energy charge + release. Variation per skill type. | 0.8s | No | HIGH |

### Phase 2: Tactical Orders & UI

| # | Cue ID | Trigger | Description | Duration | Loop | Priority |
|---|---|---|---|---|---|---|
| 18 | `SFX_ORDER_FOLLOW` | `issueTacticalOrder(FOLLOW)` | Soft confirmation chime — "acknowledged." | 0.3s | No | LOW |
| 19 | `SFX_ORDER_AGGRESSIVE` | `issueTacticalOrder(AGGRESSIVE)` | Stronger chime — assertive tone. | 0.3s | No | LOW |
| 20 | `SFX_ORDER_DEFENSIVE` | `issueTacticalOrder(DEFENSIVE)` | Defensive chime — shield/bracing sound. | 0.3s | No | LOW |
| 21 | `SFX_RETREAT_HORN` | `confirmTacticalRetreat()` | War horn — descending notes, somber. Signals retreat. | 1.5s | No | HIGH |
| 22 | `SFX_DISCOVERY_CHIME` | `claimDiscovery()` — new node found | Positive crystal chime + sparkle. | 0.6s | No | MEDIUM |
| 23 | `SFX_GOLD_PICKUP` | `bonusDiscoveryGold` increments | Coin clink — bright, satisfying. | 0.3s | No | LOW |
| 24 | `SFX_UI_TAP` | Facility/hero tap in lobby, tactical button press | Soft tap — not intrusive. | 0.15s | No | LOW |

### Phase 3: Lost Courier Encounter (Optional)

| # | Cue ID | Trigger | Description | Duration | Loop | Priority |
|---|---|---|---|---|---|---|
| 25 | `SFX_COURIER_ENCOUNTER_START` | `LostCourierEncounter` initiated, `CourierPhase.CLEAR_PATH` | Urgent notification tone — "new objective" alert. | 1.0s | No | MEDIUM |
| 26 | `SFX_COURIER_ESCORT_BEGIN` | Phase → `ESCORT` | Escort music bed starts — steady pace, tension building. | Loop | Yes | MEDIUM |
| 27 | `SFX_TELEGRAPH_WARNING` | `CourierTelegraph.remainingSec` ≤ 1.5s | Pulsing danger tone — frequency increases as timer decreases. | 1.5s | No | HIGH |
| 28 | `SFX_SCOUT_ALARM` | Phase → `SCOUT_ALARM` | Alarm horn — short, sharp alert. | 0.8s | No | HIGH |
| 29 | `SFX_COURIER_RESCUED` | Phase → `RESCUED` | Relief jingle — warm strings + positive chime. | 1.5s | No | HIGH |
| 30 | `SFX_COURIER_FAILED` | Phase → `FAILED` | Failure tone — low, somber. Not punishing. | 1.0s | No | MEDIUM |

### Phase 4: Event Wave / Boss Trigger

| # | Cue ID | Trigger | Description | Duration | Loop | Priority |
|---|---|---|---|---|---|---|
| 31 | `SFX_WAR_HORN_ALARM` | `eventWaveTriggeredForCourier()` — mid-mission event | Long war horn — dramatic, echoing. Corresponds to event banner: *"TIẾNG TÙ VÀ BÁO ĐỘNG VANG VỌNG BIÊN GIỚI!"* | 3.0s | No | HIGH |
| 32 | `BGM_BOSS_STINGER` | Boss wave spawns (`GOBLIN_WAR_CAPTAIN` at 2720f, 1070f) | 4-bar dramatic sting — heavy brass + war drums. Transitions into boss combat BGM. | 8-12s | No (transitions to #33) | HIGH |
| 33 | `BGM_BOSS_COMBAT` | Boss fight active (Kraag alive + aggressive) | Intense combat loop — faster tempo, heavier percussion, dark brass motif. Must feel distinctly more dangerous than #04. | 90-120s | Yes (seamless) | HIGH |
| 34 | `SFX_BOSS_SPAWN_ROAR` | Kraag appears on battlefield | Deep roar + ground stomp impact. | 2.0s | No | HIGH |
| 35 | `SFX_BOSS_CLEAVE` | Kraag melee attack (`GOBLIN_WAR_CAPTAIN`, `attackRange = 48f`) | Heavy cleave — deep metallic arc with impact. Larger than normal melee. | 0.6s | No | HIGH |
| 36 | `SFX_BOSS_DEATH` | Kraag HP reaches 0 | Defeated bellow + armor crash + dramatic pause. | 2.0s | No | HIGH |

### Phase 5: Victory / Defeat

| # | Cue ID | Trigger | Description | Duration | Loop | Priority |
|---|---|---|---|---|---|---|
| 37 | `BGM_VICTORY` | `MissionStage.VICTORY` | Triumphant short piece — brass fanfare + strings. 8-12 bars. | 10-15s | No | HIGH |
| 38 | `BGM_DEFEAT` | `MissionStage.DEFEAT` | Somber piece — low strings + slow fade. Not punitive feeling. | 8-10s | No | HIGH |
| 39 | `SFX_VICTORY_CHIME` | Victory screen shows | Final ascending chime at fanfare end. | 0.5s | No | HIGH |
| 40 | `SFX_RETURN_TO_SANCTUARY` | Player exits results, returns to lobby | Ascending magical transition — reverse of #02. | 2.0s | No | MEDIUM |

---

## Summary Statistics

| Category | Count | Total Duration (est.) |
|---|---|---|
| BGM tracks | 5 | ~6-8 min (looping) |
| Ambient loops | 1 | ~1 min (looping) |
| Combat SFX | 12 | ~5.5s (one-shot) |
| UI SFX | 7 | ~2.0s (one-shot) |
| Event SFX | 10 | ~12.5s (one-shot) |
| Boss SFX | 6 | ~7.6s (one-shot) |
| **Total unique cues** | **41** | — |
| **Estimated asset size** | — | **~6-8 MB** |

---

## Implementation Priority (Phased Rollout)

### Phase A — Core Combat Feel (MVP)
- Cues #04, #09, #11, #12, #15, #16, #17, #37, #38
- **Rationale:** Player must *feel* combat. Hit feedback + BGM + victory/defeat are non-negotiable.
- **Asset count:** 3 BGM + 6 SFX = 9 files, ~3 MB

### Phase B — Environment & Atmosphere
- Cues #05, #06, #07, #08, #13, #14, #22, #23
- **Rationale:** Grounds the player in the meadow biome. Distinguishes enemy types audibly.
- **Asset count:** 1 ambient + 7 SFX = 8 files, ~2 MB

### Phase C — Events & Boss
- Cues #31, #32, #33, #34, #35, #36
- **Rationale:** Boss encounter needs drama. War horn + boss BGM loop + boss-specific SFX.
- **Asset count:** 1 BGM + 5 SFX = 6 files, ~1.5 MB

### Phase D — Courier & Polish
- Cues #25-30, #01-03, #18-21, #24, #39-40
- **Rationale:** Complete experience. Optional courier audio, UI polish, transitions.
- **Asset count:** 18 files, ~1.5 MB

---

## Audio Mix Levels (Proposed Defaults)

| Channel | Default Volume | Notes |
|---|---|---|
| Master | 1.0 | User-controllable |
| BGM | 0.6 | Must not compete with SFX |
| SFX Combat | 1.0 | Full volume — critical feedback |
| SFX UI | 0.5 | Softer — non-intrusive |
| SFX Ambient | 0.3 | Background layer — never dominant |
| SFX Event | 0.9 | Nearly full — important narrative moments |

**Ducking:** BGM auto-ducks to 0.3 when boss stinger or war horn plays, returns to 0.6 over 500ms.

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Audio init blocks main thread | FPS drop on floor entry | Pre-load SoundPool during loading screen transition; init BGM async |
| Too many concurrent SFX | SoundPool stream starvation | Priority system: combat > events > UI > ambient. Evict lowest priority. |
| Audio memory bloat | APK size increase, OOM on low-end | OGG compression; max 8MB total; lazy-load boss assets |
| Seamless loop artifacts | Jarring BGM restart | Test loop points in Audacity; add 50ms crossfade at endpoints |
| Android Doze/background audio | Unexpected audio after pause | Strict lifecycle: pause in `onPause()`, release in `onDestroy()` |
