"""
Unit and integration tests for Auto Agent Dispatcher.
Validates role resolution, single-writer lock enforcement, dry-run safety,
and PR dual-review generation.
"""

import unittest
from tools.dispatcher.agent_dispatcher import (
    ROLE_REGISTRY,
    extract_role_from_text,
    extract_dependencies,
    check_file_collision,
    check_backend_availability,
    generate_claim_payload,
    generate_pr_review_dispatch,
    run_dispatcher,
)


class TestAgentDispatcher(unittest.TestCase):

    def test_role_extraction(self):
        self.assertEqual(
            extract_role_from_text("[ES-A03-002][P0] Integrate accepted Argon V2 assets", ""),
            "A03"
        )
        self.assertEqual(
            extract_role_from_text("Task title", "Some details\nROLE: A08\nMore text"),
            "A08"
        )
        self.assertEqual(
            extract_role_from_text("[ARGON-WORK-001] Co-Lead creative pilot", ""),
            "ARGON"
        )
        self.assertEqual(
            extract_role_from_text("[CODEX P30] Battle Master command latency", ""),
            "CODEX"
        )
        self.assertIsNone(
            extract_role_from_text("Random bug report with no role", "Just user feedback")
        )

    def test_dependency_extraction(self):
        body = """
        **STATE: READY**
        ROLE: A01
        DEPENDENCIES: PR #51, #42, #50
        """
        deps = extract_dependencies(body)
        self.assertEqual(deps, ["#51", "#42", "#50"])

    def test_file_collision_detection(self):
        active_locks = {
            "app/src/main/java/com/example/ui/screens/MasterHubScreen.kt": "50",
            "app/src/main/java/com/example/game/pixel/**": "78"
        }

        # Case 1: Collision on exact locked file from different issue
        has_coll, colls = check_file_collision(
            ["app/src/main/java/com/example/ui/screens/MasterHubScreen.kt"],
            active_locks,
            issue_id="76"
        )
        self.assertTrue(has_coll)
        self.assertEqual(len(colls), 1)

        # Case 2: Same issue owning the lock -> No collision
        has_coll, colls = check_file_collision(
            ["app/src/main/java/com/example/ui/screens/MasterHubScreen.kt"],
            active_locks,
            issue_id="50"
        )
        self.assertFalse(has_coll)

        # Case 3: Completely disjoint file -> No collision
        has_coll, colls = check_file_collision(
            ["app/src/main/java/com/example/data/model/MasterProfile.kt"],
            active_locks,
            issue_id="99"
        )
        self.assertFalse(has_coll)

    def test_backend_availability_safe_defaults(self):
        # By default in testing environment, API keys should not be assumed live
        is_avail, msg = check_backend_availability("arena_cli")
        self.assertFalse(is_avail)
        self.assertIn("interactive session", msg)

    def test_claim_payload_format(self):
        payload = generate_claim_payload(
            issue_number=78,
            role="A03",
            base_sha="17c99aea7786",
            branch_name="agent/a03-issue78-auto",
            allowed_files=["app/src/main/java/com/example/game/pixel/**"]
        )
        self.assertIn("[CLAIM] TASK: ES-A03-AUTO | ROLE: A03", payload)
        self.assertIn("Tôi sẽ phụ trách phần này.", payload)
        self.assertIn("BASE SHA: 17c99aea7786", payload)
        self.assertIn("BRANCH: agent/a03-issue78-auto", payload)
        self.assertIn(".agents/skills/es-a03-graphics-renderer/SKILL.md", payload)

    def test_pr_review_dispatch_dual_lead(self):
        review_plan = generate_pr_review_dispatch(
            pr_number=85,
            head_sha="abcdef123456",
            changed_files=[
                "app/src/main/java/com/example/game/pixel/SanctuaryWorldRenderer.kt",
                "app/src/main/assets/sanctuary_argon_v2/terrain.png"
            ]
        )
        self.assertEqual(review_plan["pr_number"], 85)
        self.assertEqual(review_plan["reviewers"], ["ARGON", "CODEX", "A09"])
        self.assertTrue(review_plan["no_self_merge"])
        self.assertIn("nearest-neighbor", review_plan["argon_focus"])

    def test_run_dispatcher_dry_run(self):
        title = "[ES-A08-002][P0] Validate all 24 Argon V2 art PNGs"
        body = "STATE: READY\nROLE: A08\nDEPENDENCIES: #65"
        res = run_dispatcher(
            issue_number=77,
            issue_title=title,
            issue_body=body,
            base_sha="db45a8dfed757721",
            dry_run=True
        )
        self.assertEqual(res["status"], "SIMULATED")
        self.assertEqual(res["role"], "A08")
        self.assertEqual(res["skill"], ".agents/skills/es-a08-pixel-art-vfx/SKILL.md")
        self.assertTrue(res["dry_run"])


if __name__ == "__main__":
    unittest.main()
