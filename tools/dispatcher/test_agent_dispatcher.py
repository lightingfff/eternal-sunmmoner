"""
Comprehensive Unit Test Suite for Auto Agent Dispatcher v2.

Covers:
1. Blocked task (missing STATE: READY or explicit STATE: BLOCKED/BACKLOG).
2. Unmet dependencies (dependencies not merged or unverified).
3. File collision between two competing agents.
4. Backend without credentials or unsupported background daemon (Arena Agent).
5. Successful dry-run and real dispatch gate.
6. PR Dual-Lead Review routing.
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


class TestAgentDispatcherV2(unittest.TestCase):

    def test_state_ready_enforcement(self):
        # Case 1: Missing state
        ok, msg = check_issue_state_ready("Title\nSome task description")
        self.assertFalse(ok)
        self.assertIn("missing explicit 'STATE: READY'", msg)

        # Case 2: Explicit BLOCKED state
        ok, msg = check_issue_state_ready("STATE: BLOCKED\nTask is blocked by art delivery")
        self.assertFalse(ok)
        self.assertIn("state is 'BLOCKED'", msg)

        # Case 3: Explicit BACKLOG state
        ok, msg = check_issue_state_ready("STATE: BACKLOG\nFuture task")
        self.assertFalse(ok)
        self.assertIn("state is 'BACKLOG'", msg)

        # Case 4: STATE: READY
        ok, msg = check_issue_state_ready("STATE: READY\nReady to execute")
        self.assertTrue(ok)

    def test_dependency_gate(self):
        deps = ["#51", "#42"]
        resolved_ok = {"#51": "MERGED", "#42": "MERGED"}
        ok, unmet = check_dependencies_met(deps, resolved_ok)
        self.assertTrue(ok)
        self.assertEqual(len(unmet), 0)

        # Case: PR #42 is still OPEN
        resolved_fail = {"#51": "MERGED", "#42": "OPEN"}
        ok, unmet = check_dependencies_met(deps, resolved_fail)
        self.assertFalse(ok)
        self.assertIn("#42 (current state: OPEN)", unmet)

        # Case: Unresolved remote status
        ok, unmet = check_dependencies_met(deps, None)
        self.assertFalse(ok)

    def test_file_collision_two_agents_competing(self):
        active_locks = {
            "app/src/main/java/com/example/ui/screens/MasterHubScreen.kt": "50",
            "app/src/main/java/com/example/game/pixel/**": "78"
        }

        # A03 attempts to lock pixel directory for issue 85 (already locked by issue 78)
        has_coll, colls = check_file_collision(
            ["app/src/main/java/com/example/game/pixel/SanctuaryWorldRenderer.kt"],
            active_locks,
            issue_id="85"
        )
        self.assertTrue(has_coll)
        self.assertIn("locked by #78", colls[0])

        # Issue 78 re-verifying own lock -> Allowed
        has_coll, colls = check_file_collision(
            ["app/src/main/java/com/example/game/pixel/SanctuaryWorldRenderer.kt"],
            active_locks,
            issue_id="78"
        )
        self.assertFalse(has_coll)

    def test_arena_agent_interactive_only_blocked_in_background(self):
        body = "STATE: READY\nROLE: A01\nDEPENDENCIES: #51"
        res = run_dispatcher(
            issue_number=52,
            issue_title="[ES-A01-001] Technical Director Kickoff",
            issue_body=body,
            resolved_dependencies={"#51": "MERGED"},
            dry_run=True
        )
        # Arena Agent must be BLOCKED for background daemon calls
        self.assertEqual(res["verdict"], "BLOCKED")
        self.assertIn("Arena Agent requires manual user session kickoff", res["reason"])

    def test_backend_unauthenticated_blocked(self):
        # Clear Gemini keys
        old_key = os.environ.pop("GEMINI_API_KEY", None)
        old_cli = os.environ.pop("GEMINI_CLI_ENABLED", None)
        try:
            body = "STATE: READY\nROLE: A03\nDEPENDENCIES: #77"
            res = run_dispatcher(
                issue_number=78,
                issue_title="[ES-A03-002] Integrate Argon V2 assets",
                issue_body=body,
                resolved_dependencies={"#77": "RESOLVED"},
                dry_run=True
            )
            self.assertEqual(res["verdict"], "BLOCKED")
            self.assertIn("GEMINI_API_KEY not configured", res["reason"])
        finally:
            if old_key:
                os.environ["GEMINI_API_KEY"] = old_key
            if old_cli:
                os.environ["GEMINI_CLI_ENABLED"] = old_cli

    def test_backend_authenticated_dry_run_success(self):
        os.environ["GEMINI_API_KEY"] = "mock_key_test"
        try:
            body = "STATE: READY\nROLE: A08\nDEPENDENCIES: #65"
            res = run_dispatcher(
                issue_number=77,
                issue_title="[ES-A08-002] Validate Argon V2 assets",
                issue_body=body,
                resolved_dependencies={"#65": "MERGED"},
                dry_run=True
            )
            self.assertEqual(res["verdict"], "SIMULATED_READY")
            self.assertEqual(res["role"], "A08")
            self.assertIn("max 8192 tokens", res["backend_result"]["message"])
        finally:
            os.environ.pop("GEMINI_API_KEY", None)

    def test_pr_dual_lead_review_routing(self):
        plan = generate_pr_review_dispatch(
            pr_number=88,
            head_sha="0123456789abcdef",
            changed_files=["app/src/main/java/com/example/game/combat/TowerCombatEngine.kt"]
        )
        self.assertEqual(plan["reviewers"], ["ARGON", "CODEX", "A09"])
        self.assertTrue(plan["no_self_merge"])
        self.assertIn("Critical combat invariants", plan["codex_focus"])


if __name__ == "__main__":
    unittest.main()
