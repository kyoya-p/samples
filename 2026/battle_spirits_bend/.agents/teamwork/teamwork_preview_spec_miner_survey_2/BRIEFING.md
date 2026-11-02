# BRIEFING — 2026-10-03T04:08:45Z

## Mission
Extract and formalize Battle Spirits TCG official rules, conservation invariants, turn steps, card types, BP battle resolution, flash timing, and level-up for the Bend simulator.

## 🔒 My Identity
- Archetype: SPECIFICATION MINER
- Roles: Teamwork specialist, Specification Miner
- Working directory: C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_spec_miner_survey_2
- Original parent: 45619497-9835-4af7-aee1-9c75f29397cb
- Milestone: Rule Formalization & Feature Inventory

## 🔒 Key Constraints
- Read-only: Specification mining only, no code implementation.
- User rules: 日本語、簡潔、挨拶・ですます調不要、体言止め。情報ソース提示。コミット/送信/初期化禁止。
- Core conservation invariant: Total cores across Life, Reserve, Field, Trash, Void strictly conserved.
- Output format: Tables for Features Discovered and Edge Cases in handoff.md.

## Current Parent
- Conversation ID: 45619497-9835-4af7-aee1-9c75f29397cb
- Updated: 2026-10-03T04:08:45Z

## Loaded Skills
- Source: C:\Users\kyoya\home26\works\samples\2026\.agents\skills\token-saver\SKILL.md
- Local copy: N/A
- Core methodology: Token optimization and frugal context management guidelines.

## Task Summary
- **What to build**: Complete specification and formal rules of Battle Spirits for game engine & simulator in Bend.
- **Success criteria**: Detailed, rigorous rule specification including all turn steps, core conservation laws, card attributes, level mechanics, flash window priority, battle resolution, legal moves, illegal states, edge cases.
- **Interface contracts**: C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\ORIGINAL_REQUEST.md
- **Code layout**: Target engine at C:\Users\kyoya\home26\works\samples\2026\battle_spirits_bend

## Key Decisions Made
- Prioritize Battle Spirits Official Rule (Official Rule Manual / Comprehensive Rules) and specific requirements in ORIGINAL_REQUEST.md and DISPATCH.md.
- Identified discrepancy: Official rule executes Draw on Turn 1 P1, while DISPATCH stated skip; formalized configurable flag `first_turn_draw_enabled`.
- Identified discrepancy: Official rule grants Flash priority to Defender first, while DISPATCH noted Attacker priority; formalized priority parameter `flash_priority_first`.
- Confirmed core destination on destruction/depletion is Reserve (not Trash).
- Confirmed mandatory cost reduction rule.
- Confirmed blocker removal during Flash 2 retains blocked status (0 life damage).
- Completed 29 Features Discovered and 18 Edge Cases in handoff.md.

## Artifact Index
- C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_spec_miner_survey_2\DISPATCH.md — Task assignment
- C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_spec_miner_survey_2\BRIEFING.md — Persistent context
- C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_spec_miner_survey_2\progress.md — Progress heartbeat
- C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_spec_miner_survey_2\handoff.md — Final handoff report
