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
        if self.soul == 0 {
            format!("{}", self.total)
        } else {
            format!("{}s", self.total)
        }
    }
    pub fn add(&mut self, normal: u8, soul: u8) {
        self.total += normal + soul;
        self.soul += soul;
    }
    pub fn sub(&mut self, normal: u8, soul: u8) {
        self.total -= normal + soul;
        self.soul -= soul;
    }
}

pub fn format_symbols(symbols: &[Color]) -> String {
    let mut red = 0;
    let mut purple = 0;
    let mut green = 0;
    let mut white = 0;
    let mut yellow = 0;
    let mut blue = 0;
    for col in symbols {
        match col {
            Color::Red => red += 1,
            Color::Purple => purple += 1,
            Color::Green => green += 1,
            Color::White => white += 1,
            Color::Yellow => yellow += 1,
            Color::Blue => blue += 1,
            Color::None => {}
        }
    }
    let mut parts = Vec::new();
    if red > 0 { parts.push(format!("赤{}", red)); }
    if purple > 0 { parts.push(format!("紫{}", purple)); }
    if green > 0 { parts.push(format!("緑{}", green)); }
    if white > 0 { parts.push(format!("白{}", white)); }
    if yellow > 0 { parts.push(format!("黄{}", yellow)); }
    if blue > 0 { parts.push(format!("青{}", blue)); }
    parts.join("")
}

pub fn parse_colors(s: &str) -> Vec<Color> {
    let mut cols = Vec::new();
    let color_map = [
        ("赤", Color::Red),
        ("紫", Color::Purple),
        ("緑", Color::Green),
        ("白", Color::White),
        ("黄", Color::Yellow),
        ("青", Color::Blue),
    ];
    for &(name, col) in &color_map {
        if s.contains(name) {
            if let Some(idx) = s.find(name) {
                let rest = &s[idx + name.len()..];
                let count = rest.chars().next()
                    .and_then(|c| c.to_digit(10))
                    .unwrap_or(1) as usize;
                for _ in 0..count {
                    cols.push(col);
                }
            }
        }
    }
    cols
}

pub fn format_sources(
    state: &GameState,
    payment: &[CoreSource],
    use_soul: bool,
    placement: &[CoreSource],
    place_soul: bool,
) -> String {
    let mut reserve_normal = 0;
    let mut reserve_soul = 0;
    let mut field_cores: std::collections::HashMap<String, (u8, u8)> = std::collections::HashMap::new();

    if use_soul {
        if state.player.reserve.soul > 0 {
            reserve_soul += 1;
        } else {
            for obj in &state.player.field {
                if obj.cores.soul > 0 {
                    field_cores.insert(obj.id.clone(), (0, 1));
                    break;
                }
            }
        }
    }

    if place_soul {
        let remaining_reserve_soul = state.player.reserve.soul.saturating_sub(reserve_soul);
        if remaining_reserve_soul > 0 {
            reserve_soul += 1;
        } else {
            for obj in &state.player.field {
                let taken_soul = field_cores.get(&obj.id).map(|&(_, s)| s).unwrap_or(0);
                if obj.cores.soul > taken_soul {
                    let entry = field_cores.entry(obj.id.clone()).or_insert((0, 0));
                    entry.1 += 1;
                    break;
                }
            }
        }
    }

    for src in payment {
        if src.source_id == "Reserve" {
            reserve_normal += src.count;
        } else {
            let entry = field_cores.entry(src.source_id.clone()).or_insert((0, 0));
            entry.0 += src.count;
        }
    }

    for src in placement {
        if src.source_id == "Reserve" {
            reserve_normal += src.count;
        } else {
            let entry = field_cores.entry(src.source_id.clone()).or_insert((0, 0));
            entry.0 += src.count;
        }
    }

    let mut parts = Vec::new();
    if reserve_normal > 0 || reserve_soul > 0 {
        let cores = Cores::new(reserve_normal + reserve_soul, reserve_soul);
        parts.push(format!("リザーブ:{}", cores.format()));
    }

    for obj in &state.player.field {
        if let Some(&(normal, soul)) = field_cores.get(&obj.id) {
            let cores = Cores::new(normal + soul, soul);
            parts.push(format!("{}:{}", obj.name, cores.format()));
        }
    }

    parts.join(", ")
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

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Clone, Hash)]
pub struct CoreSource {
    pub source_id: String, // "Reserve" or FieldObject ID
    pub count: u8,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Clone, Hash)]
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
                    state.player.reserve.sub(source.count, 0);
                    state.player.trash_cores.add(source.count, 0);
                } else {
                    let obj = state.player.field.iter_mut().find(|o| &o.id == &source.source_id)
                        .ok_or_else(|| "Payment source object not found".to_string())?;
                    if obj.cores.normal() < source.count {
                        return Err("Not enough normal cores on object".to_string());
                    }
                    obj.cores.sub(source.count, 0);
                    state.player.trash_cores.add(source.count, 0);
                }
            }
            if *use_soul_core {
                let mut found_soul = false;
                if state.player.reserve.soul > 0 {
                    state.player.reserve.sub(0, 1);
                    state.player.trash_cores.add(0, 1);
                    found_soul = true;
                } else {
                    for obj in &mut state.player.field {
                        if obj.cores.soul > 0 {
                            obj.cores.sub(0, 1);
                            state.player.trash_cores.add(0, 1);
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
                        state.player.reserve.sub(source.count, 0);
                        placement_total += source.count;
                    } else {
                        let obj = state.player.field.iter_mut().find(|o| &o.id == &source.source_id)
                            .ok_or_else(|| "Placement source object not found".to_string())?;
                        if obj.cores.normal() < source.count {
                            return Err("Not enough normal cores on object for placement".to_string());
                        }
                        obj.cores.sub(source.count, 0);
                        placement_total += source.count;
                    }
                }
                if *placement_soul_core {
                    let mut found_soul = false;
                    if state.player.reserve.soul > 0 {
                        state.player.reserve.sub(0, 1);
                        placement_soul += 1;
                        found_soul = true;
                    } else {
                        for obj in &mut state.player.field {
                            if obj.cores.soul > 0 {
                                obj.cores.sub(0, 1);
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

                state.player.count += 1;
                let new_obj = FieldObject {
                    id: format!("{}_{}", card.id, state.player.count),
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
            let move_soul;
            let move_normal;
            if from == "Reserve" {
                if state.player.reserve.normal() < *normal_cores {
                    return Err("Not enough normal cores in reserve".to_string());
                }
                if *soul_core && state.player.reserve.soul == 0 {
                    return Err("Soul core not in reserve".to_string());
                }
                let s = if *soul_core { 1 } else { 0 };
                state.player.reserve.sub(*normal_cores, s);
                move_normal = *normal_cores;
                move_soul = s;
            } else {
                let obj = state.player.field.iter_mut().find(|o| &o.id == from)
                    .ok_or_else(|| "Source object not found".to_string())?;
                if obj.cores.normal() < *normal_cores {
                    return Err("Not enough normal cores on source object".to_string());
                }
                if *soul_core && obj.cores.soul == 0 {
                    return Err("Soul core not on source object".to_string());
                }
                let s = if *soul_core { 1 } else { 0 };
                obj.cores.sub(*normal_cores, s);
                move_normal = *normal_cores;
                move_soul = s;
            }

            if to == "Reserve" {
                state.player.reserve.add(move_normal, move_soul);
            } else {
                let obj = state.player.field.iter_mut().find(|o| &o.id == to)
                    .ok_or_else(|| "Target object not found".to_string())?;
                obj.cores.add(move_normal, move_soul);
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
                    state.player.reserve.sub(source.count, 0);
                    state.player.trash_cores.add(source.count, 0);
                }
            }
            if *use_soul_core {
                if state.player.reserve.soul > 0 {
                    state.player.reserve.sub(0, 1);
                    state.player.trash_cores.add(0, 1);
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
                    state.opponent.reserve.add(1, 0);
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

    // 選択肢の重複排除
    let mut seen = std::collections::HashSet::new();
    let mut unique_actions = Vec::new();
    for action in actions {
        if seen.insert(action.clone()) {
            unique_actions.push(action);
        }
    }
    unique_actions
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

    let symbols = parse_colors(&symbols_str);
    let reduction_symbols = parse_colors(&reduction_str);

    if lv_costs.is_empty() {
        if card_type == CardType::Spirit || card_type == CardType::Ultimate {
            lv_costs = vec![1];
        } else {
            lv_costs = vec![0];
        }
    }

    let card_colors = if symbols.is_empty() { vec![Color::Yellow] } else { vec![symbols[0]] };

    Ok(Card {
        id: card_no.to_string(),
        name,
        base_cost: cost,
        colors: card_colors,
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

    let mut game_history: Vec<LogEntry> = Vec::new();

    loop {
        let (p1, p2) = if state.player.player_id == 1 {
            (&state.player, &state.opponent)
        } else {
            (&state.opponent, &state.player)
        };
        let entry = LogEntry {
            tuen: state.turn_count,
            phase: format_phase_camel(&state.phase),
            player1: LogSideState::from(p1),
            player2: LogSideState::from(p2),
        };
        game_history.push(entry);

        if let Ok(yaml_content) = serde_yaml::to_string(&game_history) {
            if let Err(e) = std::fs::write("bs-log.yaml", yaml_content) {
                println!("警告: bs-log.yaml の書き込みに失敗しました: {}", e);
            }
        }

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
                let status = if obj.is_exhausted { "[疲労]" } else { "" };
                let sym_str = format_symbols(&obj.base_symbols);
                let sym_part = if sym_str.is_empty() { "".to_string() } else { format!("シンボル:{}, ", sym_str) };
                println!("  - {}({}コア:{}){}", obj.name, sym_part, obj.cores.format(), status);
            }
        }
        println!("手札:");
        if state.player.hand.is_empty() {
            println!("  (なし)");
        } else {
            for card in &state.player.hand {
                let red_str = format_symbols(&card.reduction_symbols);
                let red_part = if red_str.is_empty() { "".to_string() } else { format!(", 軽減:{}", red_str) };
                println!("  {} (コスト:{}{})", card.name, card.base_cost, red_part);
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
                ActionGroup::PlayCardGroup(card_id, name) => {
                    let card = state.player.hand.iter().find(|c| &c.id == card_id).unwrap();
                    let reduction = calculate_reduction(card, &state.player.field);
                    let cost_to_pay = card.base_cost.saturating_sub(reduction);
                    println!("  {}: 【手札から使用/召喚】 {} (コスト:{}, 軽減後:{})", idx + 1, name, card.base_cost, cost_to_pay);
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
        match io::stdin().read_line(&mut input) {
            Ok(0) => {
                println!("入力の終端に達しました。サレンダーします。");
                break;
            }
            Err(_) => {
                println!("入力エラー。ゲームを終了します。");
                break;
            }
            _ => {}
        }
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

        // 詳細コア支払のソート仕様
        // - リザーブからの支払いが多いものを上位に表示
        // - ソウルコアをコストとしてトラッシュに送るものを下位に表示
        sub_actions.sort_by(|a, b| {
            let a_soul = match a {
                Action::PlayCard { use_soul_core, .. } => *use_soul_core,
                _ => false,
            };
            let b_soul = match b {
                Action::PlayCard { use_soul_core, .. } => *use_soul_core,
                _ => false,
            };

            let a_reserve = match a {
                Action::PlayCard { payment, .. } => {
                    payment.iter()
                        .filter(|p| p.source_id == "Reserve")
                        .map(|p| p.count)
                        .sum::<u8>()
                }
                _ => 0,
            };
            let b_reserve = match b {
                Action::PlayCard { payment, .. } => {
                    payment.iter()
                        .filter(|p| p.source_id == "Reserve")
                        .map(|p| p.count)
                        .sum::<u8>()
                }
                _ => 0,
            };

            a_soul.cmp(&b_soul)
                .then_with(|| b_reserve.cmp(&a_reserve))
        });

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
                Action::PlayCard { card_id: _, payment, use_soul_core, placement, placement_soul_core } => {
                    let pay_amount: u8 = payment.iter().map(|p| p.count).sum();
                    let pay_soul = if *use_soul_core { 1 } else { 0 };
                    let pay_cores = Cores::new(pay_amount + pay_soul, pay_soul);
                    
                    let place_amount: u8 = placement.iter().map(|p| p.count).sum();
                    let place_soul = if *placement_soul_core { 1 } else { 0 };
                    let place_cores = Cores::new(place_amount + place_soul, place_soul);
                    
                    let total_str = format_sources(&state, payment, *use_soul_core, placement, *placement_soul_core);
                    
                    println!("  {}: コスト:{}, 配置コア:{}, ({})", 
                        idx + 1, 
                        pay_cores.format(), 
                        place_cores.format(), 
                        total_str
                    );
                }
                Action::MoveCore { from, to, normal_cores, soul_core } => {
                    let from_name = if from == "Reserve" {
                        "リザーブ".to_string()
                    } else {
                        state.player.field.iter().find(|o| &o.id == from)
                            .map(|o| o.name.clone())
                            .unwrap_or(from.clone())
                    };
                    let to_name = if to == "Reserve" {
                        "リザーブ".to_string()
                    } else {
                        state.player.field.iter().find(|o| &o.id == to)
                            .map(|o| o.name.clone())
                            .unwrap_or(to.clone())
                    };
                    let move_cores = Cores::new(*normal_cores + if *soul_core { 1 } else { 0 }, if *soul_core { 1 } else { 0 });
                    println!("  {}: {} -> {} (コア:{})", idx + 1, from_name, to_name, move_cores.format());
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
        match io::stdin().read_line(&mut sub_input) {
            Ok(0) => {
                println!("入力の終端に達しました。サレンダーします。");
                break;
            }
            Err(_) => {
                println!("入力エラー。ゲームを終了します。");
                break;
            }
            _ => {}
        }
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

#[derive(Serialize)]
pub struct LogSideState {
    pub player_id: u8,
    pub life: u8,
    pub reserve: Cores,
    pub field: Vec<FieldObject>,
    pub hand: Vec<String>,       // カードはカード番号(ID)のみ
    pub trash: Vec<String>,      // カードはカード番号(ID)のみ
    pub trash_cores: Cores,
    pub opened: Vec<String>,     // カードはカード番号(ID)のみ
    pub count: u8,
}

impl LogSideState {
    pub fn from(side: &SideState) -> Self {
        Self {
            player_id: side.player_id,
            life: side.life,
            reserve: side.reserve,
            field: side.field.clone(),
            hand: side.hand.iter().map(|c| c.id.clone()).collect(),
            trash: side.trash.iter().map(|c| c.id.clone()).collect(),
            trash_cores: side.trash_cores,
            opened: side.opened.iter().map(|c| c.id.clone()).collect(),
            count: side.count,
        }
    }
}

#[derive(Serialize)]
pub struct LogEntry {
    pub tuen: u32,               // 誤植に合わせた "tuen" キー
    pub phase: String,
    pub player1: LogSideState,
    pub player2: LogSideState,
}

fn format_phase_camel(phase: &Phase) -> String {
    match phase {
        Phase::StartStep => "startStep".to_string(),
        Phase::CoreStep => "coreStep".to_string(),
        Phase::DrawStep => "drawStep".to_string(),
        Phase::RefreshStep => "refreshStep".to_string(),
        Phase::MainStep => "mainStep".to_string(),
        Phase::AttackStep(_) => "attackStep".to_string(),
        Phase::EndStep => "endStep".to_string(),
    }
}

fn main() {
    let args: Vec<String> = std::env::args().collect();
    
    let deck_args: Vec<&str> = args.iter()
        .skip(1)
        .map(|s| s.as_str())
        .collect();

    let deck1_path = deck_args.get(0).unwrap_or(&"deck.yaml");
    let deck2_path = deck_args.get(1).unwrap_or(&"deck2.yaml");
    
    run_interactive_loop(deck1_path, deck2_path);
}
