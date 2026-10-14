# DISPATCH — E2E Test Writer (E2E Testing Track)

## Task Description
Design and implement the comprehensive, opaque-box E2E test suite for the Battle Spirits Bend engine across Tiers 1-4.

## Identity & Working Directory
- Identity: teamwork_preview_test_writer_e2e
- Working Directory: C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_test_writer_e2e
- Target Codebase: C:\Users\kyoya\home26\works\samples\2026\battle_spirits_bend

## Inputs
- ORIGINAL_REQUEST.md: C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\ORIGINAL_REQUEST.md
- PROJECT.md: C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\orchestrator_1\PROJECT.md
- Spec Miner Handoff: C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_spec_miner_survey_2\handoff.md
- Explorer 3 Handoff: C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_explorer_survey_3\handoff.md

## File Write Ownership
You exclusively own:
- `battle_spirits_bend/tests/*`
- `C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\orchestrator_1\TEST_INFRA.md`
- `C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\orchestrator_1\TEST_READY.md`
- Files in your working directory `teamwork_preview_test_writer_e2e/`

## Scope & Instructions
1. Review the 29 features and 18 edge cases in the Spec Miner handoff.
2. Create `C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\orchestrator_1\TEST_INFRA.md` detailing the 4-tier testing philosophy and coverage matrix.
3. Write test cases in Bend under `battle_spirits_bend/tests/`:
   - `tests/test_invariants.bend`: Tier 1 & 2 tests for core conservation invariant across all phases, depletion to reserve, unblocked damage to reserve, trash recycling to reserve.
   - `tests/test_rules.bend`: Tier 1 & 2 & 3 tests for step sequences (Start->Core->Draw->Refresh->Main->Attack->Flash->Block->Battle->End), turn 1 P1 restrictions, cost payment with mandatory symbol reduction, BP comparison (win/loss/tie), unblocked life damage, flash pass priority.
   - `tests/test_scenarios.bend`: Tier 4 multi-turn gameplay scenarios exercising full turns, summoning, attacking, blocking, and victory conditions.
4. Each test file should have a `main` function that executes test assertions and returns 0 (pass) or aborts/returns error code if an invariant or assertion fails.
5. Once all tests and infra are written, create `C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\orchestrator_1\TEST_READY.md` containing runner command and tier counts.
6. Write your handoff to `C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_test_writer_e2e\handoff.md`.

## MANDATORY INTEGRITY WARNING
DO NOT CHEAT. All test cases must genuinely test game rules and invariants. DO NOT create vacuous tests (tests that pass unconditionally without checking logic). A teamwork_preview_auditor will independently verify your work.


## 2026-10-03T04:12:53Z
You are teamwork_preview_test_writer_e2e. Your working directory is C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_test_writer_e2e. Read your task assignment at C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_test_writer_e2e\DISPATCH.md and C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\ORIGINAL_REQUEST.md. Design and write the 4-tier E2E test suite in battle_spirits_bend/tests/ along with TEST_INFRA.md and TEST_READY.md. Write handoff.md in your working directory and notify the orchestrator.
MANDATORY INTEGRITY WARNING: DO NOT CHEAT. All test cases must genuinely test game rules and invariants. DO NOT create vacuous tests. A teamwork_preview_auditor will independently verify your work.
