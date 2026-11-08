# ES-A03-001 — Work order (A03)

- Baseline: `bdff6f9976dc4927e1e095a503bc339509c7a9aa` (main verified via GitHub API, 2026-11-08).
- Branch/session: `arena/788e635f-eternal-sunmmoner`, Arena.ai Agent Mode 788e635f; sole writer A03 / @lightingfff.
- Task: https://github.com/lightingfff/eternal-sunmmoner/issues/54 — READY, no previous claim. Claim: https://github.com/lightingfff/eternal-sunmmoner/issues/54#issuecomment-6717980475.
- Player benefit: reduce future misregistration/occlusion risk before integration; no gameplay or visual change in this ticket.
- Current behavior: P27B/legacy renderer; P31 assets delivered on main are not permission to integrate.
- Exact allowed NEW files: this WORK_ORDER.md, ROUND_1.md, FINAL_REPORT_VI.md in this directory; `docs/art/ES-A03-001_RENDERER_AUDIT.md`. No other writes authorized.
- No-go: runtime, PNGs, manifest, verifier, Gradle, camera/world geometry, collision, save, UI and other agents' documents.
- Dependencies: #51 merged; #50 still OPEN; #59 BLOCKED. #45 runtime stays HOLD. A08 art QA and A10 performance interfaces require serial ownership from A01 for future code work.
- Skills: es-a03-graphics-renderer; eternal-arena-environment-architect; eternal-arena-quality-loop. Router/selection guide and studio-director procedural references read. No asset generation or foreign engine APIs.
- Work: source-backed camera/anchor/atlas/layer map; inspect manifest/hash/frame contract; reproduce validator limitation without modifying its report; propose future checks, not implementation.
- Acceptance: source references exist; eight GENERATED asset hash/size checks; distinguish PROPOSED entries and missing lifecycle contracts; maintain art HOLD; docs-only diff; evidence limits explicit.
- Environment/tests: local Python/static source inspection, git diff --check, path ownership check. No Android build, runtime capture, playthrough, device FPS or thermal claim.
- Visual bookmarks for later integration: zoom .36/.88/1.48, gate interior, chimney, hero behind/in front/partially under canopy, day/night and missing asset fallback.
- Budget: one documentation critique/retest round (maximum three if needed).
- PR: own session branch; request Argon, Codex, A09 and specialists A08/A10. No auto-merge; owner only. Rollback: omit this docs-only PR.
