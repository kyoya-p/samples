# Original User Request

## Initial Request — 2026-10-03T04:01:02Z

Build a high-performance, massively parallel game engine and simulator for the Battle Spirits trading card game in the Bend programming language, optimized for Monte Carlo Tree Search (MCTS) and AI self-play.

Working directory: C:\Users\kyoya\home26\works\samples\2026\battle_spirits_bend
Integrity mode: demo

## Requirements

### R1. Core Rule Engine & State Representation
Implement Battle Spirits game state representation and state transition logic in Bend. This includes turn/step flow (Start, Core, Draw, Refresh, Main, Attack/Flash, End), core management (Reserve, Trash, Field/Card, Life, Void), and card interactions (Spirits, Magic, Nexus, cost payment with reduction).

### R2. High-Throughput Parallel Simulation & MCTS Support
Design state representations to leverage Bend/HVM mass parallelism. The engine must support branching game tree expansion, legal move generation, fast forward simulation (rollout/playout), and evaluation suitable for Monte Carlo Tree Search (MCTS).

### R3. Automated Test Suite & Rule Verification
Provide an automated test suite validating game mechanics against official rules: core conservation laws, step sequences, level-up mechanics, battle resolution (BP comparison, destruction, life reduction), and flash timing priorities.

## Acceptance Criteria

### Rule Correctness
- [ ] Automated tests pass for all basic phases: Core step, Draw step, Refresh step, Main step (summoning/cost reduction), and Attack/Flash steps.
- [ ] Core conservation invariant holds across all actions (total cores between Life, Reserve, Field, Trash, and Void strictly accounted for).
- [ ] Battle resolution correctly compares BP, destroys defeated spirits, and applies life damage when unblocked.

### Parallel Execution & Performance
- [ ] Engine compiles and executes under Bend (CPU/HVM or GPU targets).
- [ ] Parallel rollout/simulation benchmark executes multiple concurrent game paths and outputs moves/sec or throughput metrics without deadlocks or race conditions.


## Follow-up — 2026-10-03T04:26:55Z

ユーザー指示の規定（トークン週間残量10%未満で作業停止・問い合わせ）に達したため（現在Gemini週間残量: 9%）、直ちに作業を一時停止してください。
