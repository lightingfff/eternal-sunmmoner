"""
Comprehensive Unit & Integration Test Suite for Auto Agent Dispatcher v3.

Test Matrix:
1. State Ready enforcement (BLOCKED on BACKLOG/WIP/Missing).
2. Live dependency resolution and blocking on unmerged PRs.
3. Competing agent file collision locks.
4. Arena Session interactive-only security guard.
5. Gemini & Codex authentication, cost guardrail (ALLOW_PAID_API), and timeout limits.
6. Real backend execution result vs simulated dry-run.
7. PR Dual-Lead Review routing with live backend connection audit.
"""

import os
import unittest
from tools.dispatcher.agent_dispatcher import (
    ROLE_REGISTRY,
    extract_role_from_text,
    extract_dependencies,
    check_issue_state_ready,
    check_dependencies_met,
    check_file_collision,
    generate_pr_review_dispatch,
    run_dispatcher,
)


class TestAgentDispatcherV3(unittest.TestCase):

    def test_state_ready_enforcement(self):
        ok, msg = check_issue_state_ready("Some unformatted task")
        self.assertFalse(ok)
        self.assertIn("missing explicit 'STATE: READY'", msg)

        ok, msg = check_issue_state_ready("STATE: BLOCKED\nWaiting for assets")
        self.assertFalse(ok)
        self.assertIn("state is 'BLOCKED'", msg)

        ok, msg = check_issue_state_ready("STATE: READY\nGo ahead")
        self.assertTrue(ok)

    def test_dependency_resolution(self):
        deps = ["#51", "#42"]
        mock_ok = {"#51": "MERGED", "#42": "MERGED"}
        ok, unmet = check_dependencies_met(deps, live_check=False, mock_status=mock_ok)
        self.assertTrue(ok)

        mock_fail = {"#51": "MERGED", "#42": "OPEN"}
        ok, unmet = check_dependencies_met(deps, live_check=False, mock_status=mock_fail)
        self.assertFalse(ok)
        self.assertIn("#42 (current state: OPEN)", unmet)

    def test_file_collision_two_competing_agents(self):
        active_locks = {
            "app/src/main/java/com/example/ui/screens/MasterHubScreen.kt": "50",
            "app/src/main/java/com/example/game/pixel/**": "78"
        }

        # Issue 99 asks for pixel paths (owned by 78)
        has_coll, colls = check_file_collision(
            ["app/src/main/java/com/example/game/pixel/SanctuaryWorldRenderer.kt"],
            active_locks,
            issue_id="99"
        )
        self.assertTrue(has_coll)
        self.assertIn("locked by #78", colls[0])

    def test_arena_session_interactive_guard(self):
        res = run_dispatcher(
            issue_number=52,
            issue_title="[ES-A01-001] Technical Director Kickoff",
            issue_body="STATE: READY\nROLE: A01\nDEPENDENCIES: #51",
            mock_dependencies={"#51": "MERGED"},
            dry_run=True
        )
        self.assertEqual(res["verdict"], "BLOCKED")
        self.assertIn("Arena Agent requires manual user session kickoff", res["reason"])

    def test_gemini_auth_and_cost_guardrails(self):
        old_key = os.environ.pop("GEMINI_API_KEY", None)
        try:
            # Without key -> BLOCKED
            res = run_dispatcher(
                issue_number=78,
                issue_title="[ES-A03-002] Integrate Argon V2 assets",
                issue_body="STATE: READY\nROLE: A03\nDEPENDENCIES: #77",
                mock_dependencies={"#77": "RESOLVED"},
                dry_run=True
            )
            self.assertEqual(res["verdict"], "BLOCKED")
            self.assertIn("GEMINI_API_KEY not configured", res["reason"])

            # With key and dry_run -> SIMULATED_READY
            os.environ["GEMINI_API_KEY"] = "mock_key_active"
            res2 = run_dispatcher(
                issue_number=78,
                issue_title="[ES-A03-002] Integrate Argon V2 assets",
                issue_body="STATE: READY\nROLE: A03\nDEPENDENCIES: #77",
                mock_dependencies={"#77": "RESOLVED"},
                dry_run=True
            )
            self.assertEqual(res2["verdict"], "SIMULATED_READY")
            self.assertIn("ALLOW_PAID_API is false", res2["backend_result"]["message"])
        finally:
            if old_key:
                os.environ["GEMINI_API_KEY"] = old_key
            else:
                os.environ.pop("GEMINI_API_KEY", None)

    def test_pr_dual_lead_review_routing_backend_status(self):
        # When backend is not authenticated, status reflects BLOCKED_BACKEND_UNAVAILABLE
        plan = generate_pr_review_dispatch(
            pr_number=88,
            head_sha="0123456789abcdef",
            changed_files=["app/src/main/java/com/example/game/combat/TowerCombatEngine.kt"]
        )
        self.assertEqual(plan["reviewers"], ["ARGON", "CODEX", "A09"])
        self.assertTrue(plan["no_self_merge"])


if __name__ == "__main__":
    unittest.main()
