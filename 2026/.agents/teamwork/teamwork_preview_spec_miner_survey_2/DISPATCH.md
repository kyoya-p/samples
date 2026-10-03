# DISPATCH — Spec Miner (Rules & Feature Inventory)

## Task Description
Extract and document the complete specification and formal rules of Battle Spirits required for this game engine and simulator.

## Identity & Working Directory
- Identity: teamwork_preview_spec_miner_survey_2
- Working Directory: C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_spec_miner_survey_2
- Target Codebase: C:\Users\kyoya\home26\works\samples\2026\battle_spirits_bend

## Inputs
- ORIGINAL_REQUEST.md: C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\ORIGINAL_REQUEST.md

## Scope & Instructions
1. Analyze Battle Spirits TCG official rules relevant to R1 & R3:
   - Turn flow: Start Step, Core Step (1 core from Void to Reserve, except turn 1 P1), Draw Step (1 card from Deck to Hand, except turn 1 P1), Refresh Step (all exhausted cards become refreshed, cores from Trash to Reserve), Main Step (summon Spirit, place Nexus, cast Magic, move cores between Reserve/Field), Attack Step (Flash timing priority, BP comparison, unblocked life damage, destruction), End Step.
   - Core conservation: Life + Reserve + Field (all cards) + Trash + Void. Total cores invariant.
   - Card types & attributes: Spirits (Cost, Reduction symbols, Colors, Levels L1/L2/L3 with core thresholds and BP values), Nexus (persistent field effects/symbols), Magic (spell effects, flash timing).
   - Cost reduction: Symbol counting by color on field vs card cost.
   - Flash timing priority: Attacking player priority, defending player response, passing.
   - Battle resolution: BP comparison (higher survives, lower destroyed, equal both destroyed), unblocked attacks deal life damage.
2. Enumerate every specific feature as a clear, testable requirement.
3. Define concrete edge cases, illegal moves, and invariant conditions.
4. Write your complete findings to `C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_spec_miner_survey_2\handoff.md`.
5. Send a message to the orchestrator when finished.


## 2026-10-03T04:03:58Z
You are teamwork_preview_spec_miner_survey_2. Your working directory is C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_spec_miner_survey_2. Read your task assignment at C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_spec_miner_survey_2\DISPATCH.md and C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\ORIGINAL_REQUEST.md. Extract and formalize Battle Spirits TCG rules, core conservation invariants, turn steps, card types, BP battle resolution, flash timing, level-up. Write handoff.md in your working directory and send a message when done.
