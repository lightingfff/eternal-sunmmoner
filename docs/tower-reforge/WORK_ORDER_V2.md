# Tower Reforge V2 — work order (in progress)

This work order was added after the V2 reference arrived during ongoing MISSION01-B work; earlier uncommitted work predates this document. The current checkout is `mission01/floor1-rescue`, based on `7bdd1c92d3d3142d66dad4628f08ddf8d770b948`. Remote read-only ref inspection reports main `f0dc0de6b63794082ba5db88244a2cc8c486bcdb`, PR #28 head `7bdd1c92d3d3142d66dad4628f08ddf8d770b948`, and PR #33 head `c466d657f240f73fa52584f91bae9ea13537b444`. GitHub GraphQL PR metadata returned Forbidden. Main integration and remote PR/CI state remain unverified; preserve all uncommitted work and do not reset, force push or merge PRs.

Baseline before Mission01-B: three real starter-party simulations ~54–55 seconds, 42 tests passed; device input/FPS/thermal untested. A preliminary long-route implementation measured ~366 seconds, of which ~302 was travel and ~13 combat. That result does not satisfy the V2 substantive pacing requirement; interaction and route design are under review.

References: the four images attached in this conversation show phone tactical combat, fear/retreat alerts, and fantasy hero cards. They are references for visual hierarchy only; no source art, names or dialogue are game assets. REF01–REF08 from the document were not available in the current message. Wiki descriptions are unverified community references.

## Ownership

Director: integration, authored map, persistence and final checks. Encounter subagent: `LostCourierEncounter.kt`, `RescueJourney.kt`, `RescueJourneyContent.kt`. Gameplay subagent: `TowerCombatEngine.kt`, `RescueFieldActions.kt`, playthrough tests. UI subagent: battle screen, journey HUD and UI tests. Shared files have one writer at a time; review and Gradle execute serially. Existing assets and camera projection must remain intact.

## Delivery gates and risks

1. Repository/base audit: capture main/PR divergence and protect work; blocked PR API must be reported.
2. Floor 1: real decisions, tactical orders, fair danger, fear/agency and persistent rescue consequences; no timer, inflated HP, empty travel or repetition as filler.
3. Map and battle HUD: authored locations, navigable collision, readable objectives, real interaction and native engine evidence in portrait/landscape.
4. Verify Android build, actual starter-party paths, save/retry, meaningful UI interaction, lifecycle/performance, and independent review; report device and human playtests separately.

Tests: corridor and target reachability, full simulator from guest without forced damage/kills, both signal and rescue choices, failure/retry/save/reload, UI click/semantics, prior tactical and persistence regressions, `:app:assembleDebug`, asset validation, screenshot review. Risks: direct-line steering may softlock on old props; retreat from added regions may fail; extra map traversal can masquerade as content; HUD can obscure the scene; new flags may be compacted; main/PR33 may conflict with shared game files. The City rescue, fear overhaul, ultimate kits, full reference UI and real-device profiling are not yet verified.
