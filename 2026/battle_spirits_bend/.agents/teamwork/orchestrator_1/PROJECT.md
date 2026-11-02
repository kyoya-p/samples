# Project: Battle Spirits Bend Game Engine & Simulator

## Architecture
- **Language & Runtime**: Bend v0.2.38 / HVM2 v2.0.22 on WSL2 (Ubuntu 24.04).
- **Execution Target**: CPU Interpreter (`bend run`) for tests/debugging; C Multithreaded Backend (`bend run-c`) for parallel MCTS rollouts and benchmarks.
- **State Architecture**: Flat unboxed 24-bit (`u24`) tuples (no nested lists/pointers in dynamic state to prevent HVM2 DUP node bloat).
- **Static Meta Separation**: Cards spec (costs, symbols, BP tables) externalized in pure lookup function `card_lookup(id)`.
- **Concurrency Model**: Pure functional Splittable PRNG + `bend`/`fork` binary tree expansion for lock-free, zero-race parallel rollouts.
- **MCTS Model**: Root-parallel batched rollout + UCT score aggregation.

## Code Layout
```
battle_spirits_bend/
├── src/
│   ├── types.bend          # Object definitions: SpiritSlot, Player, Battle, GameState
│   ├── cards.bend          # Static card lookup table (id -> stats)
│   ├── prng.bend           # Pure 24-bit Splittable PRNG
│   ├── rules.bend          # Core conservation, step transitions, cost reduction, BP combat
│   ├── moves.bend          # Legal move generation (Main, Attack, Block, Flash)
│   ├── rollout.bend        # Fast-forward rollout simulation
│   └── mcts.bend           # Root-parallel tree rollouts and UCT selection
├── tests/
│   ├── test_invariants.bend # Core conservation invariant tests
│   ├── test_rules.bend      # Steps, summon, cost reduction, BP battle tests
│   └── test_mcts.bend       # Rollout & MCTS convergence tests
├── bench/
│   └── benchmark.bend      # 2^D parallel rollout benchmark (moves/sec)
├── run.sh                  # WSL2 execution helper script
├── Makefile                # Test and benchmark targets
└── README.md               # Engine documentation and benchmark results
```

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Setup & Tooling | Bend/HVM WSL2 toolchain, build scripts, runner | M1 | Env survey |
| 2 | State Data Structures | `SpiritSlot`, `Player`, `Battle`, `GameState` definitions | M2 | Spec Miner / Exp 3 |
| 3 | Static Card Lookup | Spirits, Nexus, Magic cost, BP, color specs | M2 | Spec Miner / Exp 3 |
| 4 | Splittable PRNG | 24-bit pure PRNG for deterministic parallel trees | M2 | Exp 3 |
| 5 | Core Total Conservation Check | Full game core invariant `Void+Life+Reserve+Field+Trash = const` | M3 | Spec Miner F2 |
| 6 | Game Initialization | 40-deck, 5 life, 4 reserve, 4 hand starting state | M3 | Spec Miner F1 |
| 7 | Start Step Resolution | Turn turnover, deckout loss check at Start step | M3 | Spec Miner F3, F27 |
| 8 | Core Step Allocation | Void -> Reserve (+1 core), skip turn 1 P1 | M3 | Spec Miner F4 |
| 9 | Draw Step Card Draw | Deck -> Hand (+1 card) | M3 | Spec Miner F5 |
| 10 | Refresh Step Cards & Cores | Recover exhausted spirits, Trash -> Reserve cores | M3 | Spec Miner F6, F7 |
| 11 | Main Step Action Dispatch | Summon, place nexus, magic, core move, pass | M3 | Spec Miner F8 |
| 12 | Mandatory Cost Reduction | Maximize discount based on field symbols (mandatory) | M3 | Spec Miner F9 |
| 13 | Spirit Summoning | Pay discounted cost to Trash, place cores ≥Lv1 | M3 | Spec Miner F10 |
| 14 | Nexus Placement | Pay cost to Trash, place nexus on field | M3 | Spec Miner F11 |
| 15 | Magic Main Cast | Pay cost to Trash, trigger effect | M3 | Spec Miner F12 |
| 16 | Free Core Reallocation | Freely move cores between reserve and friendly field | M3 | Spec Miner F13 |
| 17 | Dynamic Level & BP Update | Compute current Level and BP from cores on card | M3 | Spec Miner F14 |
| 18 | Lv0 Spirit Depletion | Spirits with <Lv1 cores vanish to Trash, cores to Reserve | M3 | Spec Miner F15 |
| 19 | Attack Step Transition | Transition to Attack step (prohibited turn 1 P1) | M3 | Spec Miner F16 |
| 20 | Attack Declaration | Exhaust 1 refreshed spirit to declare attack | M3 | Spec Miner F17 |
| 21 | Flash Timing 1 (Post-Attack)| Defender-first alternating priority flash window | M3 | Spec Miner F18 |
| 22 | Block Declaration | Defender exhausts refreshed spirit to block (or no block) | M3 | Spec Miner F19 |
| 23 | Flash Timing 2 (Post-Block) | Defender-first alternating flash window on block | M3 | Spec Miner F20 |
| 24 | BP Comparison Resolution | Higher BP wins, lower destroyed, tie both destroyed | M3 | Spec Miner F21 |
| 25 | Unblocked Damage Resolution | Unblocked attack deals symbol count damage to life | M3 | Spec Miner F22 |
| 26 | Spirit Destruction Handling | Destroyed spirit -> Trash, attached cores -> Reserve | M3 | Spec Miner F23 |
| 27 | Battle End Cleanup | End attack, return to attack step or turn turnover | M3 | Spec Miner F24, F25|
| 28 | Immediate Life Loss Check | Player life reaches 0 -> instant game over | M3 | Spec Miner F26 |
| 29 | Legal Move Generator | Generates legal actions given GameState | M4 | Spec Miner F28 |
| 30 | Fast-Forward Rollout | Fast simulation from state to terminal outcome | M4 | Spec Miner F29 / Exp 3 |
| 31 | Parallel Tree Rollout | `bend`/`fork` binary tree parallel rollouts | M4 | Exp 3 |
| 32 | MCTS / UCT Engine | Root-parallel search aggregating wins and visits | M4 | Exp 3 / R2 |
| 33 | Parallel Benchmark | $2^D$ concurrent paths, moves/sec measurement | M5 | Exp 3 / AC |
| 34 | Automated E2E Test Suite | 4-tier comprehensive opaque-box test suite | M6 / E2E Track | ORIGINAL_REQUEST R3 |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Environment & Tooling | WSL2 Bend/HVM toolchain setup, Makefile, run.sh helper | none | IN_PROGRESS |
| M2 | State Types & PRNG | `types.bend`, `cards.bend`, `prng.bend` | M1 | PLANNED |
| M3 | Rules Engine & Invariants | `rules.bend` (all steps, core conservation, BP combat, depletion) | M2 | PLANNED |
| M4 | Legal Moves, Rollout & MCTS | `moves.bend`, `rollout.bend`, `mcts.bend` | M3 | PLANNED |
| M5 | Parallel Benchmark | `bench/benchmark.bend` (throughput, moves/sec under C backend) | M4 | PLANNED |
| M6 | Final Milestone: E2E Tests & Hardening | Pass 100% E2E test suite (Tiers 1-4) + Tier 5 adversarial hardening | M5, TEST_READY | PLANNED |

## Interface Contracts
### `cards.bend` ↔ `rules.bend`
- `card_lookup(id: u24) -> (cost: u24, red: u24, lv1_cost: u24, lv1_bp: u24, lv2_cost: u24, lv2_bp: u24, sym_color: u24)`

### `prng.bend` ↔ `rollout.bend` / `mcts.bend`
- `rng_next(seed: u24) -> u24`
- `rng_split(seed: u24) -> (u24, u24)`

### `rules.bend` ↔ `moves.bend` / `rollout.bend`
- `init_game(seed: u24) -> GameState`
- `check_core_conservation(state: GameState, initial_total: u24) -> u24` # returns 1 if strictly conserved, 0 otherwise
- `step_transition(state: GameState, action: u24) -> GameState`
- `is_game_over(state: GameState) -> (u24, u24)` # (is_over: 0|1, winner: 0|1|2)

### `moves.bend` ↔ `rollout.bend` / `mcts.bend`
- `choose_random_legal_action(state: GameState, seed: u24) -> u24`

### `rollout.bend` ↔ `mcts.bend` / `bench/benchmark.bend`
- `rollout(state: GameState, seed: u24, max_steps: u24) -> (winner: u24, steps_taken: u24)`
