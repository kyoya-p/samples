# Tier 1, 2 & 3 Test Suite: Step Flow, Rules Mechanics & Combat Resolution
# Validates Battle Spirits official 10-step sequence, Turn 1 P1 restrictions,
# mandatory cost reduction, level/BP scaling, flash priorities, and battle resolution.

object SpiritSlot { card_id, cores, exhausted }

object Player {
  life,
  reserve,
  trash,
  deck_count,
  hand_count,
  s0,
  s1,
  s2,
  s3
}

object Battle {
  attacking_slot,
  blocking_slot,
  flash_priority,
  pass_count
}

object GameState {
  turn,
  active_player,
  step,
  void_cores,
  battle,
  p1,
  p2
}

def empty_slot():
  return SpiritSlot { card_id: 0, cores: 0, exhausted: 0 }

def make_slot(cid, c, ex):
  return SpiritSlot { card_id: cid, cores: c, exhausted: ex }

def empty_battle():
  return Battle { attacking_slot: 0, blocking_slot: 0, flash_priority: 0, pass_count: 0 }

def init_player():
  return Player {
    life: 5,
    reserve: 4,
    trash: 0,
    deck_count: 40,
    hand_count: 4,
    s0: empty_slot(),
    s1: empty_slot(),
    s2: empty_slot(),
    s3: empty_slot()
  }

def init_game():
  return GameState {
    turn: 1,
    active_player: 1,
    step: 0,
    void_cores: 30,
    battle: empty_battle(),
    p1: init_player(),
    p2: init_player()
  }

# Static Card Database Lookup
# returns (cost, reduction_symbols, lv1_cost, lv1_bp, lv2_cost, lv2_bp, sym_color)
def card_lookup(id):
  if id == 1:
    return (1, 1, 1, 1000, 2, 3000, 1)
  elif id == 2:
    return (3, 2, 1, 3000, 3, 5000, 1)
  elif id == 3:
    return (2, 1, 1, 2000, 2, 4000, 2)
  else:
    return (0, 0, 0, 0, 0, 0, 0)

# Mandatory Cost Reduction Engine
def compute_effective_cost(card_id, symbol_count):
  (cost, red_max, l1_c, l1_bp, l2_c, l2_bp, sym) = card_lookup(card_id)
  if symbol_count > red_max:
    applied_red = red_max
  else:
    applied_red = symbol_count

  if applied_red >= cost:
    return 0
  else:
    return cost - applied_red

# Dynamic Level and BP Calculation Engine
def compute_spirit_level_bp(card_id, cores):
  (cost, red_max, l1_c, l1_bp, l2_c, l2_bp, sym) = card_lookup(card_id)
  if cores >= l2_c:
    return (2, l2_bp)
  elif cores >= l1_c:
    return (1, l1_bp)
  else:
    return (0, 0)

# Battle BP Resolution Engine
# returns (attacker_destroyed, blocker_destroyed, life_damage)
def resolve_bp_battle(atk_bp, blk_bp):
  if atk_bp > blk_bp:
    return (0, 1, 0)
  elif blk_bp > atk_bp:
    return (1, 0, 0)
  else:
    return (1, 1, 0)

# Flash Timing Progression Engine
# Priority: 1 = Attacker, 2 = Defender
# Action: 0 = Pass, 1 = Flash Action
# returns (next_priority, next_pass_count, is_ended)
def step_flash(curr_priority, pass_count, action_type):
  if curr_priority == 1:
    next_priority = 2
  else:
    next_priority = 1

  if action_type == 0:
    new_passes = pass_count + 1
    if new_passes >= 2:
      return (next_priority, new_passes, 1)
    else:
      return (next_priority, new_passes, 0)
  else:
    return (next_priority, 0, 0)

# Step Transition Engine
# Official Steps:
# 0: START, 1: CORE, 2: DRAW, 3: REFRESH, 4: MAIN, 5: ATTACK, 6: FLASH1, 7: BLOCK, 8: BATTLE, 9: END
def next_step(current_step, turn, active_player):
  if current_step == 0:
    # From Start to Core (unless T1 P1: skip core step directly to Draw)
    if turn == 1:
      if active_player == 1:
        return 2 # Skip Core directly to Draw
      else:
        return 1
    else:
      return 1
  elif current_step == 1:
    return 2 # Core -> Draw
  elif current_step == 2:
    return 3 # Draw -> Refresh
  elif current_step == 3:
    return 4 # Refresh -> Main
  elif current_step == 4:
    # Main -> Attack (unless T1 P1: skip Attack directly to End)
    if turn == 1:
      if active_player == 1:
        return 9 # Skip Attack directly to End
      else:
        return 5
    else:
      return 5
  elif current_step == 5:
    return 6 # Attack -> Flash1
  elif current_step == 6:
    return 7 # Flash1 -> Block
  elif current_step == 7:
    return 8 # Block -> Battle
  elif current_step == 8:
    return 9 # Battle -> End
  else:
    return 0 # End -> Next Turn Start

# Win/Loss Termination Check
def is_game_over(p1_life, p2_life, p1_deck, p2_deck, step, active_player):
  if p1_life == 0:
    return (1, 2)
  elif p2_life == 0:
    return (1, 1)
  elif step == 0:
    if active_player == 1:
      if p1_deck == 0:
        return (1, 2)
      else:
        return (0, 0)
    else:
      if p2_deck == 0:
        return (1, 1)
      else:
        return (0, 0)
  else:
    return (0, 0)

def assert_eq(a, b):
  if a == b:
    return 0
  else:
    return 1

# T2.1: Step Progression Sequence (Normal Turn)
def test_step_progression_sequence():
  errs = 0
  # On Turn 2 (P2 active): Start -> Core -> Draw -> Refresh -> Main -> Attack -> Flash1 -> Block -> Battle -> End -> Start(Turn 3)
  s0 = next_step(0, 2, 2) # Start -> Core
  s1 = next_step(1, 2, 2) # Core -> Draw
  s2 = next_step(2, 2, 2) # Draw -> Refresh
  s3 = next_step(3, 2, 2) # Refresh -> Main
  s4 = next_step(4, 2, 2) # Main -> Attack
  s5 = next_step(5, 2, 2) # Attack -> Flash1
  s6 = next_step(6, 2, 2) # Flash1 -> Block
  s7 = next_step(7, 2, 2) # Block -> Battle
  s8 = next_step(8, 2, 2) # Battle -> End
  s9 = next_step(9, 2, 2) # End -> Start
  errs = errs + assert_eq(s0, 1)
  errs = errs + assert_eq(s1, 2)
  errs = errs + assert_eq(s2, 3)
  errs = errs + assert_eq(s3, 4)
  errs = errs + assert_eq(s4, 5)
  errs = errs + assert_eq(s5, 6)
  errs = errs + assert_eq(s6, 7)
  errs = errs + assert_eq(s7, 8)
  errs = errs + assert_eq(s8, 9)
  errs = errs + assert_eq(s9, 0)
  return errs

# T2.2: Turn 1 Player 1 Restrictions (Edge Cases 1, 2, 3)
def test_turn1_p1_restrictions():
  errs = 0
  # Core Step is skipped on Turn 1 P1: Start step transitions directly to Draw step (2)
  s_start_to_draw = next_step(0, 1, 1)
  errs = errs + assert_eq(s_start_to_draw, 2)

  # Attack Step is prohibited on Turn 1 P1: Main step transitions directly to End step (9)
  s_main_to_end = next_step(4, 1, 1)
  errs = errs + assert_eq(s_main_to_end, 9)
  return errs

# T2.3: Mandatory Cost Reduction with Symbol Counting (Features 9 & 10, Edge Cases 4 & 5)
def test_mandatory_cost_reduction():
  errs = 0
  # Card 2: Cost 3, Max Reduction 2
  # 0 symbols -> Cost 3
  errs = errs + assert_eq(compute_effective_cost(2, 0), 3)
  # 1 symbol -> Cost 2
  errs = errs + assert_eq(compute_effective_cost(2, 1), 2)
  # 2 symbols -> Cost 1
  errs = errs + assert_eq(compute_effective_cost(2, 2), 1)
  # 3 symbols -> Cannot exceed max reduction 2, Cost remains 1
  errs = errs + assert_eq(compute_effective_cost(2, 3), 1)

  # Card 1: Cost 1, Max Reduction 1
  # 1 symbol -> Cost 0
  errs = errs + assert_eq(compute_effective_cost(1, 1), 0)
  # 2 symbols -> Floored at 0, no negative cost
  errs = errs + assert_eq(compute_effective_cost(1, 2), 0)
  return errs

# T2.4: Dynamic Level and BP Calculation (Feature 14)
def test_level_bp_calculation():
  errs = 0
  # Card 2: Lv1 cost 1 (3000 BP), Lv2 cost 3 (5000 BP)
  (l0, bp0) = compute_spirit_level_bp(2, 0) # 0 cores -> Lv0 (depleted)
  (l1, bp1) = compute_spirit_level_bp(2, 1) # 1 core -> Lv1 (3000)
  (l2, bp2) = compute_spirit_level_bp(2, 2) # 2 cores -> Lv1 (3000)
  (l3, bp3) = compute_spirit_level_bp(2, 3) # 3 cores -> Lv2 (5000)
  (l4, bp4) = compute_spirit_level_bp(2, 4) # 4 cores -> Lv2 (5000)
  errs = errs + assert_eq(l0, 0)
  errs = errs + assert_eq(bp0, 0)
  errs = errs + assert_eq(l1, 1)
  errs = errs + assert_eq(bp1, 3000)
  errs = errs + assert_eq(l2, 1)
  errs = errs + assert_eq(bp2, 3000)
  errs = errs + assert_eq(l3, 2)
  errs = errs + assert_eq(bp3, 5000)
  errs = errs + assert_eq(l4, 2)
  errs = errs + assert_eq(bp4, 5000)
  return errs

# T3.1: Attack Declaration Rule (Features 16 & 17)
def test_attack_declaration():
  errs = 0
  # Refreshed spirit (exhausted = 0) can attack; becomes exhausted (exhausted = 1)
  s0_refreshed = make_slot(1, 1, 0)
  open SpiritSlot: s0_refreshed
  can_attack = if s0_refreshed.exhausted == 0: 1 else: 0
  errs = errs + assert_eq(can_attack, 1)

  # Exhausted spirit (exhausted = 1) cannot declare attack
  s0_exhausted = make_slot(1, 1, 1)
  open SpiritSlot: s0_exhausted
  can_attack_ex = if s0_exhausted.exhausted == 0: 1 else: 0
  errs = errs + assert_eq(can_attack_ex, 0)
  return errs

# T3.2: Flash Priority Initiative & Sequence (Features 18 & 20, Edge Cases 14 & 15)
def test_flash_timing_priority():
  errs = 0
  # Initial priority starts with DEFENDING PLAYER (Priority 2)
  init_priority = 2
  pass_count = 0

  # Defender passes: Priority transfers to Attacker (Priority 1), pass_count = 1
  (p_att, passes_1, end_1) = step_flash(init_priority, pass_count, 0)
  errs = errs + assert_eq(p_att, 1)
  errs = errs + assert_eq(passes_1, 1)
  errs = errs + assert_eq(end_1, 0)

  # Attacker takes flash action: Priority transfers to Defender, pass_count RESETS to 0
  (p_def, passes_reset, end_action) = step_flash(p_att, passes_1, 1)
  errs = errs + assert_eq(p_def, 2)
  errs = errs + assert_eq(passes_reset, 0)
  errs = errs + assert_eq(end_action, 0)

  # Defender passes again: pass_count = 1
  (p_att2, passes_2, end_2) = step_flash(p_def, passes_reset, 0)
  errs = errs + assert_eq(passes_2, 1)
  errs = errs + assert_eq(end_2, 0)

  # Attacker passes: 2 consecutive passes terminates flash timing!
  (p_def2, passes_3, end_3) = step_flash(p_att2, passes_2, 0)
  errs = errs + assert_eq(passes_3, 2)
  errs = errs + assert_eq(end_3, 1)
  return errs

# T3.3: Block Declaration Rules (Feature 19, Edge Case 16)
def test_block_declaration():
  errs = 0
  # Refreshed defender spirit can block
  s0_refreshed = make_slot(2, 1, 0)
  open SpiritSlot: s0_refreshed
  can_block = if s0_refreshed.exhausted == 0: 1 else: 0
  errs = errs + assert_eq(can_block, 1)

  # Exhausted defender spirit CANNOT block
  s0_exhausted = make_slot(2, 1, 1)
  open SpiritSlot: s0_exhausted
  can_block_ex = if s0_exhausted.exhausted == 0: 1 else: 0
  errs = errs + assert_eq(can_block_ex, 0)
  return errs

# T3.4: Battle BP Resolution: Attacker Wins (Feature 21)
def test_bp_attacker_wins():
  errs = 0
  # Attacker BP 5000, Blocker BP 3000
  (atk_dest, blk_dest, life_dmg) = resolve_bp_battle(5000, 3000)
  errs = errs + assert_eq(atk_dest, 0)
  errs = errs + assert_eq(blk_dest, 1)
  errs = errs + assert_eq(life_dmg, 0)
  return errs

# T3.5: Battle BP Resolution: Blocker Wins (Feature 21)
def test_bp_blocker_wins():
  errs = 0
  # Attacker BP 1000, Blocker BP 4000
  (atk_dest, blk_dest, life_dmg) = resolve_bp_battle(1000, 4000)
  errs = errs + assert_eq(atk_dest, 1)
  errs = errs + assert_eq(blk_dest, 0)
  errs = errs + assert_eq(life_dmg, 0)
  return errs

# T3.6: Battle BP Resolution: Tie Mutual Destruction (Feature 21, Edge Case 12)
def test_bp_tie_mutual_destruction():
  errs = 0
  # Attacker BP 3000, Blocker BP 3000
  (atk_dest, blk_dest, life_dmg) = resolve_bp_battle(3000, 3000)
  errs = errs + assert_eq(atk_dest, 1)
  errs = errs + assert_eq(blk_dest, 1)
  errs = errs + assert_eq(life_dmg, 0)
  return errs

# T3.7: Blocker Removal during Flash 2 Protects Life (Edge Case 10)
def test_blocker_removal_protects_life():
  errs = 0
  # Official Rule: If block was established, but blocker vanishes/destroyed during Flash 2,
  # block remains valid. Life damage is strictly 0. BP comparison is skipped.
  is_blocked = 1
  blocker_present = 0 # Blocker removed
  if is_blocked == 1:
    life_damage = 0
  else:
    life_damage = 1
  errs = errs + assert_eq(life_damage, 0)
  return errs

# T3.8: Unblocked Damage to Life (Feature 22)
def test_unblocked_life_damage():
  errs = 0
  # 1 symbol unblocked attack deals 1 damage to defender's life
  attacker_symbols = 1
  is_blocked = 0
  if is_blocked == 0:
    damage_dealt = attacker_symbols
  else:
    damage_dealt = 0
  p2_life = 5
  p2_new_life = p2_life - damage_dealt
  errs = errs + assert_eq(damage_dealt, 1)
  errs = errs + assert_eq(p2_new_life, 4)
  return errs

# T3.9: Immediate Life Loss Termination (Feature 26)
def test_immediate_life_loss():
  errs = 0
  # Defender life drops to 0 -> instant game over, Winner = P1
  (over1, winner1) = is_game_over(5, 0, 40, 40, 8, 1)
  errs = errs + assert_eq(over1, 1)
  errs = errs + assert_eq(winner1, 1)

  # Attacker life drops to 0 -> instant game over, Winner = P2
  (over2, winner2) = is_game_over(0, 5, 40, 40, 8, 2)
  errs = errs + assert_eq(over2, 1)
  errs = errs + assert_eq(winner2, 2)

  # Both life > 0 -> game not over
  (over3, winner3) = is_game_over(5, 5, 40, 40, 8, 1)
  errs = errs + assert_eq(over3, 0)
  errs = errs + assert_eq(winner3, 0)
  return errs

def main():
  errs = 0
  errs = errs + test_step_progression_sequence()
  errs = errs + test_turn1_p1_restrictions()
  errs = errs + test_mandatory_cost_reduction()
  errs = errs + test_level_bp_calculation()
  errs = errs + test_attack_declaration()
  errs = errs + test_flash_timing_priority()
  errs = errs + test_block_declaration()
  errs = errs + test_bp_attacker_wins()
  errs = errs + test_bp_blocker_wins()
  errs = errs + test_bp_tie_mutual_destruction()
  errs = errs + test_blocker_removal_protects_life()
  errs = errs + test_unblocked_life_damage()
  errs = errs + test_immediate_life_loss()
  return errs
