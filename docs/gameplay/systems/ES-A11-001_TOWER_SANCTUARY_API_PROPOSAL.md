# ES-A11-001 — Tower → Sanctuary quest/reward UI API proposal

**Status: PROPOSED / docs only / REVIEW PENDING.** A11 (user A011), 2026-11-08 UTC.
No symbols in the proposed API below are implemented by this PR. No save migration,
event implementation, economy rebalance, UI wiring or new quest inventory is authorized.
Baseline: `bdff6f9976dc4927e1e095a503bc339509c7a9aa`.
[Work order](ES-A11-001_WORK_ORDER.md) · [Audit evidence](ES-A11-001_AUDIT_REPORT.md) ·
[Task #62](https://github.com/lightingfff/eternal-sunmmoner/issues/62).

## 1. What main actually does

All source links below refer to **the baseline version of each path**, not moving main.
The `#L` anchors identify source lines for GitHub; inspect at the baseline SHA when reviewing.

| Existing feature | Baseline evidence | Constraint for UI glue |
|---|---|---|
| Tower completion reaches the ViewModel | [MainActivity.kt:335–337](../../../app/src/main/java/com/example/MainActivity.kt#L335); [finishTowerExpedition:386–424](../../../app/src/main/java/com/example/ui/SummonerViewModel.kt#L386) | Existing repository call owns settlement; an adapter must never call it on screen entry |
| Result modal reads committed call result | [TowerBattleScreen.kt:1266–1270, 1343–1405](../../../app/src/main/java/com/example/ui/screens/TowerBattleScreen.kt#L1266); [RewardSummary in FantasySkin.kt:44](../../../app/src/main/java/com/example/ui/components/FantasySkin.kt#L44) | Return is enabled only after non-null settlement and not busy; resource amounts come from result, not balance subtraction |
| Result is ephemeral | [ViewModel fields:63](../../../app/src/main/java/com/example/ui/SummonerViewModel.kt#L63); [return/launch:163–177](../../../app/src/main/java/com/example/ui/SummonerViewModel.kt#L163) | Return clears `_lastSettlement`. No durable last-expedition receipt/history exists in this inspected path; do not promise a recap on re-entry/restart |
| Settlement has no run-ID input | [SummonerDataSource.kt:131–143](../../../app/src/main/java/com/example/data/SummonerDataSource.kt#L131); [result shape:55–73](../../../app/src/main/java/com/example/data/firebase/SummonerRepository.kt#L55) | No sourceId, event ID, actor-ID list or revision in result. Names cannot be reverse-mapped into stable hero identities |
| Guest settlement writes a snapshot | [LocalGuestSaveManager.kt:1136–1238](../../../app/src/main/java/com/example/data/local/LocalGuestSaveManager.kt#L1136); [mutateSnapshot:114–126](../../../app/src/main/java/com/example/data/local/LocalGuestSaveManager.kt#L114) | Mutation writes before publishing snapshot. Atomic file write does **not** imply replay suppression across calls |
| Victory/retreat/defeat all have settlement policy | [guest reward formulas:1094–1134](../../../app/src/main/java/com/example/data/local/LocalGuestSaveManager.kt#L1094) | Retreat/defeat can earn resources; absence of a success event must not erase valid failure rewards |
| Courier quest facts and service consequence | [LostCourierProgression.kt:6–31](../../../app/src/main/java/com/example/data/LostCourierProgression.kt#L6); [CourierOutcome.kt:4](../../../app/src/main/java/com/example/data/model/CourierOutcome.kt#L4); [outcome authorization:1124–1126](../../../app/src/main/java/com/example/data/local/LocalGuestSaveManager.kt#L1124) | Main has `RESCUED` / `FAILED`, rescue only on Floor 1 victory; first-rescue flag protects service/memory, not all expedition grants |
| Forge UI already reflects persistent rescue | [SanctuaryFacilityModals.kt:927–950](../../../app/src/main/java/com/example/ui/screens/SanctuaryFacilityModals.kt#L927) | Cost is 90 gold after rescue, otherwise 120; use existing helper, no second claim/reward button |
| Profile/hero facts are durable data shapes | [FirestoreModels.kt:348–416](../../../app/src/main/java/com/example/data/model/FirestoreModels.kt#L348); [GuestSaveSnapshot schema 3:34–45](../../../app/src/main/java/com/example/data/SummonerDataSource.kt#L34) | Current state includes flags, resources, highest floor, injury/status and bond text, not a general quest inventory/event ledger |
| Training/recovery/meal actions exist | [ViewModel:220–368](../../../app/src/main/java/com/example/ui/SummonerViewModel.kt#L220) | Route explicit user intent to existing action owner only after a separately approved UI task; presentation does not execute them |
| Lobby actor rules exist independently | [manual commands:596–638](../../../app/src/main/java/com/example/game/lobby/SanctuaryLobbyEngine.kt#L596); [injured interior actors:700–725](../../../app/src/main/java/com/example/game/lobby/SanctuaryLobbyEngine.kt#L700); [syncHeroes:870–921](../../../app/src/main/java/com/example/game/lobby/SanctuaryLobbyEngine.kt#L870) | Injured heroes excluded outdoors; commanded flags exist. Do not infer comprehensive priority guarantees: status synchronization also assigns destinations; regression tests still needed |
| Notice component is real but volatile | [ESNotices.kt:40–81](../../../app/src/main/java/com/example/ui/components/p28/ESNotices.kt#L40) | `stableId` dedup of current entries, priority/time sorting, 8 retained / 3 visible; no durable ack or settlement ownership |
| Lobby currently shows banner feedback | [SanctuaryLobbyScreen.kt:943–948](../../../app/src/main/java/com/example/ui/screens/SanctuaryLobbyScreen.kt#L943) | This is `ESBannerNotice`, not proof a durable Hub feed or quest history has been integrated |

Cloud has its own settlement implementation: [SummonerRepository.kt:941–990](../../../app/src/main/java/com/example/data/firebase/SummonerRepository.kt#L941).
Courier outcomes take a transaction with refreshed profile/heroes; other outcomes use a batch.
Neither signature accepts a durable expedition source identity. This is a **static observation**,
not a reproduction of a cloud or guest replay exploit. Guest/offline is the primary acceptance
lane; no network dependency may be added to resume a guest game.

## 2. Provenance and bounded feature wish list

The authoritative event proposal is [Codex CP0 #46 comment](https://github.com/lightingfff/eternal-sunmmoner/issues/46#issuecomment-6690195127):
`HubEventV1(id, sourceId, kind, actorIds, occurredAtEpochSec, anchorKey, priority)`,
`HubSnapshot(revision, pending)`, `HubEventFeed.snapshots` and durable `acknowledge(eventId)`.
These names, wire strings, 8 pending-domain-event default and three-visible-notice policy
are **PROPOSED**, not present as implemented Hub symbols in inspected `app/src/main`.
This document consumes that direction; it does not supersede Codex's contract.

| Priority / idea | Main today | Proposed next bounded slice / gate |
|---|---|---|
| P1: courier status + forge benefit in Sanctuary | Persistent flags/helper and forge text already exist | Read-only quest row derived from current profile; A01/A04 approve placement; do not recreate the service grant |
| P1: honest expedition recap | Tower modal exists, result clears on return | Existing modal stays authoritative. Sanctuary recap requires a Codex-approved receipt/source contract, not copying `_lastSettlement` into a fake durable queue |
| P1: recovery next step | Injury deadlines/status and recovery actions exist | Offer navigation to quarters, show known actor IDs; no auto-heal or resource charge from a notice |
| P2: living return/rescued visitor | Local routines exist; #46 is proposal | Codex commits authorized event atomically; A03 supplies optional visual adapter after approval, no fake return from lobby entry |
| P2: feast/training notices | Real actions exist; durable feed not integrated | Announce committed actions only after their operation-ID contract exists; routine animation alone proves no resource/stat change |
| Deferred: general quest inventory / supply branch / new rewards | No `QuestRecord` / `QuestRepository` found in inspected main source | Separate owner-approved task and schema design if needed; no new save fields or invented rewards here |

At audit time PR [#28](https://github.com/lightingfff/eternal-sunmmoner/pull/28) (`09c553d81c53046fe1038b70c7046e2a6cff0661`),
[#47](https://github.com/lightingfff/eternal-sunmmoner/pull/47) (`c296e0958e35b1e8d0d4b339857e188f81776ea8`)
and [#50](https://github.com/lightingfff/eternal-sunmmoner/pull/50) (`8cc082fbbee5938dbfcfe159d2d2bba0d1645b1c`) are **OPEN**.
Their source diffs were not adopted as baseline functionality; PR28 mission branches are not
available merely because that PR exists. Refresh all SHAs/statuses before implementing.

## 3. Proposed presentation API — no repository mutation

**Illustrative Kotlin only; not compiled or a new persistence/wire schema.** Names are
local UI-facing suggestions subject to A01/Codex approval. Nothing below grants permission
to edit a Kotlin file. No new framework, engine or save format is introduced.

```kotlin
// Ephemeral, immutable UI values. Not serialized into GuestSaveSnapshot.
enum class FeedReadiness { LOADING, READY, UNAVAILABLE, ERROR }
enum class CourierQuestUiStatus { NOT_RESCUED, RETRY_AVAILABLE, RESCUED }

data class CourierQuestUi(
  val status: CourierQuestUiStatus,
  val forgeGoldCost: Long, // existing synthesisGoldCost(profile.storyFlags)
)

data class CommittedRewardUi(
  val sourceId: String, // supplied by approved domain receipt, never invented by UI
  val floorNumber: Int,
  val outcomeKey: String, // approved versioned domain outcome mapping, including unknown fallback
  val gold: Long,
  val mana: Long,
  val shards: Long,
  val ore: Long,
  val essence: Long,
)

data class SanctuaryProgressUi(
  val readiness: FeedReadiness,
  val courier: CourierQuestUi?,
  val recoveringHeroIds: List<String>,
  val latestCommittedReward: CommittedRewardUi?,
  val notices: List<GameNoticeUi>, // existing P28 presentation shape, <= 8 retained
)

interface SanctuaryProgressReadModel {
  val states: StateFlow<SanctuaryProgressUi>
}
```

No `claimReward`, `settle`, `recover`, `enqueue`, `acknowledge` or repository handle is
exposed to a Compose read-model consumer. New commands would be a separately approved
interface. Existing explicit facility actions keep their current validation/ownership.
The optional receipt is deliberately **unavailable on current main**: the existing result
cannot populate a stable `sourceId`. Do not synthesize it from floor number, hero names,
wall clock, current balance, `totalExpeditions`, or a new UUID on each UI retry.

### Data/adapter rules

1. **Current quest facts:** use committed profile flags. `COURIER_RESCUED` wins if both
   rescued/failed flags exist; else failed → `RETRY_AVAILABLE`, else `NOT_RESCUED`.
   The last state does not imply a tracked/not-started objective: main has no quest-start
   record. Rescue persists even if a later attempt fails. Forge cost always comes from
   `LostCourierProgression.synthesisGoldCost`, not a duplicated balance formula.
2. **Rewards:** receipt amounts are informational, already settled. No "Claim" CTA.
   Do not derive rewards from balance changes (spending/training may occur meanwhile),
   recompute formulas, add discovery amounts again, or parse localized loot names as IDs.
   Result totals already incorporate discovery bonuses; bonus text is a breakdown.
   Call the proposed values settlement-reported amounts, not verified net wallet deltas
   (baseline resource caps may affect the actual increase). Null receipt → omit recap
   or label "Không có bản tổng kết đã lưu"; never show zero as a verified amount.
3. **Availability:** absent profile → `LOADING` with no quest assertion. Ready committed
   profile + unavailable #46 feed → `UNAVAILABLE`, profile-backed quest/recovery may show,
   no synthetic notice/history. Feed error → `ERROR` with safe current facts only, no
   settlement retry from presentation. A truly empty supported feed is `READY`, not error.
4. **Consistent inputs:** the future domain adapter must supply profile, heroes and receipt
   from an agreed coherent commit/revision and current account scope. #46's proposed
   `HubSnapshot.revision` covers the feed; it does **not** currently prove profile/hero
   revision coherence. Do not expose a new result mixed with old injury/quest state. If
   coherence cannot be established, suppress the recap until reconciled. A01/Codex must
   resolve this before runtime implementation; A11 must not add a save revision field.
5. **Offline/session lifecycle:** bind observation to the active guest/profile session;
   cancel prior collectors and clear presentation on logout/account switch. No cross-guest
   event/receipt reuse. Restart uses committed guest facts; absence of a receipt/feed is
   supported, not a reason to call settlement again. Inject time into recovery projections
   using `isCurrentlyInjured(nowEpochSec)`; do not persist presentation timers.
6. **Notice mapping:** one `stableId = event.id`, source actor IDs only, labels resolved
   separately. Missing hero → neutral wording, never guess ID from names. Map known
   domain priorities explicitly to existing `NoticePriority`; unknown priority defaults
   conservatively without changing domain ordering. Epoch seconds → milliseconds with
   range checking; do not invent "now" for replay timestamps. Observe changes outside
   composition/per-frame rendering and offer notices through a lifecycle-aware adapter.
7. **Notices are optional:** an unknown kind/version, invalid timestamp, missing art or
   unresolved semantic anchor must not block profile/quest facts. Omit unsafe animation,
   use neutral text only if trustworthy fields exist. No raw coordinates in events/save;
   A03 resolves visual anchors, never using a chimney/roof as a hero destination. Maintain
   existing injured-rest rules/manual commands; presentation cannot move actors.

## 4. Settlement, acknowledgement and queue boundaries

```text
Tower terminal result
  -> existing settlement owner (Codex-owned future durable source identity)
  -> one authorized durable commit: rewards + hero/profile facts + event/receipt
  -> committed read model -> optional notice / optional A03 effect
                             -> consumption coordinator -> #46 durable visual ack
```

The **future commit/event/receipt flow is NOT implemented** by this PR. Main currently
has a result and durable profile/hero changes, not this complete chain.

- Codex must choose a run/operation ID persisted at start and a transactional duplicate
  check. Retrying a settlement must reuse that ID. Old saves need safe defaults; never
  infer a replayable event from old flags. A11 must not design/modify the durable ledger.
- Domain consequence/reward must be once-only independently of display/ack. A committed
  outcome may remain visible after a crash before ack: **at-least-once visual replay is
  possible**, not exactly-once pixels. Deduplicate active effects/notices by event ID.
- `NoticeCenter.dismiss` only edits its in-memory queue. It is not #46 acknowledgement.
  UI eviction/timeouts/navigation must not call settlement or undo a quest. Durable ack
  failures may cause re-display but must never duplicate gold/items/service/memory.
- One **Codex/A01-approved consumption coordinator** owns durable ack. Read-model observers
  and A03 effects must not each race to ack a shared event before the other can observe it.
  Coordinator tracks which optional consumers are supported/finished for the active
  session; unsupported kinds/assets may complete as visual no-ops per #46. The precise
  multi-consumer lifecycle is an open approval gate, not a second event service here.
- Domain pending cap (proposed 8) and UI cap (8 retained / 3 visible) are different layers.
  Cosmetic entries should coalesce/evict first; durable quest facts/rewards must survive
  notice loss and cannot depend on queue retention. Exact overflow/tie-breaking/retention
  policy remains Codex/A01-owned. UI may not blindly treat 8 retained as 8 unacked records.
- Failure/abandon must not emit a **success/rescue** event. This does not prohibit neutral
  failure info from committed state or the existing defeat/retreat rewards. Abandon
  semantics must be agreed explicitly; this proposal adds no settlement on raw navigation.

## 5. Future acceptance tests (planned, NOT RUN)

These are requirements for a later implementation ticket, not tests added by this PR.

| ID | Scenario | Expected / owner |
|---|---|---|
| T01 | Floor1 victory + authorized rescued courier | One durable settlement/source, rescue/service/memory, one eligible event; UI informational only. Codex + A09 |
| T02 | Courier failure, then later successful retry | Failed state allows retry; new run gets new source; rescue persists and clears failure per existing policy. Codex/A11 + A09 |
| T03 | Tactical retreat / defeat | Preserve real authorized resource amounts; no victory/rescue event; injuries follow committed policy. Codex + A09 |
| T04 | Abandon/back navigation before terminal result | No synthetic success or receipt from lobby entry; approved abandon policy exercised explicitly. Codex/A04 + A09 |
| T05 | Same-source retry, double finish, restart after commit before result/ack | One reward/loot/quest consequence/event; optional redisplay deduped by ID. Critical Codex regression |
| T06 | Restart before commit; failed disk write then retry | No false receipt/notice; valid old save intact; retry same source; one eventual commit. Codex + A09 |
| T07 | Old guest save without event fields / offline startup | Existing progress readable; no retroactive event or reward minted; no network dependency. Codex + A09 |
| T08 | Burst >8 events / >3 notices; identical priorities/timestamps | Documented deterministic domain retention, max UI retention/visibility; quest facts survive eviction. Codex/A04 + A09 |
| T09 | Unknown version/kind/anchor/hero, missing asset, ack failure | Safe no-op/neutral fallback; no actor relocation or settlement retry; ack retry cannot mutate economy. Codex/A03 + A09 |
| T10 | Injured, training or player-commanded hero at return | Rest constraints retained; effect does not override move/hold/task; no free heal/training. Codex/A03 + A09 |
| T11 | Recomposition, rotate, return/re-entry, two collectors | No duplicate active animation, grant or racing durable ack; no unsupported durable recap claim. A04/Codex + A09 |
| T12 | Profile update arrives before/after heroes/feed; logout/switch guest | No mixed-revision recap, wrong hero or cross-account notice; collectors cancelled. A01/Codex + A09 |
| T13 | Both courier flags present; extreme/invalid timestamps; capped wallets | Rescue benefit wins; invalid times handled safely; no fabricated net reward delta. A11/Codex + A09 |

Existing tests are **source evidence only**, not fresh passes:
[LostCourierPersistenceRobolectricTest.kt:33–137](../../../app/src/test/java/com/example/data/LostCourierPersistenceRobolectricTest.kt#L33)
checks first service/memory persistence, later retry, exact-90-gold crafting and failed
write behavior. Its stale-profile replay test does **not** assert that all currency/loot
are unchanged. [GuestSavePersistenceRobolectricTest.kt:48–138](../../../app/src/test/java/com/example/data/GuestSavePersistenceRobolectricTest.kt#L48)
checks guest restart/backup behavior, not a durable event ledger.
[P28ComponentsTest.kt:76–99](../../../app/src/test/java/com/example/ui/P28ComponentsTest.kt#L76)
checks current notice dedup/cap/order, not restart-safe domain ack.

## 6. Approval and implementation handoff

1. Codex: review source identity, atomicity, receipt availability, revisions, restart,
   unsupported-event ack and failure/abandon semantics; approve exact event contract
   in #46. This is scoped design review, not a request to divert from PR47 P0 fixes.
2. A01 + Argon: choose a small user-visible slice, agree navigation/wording and placement
   with A04; confirm technical vs creative file ownership. A09 supplies independent
   tests/reproduction. Argon checks design clarity, not unmeasured runtime visuals.
3. Owner creates/approves a separate READY implementation ticket with **exact** Kotlin/test
   paths. A11 may own noncritical mapper only after interface approval; Codex retains
   all settlement/save/ledger writes. A03/A04 integrate their own approved adapters.
4. Reviews on the exact PR SHA: `[ARGON_REVIEW]`, `[CODEX_REVIEW]`, A09 `[QA_REVIEW]`.
   Coordinator `[FIX_PLAN]` assigns one writer per file; new SHA needs renewed review.
   **No implementation approval, independent review result or merge is claimed here.**
