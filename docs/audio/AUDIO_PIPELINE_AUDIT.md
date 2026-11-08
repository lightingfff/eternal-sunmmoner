# Audio Pipeline Audit — ES-A12-001

**ROLE:** A12 Audio & SFX Designer  
**TASK:** ES-A12-001  
**BASE SHA:** bdff6f9976dc4927e1e095a503bc339509c7a9aa  
**DATE:** 2026-11-08  
**REVIEWERS:** A05 (Tower/Level Design), A10 (Android Performance)  
**STATUS:** AUDIT COMPLETE — READY FOR SPEC REVIEW

---

## 1. Current State — No Audio Implementation Exists

### 1.1 Codebase Scan Results

| Search Target | Files Found | Notes |
|---|---|---|
| `android.media.MediaPlayer` usage | 0 | No Android SDK audio APIs used |
| `android.media.SoundPool` usage | 0 | No low-latency SFX playback |
| `android.media.AudioManager` usage | 0 | No volume routing or focus handling |
| ExoPlayer / Media3 dependency | 0 | Not in `build.gradle.kts` |
| Any third-party audio library | 0 | Clean — no audio deps |
| `app/src/main/assets/audio/` | Directory missing | No audio assets shipped |
| `app/src/main/res/raw/` (audio) | Empty | No raw audio resources |
| Kotlin files referencing `sound`/`audio`/`sfx`/`music` | 0 | No audio code paths at all |

### 1.2 Conclusion

**ETERNAL SUMMONER is currently a silent game.** No audio pipeline, no audio manager, no asset directory, no dependencies. This is a greenfield implementation — the entire audio system must be designed from scratch.

**Advantage:** No legacy audio debt to untangle.  
**Risk:** Audio integration touches the main game loop and UI lifecycle — must be designed carefully to avoid impacting the existing 10 FPS performance budget documented in AGENTS.md.

---

## 2. Audio Events Identified from Game Systems

The following analysis maps all gameplay-relevant audio cue points discovered from reading the Kotlin source:

### 2.1 Tower Combat Engine (`TowerCombatEngine.kt`)

| Game Event | Source Location | Audio Priority | Notes |
|---|---|---|---|
| `MissionStage.INTRO_DIALOGUE` → start | Line 19-24 | HIGH | Floor entry dialogue — needs ambient + UI bed |
| `MissionStage.IN_PROGRESS` → active | Line 447+ | MEDIUM | Main gameplay loop — ambient loop |
| `MissionStage.EVENT_CUTSCENE_BANNER` → trigger | Line 529-543 | HIGH | Mid-mission boss/ambush wave — fanfare + alarm |
| `MissionStage.VICTORY` | Line 646 | HIGH | Victory jingle + result screen |
| `MissionStage.DEFEAT` | Line 649 | HIGH | Defeat sting + retreat |
| `applyDamageToEnemy()` — enemy hit | Line 824 | HIGH | Impact SFX (varies by enemy type) |
| `applyDamageToHero()` — hero hit | Line 851 | HIGH | Hero damage SFX |
| `applyDamageToMaster()` — master hit | Line 870 | HIGH | Master takes damage — distinct, concerning SFX |
| `confirmTacticalRetreat()` | Line 245 | HIGH | Retreat order — horn/whistle |
| `issueTacticalOrder()` — FOLLOW/AGGRESSIVE/DEFENSIVE | Line 276 | MEDIUM | Order confirmation SFX |
| `triggerHeroManualSkill()` | Line 424 | HIGH | Hero skill activation — impactful SFX |
| `ContactAttackTimeline` — hit contact | ContactAttackTimeline.kt | HIGH | Melee impact at contact frame (0.22s normal, 0.30s skill) |
| Enemy aggro / detection | `updateEnemyAi()` line 659 | MEDIUM | Enemy alert sound when entering aggro range |
| Discovery node claimed | `claimDiscovery()` line 209 | MEDIUM | Positive discovery chime |
| `bonusDiscoveryGold` | Line 82 | LOW | Gold pickup sparkle |

### 2.2 Lost Courier Encounter (`LostCourierEncounter.kt`)

| Game Event | Audio Priority | Notes |
|---|---|---|
| Courier encounter starts (`CLEAR_PATH` phase) | MEDIUM | New objective notification |
| Phase transitions: `ESCORT` → `SCOUT_ALARM` → `GATE_GUARD` | HIGH | Escalating tension audio |
| `CourierTelegraph` warning (1.5s countdown) | HIGH | Danger telegraph — urgent pulsing |
| Courier rescued (`RESCUED`) | HIGH | Positive rescue jingle |
| Courier failed (`FAILED`) | MEDIUM | Failure tone — can retry |
| `CourierOrder` choice (PROTECT vs INTERCEPT) | LOW | Branching decision chime |

### 2.3 Sanctuary Lobby (`SanctuaryLobbyEngine.kt`)

| Game Event | Audio Priority | Notes |
|---|---|---|
| Lobby ambient (day/night cycle) | MEDIUM | Peaceful Sanctuary background music loop |
| Hero activity state changes (TRAINING/DINING/RESTING/etc.) | LOW | Soft activity confirmation |
| Camera pan/zoom | LOW | Subtle movement whoosh (optional) |
| Facility selection/interaction | MEDIUM | UI tap confirmation |
| Hero selection (tap) | MEDIUM | Character acknowledge |
| Island expansion milestone unlock | HIGH | Major achievement fanfare |

### 2.4 Tower Floor Entry/Exit

| Game Event | Audio Priority | Notes |
|---|---|---|
| Tower Gate entry transition | HIGH | Portal/staircase descent SFX |
| Floor banner display (`bannerRes`) | MEDIUM | Transition whoosh |
| Return to Sanctuary from Tower | HIGH | Ascending transition |
| Interior biome transitions (Restaurant/Dormitory/etc.) | MEDIUM | Door/interior ambient change |

---

## 3. Biome Audio Profiles

From `BiomeTheme` enum and `TowerFloorSpec` data:

| Biome | Audio Character | Key Layers |
|---|---|---|
| `FIRST_FIELD_MEADOW` (Floor 1) | Bright meadow, wind through grass, birdsong, distant stone ruins ambience | Wind bed + bird calls + grass rustle |
| `ELDERWOOD_FOREST` (Floor 2) | Dense canopy, creaking trees, mysterious undertones | Deep wind + branch creaks + distant owl |
| `SUNKEN_RUINS` (Floor 3-4) | Dripping water, hollow echoes, eerie drone | Water drops + reverb drone + stone grinding |
| `CRIMSON_CAVERN` (Floor 4-5) | Underground lava, heartbeat pulse, oppressive heat | Low rumble + lava crackle + heartbeat drone |
| `FALLEN_STAR_TEMPLE` (Floor 5+) | Celestial choir undertone, crystalline resonance, cosmic wind | Crystal chimes + deep pad + cosmic wind |
| `SANCTUARY_ASTRAL` | Warm village life, distant forge, market murmur, peaceful strings | Market ambience + distant anvil + gentle strings |
| Interior biomes (Restaurant/Dormitory/Training/Armory) | Indoor reverb, specific activity sounds | Varies per facility |

---

## 4. Enemy Audio Profiles

From `EnemyArchetype` enum:

| Enemy | Attack SFX | Alert/Aggro SFX | Death SFX |
|---|---|---|---|
| `FIELD_GOBLIN_RAIDER` | Crude blade slash (metallic, short) | Guttural war cry (short) | Collapse thud + weapon clatter |
| `FIELD_GOBLIN_ARCHER` | Bow twang + arrow whistle | Hissing alert | Collapse + bow drop |
| `DIRE_WOLF` | Snap/bite (animalistic) | Howl (medium, chilling) | Whimper + thud |
| `GOBLIN_WAR_CAPTAIN` (Boss) | Heavy cleave (deep, resonant) | War horn + roar (intimidating) | Defeated bellow + armor crash |

---

## 5. Proposed Technical Architecture

### 5.1 Android Audio Stack Recommendation

```
┌─────────────────────────────────────────────┐
│            GameAudioManager                  │
│  (singleton, lifecycle-aware)               │
│                                             │
│  ┌─────────────┐  ┌──────────────────────┐  │
│  │ BGM Channel  │  │  SFX Channel          │  │
│  │ MediaPlayer  │  │  SoundPool (max 12    │  │
│  │ or ExoPlayer │  │  concurrent streams)  │  │
│  │ (1 instance) │  │                       │  │
│  └─────────────┘  └──────────────────────┘  │
│                                             │
│  ┌──────────────────────────────────────┐   │
│  │ CueRegistry (maps CueId → asset     │   │
│  │ path, priority, pool behavior)       │   │
│  └──────────────────────────────────────┘   │
│                                             │
│  Volume: BGM × masterVol, SFX × masterVol  │
│  Lifecycle: pause on Activity.onPause(),   │
│  resume on onResume(), release onDestroy() │
└─────────────────────────────────────────────┘
```

**Why SoundPool for SFX:**
- Low latency (<20ms typical) — critical for combat hit feedback
- Pre-loaded samples — no disk I/O during gameplay
- Hardware mixing — avoids main thread contention
- Max 12 concurrent streams covers Floor 1 peak events (2 hero attacks + 2 enemy hits + ambient + UI + boss trigger)

**Why MediaPlayer/ExoPlayer for BGM:**
- Streaming from assets — doesn't preload entire music track into RAM
- Loop support — seamless ambient loops
- Fade in/out — smooth transitions between biomes/states

### 5.2 Memory Budget

| Asset Type | Format | Estimated Size | Count (Floor 1) |
|---|---|---|---|
| BGM tracks | OGG Vorbis (64kbps) | ~1.5 MB each | 3 (lobby, floor1 combat, floor1 boss) |
| SFX clips | OGG Vorbis (96kbps) | 20-80 KB each | ~25 unique clips |
| Total Floor 1 audio | — | ~5-7 MB | — |

### 5.3 Latency Strategy

- **Combat SFX:** SoundPool with pre-loaded assets. Target <20ms from game event to audio output.
- **BGM transitions:** Cross-fade over 500ms — mask loading latency.
- **Telegraph audio:** Start 200ms before visual telegraph peak — `CourierTelegraph.remainingSec = 1.5f` should trigger audio at 1.3s remaining.
- **ContactAttackTimeline:** Audio triggers at `elapsed >= contact` frame (0.22s normal / 0.30s skill) — same frame as damage calculation.

### 5.4 Audio-Off Fallback

- All gameplay must function identically with audio disabled.
- Honor `AudioManager.RINGER_MODE_SILENT` and system volume = 0.
- Provide in-game toggle (mute BGM, mute SFX, mute all) in settings.
- No gameplay information conveyed *solely* through audio — all audio cues must have visual counterparts (existing telegraph/UI systems satisfy this).

---

## 6. License & Asset Sourcing Strategy

### 6.1 Principles

- **No commercial/licensed music.** All assets must be:
  - Original compositions (commissioned or AI-assisted with clear license)
  - CC0 / Public Domain
  - CC-BY with attribution (requires owner approval per asset)
- **No paid asset stores** without explicit owner authorization.
- **No runtime downloads** — game is offline-first per AGENTS.md.

### 6.2 Recommended Sources (Free/Clear License)

| Source | License | Suitable For | Notes |
|---|---|---|---|
| Freesound.org (CC0 filter) | CC0 | SFX (impacts, nature) | Verify per-file license |
| OpenGameArt.org | CC0/CC-BY-3.0 | BGM loops, SFX packs | Check individual assets |
| Pixabay Music | Pixabay License | BGM (may need adaptation) | Free for commercial use, no attribution required |
| Original AI composition | Owner-controlled | BGM, themes | Requires owner tool authorization |

### 6.3 Asset Naming Convention

```
assets/audio/
├── bgm/
│   ├── sanctuary_day.ogg
│   ├── sanctuary_night.ogg
│   ├── floor1_meadow_combat.ogg
│   ├── floor1_boss_encounter.ogg
│   └── victory_result.ogg
├── sfx/
│   ├── combat/
│   │   ├── melee_hit_light.ogg
│   │   ├── melee_hit_heavy.ogg
│   │   ├── arrow_release.ogg
│   │   ├── wolf_bite.ogg
│   │   ├── boss_cleave.ogg
│   │   ├── enemy_death_goblin.ogg
│   │   ├── enemy_death_wolf.ogg
│   │   └── hero_skill_activate.ogg
│   ├── ui/
│   │   ├── tap_confirm.ogg
│   │   ├── order_issued.ogg
│   │   ├── discovery_chime.ogg
│   │   └── gold_pickup.ogg
│   ├── events/
│   │   ├── war_horn_alarm.ogg
│   │   ├── boss_spawn_fanfare.ogg
│   │   ├── courier_rescue.ogg
│   │   └── retreat_horn.ogg
│   └── ambient/
│       ├── meadow_wind.ogg
│       ├── birdsong_loop.ogg
│       └── ruins_distant.ogg
└── manifest.json
```

---

## 7. Integration Points (No Code Changes — Informational Only)

This audit identifies where audio hooks would connect to existing game systems. **No code changes are authorized under this task.** A follow-up implementation task would need explicit file-level ownership from A01 (technical director).

| System | Hook Location | Event to Emit | Audio Priority |
|---|---|---|---|
| TowerCombatEngine | `stage` transitions (line ~646/649) | `CueId.VICTORY` / `CueId.DEFEAT` | HIGH |
| TowerCombatEngine | `applyDamageToEnemy()` (line 824) | `CueId.ENEMY_HIT_<type>` | HIGH |
| TowerCombatEngine | `triggerHeroManualSkill()` (line 424) | `CueId.HERO_SKILL` | HIGH |
| TowerCombatEngine | `eventWaveTriggered` (line 543) | `CueId.BOSS_SPAWN_FANFARE` | HIGH |
| ContactAttackTimeline | `advance()` returns true (hit frame) | `CueId.MELEE_IMPACT` | HIGH |
| LostCourierEncounter | `phase` transitions | `CueId.COURIER_PHASE_<x>` | MEDIUM |
| SanctuaryLobbyEngine | `selectHeroAt()` / `selectFacilityAt()` | `CueId.UI_TAP` | MEDIUM |
| Activity lifecycle | `onPause()` / `onResume()` | Pause/resume all audio | CRITICAL |
| Settings | Mute toggle | Set BGM/SFX volume to 0 | HIGH |

---

## 8. Open Questions for Owner / Other Agents

1. **A03 (Graphics Renderer):** Are there any frame budget concerns if we add SoundPool initialization on the main thread during floor entry? Recommend initializing during the loading screen transition.
2. **A10 (Performance):** SoundPool supports up to 32 concurrent streams by default. Budget of 12 for Floor 1 — is this safe given the thermal/FPS constraints documented in AGENTS.md?
3. **A05 (Tower Level Design):** The Floor 1 cue sheet assumes 3 BGM states (meadow ambient, combat loop, boss encounter). Should there be a 4th state for the Lost Courier escort phase?
4. **Owner:** Audio asset creation tool — should we use original AI composition, CC0 asset packs, or a combination? This affects timeline and budget.
5. **A01 (Technical Director):** File ownership for `GameAudioManager.kt` — should A12 own this file, or should A01 integrate it?

---

## 9. Next Steps

1. ✅ Audit complete (this document).
2. ⏳ Owner/A05/A10 review of cue sheet priorities.
3. ⏳ Asset sourcing decision from owner.
4. ⏳ Implementation task (new ticket) with exact file paths for `GameAudioManager.kt`, `AudioCueId.kt`, and `app/src/main/assets/audio/**`.
5. ⏳ First playable: Floor 1 combat SFX + BGM loop as proof of concept.
