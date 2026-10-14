# Test Infrastructure Specification (TEST_INFRA.md)

## 1. Testing Philosophy & 4-Tier Architecture

The Battle Spirits Bend engine test suite employs an opaque-box, multi-tier validation architecture. The engine operates on pure functional interaction combinator semantics under Bend/HVM2, requiring tests that verify mathematical invariants, phase transitions, tactical combat rules, and long-horizon multi-turn play without internal state leakage.

```
+-----------------------------------------------------------------------+
|  Tier 4: Multi-Turn End-to-End Gameplay Scenarios                     |
|  (Full Game Loops, Multi-Turn Summoning, Tactical Attacks, Lethal)    |
+-----------------------------------------------------------------------+
                                    ^
+-----------------------------------------------------------------------+
|  Tier 3: Combat, Flash Priority & Battle Resolution                   |
|  (Attack/Block, BP Win/Loss/Tie, Blocker Removal, Flash Priority)     |
+-----------------------------------------------------------------------+
                                    ^
+-----------------------------------------------------------------------+
|  Tier 2: Step Sequences & Phase Transition Rules                      |
|  (Start->Core->Draw->Refresh->Main->Attack, Turn 1 P1, Cost Reduction)|
+-----------------------------------------------------------------------+
                                    ^
+-----------------------------------------------------------------------+
|  Tier 1: Core Conservation & Mathematical Invariants                  |
|  (Total Core Sum Invariant, Lv0 Depletion, Core Recycling)            |
+-----------------------------------------------------------------------+
```

### Tier 1: Core Conservation & Mathematical Invariants
- **Scope**: Rigorous conservation laws across all game zones.
- **Invariant**:
  $$\text{Void} + \sum_{p \in \{1, 2\}} (\text{Life}_p + \text{Reserve}_p + \text{Field}_p + \text{Trash}_p) = \text{CONSTANT}$$
- **Mechanics Verified**:
  - Core generation strictly via Core Step ($\text{Void} \to \text{Reserve}$).
  - Cost payment ($\text{Reserve/Field} \to \text{Trash}$).
  - Refresh recycling ($\text{Trash} \to \text{Reserve}$).
  - Lv0 depletion core salvage ($\text{Field} \to \text{Reserve}$, card $\to \text{Trash}$).
  - Battle destruction core salvage ($\text{Field} \to \text{Reserve}$, card $\to \text{Trash}$).
  - Unblocked battle damage ($\text{Life} \to \text{Reserve}$).

### Tier 2: Step Sequences & Phase Transition Rules
- **Scope**: Valid state transitions following the official 10-step Battle Spirits turn sequence.
- **Progression**:
  $$\text{Start} \to \text{Core} \to \text{Draw} \to \text{Refresh} \to \text{Main} \to \text{Attack} \to \text{Flash} \to \text{Block} \to \text{Battle} \to \text{End}$$
- **Rules Verified**:
  - **Turn 1 Player 1 Restrictions**: Core step skipped (+0 core), Attack step prohibited (forced transition to End step).
  - **Card Draw**: Deck $\to$ Hand (+1 card), handling Deck = 0 (game does not crash; loss checked at Start step).
  - **Refresh Step**: All friendly exhausted spirits awakened; all trash cores moved to reserve.
  - **Mandatory Cost Reduction**: Field symbols discount card cost down to minimum cost; overpayment is strictly illegal.
  - **Level & BP Calculation**: Dynamic computation based on core count on spirit slot.

### Tier 3: Combat, Flash Priority & Battle Resolution
- **Scope**: Tactical attack phase and battle interaction mechanics.
- **Mechanics Verified**:
  - **Attack Declaration**: Refreshed spirit is exhausted; enters attack state.
  - **Flash Priority Sequence**: Defender-first initiative. Alternating passes. Two consecutive passes terminate flash window.
  - **Block Declaration**: Defending player exhausts refreshed spirit to intercept.
  - **BP Comparison Resolution**:
    - Higher BP destroys lower BP spirit; winner survives; 0 life damage.
    - Equal BP (Tie) destroys both spirits; attached cores return to respective reserves; 0 life damage.
  - **Blocker Removal Edge Case**: If blocker is destroyed or removed during Flash 2, block remains established. Attacking spirit does not deal life damage; BP comparison is skipped.
  - **Unblocked Attack Resolution**: Deals symbol count damage to defending player's life. Cores move from Life to Defender Reserve. If life reaches 0, immediate attacker victory.

### Tier 4: Multi-Turn End-to-End Scenarios
- **Scope**: Multi-turn sequential game flows from turn 1 through endgame conditions.
- **Scenarios Verified**:
  - **Scenario 1 (Opening Game Flow)**: Turn 1 P1 restrictions honored $\to$ Turn 2 P2 Core/Draw/Summon $\to$ Turn 3 P1 Core/Draw/Summon/Attack.
  - **Scenario 2 (Combat & Mutual Destruction)**: Multi-turn buildup $\to$ attack with block $\to$ BP tie $\to$ both destroyed $\to$ core recycling in next refresh.
  - **Scenario 3 (Lethal Life Damage)**: Consecutive unblocked attacks reducing opponent life from 5 to 0 $\to$ Immediate victory trigger.
  - **Scenario 4 (Deckout Loss)**: Deck exhausted $\to$ turn cycle advances to Start Step with 0 cards in deck $\to$ immediate deckout loss.

---

## 2. Comprehensive Coverage Matrix

### 2.1 Feature Inventory Mapping (29 Features from Spec Miner)

| Feature ID | Feature Description | Tier | Test File | Test Case Name |
|---|---|---|---|---|
| F1 | Game Initialization (Deck 40, Life 5, Reserve 4, Hand 4) | T1 | `test_invariants.bend` | `test_init_game_state` |
| F2 | Core Total Conservation Check | T1 | `test_invariants.bend` | `test_core_conservation_all_steps` |
| F3 | Start Step Resolution (Turn handover, deckout check) | T2 | `test_rules.bend` | `test_start_step_resolution` |
| F4 | Core Step Allocation (Void -> Reserve, skip T1 P1) | T2 | `test_rules.bend` | `test_core_step_allocation` |
| F5 | Draw Step Card Draw (Deck -> Hand, deckout survival) | T2 | `test_rules.bend` | `test_draw_step` |
| F6 | Refresh Step Awakening (Exhausted -> Refreshed) | T2 | `test_rules.bend` | `test_refresh_step_awakening` |
| F7 | Refresh Step Core Collection (Trash -> Reserve) | T1 | `test_invariants.bend` | `test_refresh_core_recycling` |
| F8 | Main Step Action Dispatch | T2 | `test_rules.bend` | `test_main_step_actions` |
| F9 | Mandatory Symbol Cost Reduction | T2 | `test_rules.bend` | `test_mandatory_cost_reduction` |
| F10 | Spirit Summoning (Cost to trash, Lv1 cores on card) | T2 | `test_rules.bend` | `test_spirit_summoning` |
| F11 | Nexus Placement | T2 | `test_rules.bend` | `test_nexus_placement` |
| F12 | Magic Main Cast | T2 | `test_rules.bend` | `test_magic_cast` |
| F13 | Free Core Reallocation (Reserve <-> Field) | T1 | `test_invariants.bend` | `test_free_core_reallocation` |
| F14 | Dynamic Level & BP Update | T2 | `test_rules.bend` | `test_level_bp_calculation` |
| F15 | Lv0 Spirit Depletion (Card to trash, cores to reserve) | T1 | `test_invariants.bend` | `test_lv0_depletion_to_reserve` |
| F16 | Attack Step Transition (Skip T1 P1) | T2 | `test_rules.bend` | `test_attack_step_transition` |
| F17 | Attack Declaration (Exhaust 1 spirit) | T3 | `test_rules.bend` | `test_attack_declaration` |
| F18 | Flash Timing 1 (Post-Attack, Defender-first) | T3 | `test_rules.bend` | `test_flash_timing_priority` |
| F19 | Block Declaration (Exhaust defender spirit) | T3 | `test_rules.bend` | `test_block_declaration` |
| F20 | Flash Timing 2 (Post-Block, Defender-first) | T3 | `test_rules.bend` | `test_flash_timing_2` |
| F21 | Battle Resolution: BP Comparison (Win/Loss/Tie) | T3 | `test_rules.bend` | `test_bp_comparison_resolution` |
| F22 | Battle Resolution: Unblocked Life Damage | T3 | `test_rules.bend` | `test_unblocked_life_damage` |
| F23 | Spirit Destruction Handling (Card to trash, cores to reserve) | T1 | `test_invariants.bend` | `test_destruction_cores_to_reserve` |
| F24 | Battle End Cleanup | T3 | `test_rules.bend` | `test_battle_end_cleanup` |
| F25 | End Step Turn Turnover | T2 | `test_rules.bend` | `test_end_step_turnover` |
| F26 | Immediate Life Loss Check (Life 0 -> instant loss) | T3 | `test_rules.bend` | `test_immediate_life_loss` |
| F27 | Deckout Loss Check (Start step deck 0 -> loss) | T4 | `test_scenarios.bend` | `test_scenario_deckout_loss` |
| F28 | Legal Move Generator | T2 | `test_rules.bend` | `test_legal_moves_step` |
| F29 | Deterministic Forward Step | T2 | `test_rules.bend` | `test_deterministic_forward_step` |

### 2.2 Edge Case Mapping (18 Edge Cases from Spec Miner)

| Edge Case ID | Edge Case Description | Tier | Test File | Test Assertion |
|---|---|---|---|---|
| E1 | Turn 1 Player 1 Core Step skipped (+0 core from Void) | T2 | `test_rules.bend` | `assert_t1_p1_core_skip` |
| E2 | Turn 1 Player 1 Attack Step prohibited (Main -> End) | T2 | `test_rules.bend` | `assert_t1_p1_attack_prohibited` |
| E3 | Turn 1 Player 1 Draw Step draws 1 card | T2 | `test_rules.bend` | `assert_t1_p1_draw_handled` |
| E4 | Cost Reduction exceeds card cost (min effective cost = 0, no gain) | T2 | `test_rules.bend` | `assert_reduction_floor_zero` |
| E5 | Mandatory reduction enforcement (overpayment rejected) | T2 | `test_rules.bend` | `assert_mandatory_reduction` |
| E6 | Summon cost paid with all cores of existing spirit (Lv0 vanish) | T1 | `test_invariants.bend` | `assert_summon_cores_vanish` |
| E7 | Spirit summoned with <Lv1 maintenance cores immediately vanishes | T1 | `test_invariants.bend` | `assert_under_core_vanish` |
| E8 | Destroyed spirit cores return to Reserve (NOT Trash) | T1 | `test_invariants.bend` | `assert_destroyed_cores_to_reserve` |
| E9 | Flash 1 Attacker removal stops attack with 0 life damage | T3 | `test_rules.bend` | `assert_flash1_attacker_removed` |
| E10 | Flash 2 Blocker removal maintains block; 0 life damage, BP skip | T3 | `test_rules.bend` | `assert_flash2_blocker_removed` |
| E11 | Attacker refreshed during Flash retains attacking state | T3 | `test_rules.bend` | `assert_flash_attacker_refresh` |
| E12 | BP Comparison Tie destroys both spirits, cores to reserve, 0 life damage | T3 | `test_rules.bend` | `assert_bp_tie_mutual_destruction` |
| E13 | Deck reaches 0 during Draw step does not instantly lose; loses at Start | T4 | `test_scenarios.bend` | `assert_deckout_start_step_loss` |
| E14 | Flash pass sequence: one side passes, other acts, first side regains priority | T3 | `test_rules.bend` | `assert_flash_pass_reactivation` |
| E15 | Flash initiative begins with defending player | T3 | `test_rules.bend` | `assert_flash_defender_first` |
| E16 | Exhausted spirit cannot declare block | T3 | `test_rules.bend` | `assert_exhausted_block_illegal` |
| E17 | Nexus cannot declare attack | T3 | `test_rules.bend` | `assert_nexus_attack_illegal` |
| E18 | Conservation invariant holds before and after EVERY single action | T1 | `test_invariants.bend` | `assert_strict_conservation_all` |

---

## 3. Test Harness Execution & Interface Conventions

### 3.1 Test Execution Architecture
In Bend, each test file is a standalone executable containing a `main` function.
Tests follow an assertion accumulator pattern:
- A test function evaluates a predicate.
- If an assertion fails, the runner aggregates failure counts and returns a non-zero exit code or fails with a diagnostic code.
- If all assertions pass, `main` returns `0`.

```bend
def main:
  # Run all test suites
  failures = 0
  failures = failures + test_init_game_state()
  failures = failures + test_core_conservation_all_steps()
  # ...
  if failures == 0:
    return 0 # All passed
  else:
    return failures # Number of failed test cases
```

### 3.2 WSL2 Test Execution Commands
The test suite can be run via WSL2 with either interpreter or compiled C runtime:
```bash
# Tier 1 & 2 Invariants:
wsl bash -c "cd battle_spirits_bend && bend run tests/test_invariants.bend"

# Tier 2 & 3 Rules & Combat:
wsl bash -c "cd battle_spirits_bend && bend run tests/test_rules.bend"

# Tier 4 Multi-Turn End-to-End Scenarios:
wsl bash -c "cd battle_spirits_bend && bend run tests/test_scenarios.bend"
```

---

## 4. Anti-Vacuity & Test Integrity Rules
1. **Explicit Pre/Post Verification**: Tests check exact counts before and after every action. Never assert tautologies (e.g. `1 == 1`).
2. **Zone Conservation Accounting**: Every state change explicitly tracks all 5 core zones ($\Delta\text{Void} + \Delta\text{Life} + \Delta\text{Reserve} + \Delta\text{Field} + \Delta\text{Trash} == 0$).
3. **Independent Reference Derivation**: Expected outputs are derived directly from the official Battle Spirits Standard Rules Manual and `BSTools/model/src/Rules.kt`.
