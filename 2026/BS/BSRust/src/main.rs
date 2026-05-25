use serde::{Deserialize, Serialize};
use std::io::{self, Write};

#[derive(Debug, Serialize, Deserialize, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Color {
    Red,
    Purple,
    Green,
    White,
    Yellow,
    Blue,
    None,
}

#[derive(Debug, Serialize, Deserialize, Clone, Copy, PartialEq, Eq)]
pub enum CardType {
    Spirit,
    Nexus,
    Magic,
    Brave,
    Ultimate,
}

/// コアの構成 (合計数, ソウルコア数)
#[derive(Debug, Serialize, Deserialize, Clone, Copy, PartialEq, Eq)]
pub struct Cores {
    pub total: u8,
    pub soul: u8, // 0 or 1
}

impl Cores {
    pub fn new(total: u8, soul: u8) -> Self {
        Self { total, soul }
    }
    pub fn normal(&self) -> u8 {
        self.total - self.soul
    }
    pub fn format(&self) -> String {
        let normal = self.normal();
        if self.soul == 0 {
            format!("{}", normal)
        } else if normal == 0 {
            "ソウル:1".to_string()
        } else {
            format!("{}, ソウル:1", normal)
        }
    }
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Card {
    pub id: String,
    pub name: String,
    pub base_cost: u8,
    pub colors: Vec<Color>,
    pub reduction_symbols: Vec<Color>,
    pub card_type: CardType,
    pub lv_costs: Vec<u8>,
    pub symbols: Vec<Color>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct FieldObject {
    pub id: String,
    pub name: String,
    pub colors: Vec<Color>,
    pub card_type: CardType,
    pub cores: Cores,
    pub is_exhausted: bool,
    pub lv_costs: Vec<u8>,
    pub base_symbols: Vec<Color>,
}

impl FieldObject {
    pub fn current_lv(&self) -> u8 {
        if self.card_type != CardType::Spirit && self.card_type != CardType::Ultimate {
            return 1;
        }
        let core_count = self.cores.total;
        let mut lv = 0;
        for (i, &cost) in self.lv_costs.iter().enumerate() {
            if core_count >= cost {
                lv = (i + 1) as u8;
            }
        }
        lv
    }

    pub fn active_symbols(&self) -> Vec<Color> {
        if self.current_lv() == 0 {
            return vec![];
        }
        self.base_symbols.clone()
    }
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct SideState {
    pub player_id: u8,
    pub life: u8,
    pub reserve: Cores,
    pub field: Vec<FieldObject>,
    pub hand: Vec<Card>,
    pub trash: Vec<Card>,
    pub trash_cores: Cores, // トラッシュのコア
    pub opened: Vec<Card>,
    pub count: u8,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct GameState {
    pub player: SideState,
    pub opponent: SideState,
    pub phase: Phase,
    pub turn_count: u32,
    pub active_attacker: Option<String>,
    pub active_blocker: Option<String>,
}

#[derive(Debug, Serialize, Deserialize, Clone, Copy, PartialEq, Eq)]
pub enum Phase {
    StartStep,
    CoreStep,
    DrawStep,
    RefreshStep,
    MainStep,
    AttackStep(AttackSubPhase),
    EndStep,
}

#[derive(Debug, Serialize, Deserialize, Clone, Copy, PartialEq, Eq)]
pub enum AttackSubPhase {
    DeclareAttack,
    AttackFlash {
        priority: Priority,
        consecutive_passes: u8,
    },
    DeclareBlock,
    BlockFlash {
        priority: Priority,
        consecutive_passes: u8,
    },
    BattleResolution,
    End,
}

#[derive(Debug, Serialize, Deserialize, Clone, Copy, PartialEq, Eq)]
pub enum Priority {
    Attacker,
    Defender,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Clone)]
pub struct CoreSource {
    pub source_id: String, // "Reserve" or FieldObject ID
    pub count: u8,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Clone)]
pub enum Action {
    PlayCard {
        card_id: String,
        payment: Vec<CoreSource>,
        use_soul_core: bool,
        placement: Vec<CoreSource>,
        placement_soul_core: bool,
    },
    MoveCore {
        from: String,
        to: String,
        normal_cores: u8,
        soul_core: bool,
    },
    Attack { object_id: String },
    Block { object_id: String },
    UseActiveEffect {
        object_id: String,
        effect_name: String,
        payment: Vec<CoreSource>,
        use_soul_core: bool,
    },
    Pass,
    EndStep,
}

#[derive(Clone, Debug)]
struct CorePool {
    id: String,
    normal: u8,
    soul: u8,
}

fn has_soul_core_in_pool(pools: &[CorePool]) -> bool {
    pools.iter().any(|p| p.soul > 0)
}

fn calculate_reduction(card: &Card, field: &[FieldObject]) -> u8 {
    let mut available_symbols = Vec::new();
    for obj in field {
        available_symbols.extend(obj.active_symbols());
    }

    let mut reduction = 0;
    let reduction_req = card.reduction_symbols.clone();

    for req_color in reduction_req {
        if let Some(pos) = available_symbols.iter().position(|&col| col == req_color) {
            reduction += 1;
            available_symbols.remove(pos);
        }
    }
    reduction
}

fn get_payment_combinations(
    pools: &[CorePool],
    required: u8,
    use_soul: bool,
) -> Vec<(Vec<CoreSource>, Vec<CorePool>)> {
    let mut results = Vec::new();
    let mut current_payment = Vec::new();
    
    if use_soul {
        for i in 0..pools.len() {
            if pools[i].soul > 0 {
                let mut temp_remaining = pools.to_vec();
                temp_remaining[i].soul -= 1;
                
                let mut sub_results = Vec::new();
                search_normal_combinations(&temp_remaining, required, 0, &mut current_payment, &mut sub_results);
                for (pay, rem) in sub_results {
                    results.push((pay, rem));
                }
            }
        }
    } else {
        search_normal_combinations(pools, required, 0, &mut current_payment, &mut results);
    }
    
    results
}

fn search_normal_combinations(
    pools: &[CorePool],
    required: u8,
    pool_index: usize,
    current: &mut Vec<CoreSource>,
    results: &mut Vec<(Vec<CoreSource>, Vec<CorePool>)>,
) {
    if required == 0 {
        results.push((current.clone(), pools.to_vec()));
        return;
    }
    if pool_index >= pools.len() {
        return;
    }

    let pool = &pools[pool_index];
    let max_take = pool.normal.min(required);
    for take in 0..=max_take {
        if take > 0 {
            current.push(CoreSource {
                source_id: pool.id.clone(),
                count: take,
            });
        }
        
        let mut next_pools = pools.to_vec();
        next_pools[pool_index].normal -= take;
        
        search_normal_combinations(
            &next_pools,
            required - take,
            pool_index + 1,
            current,
            results,
        );
        
        if take > 0 {
            current.pop();
        }
    }
}

fn get_placement_combinations(
    pools: &[CorePool],
    required: u8,
    place_soul: bool,
) -> Vec<Vec<CoreSource>> {
    let mut results = Vec::new();
    let mut current_placement = Vec::new();
    
    if place_soul {
        for i in 0..pools.len() {
            if pools[i].soul > 0 {
                let mut temp_pools = pools.to_vec();
                temp_pools[i].soul -= 1;
                
                let mut sub_results = Vec::new();
                search_placement_combinations(&temp_pools, required, 0, &mut current_placement, &mut sub_results);
                results.extend(sub_results);
            }
        }
    } else {
        search_placement_combinations(pools, required, 0, &mut current_placement, &mut results);
    }
    results
}

fn search_placement_combinations(
    pools: &[CorePool],
    required: u8,
    pool_index: usize,
    current: &mut Vec<CoreSource>,
    results: &mut Vec<Vec<CoreSource>>,
) {
    if required == 0 {
        results.push(current.clone());
        return;
    }
    if pool_index >= pools.len() {
        return;
    }

    let pool = &pools[pool_index];
    let max_take = pool.normal.min(required);
    for take in 0..=max_take {
        if take > 0 {
            current.push(CoreSource {
                source_id: pool.id.clone(),
                count: take,
            });
        }
        
        let mut next_pools = pools.to_vec();
        next_pools[pool_index].normal -= take;
        
        search_placement_combinations(
            &next_pools,
            required - take,
            pool_index + 1,
            current,
            results,
        );
        
        if take > 0 {
            current.pop();
        }
    }
}

/// コア移動後のスピリット消滅処理
pub fn check_and_process_depletion(side: &mut SideState) {
    let mut i = 0;
    while i < side.field.len() {
        let obj = &side.field[i];
        if obj.card_type == CardType::Spirit || obj.card_type == CardType::Ultimate {
            let lv1_cost = obj.lv_costs[0];
            if obj.cores.total < lv1_cost {
                let removed = side.field.remove(i);
                side.reserve.total += removed.cores.total;
                side.reserve.soul += removed.cores.soul;
                let card = Card {
                    id: removed.id.clone(),
                    name: removed.name.clone(),
                    base_cost: 0,
                    colors: removed.colors.clone(),
                    reduction_symbols: vec![],
                    card_type: removed.card_type,
                    lv_costs: removed.lv_costs.clone(),
                    symbols: removed.base_symbols.clone(),
                };
                side.trash.push(card);
                continue;
            }
        }
        i += 1;
    }
}

/// スタート〜リフレッシュまでの自動進行処理
pub fn process_automatic_steps(state: &mut GameState) {
    loop {
        match state.phase {
            Phase::StartStep => {
                state.phase = Phase::CoreStep;
            }
            Phase::CoreStep => {
                if !(state.turn_count == 1 && state.player.player_id == 1) {
                    state.player.reserve.total += 1;
                }
                state.phase = Phase::DrawStep;
            }
            Phase::DrawStep => {
                // ドローステップでのドローは先攻第1ターンでも実行される（契約編以降の最新ルールに準拠）
                if !state.player.opened.is_empty() {
                    let drawn = state.player.opened.remove(0);
                    state.player.hand.push(drawn);
                }
                state.phase = Phase::RefreshStep;
            }
            Phase::RefreshStep => {
                state.player.reserve.total += state.player.trash_cores.total;
                state.player.reserve.soul += state.player.trash_cores.soul;
                state.player.trash_cores = Cores::new(0, 0);
                for obj in &mut state.player.field {
                    obj.is_exhausted = false;
                }
                state.phase = Phase::MainStep;
            }
            _ => break,
        }
    }
}

/// ゲームルール処理エンジン（GameStateの遷移）
pub fn apply_action(state: &mut GameState, action: &Action) -> Result<(), String> {
    match (&state.phase, action) {
        (Phase::MainStep, Action::PlayCard { card_id, payment, use_soul_core, placement, placement_soul_core }) => {
            let card_idx = state.player.hand.iter().position(|c| &c.id == card_id)
                .ok_or_else(|| "Card not in hand".to_string())?;
            let card = state.player.hand.remove(card_idx);

            // コスト支払い処理
            for source in payment {
                if source.source_id == "Reserve" {
                    if state.player.reserve.normal() < source.count {
                        return Err("Not enough normal cores in reserve".to_string());
                    }
                    state.player.reserve.total -= source.count;
                    state.player.trash_cores.total += source.count;
                } else {
                    let obj = state.player.field.iter_mut().find(|o| &o.id == &source.source_id)
                        .ok_or_else(|| "Payment source object not found".to_string())?;
                    if obj.cores.normal() < source.count {
                        return Err("Not enough normal cores on object".to_string());
                    }
                    obj.cores.total -= source.count;
                    state.player.trash_cores.total += source.count;
                }
            }
            if *use_soul_core {
                let mut found_soul = false;
                if state.player.reserve.soul > 0 {
                    state.player.reserve.soul -= 1;
                    state.player.trash_cores.soul += 1;
                    found_soul = true;
                } else {
                    for obj in &mut state.player.field {
                        if obj.cores.soul > 0 {
                            obj.cores.soul -= 1;
                            state.player.trash_cores.soul += 1;
                            found_soul = true;
                            break;
                        }
                    }
                }
                if !found_soul {
                    return Err("Soul core not found for payment".to_string());
                }
            }

            // スピリット/アルティメット/ネクサスのフィールド配置
            if card.card_type == CardType::Spirit || card.card_type == CardType::Ultimate || card.card_type == CardType::Nexus {
                let mut placement_total = 0;
                let mut placement_soul = 0;
                for source in placement {
                    if source.source_id == "Reserve" {
                        if state.player.reserve.normal() < source.count {
                            return Err("Not enough normal cores in reserve for placement".to_string());
                        }
                        state.player.reserve.total -= source.count;
                        placement_total += source.count;
                    } else {
                        let obj = state.player.field.iter_mut().find(|o| &o.id == &source.source_id)
                            .ok_or_else(|| "Placement source object not found".to_string())?;
                        if obj.cores.normal() < source.count {
                            return Err("Not enough normal cores on object for placement".to_string());
                        }
                        obj.cores.total -= source.count;
                        placement_total += source.count;
                    }
                }
                if *placement_soul_core {
                    let mut found_soul = false;
                    if state.player.reserve.soul > 0 {
                        state.player.reserve.soul -= 1;
                        placement_soul += 1;
                        found_soul = true;
                    } else {
                        for obj in &mut state.player.field {
                            if obj.cores.soul > 0 {
                                obj.cores.soul -= 1;
                                placement_soul += 1;
                                found_soul = true;
                                break;
                            }
                        }
                    }
                    if !found_soul {
                        return Err("Soul core not found for placement".to_string());
                    }
                }

                let new_obj = FieldObject {
                    id: card.id.clone(),
                    name: card.name.clone(),
                    colors: card.colors.clone(),
                    card_type: card.card_type,
                    cores: Cores::new(placement_total + placement_soul, placement_soul),
                    is_exhausted: false,
                    lv_costs: card.lv_costs.clone(),
                    base_symbols: card.symbols.clone(),
                };
                state.player.field.push(new_obj);
            } else if card.card_type == CardType::Magic {
                state.player.trash.push(card);
            }

            check_and_process_depletion(&mut state.player);
        }
        (Phase::MainStep, Action::MoveCore { from, to, normal_cores, soul_core }) => {
            let mut move_soul = 0;
            let mut _move_normal = 0;
            if from == "Reserve" {
                if state.player.reserve.total < *normal_cores {
                    return Err("Not enough cores in reserve".to_string());
                }
                if *soul_core && state.player.reserve.soul == 0 {
                    return Err("Soul core not in reserve".to_string());
                }
                state.player.reserve.total -= *normal_cores;
                _move_normal = *normal_cores;
                if *soul_core {
                    state.player.reserve.soul -= 1;
                    move_soul = 1;
                }
            } else {
                let obj = state.player.field.iter_mut().find(|o| &o.id == from)
                    .ok_or_else(|| "Source object not found".to_string())?;
                if obj.cores.total < *normal_cores {
                    return Err("Not enough cores on source object".to_string());
                }
                if *soul_core && obj.cores.soul == 0 {
                    return Err("Soul core not on source object".to_string());
                }
                obj.cores.total -= *normal_cores;
                _move_normal = *normal_cores;
                if *soul_core {
                    obj.cores.soul -= 1;
                    move_soul = 1;
                }
            }

            if to == "Reserve" {
                state.player.reserve.total += _move_normal + move_soul;
                state.player.reserve.soul += move_soul;
            } else {
                let obj = state.player.field.iter_mut().find(|o| &o.id == to)
                    .ok_or_else(|| "Target object not found".to_string())?;
                obj.cores.total += _move_normal + move_soul;
                obj.cores.soul += move_soul;
            }

            check_and_process_depletion(&mut state.player);
        }
        (Phase::MainStep, Action::EndStep) => {
            if state.turn_count == 1 {
                state.phase = Phase::EndStep;
            } else {
                state.phase = Phase::AttackStep(AttackSubPhase::DeclareAttack);
            }
        }
        (Phase::AttackStep(AttackSubPhase::DeclareAttack), Action::Attack { object_id }) => {
            let obj = state.player.field.iter_mut().find(|o| &o.id == object_id)
                .ok_or_else(|| "Attacker object not found".to_string())?;
            if obj.is_exhausted {
                return Err("Attacker already exhausted".to_string());
            }
            obj.is_exhausted = true;
            state.active_attacker = Some(object_id.clone());
            state.phase = Phase::AttackStep(AttackSubPhase::AttackFlash {
                priority: Priority::Defender,
                consecutive_passes: 0,
            });
        }
        (Phase::AttackStep(AttackSubPhase::DeclareAttack), Action::EndStep) => {
            state.phase = Phase::EndStep;
        }
        (Phase::AttackStep(AttackSubPhase::AttackFlash { priority, consecutive_passes }), Action::Pass) => {
            let next_passes = consecutive_passes + 1;
            let next_priority = match priority {
                Priority::Attacker => Priority::Defender,
                Priority::Defender => Priority::Attacker,
            };
            if next_passes >= 2 {
                state.phase = Phase::AttackStep(AttackSubPhase::DeclareBlock);
            } else {
                state.phase = Phase::AttackStep(AttackSubPhase::AttackFlash {
                    priority: next_priority,
                    consecutive_passes: next_passes,
                });
            }
        }
        (Phase::AttackStep(AttackSubPhase::AttackFlash { priority, .. }), Action::PlayCard { card_id, payment, use_soul_core, .. }) => {
            let next_priority = match priority {
                Priority::Attacker => Priority::Defender,
                Priority::Defender => Priority::Attacker,
            };
            state.phase = Phase::AttackStep(AttackSubPhase::AttackFlash {
                priority: next_priority,
                consecutive_passes: 0,
            });
            let card_idx = state.player.hand.iter().position(|c| &c.id == card_id)
                .ok_or_else(|| "Card not in hand".to_string())?;
            let card = state.player.hand.remove(card_idx);
            state.player.trash.push(card);
            for source in payment {
                if source.source_id == "Reserve" {
                    state.player.reserve.total -= source.count;
                    state.player.trash_cores.total += source.count;
                }
            }
            if *use_soul_core {
                if state.player.reserve.soul > 0 {
                    state.player.reserve.soul -= 1;
                    state.player.trash_cores.soul += 1;
                }
            }
        }
        (Phase::AttackStep(AttackSubPhase::DeclareBlock), Action::Block { object_id }) => {
            let obj = state.player.field.iter_mut().find(|o| &o.id == object_id)
                .ok_or_else(|| "Blocker object not found".to_string())?;
            if obj.is_exhausted {
                return Err("Blocker already exhausted".to_string());
            }
            obj.is_exhausted = true;
            state.active_blocker = Some(object_id.clone());
            state.phase = Phase::AttackStep(AttackSubPhase::BlockFlash {
                priority: Priority::Defender,
                consecutive_passes: 0,
            });
        }
        (Phase::AttackStep(AttackSubPhase::DeclareBlock), Action::Pass) => {
            state.phase = Phase::AttackStep(AttackSubPhase::BattleResolution);
        }
        (Phase::AttackStep(AttackSubPhase::BlockFlash { priority, consecutive_passes }), Action::Pass) => {
            let next_passes = consecutive_passes + 1;
            let next_priority = match priority {
                Priority::Attacker => Priority::Defender,
                Priority::Defender => Priority::Attacker,
            };
            if next_passes >= 2 {
                state.phase = Phase::AttackStep(AttackSubPhase::BattleResolution);
            } else {
                state.phase = Phase::AttackStep(AttackSubPhase::BlockFlash {
                    priority: next_priority,
                    consecutive_passes: next_passes,
                });
            }
        }
        (Phase::AttackStep(AttackSubPhase::BattleResolution), _) => {
            if let Some(_attacker_id) = &state.active_attacker {
                if let Some(_blocker_id) = &state.active_blocker {
                    // ブロックあり戦闘
                } else {
                    // ノーブロック：ライフ減少
                    state.opponent.life = state.opponent.life.saturating_sub(1);
                    state.opponent.reserve.total += 1;
                }
            }
            state.active_attacker = None;
            state.active_blocker = None;
            state.phase = Phase::AttackStep(AttackSubPhase::End);
        }
        (Phase::AttackStep(AttackSubPhase::End), _) => {
            state.phase = Phase::AttackStep(AttackSubPhase::DeclareAttack);
        }
        (Phase::EndStep, Action::EndStep) => {
            let temp = state.player.clone();
            state.player = state.opponent.clone();
            state.opponent = temp;
            state.turn_count += 1;
            state.phase = Phase::StartStep;
            process_automatic_steps(state);
        }
        _ => return Err("Invalid action for current phase".to_string()),
    }
    Ok(())
}

/// 局面に応じた選択肢（合法手）の全列挙ロジック
pub fn generate_legal_actions(state: &GameState) -> Vec<Action> {
    let mut actions = Vec::new();

    match &state.phase {
        Phase::MainStep => {
            // 1. PlayCard アクション
            for card in &state.player.hand {
                let reduction = calculate_reduction(card, &state.player.field);
                let cost_to_pay = card.base_cost.saturating_sub(reduction);

                let mut pools = Vec::new();
                pools.push(CorePool {
                    id: "Reserve".to_string(),
                    normal: state.player.reserve.normal(),
                    soul: state.player.reserve.soul,
                });
                for obj in &state.player.field {
                    pools.push(CorePool {
                        id: obj.id.clone(),
                        normal: obj.cores.normal(),
                        soul: obj.cores.soul,
                    });
                }

                let total_available: u8 = pools.iter().map(|p| p.normal + p.soul).sum();
                let required_placement = if card.card_type == CardType::Spirit || card.card_type == CardType::Ultimate {
                    card.lv_costs[0]
                } else {
                    0
                };

                if total_available >= cost_to_pay + required_placement {
                    for use_soul in &[false, true] {
                        if *use_soul && !has_soul_core_in_pool(&pools) {
                            continue;
                        }
                        let required_normal = if *use_soul {
                            cost_to_pay.saturating_sub(1)
                        } else {
                            cost_to_pay
                        };

                        let payment_combinations = get_payment_combinations(&pools, required_normal, *use_soul);
                        for (payment, remaining_pools) in payment_combinations {
                            if required_placement == 0 {
                                actions.push(Action::PlayCard {
                                    card_id: card.id.clone(),
                                    payment: payment.clone(),
                                    use_soul_core: *use_soul,
                                    placement: vec![],
                                    placement_soul_core: false,
                                });
                            } else {
                                for place_soul in &[false, true] {
                                    if *place_soul && !has_soul_core_in_pool(&remaining_pools) {
                                        continue;
                                    }
                                    let req_place_normal = if *place_soul {
                                        required_placement.saturating_sub(1)
                                    } else {
                                        required_placement
                                    };

                                    let placement_combinations = get_placement_combinations(&remaining_pools, req_place_normal, *place_soul);
                                    for placement_item in placement_combinations {
                                        actions.push(Action::PlayCard {
                                            card_id: card.id.clone(),
                                            payment: payment.clone(),
                                            use_soul_core: *use_soul,
                                            placement: placement_item,
                                            placement_soul_core: *place_soul,
                                        });
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. MoveCore アクション
            let mut core_locations = vec!["Reserve".to_string()];
            for obj in &state.player.field {
                core_locations.push(obj.id.clone());
            }

            for from in &core_locations {
                for to in &core_locations {
                    if from == to {
                        continue;
                    }
                    
                    let (has_normal, has_soul) = if from == "Reserve" {
                        (state.player.reserve.normal() > 0, state.player.reserve.soul > 0)
                    } else {
                        let obj = state.player.field.iter().find(|o| &o.id == from).unwrap();
                        (obj.cores.normal() > 0, obj.cores.soul > 0)
                    };
                    
                    if has_normal {
                        actions.push(Action::MoveCore {
                            from: from.clone(),
                            to: to.clone(),
                            normal_cores: 1,
                            soul_core: false,
                        });
                    }
                    if has_soul {
                        actions.push(Action::MoveCore {
                            from: from.clone(),
                            to: to.clone(),
                            normal_cores: 0,
                            soul_core: true,
                        });
                    }
                }
            }

            actions.push(Action::EndStep);
        }
        Phase::AttackStep(sub_phase) => {
            match sub_phase {
                AttackSubPhase::DeclareAttack => {
                    for obj in &state.player.field {
                        if (obj.card_type == CardType::Spirit || obj.card_type == CardType::Ultimate)
                            && !obj.is_exhausted
                            && obj.current_lv() >= 1
                        {
                            actions.push(Action::Attack { object_id: obj.id.clone() });
                        }
                    }
                    actions.push(Action::EndStep);
                }
                AttackSubPhase::AttackFlash { priority: _, .. } => {
                    let is_my_turn = true; // 簡略化してプレイヤーの手番を仮定
                    if is_my_turn {
                        for card in &state.player.hand {
                            if card.card_type == CardType::Magic {
                                let reduction = calculate_reduction(card, &state.player.field);
                                let cost_to_pay = card.base_cost.saturating_sub(reduction);
                                if state.player.reserve.total >= cost_to_pay {
                                    actions.push(Action::PlayCard {
                                        card_id: card.id.clone(),
                                        payment: vec![CoreSource { source_id: "Reserve".to_string(), count: cost_to_pay }],
                                        use_soul_core: false,
                                        placement: vec![],
                                        placement_soul_core: false,
                                    });
                                }
                            }
                        }
                    }
                    actions.push(Action::Pass);
                }
                AttackSubPhase::DeclareBlock => {
                    for obj in &state.player.field {
                        if (obj.card_type == CardType::Spirit || obj.card_type == CardType::Ultimate)
                            && !obj.is_exhausted
                            && obj.current_lv() >= 1
                        {
                            actions.push(Action::Block { object_id: obj.id.clone() });
                        }
                    }
                    actions.push(Action::Pass);
                }
                AttackSubPhase::BlockFlash { .. } => {
                    actions.push(Action::Pass);
                }
                AttackSubPhase::BattleResolution => {
                    actions.push(Action::Pass);
                }
                AttackSubPhase::End => {
                    actions.push(Action::Pass);
                }
            }
        }
        Phase::EndStep => {
            actions.push(Action::EndStep);
        }
        _ => {}
    }
    actions
}

// ----------------------------------------------------
// YAML キャッシュファイルからのカード簡易パース機能
// ----------------------------------------------------
fn load_card_from_yaml(card_no: &str) -> Result<Card, String> {
    let user_home = std::env::var("USERPROFILE").map_err(|_| "USERPROFILE env not found".to_string())?;
    let path = format!("{}\\.bscards\\yaml\\{}.yaml", user_home, card_no);
    let content = std::fs::read_to_string(&path)
        .map_err(|e| format!("Failed to read file {}: {}", path, e))?;

    let mut name = String::new();
    let mut cost = 3;
    let mut category = String::new();
    let mut symbols_str = String::new();
    let mut reduction_str = String::new();
    let mut lv_costs = Vec::new();
    let mut in_lv_info = false;

    for line in content.lines() {
        let trimmed = line.trim();
        if trimmed.starts_with("name:") {
            name = trimmed["name:".len()..].trim().trim_matches('"').to_string();
        } else if trimmed.starts_with("cost:") {
            if let Ok(c) = trimmed["cost:".len()..].trim().parse::<u8>() {
                cost = c;
            }
        } else if trimmed.starts_with("category:") {
            category = trimmed["category:".len()..].trim().trim_matches('"').to_string();
        } else if trimmed.starts_with("symbols:") {
            symbols_str = trimmed["symbols:".len()..].trim().trim_matches('"').to_string();
        } else if trimmed.starts_with("reductionSymbols:") {
            reduction_str = trimmed["reductionSymbols:".len()..].trim().trim_matches('"').to_string();
        } else if trimmed.starts_with("lvInfo:") {
            in_lv_info = true;
        } else if in_lv_info && trimmed.starts_with("-") {
            let info = trimmed["-".len()..].trim().trim_matches('"');
            if let Some(comma_pos) = info.find(',') {
                if let Ok(lv_cost) = info[..comma_pos].trim().parse::<u8>() {
                    lv_costs.push(lv_cost);
                }
            }
        } else if in_lv_info && !trimmed.starts_with("-") && !trimmed.is_empty() {
            in_lv_info = false;
        }
    }

    if name.is_empty() {
        name = card_no.to_string();
    }

    let card_type = if category.contains("スピリット") {
        CardType::Spirit
    } else if category.contains("ネクサス") {
        CardType::Nexus
    } else if category.contains("マジック") {
        CardType::Magic
    } else if category.contains("アルティメット") {
        CardType::Ultimate
    } else if category.contains("ブレイヴ") {
        CardType::Brave
    } else {
        CardType::Spirit
    };

    let mut symbols = Vec::new();
    if symbols_str.contains("黄") {
        let count = symbols_str.chars().filter(|c| c.is_digit(10))
            .collect::<String>().parse::<usize>().unwrap_or(1);
        for _ in 0..count {
            symbols.push(Color::Yellow);
        }
    } else if symbols_str.contains("赤") {
        symbols.push(Color::Red);
    } else if symbols_str.contains("紫") {
        symbols.push(Color::Purple);
    } else if symbols_str.contains("緑") {
        symbols.push(Color::Green);
    } else if symbols_str.contains("白") {
        symbols.push(Color::White);
    } else if symbols_str.contains("青") {
        symbols.push(Color::Blue);
    }

    let mut reduction_symbols = Vec::new();
    if reduction_str.contains("黄") {
        let count = reduction_str.chars().filter(|c| c.is_digit(10))
            .collect::<String>().parse::<usize>().unwrap_or(1);
        for _ in 0..count {
            reduction_symbols.push(Color::Yellow);
        }
    }

    if lv_costs.is_empty() {
        if card_type == CardType::Spirit || card_type == CardType::Ultimate {
            lv_costs = vec![1];
        } else {
            lv_costs = vec![0];
        }
    }

    Ok(Card {
        id: card_no.to_string(),
        name,
        base_cost: cost,
        colors: vec![Color::Yellow],
        reduction_symbols,
        card_type,
        lv_costs,
        symbols,
    })
}

// ----------------------------------------------------
// デッキ定義ファイルからのカードロード機能
// ----------------------------------------------------
#[derive(Debug, Deserialize)]
struct YamlDeck {
    cards: std::collections::HashMap<String, usize>,
}

fn load_deck_from_file(filename: &str) -> Result<Vec<Card>, String> {
    let path = filename;
    let content = std::fs::read_to_string(path)
        .map_err(|e| format!("Failed to read deck file {}: {}", path, e))?;

    let yaml_deck: YamlDeck = serde_yaml::from_str(&content)
        .map_err(|e| format!("Failed to parse deck file {}: {}", path, e))?;

    let mut deck = Vec::new();
    for (card_id, count) in yaml_deck.cards {
        if let Ok(card) = load_card_from_yaml(&card_id) {
            for _ in 0..count {
                deck.push(card.clone());
            }
        } else {
            let placeholder = Card {
                id: card_id.clone(),
                name: format!("Placeholder ({})", card_id),
                base_cost: 3,
                colors: vec![Color::Yellow],
                reduction_symbols: vec![Color::Yellow],
                card_type: CardType::Spirit,
                lv_costs: vec![1],
                symbols: vec![Color::Yellow],
            };
            for _ in 0..count {
                deck.push(placeholder.clone());
            }
        }
    }

    Ok(deck)
}

// 疑似乱数シャッフル
fn pseudo_shuffle(deck: &mut Vec<Card>) {
    let mut seed: u64 = 123456789;
    for i in (1..deck.len()).rev() {
        seed = seed.wrapping_mul(6364136223846793005).wrapping_add(1442695040888963407);
        let j = (seed % (i as u64 + 1)) as usize;
        deck.swap(i, j);
    }
}

// ----------------------------------------------------
// 対話型CLIゲームループの実装 (二段階入力ウィザード)
// ----------------------------------------------------
fn run_interactive_loop(deck1_path: &str, deck2_path: &str) {
    println!("=== TCG BattleSpirits デッキシミュレータ (エターナルフォーマット) ===");

    let mut deck = match load_deck_from_file(deck1_path) {
        Ok(d) => d,
        Err(e) => {
            println!("プレイヤー1のデッキの読み込みに失敗しました: {}", e);
            return;
        }
    };
    println!("プレイヤー1のデッキをロードしました。合計枚数: {}枚", deck.len());
    
    let target_fixed_ids = vec![
        "BS75-CX03".to_string(), "BS75-042".to_string(), "BS75-043".to_string(), "BS75-068".to_string(),
        "BS49-X09".to_string(), "BS49-040".to_string(), "BS49-X04".to_string(), "SD56-RV009".to_string(),
        "26RSD03-X01".to_string(), "26RSD03-X02".to_string(), "26RSD03-001".to_string(), "26RSD03-003".to_string()
    ];

    let mut hand = Vec::new();
    for card_id in &target_fixed_ids {
        if hand.len() >= 4 { break; }
        if let Some(idx) = deck.iter().position(|c| &c.id == card_id) {
            let card = deck.remove(idx);
            println!("プレイヤー1初期手札固定カード [{}] {} をセットしました。", card.id, card.name);
            hand.push(card);
        }
    }
    
    pseudo_shuffle(&mut deck);
    while hand.len() < 4 && !deck.is_empty() {
        hand.push(deck.remove(0));
    }

    let mut opponent_deck = match load_deck_from_file(deck2_path) {
        Ok(d) => d,
        Err(e) => {
            println!("プレイヤー2のデッキの読み込みに失敗しました: {}", e);
            return;
        }
    };
    println!("プレイヤー2のデッキをロードしました。合計枚数: {}枚", opponent_deck.len());

    let mut opponent_hand = Vec::new();
    for card_id in &target_fixed_ids {
        if opponent_hand.len() >= 4 { break; }
        if let Some(idx) = opponent_deck.iter().position(|c| &c.id == card_id) {
            let card = opponent_deck.remove(idx);
            println!("プレイヤー2初期手札固定カード [{}] {} をセットしました。", card.id, card.name);
            opponent_hand.push(card);
        }
    }

    pseudo_shuffle(&mut opponent_deck);
    while opponent_hand.len() < 4 && !opponent_deck.is_empty() {
        opponent_hand.push(opponent_deck.remove(0));
    }

    let player = SideState {
        player_id: 1,
        life: 5,
        reserve: Cores::new(4, 1), // 通常コア3個, ソウルコア1個 (合計4)
        field: vec![],
        hand,
        trash: vec![],
        trash_cores: Cores::new(0, 0),
        opened: deck, // 山札
        count: 0,
    };

    let opponent = SideState {
        player_id: 2,
        life: 5,
        reserve: Cores::new(4, 1), // 通常コア3個, ソウルコア1個 (合計4)
        field: vec![],
        hand: opponent_hand,
        trash: vec![],
        trash_cores: Cores::new(0, 0),
        opened: opponent_deck, // 山札
        count: 0,
    };

    let mut state = GameState {
        player,
        opponent,
        phase: Phase::StartStep,
        turn_count: 1,
        active_attacker: None,
        active_blocker: None,
    };

    // 初期自動遷移
    process_automatic_steps(&mut state);

    loop {
        println!("\n=======================================================");
        println!("【ゲーム局面】 プレイヤー{} のターン / フェイズ: {:?} (ターン: {})", state.player.player_id, state.phase, state.turn_count);
        println!("-------------------------------------------------------");
        print!("ライフ: ");
        for _ in 0..state.player.life {
            print!("■ ");
        }
        println!("({}/5)", state.player.life);
        println!("リザーブコア: {}", state.player.reserve.format());
        println!("トラッシュコア: {}", state.player.trash_cores.format());
        println!("フィールド:");
        if state.player.field.is_empty() {
            println!("  (なし)");
        } else {
            for obj in &state.player.field {
                let status = if obj.is_exhausted { "[疲労]" } else { "[回復]" };
                println!("  - [{}] {} (コア: {}) {}", obj.id, obj.name, obj.cores.format(), status);
            }
        }
        println!("手札:");
        if state.player.hand.is_empty() {
            println!("  (なし)");
        } else {
            for (idx, card) in state.player.hand.iter().enumerate() {
                let reduction = calculate_reduction(card, &state.player.field);
                let cost_to_pay = card.base_cost.saturating_sub(reduction);
                println!("  {}. [{}] {} (コスト: {}, 軽減後: {})", idx + 1, card.id, card.name, card.base_cost, cost_to_pay);
            }
        }
        println!("-------------------------------------------------------");

        let actions = generate_legal_actions(&state);
        if actions.is_empty() {
            println!("実行可能なアクションがありません。自動ステップ処理を行います。");
            state.phase = Phase::EndStep;
            apply_action(&mut state, &Action::EndStep).unwrap();
            continue;
        }

        // 第1階層のグループ分類
        #[derive(Clone, PartialEq, Eq)]
        enum ActionGroup {
            PlayCardGroup(String, String), // CardId, Name
            MoveCoreGroup,
            AttackGroup(String, String), // ObjectId, Name
            BlockGroup(String, String),  // ObjectId, Name
        }

        let mut groups: Vec<ActionGroup> = Vec::new();

        for action in &actions {
            match action {
                Action::PlayCard { card_id, .. } => {
                    if !groups.iter().any(|g| match g {
                        ActionGroup::PlayCardGroup(id, _) => id == card_id,
                        _ => false
                    }) {
                        let card = state.player.hand.iter().find(|c| &c.id == card_id).unwrap();
                        groups.push(ActionGroup::PlayCardGroup(card_id.clone(), card.name.clone()));
                    }
                }
                Action::MoveCore { .. } => {
                    if !groups.iter().any(|g| matches!(g, ActionGroup::MoveCoreGroup)) {
                        groups.push(ActionGroup::MoveCoreGroup);
                    }
                }
                Action::Attack { object_id } => {
                    if !groups.iter().any(|g| match g {
                        ActionGroup::AttackGroup(id, _) => id == object_id,
                        _ => false
                    }) {
                        let obj = state.player.field.iter().find(|o| &o.id == object_id).unwrap();
                        groups.push(ActionGroup::AttackGroup(object_id.clone(), obj.name.clone()));
                    }
                }
                Action::Block { object_id } => {
                    if !groups.iter().any(|g| match g {
                        ActionGroup::BlockGroup(id, _) => id == object_id,
                        _ => false
                    }) {
                        let obj = state.player.field.iter().find(|o| &o.id == object_id).unwrap();
                        groups.push(ActionGroup::BlockGroup(object_id.clone(), obj.name.clone()));
                    }
                }
                _ => {}
            }
        }

        println!("選択可能なアクション (カテゴリ):");
        for (idx, group) in groups.iter().enumerate() {
            match group {
                ActionGroup::PlayCardGroup(_, name) => {
                    println!("  {}: 【手札から使用/召喚】 {}", idx + 1, name);
                }
                ActionGroup::MoveCoreGroup => {
                    println!("  {}: 【コア移動】 フィールド・リザーブ間のコア移動", idx + 1);
                }
                ActionGroup::AttackGroup(_, name) => {
                    println!("  {}: 【アタック宣言】 {}", idx + 1, name);
                }
                ActionGroup::BlockGroup(_, name) => {
                    println!("  {}: 【ブロック宣言】 {}", idx + 1, name);
                }
            }
        }
        
        let has_end = actions.contains(&Action::EndStep);
        let has_pass = actions.contains(&Action::Pass);
        
        if has_end {
            println!("  n: ステップ終了 / ターン終了");
        } else if has_pass {
            println!("  n: パス / スキップ");
        }
        println!("  q: サレンダー（ゲームを終了する）");

        if has_end || has_pass {
            print!("\n選択してください [1-{}, n:終了, q:サレンダー]: ", groups.len());
        } else {
            print!("\n選択してください [1-{}, q:サレンダー]: ", groups.len());
        }
        io::stdout().flush().unwrap();

        let mut input = String::new();
        io::stdin().read_line(&mut input).unwrap();
        let trimmed = input.trim();

        if trimmed.to_lowercase() == "q" {
            println!("サレンダーしました。ゲームオーバー。");
            break;
        }

        if trimmed.to_lowercase() == "n" {
            if actions.contains(&Action::EndStep) {
                println!(">> アクションを実行: {:?}", Action::EndStep);
                if let Err(e) = apply_action(&mut state, &Action::EndStep) {
                    println!("エラー: {}", e);
                }
                process_automatic_steps(&mut state);
                continue;
            } else if actions.contains(&Action::Pass) {
                println!(">> アクションを実行: {:?}", Action::Pass);
                if let Err(e) = apply_action(&mut state, &Action::Pass) {
                    println!("エラー: {}", e);
                }
                process_automatic_steps(&mut state);
                continue;
            } else {
                println!("現在ステップ終了（終了/パス）は選択できません。");
                continue;
            }
        }

        let first_choice = match trimmed.parse::<usize>() {
            Ok(num) if num >= 1 && num <= groups.len() => num - 1,
            _ => {
                println!("無効な入力です。");
                continue;
            }
        };

        let selected_group = &groups[first_choice];

        // 第2階層の具体的な選択肢の抽出
        let mut sub_actions = Vec::new();
        for action in &actions {
            let is_match = match (selected_group, action) {
                (ActionGroup::PlayCardGroup(card_id, _), Action::PlayCard { card_id: action_card_id, .. }) => card_id == action_card_id,
                (ActionGroup::MoveCoreGroup, Action::MoveCore { .. }) => true,
                (ActionGroup::AttackGroup(id, _), Action::Attack { object_id }) => id == object_id,
                (ActionGroup::BlockGroup(id, _), Action::Block { object_id }) => id == object_id,
                _ => false
            };
            if is_match {
                sub_actions.push(action.clone());
            }
        }

        // 第2階層が1つしかない場合は自動で適用
        if sub_actions.len() == 1 {
            let chosen_action = &sub_actions[0];
            println!(">> アクションを実行: {:?}", chosen_action);
            if let Err(e) = apply_action(&mut state, chosen_action) {
                println!("エラー: {}", e);
            }
            process_automatic_steps(&mut state);
            continue;
        }

        // 複数ある場合は第2段階の詳細選択画面を表示
        println!("\n--- 詳細なオプションを選択してください ---");
        for (idx, action) in sub_actions.iter().enumerate() {
            match action {
                Action::PlayCard { payment, use_soul_core, placement, placement_soul_core, .. } => {
                    let pay_amount: u8 = payment.iter().map(|p| p.count).sum();
                    let place_amount: u8 = placement.iter().map(|p| p.count).sum();
                    let pay_cores = Cores::new(pay_amount + if *use_soul_core { 1 } else { 0 }, if *use_soul_core { 1 } else { 0 });
                    let place_cores = Cores::new(place_amount + if *placement_soul_core { 1 } else { 0 }, if *placement_soul_core { 1 } else { 0 });
                    println!("  {}: 支払コスト: {} / 配置コア: {}", idx + 1, pay_cores.format(), place_cores.format());
                }
                Action::MoveCore { from, to, normal_cores, soul_core } => {
                    let move_cores = Cores::new(*normal_cores + if *soul_core { 1 } else { 0 }, if *soul_core { 1 } else { 0 });
                    println!("  {}: {} -> {} (コア: {})", idx + 1, from, to, move_cores.format());
                }
                _ => {
                    println!("  {}: {:?}", idx + 1, action);
                }
            }
        }
        println!("  b: 戻る (手札選択に戻る)");

        print!("\n選択してください [1-{}, b]: ", sub_actions.len());
        io::stdout().flush().unwrap();

        let mut sub_input = String::new();
        io::stdin().read_line(&mut sub_input).unwrap();
        let sub_trimmed = sub_input.trim();

        if sub_trimmed.to_lowercase() == "b" {
            println!("戻ります。");
            continue;
        }

        match sub_trimmed.parse::<usize>() {
            Ok(num) if num >= 1 && num <= sub_actions.len() => {
                let chosen_action = &sub_actions[num - 1];
                println!(">> アクションを実行: {:?}", chosen_action);
                if let Err(e) = apply_action(&mut state, chosen_action) {
                    println!("エラー: {}", e);
                }
                process_automatic_steps(&mut state);
            }
            _ => {
                println!("無効な入力です。");
            }
        }
    }
}


fn run_ai_simulation(deck1_path: &str, deck2_path: &str) {
    println!("=== TCG BattleSpirits AI自立対戦シミュレーション ===");

    let mut deck = match load_deck_from_file(deck1_path) {
        Ok(d) => d,
        Err(e) => {
            println!("プレイヤー1のデッキの読み込みに失敗しました: {}", e);
            return;
        }
    };
    
    let target_fixed_ids = vec![
        "BS75-CX03".to_string(), "BS75-042".to_string(), "BS75-043".to_string(), "BS75-068".to_string(),
        "BS49-X09".to_string(), "BS49-040".to_string(), "BS49-X04".to_string(), "SD56-RV009".to_string(),
        "26RSD03-X01".to_string(), "26RSD03-X02".to_string(), "26RSD03-001".to_string(), "26RSD03-003".to_string()
    ];

    let mut hand = Vec::new();
    for card_id in &target_fixed_ids {
        if hand.len() >= 4 { break; }
        if let Some(idx) = deck.iter().position(|c| &c.id == card_id) {
            hand.push(deck.remove(idx));
        }
    }
    pseudo_shuffle(&mut deck);
    while hand.len() < 4 && !deck.is_empty() {
        hand.push(deck.remove(0));
    }

    let mut opponent_deck = match load_deck_from_file(deck2_path) {
        Ok(d) => d,
        Err(e) => {
            println!("プレイヤー2のデッキの読み込みに失敗しました: {}", e);
            return;
        }
    };

    let mut opponent_hand = Vec::new();
    for card_id in &target_fixed_ids {
        if opponent_hand.len() >= 4 { break; }
        if let Some(idx) = opponent_deck.iter().position(|c| &c.id == card_id) {
            opponent_hand.push(opponent_deck.remove(idx));
        }
    }
    pseudo_shuffle(&mut opponent_deck);
    while opponent_hand.len() < 4 && !opponent_deck.is_empty() {
        opponent_hand.push(opponent_deck.remove(0));
    }

    let player = SideState {
        player_id: 1,
        life: 5,
        reserve: Cores::new(4, 1),
        field: vec![],
        hand,
        trash: vec![],
        trash_cores: Cores::new(0, 0),
        opened: deck,
        count: 0,
    };

    let opponent = SideState {
        player_id: 2,
        life: 5,
        reserve: Cores::new(4, 1),
        field: vec![],
        hand: opponent_hand,
        trash: vec![],
        trash_cores: Cores::new(0, 0),
        opened: opponent_deck,
        count: 0,
    };

    let mut state = GameState {
        player,
        opponent,
        phase: Phase::StartStep,
        turn_count: 1,
        active_attacker: None,
        active_blocker: None,
    };

    process_automatic_steps(&mut state);

    let print_state = |state: &GameState| {
        println!("\n-------------------------------------------------------");
        println!("【AI局面】 プレイヤー{} のターン / フェイズ: {:?} (ターン: {})", state.player.player_id, state.phase, state.turn_count);
        println!("  プレイヤー1 ライフ: {} / リザーブコア: {}", state.player.life, state.player.reserve.format());
        println!("  プレイヤー2 ライフ: {} / リザーブコア: {}", state.opponent.life, state.opponent.reserve.format());
        println!("  P{} フィールド:", state.player.player_id);
        for obj in &state.player.field {
            println!("    - [{}] {} (コア: {})", obj.id, obj.name, obj.cores.format());
        }
    };

    let mut loop_count = 0;
    while state.player.life > 0 && state.opponent.life > 0 && loop_count < 100 {
        loop_count += 1;
        print_state(&state);

        let actions = generate_legal_actions(&state);
        if actions.is_empty() {
            state.phase = Phase::EndStep;
            apply_action(&mut state, &Action::EndStep).unwrap();
            process_automatic_steps(&mut state);
            continue;
        }

        // AIの簡易意思決定ヒューリスティック
        // 優先度: PlayCard > Attack > Block > MoveCore > Pass/EndStep
        let mut chosen_action = actions[0].clone();
        
        if let Some(act) = actions.iter().find(|a| matches!(a, Action::PlayCard { .. })) {
            chosen_action = act.clone();
        } else if let Some(act) = actions.iter().find(|a| matches!(a, Action::Attack { .. })) {
            chosen_action = act.clone();
        } else if let Some(act) = actions.iter().find(|a| matches!(a, Action::Block { .. })) {
            chosen_action = act.clone();
        } else if let Some(act) = actions.iter().find(|a| matches!(a, Action::Pass)) {
            chosen_action = act.clone();
        } else if let Some(act) = actions.iter().find(|a| matches!(a, Action::EndStep)) {
            chosen_action = act.clone();
        }

        println!(">> AI選択アクション: {:?}", chosen_action);
        if let Err(e) = apply_action(&mut state, &chosen_action) {
            println!("エラー: {}", e);
            break;
        }
        process_automatic_steps(&mut state);
    }

    println!("\n=============================================");
    println!("  ★ AI自立対戦シミュレーション終了 ★");
    println!("  プレイヤー1 ライフ: {}", state.player.life);
    println!("  プレイヤー2 ライフ: {}", state.opponent.life);
    println!("=============================================");
}

fn main() {
    let args: Vec<String> = std::env::args().collect();
    let is_ai_sim = args.iter().any(|arg| arg == "--ai-sim");
    
    let deck_args: Vec<&str> = args.iter()
        .skip(1)
        .filter(|arg| *arg != "--ai-sim")
        .map(|s| s.as_str())
        .collect();

    let deck1_path = deck_args.get(0).unwrap_or(&"deck.yaml");
    let deck2_path = deck_args.get(1).unwrap_or(&"deck2.yaml");
    
    if is_ai_sim {
        run_ai_simulation(deck1_path, deck2_path);
    } else {
        run_interactive_loop(deck1_path, deck2_path);
    }
}
