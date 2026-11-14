---
name: superpowers-eternal
description: Use when selecting Superpowers references for ETERNAL SUMMONER debugging, implementation planning, testing, peer review or branch handoff.
---

# Superpowers references for ETERNAL SUMMONER

This is a project reference adapter, not an installed Superpowers plugin or a
replacement for the user's instructions and this project's agent workflow.
Start with the [catalog](../../../docs/skill-library/superpowers/CATALOG.md) and
read only the source relevant to the task. Announce the selected guidance.

| Current need | Source to read | Concrete output |
|---|---|---|
| Bug, failed build, stutter | `systematic-debugging` | Reproduction, observed failing boundary, one hypothesis and a scoped fix. |
| Behavior change / regression | `test-driven-development` and its test guide | A meaningful failing behavior check, minimal implementation, passing regression checks. |
| Several implementation steps | `writing-plans`, then an appropriate execution route | Exact files/interfaces, ownership, acceptance and commands; a progress record with commits and remaining work. |
| Independent investigations | `dispatching-parallel-agents` | Actual native agents with explicit disjoint ownership or read-only scope, only when authorized/applicable. |
| Sending or receiving review | `requesting-code-review`, `receiving-code-review` | BASE/HEAD diff, evidence, severity, technical response and exact-SHA re-review. |
| Reporting success / delivering PR | `verification-before-completion` | Fresh command results on the reported SHA, limits, artifact/PR links and requested review. |

The [project adaptation notes](../../../docs/skill-library/superpowers/READING_NOTES.md)
explain differences from the upstream workflow. In particular:

- Read existing context and continue already authorized work. Do not reopen settled
  choices or add repetitive design/plan/finish approval prompts. Ask only for missing
  decisions that materially affect the result or required authorization.
- Use tests appropriate to the change and required project gates. For consequential
  behavior fixes, reproduce failure before fixing when feasible. Do not delete
  existing work to stage a TDD demonstration, write tests that mirror implementation,
  or run an entire unrelated suite for a documentation edit. Report unrun checks.
- Use the tools actually available; reading source does not enable plugins, model
  presets or native subagents. Do not modify global configuration or claim logical
  roles are independent sessions. A narrow task does not require parallel workers.
- Respect claimed paths and one writer per file. Shared rendering/save/combat files
  remain assigned serial integration points; preserve other worktrees and branches.
- Preserve the project's exact-SHA independent review and re-review after fixes.
  Upstream inline-execution advice to skip a second review does not waive it.
  Use the project's three-cycle limit; unresolved blockers remain REVIEW/BLOCKED.
- Finish with a review PR when authorized, preserving its worktree and evidence.
  The user alone merges. No direct main push, automatic merge, force push or cleanup
  of another task's work/evidence.

Kotlin/Compose Canvas, ground Y=.72, upright nearest-filtered sprites, shared
feet/shadows, maps/collision/navigation, saves and Android identity remain unchanged
unless a separately assigned task explicitly changes a contract. A green host test
or successful build does not establish physical-device FPS, heat or human enjoyment.

All upstream text/scripts are inactive references under
`docs/skill-library/superpowers/upstream/`. Read a helper before using it for a later
authorized task; do not start hooks/visual servers, synchronize plugins or access
diagnostic sessions just because a reference suggests it. Do not upload transcripts
or contribute to the upstream repository under this learning task.
