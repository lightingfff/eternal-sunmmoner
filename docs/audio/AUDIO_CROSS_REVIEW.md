# AUDIO CROSS-REVIEW — ES-A12-001

**ROLE:** A12 Audio & SFX Designer  
**TASK:** ES-A12-001  
**HEAD SHA:** f772aa31dd0dfb2a490beecc131ccfc5e1ea6951 (PR #72)  
**DATE:** 2026-11-08  
**PURPOSE:** Read-only cross-review of Floor1 cue timing vs PR #75 (A05 rescue spec) + PR #68 (A07 narrative)  
**DISPATCH REF:** [COORDINATOR DISPATCH — ROUND 2] 2026-11-08T08:30:21Z

---

## 1. Timing Alignment: Cue Sheet vs A05 Beat Structure

A05's spec defines 6 beats (B1–B6) on real meadow coordinates. A12's cue sheet must fire SFX/BGM transitions at the **exact beat boundaries**. Below is the synchronized timeline:

| A05 Beat | Beat Name | Sim Budget | Key Coordinates | Audio Transition |
|---|---|---|---|---|
| **B1** | Mở đường | 20–30s | 600→1100, guards (880,1000)+(940,1140) | `BGM_MEADOW_COMBAT` active. SFX: goblin aggro (#07) on approach; melee_hit_light (#09) on FOCUS_FIRE; enemy_death_goblin (#13) on kill. Courier join = `SFX_COURIER_ENCOUNTER_START` (#25). |
| **B2** | Ngã ba & tiếp vận | 15–30s | (1220,1130) | Courier stops. **NEW:** `SFX_BANDAGE_APPLY` (SUPPORT) or `SFX_CHANNEL_PICKUP` (SUPPLIES). If raider pressure during channel: goblin aggro SFX. **GAP IDENTIFIED** — cue sheet lacks supply-choice audio. |
| **B3** | Chọn tuyến Bắc/Nam | 45–70s | Bắc: (1340,640)→(1740,630); Nam: (1480,1520)→(1820,1530) | Route decision moment: **UI choice chime** (#24 extended). North route = wolf_aggro (#08, heavier), archer_aggro (#07). South route = lighter combat. **Ambient shift**: subtle tension increase on North, calm reassurance on South. |
| **B4** | Báo động | 10–20s | After (2120,1030) or (2240,1075) | **CRITICAL SYNC:** Scout runs 3s → 6s horn. `SFX_TELEGRAPH_WARNING` (#27) must fire at t=0 of scout run (1.5s audio, loops/increases). At t=3s: `SFX_SCOUT_ALARM` (#28) — alarm horn, must match the 6-second horn window. If INTERCEPT: scout dies silent. If PROTECT: alarm sounds, reinforcements incoming. |
| **B5** | Kraag chắn cổng | 45–75s | (2540,1050) | **CRITICAL SYNC:** Telegraph 1.5s → MOVE_TO dodge → 1.8s FOCUS_FIRE window. `SFX_WAR_HORN_ALARM` (#31, 3s) plays first. Then `BGM_BOSS_STINGER` (#32, 8-12s) transitions into `BGM_BOSS_COMBAT` (#33). `SFX_BOSS_SPAWN_ROAR` (#34) at Kraag appear. Boss cleave telegraph: `SFX_TELEGRAPH_WARNING` variant (1.5s). Boss cleave hit: `SFX_BOSS_CLEAVE` (#35). Boss death: `SFX_BOSS_DEATH` (#36). |
| **B6** | Rút về cổng | 15–25s | (2540→3035,1060) | Kraag dies → `SFX_BOSS_DEATH` (#36). Max 2 wolves spawn (A05 spec) — `SFX_WOLF_BITE` (#12), `SFX_ENEMY_AGGRO_WOLF` (#08). `SFX_RETREAT_HORN` (#21) plays once. Courier walks to portal → `SFX_COURIER_RESCUED` (#29) at (3035,1060) arrival. BGM cross-fades from boss to `BGM_VICTORY` (#37). |

### Outcome Audio (A07 Narrative Beats)

| A07 Sequence | Trigger | Beats | Audio |
|---|---|---|---|
| **NO-F1-A** "Cổng Sáng Trở Lại" (RESCUED) | `outcome == RESCUED` | 4 beats, ≤9s | Beat 1-3: soft ambient pad (warm strings). Beat 4 (Bách's name reveal): gentle chime accent. Overall: tired-but-standing tone. `SFX_VICTORY_CHIME` (#39) at end. |
| **NO-F1-B** "Tiếng Tù Và Không Đáp" (FAILED) | `outcome == FAILED` | 4 beats, ≤9s | Beat 1-2: low drone, somber. Beat 3: silence/pause. Beat 4 (narrator): single distant horn echo. `BGM_DEFEAT` (#38) underneath. No victory chime. |

---

## 2. Gaps Identified in Current Cue Sheet (PR #72)

| Gap # | Missing Cue | Context from A05/A07 | Priority | Recommendation |
|---|---|---|---|---|
| G1 | Supply choice audio (SUPPORT vs SUPPLIES) | B2: 15-30s channel decision at (1220,1130) | MEDIUM | Add 2 SFX: `sfx_bandage_apply.ogg` (0.5s, soft cloth/unwrap) + `sfx_channel_pickup.ogg` (0.8s, crate lift). If raider pressure during channel: reuse #07. |
| G2 | Route decision moment | B3: North/South fork choice | LOW | Extend `SFX_UI_TAP` to include a slightly more impactful "decision made" tone. Or add `sfx_route_confirmed.ogg` (0.4s, horn acknowledgment). |
| G3 | North vs South ambient differentiation | B3: different danger levels per route | LOW | Ambient tension layer: `sfx_north_tension_bed.ogg` (loop, 30s, subtle unease) vs `sfx_south_calm_bed.ogg` (loop, 30s, reassuring). Volume 0.2 — barely perceptible. |
| G4 | Hero fear reaction audio | A07 SCOUT_ALARM: hero fears for 10s | MEDIUM | `sfx_hero_fear_reaction.ogg` (0.6s, gasp/shock). Plays when `frightenedHeroId` is set. Distinct from combat hit. |
| G5 | B6 wolf pursuit ambient | A07 §6: max 2 wolves chase courier post-Kraag | LOW | Layer over existing wolf SFX. Optional: `sfx_wolf_chase_loop.ogg` (loop, 5s, panting + pawsteps, fades when wolves die). |
| G6 | Narrative beat ambient beds | A07: 4-beat dialogue sequences (≤9s each) | MEDIUM | `sfx_narrative_rescued_bed.ogg` (10s, warm pad, gentle strings) + `sfx_narrative_failed_bed.ogg` (10s, low drone, distant echo). Must not compete with future voice acting. |
| G7 | Panic → Calm BGM transition | A05 B4→B5: alarm to boss fight | HIGH | Need explicit cross-fade logic doc. War horn (#31) → 2s silence → boss stinger (#32) → boss BGM (#33). Current sheet implies this but doesn't specify the silence gap. |

---

## 3. Panic/Calm Transition Map

Based on A05's beat structure, here is the emotional arc and corresponding BGM/SFX dynamics:

```
B1 (Calm→Action)     : BGM_MEADOW_COMBAT steady at 0.6 vol
B2 (Decision/Calm)   : BGM ducks to 0.4, ambient rises slightly
B3-North (Tension)   : BGM returns 0.6, ambient tension layer at 0.2
B3-South (Calm)      : BGM returns 0.6, ambient calm layer at 0.15
B4 (Panic!)          : BGM ducks to 0.3, war horn at 0.9, then BGM rises
B5 (Peak Combat)     : BGM_BOSS at 0.7, SFX at 1.0, BGM ducks 0.3 on boss stinger
B6 (Relief→Tension)  : Boss BGM fades, meadow combat returns at 0.5, wolf pursuit SFX
RESCUED (Calm/Warm)  : BGM_VICTORY at 0.6, narrative bed at 0.3
FAILED (Somber)      : BGM_DEFEAT at 0.5, narrative bed at 0.25
```

**Ducking rule:** All BGM auto-duck to 0.3× when war horn or boss stinger plays (200ms fade), return to previous level over 500ms.

---

## 4. Rescued/Failed Ending Audio Design

### RESCUED ("Cổng Sáng Trở Lại")
- **Emotion:** Tired but standing. Not triumphant — morale is −6.
- **Audio approach:**
  - BGM: gentle victory pad, NOT a fanfare. Think "we survived" not "we conquered."
  - Narrative bed: warm cello + soft piano, volume 0.3.
  - Beat 4 (Bách says his name): single gentle chime (C major, mid register).
  - Final beat → 1s silence → `SFX_VICTORY_CHIME` → settlement screen.
- **Forbidden:** No brass fanfare, no triumphant drums. Misleading given −6 morale.

### FAILED ("Tiếng Tù Và Không Đáp")
- **Emotion:** Heavy, resolute, with a way out. NOT mocking, NOT blaming.
- **Audio approach:**
  - BGM: low string drone, very slow tempo, volume 0.4.
  - Beat 3 (Vesper's line about memory): brief silence (0.5s) — let words land.
  - Beat 4 (narrator): single distant horn echo with long reverb tail (2s).
  - Final beat → 1.5s silence → settlement screen (no chime, no sting).
- **Forbidden:** No "game over" buzzer, no punitive sound. Player must want to retry.

---

## 5. CC0/ Licensed-Original MVP SFX Package Proposal

### 5.1 Principles
- **All assets must be CC0 or owner-commissioned original.**
- No copyrighted music, no paid asset stores without owner approval.
- No runtime downloads — offline-first.
- Provenance must be documented per asset (source URL, license, modification notes).

### 5.2 MVP Package — "Floor 1 Combat Essentials" (Phase A)

**9 files, estimated ~2.5 MB, targeting core combat feel:**

| # | File | Description | Suggested Source | License | Est. Size |
|---|---|---|---|---|---|
| 1 | `floor1_meadow_combat.ogg` | BGM: dark-fantasy mid-tempo loop (bodhrán + strings) | **Owner-commissioned** or Pixabay Music (verify license per track) | CC0 or Original | ~800KB |
| 2 | `victory_result.ogg` | BGM: gentle victory pad (not triumphant) | **Owner-commissioned** | Original | ~300KB |
| 3 | `defeat_sting.ogg` | BGM: somber low strings | **Owner-commissioned** | Original | ~200KB |
| 4 | `melee_hit_light.ogg` | Quick metallic impact | Freesound.org (search: "sword hit light", CC0 filter) | CC0 | ~40KB |
| 5 | `arrow_release.ogg` | Bow twang + arrow whistle | Freesound.org (search: "bow release", CC0 filter) | CC0 | ~60KB |
| 6 | `wolf_bite.ogg` | Animal snap/bite | Freesound.org (search: "animal bite snap", CC0 filter) | CC0 | ~30KB |
| 7 | `hero_skill_activate.ogg` | Energy charge + release | Freesound.org (search: "magic cast", CC0 filter) or original | CC0/Original | ~80KB |
| 8 | `hero_hit.ogg` | Impact grunt + armor clang | Freesound.org (search: "armor hit", CC0 filter) | CC0 | ~40KB |
| 9 | `victory_chime.ogg` | Ascending chime | Freesound.org (search: "bell chime ascending", CC0 filter) | CC0 | ~20KB |

### 5.3 Provenance Requirements
For each CC0 asset from Freesound.org:
- Document the exact URL
- Record the uploader's username
- Note any modifications made (trimming, pitch shift, format conversion)
- Verify CC0 status on the file's page (not just the search filter)

For commissioned originals:
- Document the tool/platform used
- Record the generation parameters if AI-assisted
- Owner retains full rights

### 5.4 Why Not Full CC0 Pack?
Real CC0 SFX from Freesound.org require per-file verification and often need processing (trimming, normalizing, OGG encoding). A fully CC0 MVP is achievable but requires:
1. Owner authorization to use Freesound.org as source
2. Time for A12 to search, verify, download, and process each file
3. A10 review of memory/latency impact per file

**Recommendation:** Start with owner-commissioned BGM (3 tracks) + CC0 Freesound SFX (6 clips). This gives the best quality-to-effort ratio for the MVP.

---

## 6. Questions for Reviewers

| To | Question |
|---|---|
| **A05** | B2 supply channel (15-30s): should audio differ between SUPPORT (instant bandage) vs SUPPLIES (3s channel with raider pressure)? |
| **A05** | B3 route choice: is the North/South ambient differentiation too subtle to matter, or should it be more pronounced? |
| **A07** | Narrative beats (4 beats ≤9s): should audio fade out before settlement screen, or cross-fade into settlement UI music? |
| **A07** | Hero fear reaction (10s): should there be a recurring audio cue during the fear duration, or just a one-shot at fear start? |
| **A10** | SoundPool init during B1→B2 transition (15-30s window): acceptable latency, or should it preload during floor entry loading screen? |
| **Owner** | CC0 Freesound.org + commissioned BGM approach acceptable, or prefer fully original AI-composed package? |

---

## 7. Summary

✅ Cue sheet aligns with A05's 6-beat structure and A07's narrative sequences  
⚠️ 7 gaps identified (G1-G7), 3 HIGH priority (G4 hero fear, G6 narrative beds, G7 panic→calm transition)  
⚠️ MVP package proposed: 9 files ~2.5MB, mix of CC0 SFX + commissioned BGM  
🚫 No audio runtime code changes authorized; awaiting separate implementation ticket  
📋 Action: Freeze PR #72 authorship pending owner/reviewer feedback on gaps and licensing  

**STATUS:** CROSS-REVIEW COMPLETE, AWAITING DISPATCH
