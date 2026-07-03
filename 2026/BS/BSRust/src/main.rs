use serde::{Deserialize, Serialize};
use std::io::{self, Write};
use burn::record::{CompactRecorder, Recorder};
use burn::module::Module;

pub mod ai;
pub mod cards;
use crate::ai::decision::evaluate_action;


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
    pub systems: Vec<String>,
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
    pub systems: Vec<String>,
    pub under_cards: Vec<Card>,
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
    pub token_pool: Vec<Card>, // トークンプール（デッキ外）
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
    pub token_summoned_this_turn: bool, // ターン中のトークン召喚コアブースト済フラグ
    pub last_move_core: Option<(String, String)>,
    pub core_move_count_this_turn: u8,
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
    ResolveFaraEffect { is_placement: bool },
    ResolveBasiliskEffect { is_main: bool },
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
    ResolveFaraToken { summon: bool },
    ResolveBasilisk { use_effect: bool, destroy_bug: bool },
    Kourin {
        card_id: String,
        target_id: String,
        payment: Vec<CoreSource>,
        use_soul_core: bool,
        placement: Vec<CoreSource>,
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
                let mut card_id = removed.id.clone();
                if let Some(idx) = card_id.find('_') {
                    card_id.truncate(idx);
                }
                let card = Card {
                    id: card_id,
                    name: removed.name.clone(),
                    base_cost: 0,
                    colors: removed.colors.clone(),
                    reduction_symbols: vec![],
                    card_type: removed.card_type,
                    lv_costs: removed.lv_costs.clone(),
                    symbols: removed.base_symbols.clone(),
                    systems: removed.systems.clone(),
                };
                let returns_to_pool = cards::CARD_REGISTRY.get(&card.id)
                    .map_or(false, |e| e.returns_to_token_pool());
                if returns_to_pool {
                    side.token_pool.push(card);
                } else {
                    side.trash.push(card);
                }
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
                state.token_summoned_this_turn = false; // ターンのフラグリセット
                state.phase = Phase::MainStep;
            }
            _ => break,
        }
    }
}

pub fn try_summon_token(state: &mut GameState, token_card_id: &str) {
    if let Some(token_idx) = state.player.token_pool.iter().position(|c| c.id == token_card_id) {
        let token = &state.player.token_pool[token_idx];
        let lv1_cost = token.lv_costs.get(0).copied().unwrap_or(1);
        
        let mut cores_to_place = 0;
        let mut void_boost = false;
        
        if !state.token_summoned_this_turn {
            // ターンに1回、ボイドからLv1コスト分のコアを置いて出せる
            cores_to_place = lv1_cost;
            void_boost = true;
        } else {
            // ボイドから置けない場合、リザーブやフィールドから維持コスト分のコアを確保する
            let needed = lv1_cost;
            let mut available = 0;
            
            // 1. リザーブから
            let from_reserve = state.player.reserve.normal().min(needed);
            available += from_reserve;
            
            // 2. フィールド（ネクサス）から
            if available < needed {
                for obj in &state.player.field {
                    if obj.card_type == CardType::Nexus {
                        let take = obj.cores.normal().min(needed - available);
                        available += take;
                        if available >= needed { break; }
                    }
                }
            }
            
            // 3. フィールド（スピリット/アルティメットの余剰コア）から
            if available < needed {
                for obj in &state.player.field {
                    if obj.card_type == CardType::Spirit || obj.card_type == CardType::Ultimate {
                        let lv1 = obj.lv_costs.get(0).copied().unwrap_or(1);
                        if obj.cores.normal() > lv1 {
                            let excess = obj.cores.normal() - lv1;
                            let take = excess.min(needed - available);
                            available += take;
                            if available >= needed { break; }
                        }
                    }
                }
            }
            
            if available >= needed {
                let mut remaining = needed;
                
                // リザーブから引く
                let take_res = state.player.reserve.normal().min(remaining);
                state.player.reserve.sub(take_res, 0);
                remaining -= take_res;
                
                // ネクサスから引く
                if remaining > 0 {
                    for obj in &mut state.player.field {
                        if obj.card_type == CardType::Nexus {
                            let take = obj.cores.normal().min(remaining);
                            obj.cores.sub(take, 0);
                            remaining -= take;
                            if remaining == 0 { break; }
                        }
                    }
                }
                
                // スピリットから引く
                if remaining > 0 {
                    for obj in &mut state.player.field {
                        if obj.card_type == CardType::Spirit || obj.card_type == CardType::Ultimate {
                            let lv1 = obj.lv_costs.get(0).copied().unwrap_or(1);
                            if obj.cores.normal() > lv1 {
                                let excess = obj.cores.normal() - lv1;
                                let take = excess.min(remaining);
                                obj.cores.sub(take, 0);
                                remaining -= take;
                                if remaining == 0 { break; }
                            }
                        }
                    }
                }
                
                cores_to_place = needed;
            }
        }
        
        if cores_to_place >= lv1_cost {
            let token = state.player.token_pool.remove(token_idx);
            state.player.count += 1;
            let token_obj = FieldObject {
                id: format!("{}_{}", token.id, state.player.count),
                name: token.name.clone(),
                colors: token.colors.clone(),
                card_type: token.card_type,
                cores: Cores::new(cores_to_place, 0),
                is_exhausted: false,
                lv_costs: token.lv_costs.clone(),
                base_symbols: token.symbols.clone(),
                systems: token.systems.clone(),
                under_cards: vec![],
            };
            state.player.field.push(token_obj);
            
            if void_boost {
                state.token_summoned_this_turn = true;
            }
        }
    }
}

/// ゲームルール処理エンジン（GameStateの遷移）
pub fn apply_action(state: &mut GameState, action: &Action) -> Result<(), String> {
    if !matches!(action, Action::MoveCore { .. }) {
        state.last_move_core = None;
    }
    if matches!(action, Action::EndStep) {
        state.core_move_count_this_turn = 0;
    }

    let phase = state.phase;
    match (phase, action) {
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
                    systems: card.systems.clone(),
                    under_cards: vec![],
                };
                state.player.field.push(new_obj);

                // カード固有の配置時効果（CardRegistry経由）
                if let Some(effect) = cards::CARD_REGISTRY.get(&card.id) {
                    if let Some(new_phase) = effect.on_placement(state) {
                        state.phase = new_phase;
                    }
                }
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
            state.last_move_core = Some((from.clone(), to.clone()));
            state.core_move_count_this_turn += 1;
        }
        (Phase::MainStep, Action::EndStep) => {
            if state.turn_count == 1 {
                state.phase = Phase::EndStep;
            } else {
                state.phase = Phase::AttackStep(AttackSubPhase::DeclareAttack);
            }
        }
        (Phase::AttackStep(AttackSubPhase::DeclareAttack), Action::Attack { object_id }) => {
            let attack_phase_override = {
                let obj = state.player.field.iter_mut().find(|o| &o.id == object_id)
                    .ok_or_else(|| "Attacker object not found".to_string())?;
                if obj.is_exhausted {
                    return Err("Attacker already exhausted".to_string());
                }
                obj.is_exhausted = true;
                // CardRegistry経由でアタック時効果を判定
                cards::CARD_REGISTRY.get(&obj.id)
                    .and_then(|e| e.on_attack(state, object_id))
            };
            state.active_attacker = Some(object_id.clone());
            if let Some(new_phase) = attack_phase_override {
                state.phase = new_phase;
            } else {
                state.phase = Phase::AttackStep(AttackSubPhase::AttackFlash {
                    priority: Priority::Defender,
                    consecutive_passes: 0,
                });
            }
        }
        (Phase::ResolveFaraEffect { is_placement }, Action::ResolveFaraToken { summon }) => {
            if *summon {
                try_summon_token(state, "BS76-T001");
            }
            if is_placement {
                state.phase = Phase::MainStep;
            } else {
                state.phase = Phase::AttackStep(AttackSubPhase::AttackFlash {
                    priority: Priority::Defender,
                    consecutive_passes: 0,
                });
            }
        }
        (Phase::ResolveBasiliskEffect { is_main }, Action::ResolveBasilisk { use_effect, destroy_bug }) => {
            if *use_effect {
                let mut max_add = 1;
                if *destroy_bug {
                    if let Some(bug_idx) = state.player.field.iter().position(|o| o.name == "プラチナム・バグ") {
                        let bug = state.player.field.remove(bug_idx);
                        state.player.reserve.add(bug.cores.normal(), bug.cores.soul);
                        if let Some(effect) = cards::CARD_REGISTRY.get(&bug.id) {
                            if effect.returns_to_token_pool() {
                                let placeholder = Card {
                                    id: bug.id.clone(),
                                    name: bug.name.clone(),
                                    base_cost: 0,
                                    colors: bug.colors.clone(),
                                    reduction_symbols: vec![],
                                    card_type: bug.card_type,
                                    lv_costs: bug.lv_costs.clone(),
                                    symbols: bug.base_symbols.clone(),
                                    systems: bug.systems.clone(),
                                };
                                state.player.token_pool.push(placeholder);
                            }
                        }
                        max_add = 2;
                    } else {
                        return Err("Platinum Bug not found to destroy".to_string());
                    }
                }

                let mut open_cards = Vec::new();
                for _ in 0..3 {
                    if !state.player.opened.is_empty() {
                        open_cards.push(state.player.opened.remove(0));
                    }
                }

                let mut added_count = 0;
                let mut remains = Vec::new();

                for card in open_cards {
                    let is_white = card.colors.contains(&Color::White);
                    let is_hishu = card.systems.iter().any(|s| s == "旗種");
                    if is_white && is_hishu && added_count < max_add {
                        state.player.hand.push(card);
                        added_count += 1;
                    } else {
                        remains.push(card);
                    }
                }

                state.player.opened.extend(remains);
            }

            if is_main {
                state.phase = Phase::MainStep;
            } else {
                state.phase = Phase::AttackStep(AttackSubPhase::AttackFlash {
                    priority: Priority::Defender,
                    consecutive_passes: 0,
                });
            }
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
            let (acting_side, _opponent_side) = match priority {
                Priority::Attacker => (&mut state.player, &mut state.opponent),
                Priority::Defender => (&mut state.opponent, &mut state.player),
            };
            let card_idx = acting_side.hand.iter().position(|c| &c.id == card_id)
                .ok_or_else(|| "Card not in hand".to_string())?;
            let card = acting_side.hand.remove(card_idx);
            acting_side.trash.push(card);
            for source in payment {
                if source.source_id == "Reserve" {
                    acting_side.reserve.sub(source.count, 0);
                    acting_side.trash_cores.add(source.count, 0);
                }
            }
            if *use_soul_core {
                if acting_side.reserve.soul > 0 {
                    acting_side.reserve.sub(0, 1);
                    acting_side.trash_cores.add(0, 1);
                }
            }
        }
        (Phase::AttackStep(AttackSubPhase::DeclareBlock), Action::Block { object_id }) => {
            let obj = state.opponent.field.iter_mut().find(|o| &o.id == object_id)
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
            if let Some(attacker_id) = &state.active_attacker {
                if let Some(_blocker_id) = &state.active_blocker {
                    // ブロックあり戦闘
                } else {
                    // ノーブロック：ライフ減少 (アタッカーのシンボル数に応じたダメージ)
                    let attacker = state.player.field.iter().chain(&state.opponent.field)
                        .find(|o| &o.id == attacker_id);
                    let dmg = attacker.map(|a| a.active_symbols().len() as u8).unwrap_or(0);
                    if dmg > 0 {
                        state.opponent.life = state.opponent.life.saturating_sub(dmg);
                        state.opponent.reserve.add(dmg, 0);
                    }
                }
            }
            state.active_attacker = None;
            state.active_blocker = None;
            state.phase = Phase::AttackStep(AttackSubPhase::End);
        }
        (Phase::AttackStep(AttackSubPhase::End), _) => {
            state.phase = Phase::AttackStep(AttackSubPhase::DeclareAttack);
        }
        (Phase::MainStep, Action::Kourin { card_id, target_id, payment, use_soul_core: _, placement }) |
        (Phase::AttackStep(AttackSubPhase::AttackFlash { .. }), Action::Kourin { card_id, target_id, payment, use_soul_core: _, placement }) => {
            let is_attack_flash = matches!(phase, Phase::AttackStep(AttackSubPhase::AttackFlash { .. }));
            let acting_side = if is_attack_flash {
                let priority = match phase {
                    Phase::AttackStep(AttackSubPhase::AttackFlash { priority, .. }) => priority,
                    _ => Priority::Attacker,
                };
                match priority {
                    Priority::Attacker => &mut state.player,
                    Priority::Defender => &mut state.opponent,
                }
            } else {
                &mut state.player
            };

            let card_idx = acting_side.hand.iter().position(|c| &c.id == card_id)
                .ok_or_else(|| "Card not in hand".to_string())?;
            let card = acting_side.hand.remove(card_idx);

            let mut collected_cores = 0;

            // ソウルコアの支払い処理
            for source in payment {
                if source.source_id == "Reserve" {
                    if acting_side.reserve.soul == 0 {
                        return Err("Soul core not in reserve".to_string());
                    }
                    acting_side.reserve.sub(0, 1);
                    acting_side.trash_cores.add(0, 1);
                } else {
                    let obj = acting_side.field.iter_mut().find(|o| &o.id == &source.source_id)
                        .ok_or_else(|| "Payment source object not found".to_string())?;
                    if obj.cores.soul == 0 {
                        return Err("Soul core not on object".to_string());
                    }
                    obj.cores.sub(0, 1);
                    acting_side.trash_cores.add(0, 1);
                }
            }

            // 指定された placement に従ってコアを収集
            for source in placement {
                let count = source.count;
                if count > 0 {
                    if source.source_id == "Reserve" {
                        if acting_side.reserve.normal() < count {
                            return Err(format!("Not enough normal cores in reserve: need {}, have {}", count, acting_side.reserve.normal()));
                        }
                        acting_side.reserve.sub(count, 0);
                        collected_cores += count;
                    } else {
                        let obj = acting_side.field.iter_mut().find(|o| &o.id == &source.source_id)
                            .ok_or_else(|| "Placement source object not found".to_string())?;
                        if obj.cores.normal() < count {
                            return Err(format!("Not enough normal cores on {}: need {}, have {}", source.source_id, count, obj.cores.normal()));
                        }
                        obj.cores.sub(count, 0);
                        collected_cores += count;
                    }
                }
            }

            // 3. 重ねる対象のオブジェクトを取得して更新する
            let obj = acting_side.field.iter_mut().find(|o| &o.id == target_id)
                .ok_or_else(|| "Target object not found".to_string())?;
            
            // 煌臨元のカードを退避
            let prev_card = Card {
                id: obj.id.clone(),
                name: obj.name.clone(),
                base_cost: 0,
                colors: obj.colors.clone(),
                reduction_symbols: vec![],
                card_type: obj.card_type,
                lv_costs: obj.lv_costs.clone(),
                symbols: obj.base_symbols.clone(),
                systems: obj.systems.clone(),
            };
            obj.under_cards.push(prev_card);
            
            // 煌臨スピリットの情報に書き換える
            obj.name = card.name.clone();
            obj.colors = card.colors.clone();
            obj.card_type = CardType::Spirit; // 煌臨スピリットになる
            obj.lv_costs = card.lv_costs.clone();
            obj.base_symbols = card.symbols.clone();
            obj.systems = card.systems.clone();

            // 収集したコアを追加
            if collected_cores > 0 {
                obj.cores.add(collected_cores, 0);
            }

            if phase != Phase::MainStep {
                let priority = match phase {
                    Phase::AttackStep(AttackSubPhase::AttackFlash { priority, .. }) => priority,
                    _ => Priority::Defender,
                };
                let next_priority = match priority {
                    Priority::Attacker => Priority::Defender,
                    Priority::Defender => Priority::Attacker,
                };
                state.phase = Phase::AttackStep(AttackSubPhase::AttackFlash {
                    priority: next_priority,
                    consecutive_passes: 0,
                });
            }

            check_and_process_depletion(acting_side);
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
                let mut placement_options = Vec::new();
                if card.card_type == CardType::Spirit || card.card_type == CardType::Ultimate {
                    placement_options.push(card.lv_costs[0]);
                } else if card.card_type == CardType::Nexus {
                    placement_options.push(0);
                    placement_options.push(1);
                } else {
                    placement_options.push(0);
                }

                for required_placement in placement_options {
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
            }

            // 2. MoveCore アクション
            if state.core_move_count_this_turn < 5 {
                let mut core_locations = vec!["Reserve".to_string()];
                for obj in &state.player.field {
                    core_locations.push(obj.id.clone());
                }

                for from in &core_locations {
                    for to in &core_locations {
                        if from == to {
                            continue;
                        }

                        if let Some((last_from, last_to)) = &state.last_move_core {
                            if last_from == to && last_to == from {
                                continue;
                            }
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
            }

            // 煌臨アクションの列挙（MainStep内 / CardRegistry経由）
            for card in &state.player.hand {
                if let Some(effect) = cards::CARD_REGISTRY.get(&card.id) {
                    if !effect.can_kourin_main_step() { continue; }
                    if effect.kourin_condition(&state.player).is_none() { continue; }
                    
                    let mut soul_pools = Vec::new();
                    if state.player.reserve.soul > 0 {
                        soul_pools.push("Reserve".to_string());
                    }
                    for obj in &state.player.field {
                        if obj.cores.soul > 0 {
                            soul_pools.push(obj.id.clone());
                        }
                    }

                    for src_id in &soul_pools {
                        for target in &state.player.field {
                            if effect.is_valid_kourin_target(target) {
                                // 煌臨元に元々乗っているコア数を算出
                                let inherited_cores = target.cores.normal() + target.cores.soul 
                                    - if src_id == &target.id { 1 } else { 0 };

                                // プール（リザーブ ＋ 他のフィールドオブジェクト）の作成
                                let mut pools = Vec::new();
                                pools.push(CorePool {
                                    id: "Reserve".to_string(),
                                    normal: state.player.reserve.normal(),
                                    soul: state.player.reserve.soul,
                                });
                                for obj in &state.player.field {
                                    if &obj.id != &target.id {
                                        pools.push(CorePool {
                                            id: obj.id.clone(),
                                            normal: obj.cores.normal(),
                                            soul: obj.cores.soul,
                                        });
                                    }
                                }

                                // ソウルコア支払い分のプール減少
                                for p in &mut pools {
                                    if &p.id == src_id {
                                        if p.soul > 0 {
                                            p.soul -= 1;
                                        }
                                    }
                                }

                                let total_available_normal: u8 = pools.iter().map(|p| p.normal).sum();

                                // 煌臨スピリットのレベル維持コストごとに必要な追加コア数を算出
                                let mut placement_options = Vec::new();
                                for &cost in &card.lv_costs {
                                    let needed = cost.saturating_sub(inherited_cores);
                                    if needed <= total_available_normal && !placement_options.contains(&needed) {
                                        placement_options.push(needed);
                                    }
                                }

                                for needed in placement_options {
                                    let placement_combinations = get_placement_combinations(&pools, needed, false);
                                    for placement_item in placement_combinations {
                                        actions.push(Action::Kourin {
                                            card_id: card.id.clone(),
                                            target_id: target.id.clone(),
                                            payment: vec![CoreSource {
                                                source_id: src_id.clone(),
                                                count: 0,
                                            }],
                                            use_soul_core: true,
                                            placement: placement_item,
                                        });
                                    }
                                }
                            }
                        }
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
                AttackSubPhase::AttackFlash { priority, .. } => {
                    let active_side = match priority {
                        Priority::Attacker => &state.player,
                        Priority::Defender => &state.opponent,
                    };
                    
                    // 煌臨アクションの列挙（Flash / CardRegistry経由）
                    for card in &active_side.hand {
                        if let Some(effect) = cards::CARD_REGISTRY.get(&card.id) {
                            if effect.kourin_condition(active_side).is_some() {
                                let mut soul_pools = Vec::new();
                                if active_side.reserve.soul > 0 {
                                    soul_pools.push("Reserve".to_string());
                                }
                                for obj in &active_side.field {
                                    if obj.cores.soul > 0 {
                                        soul_pools.push(obj.id.clone());
                                    }
                                }

                                for src_id in &soul_pools {
                                    for target in &active_side.field {
                                        if effect.is_valid_kourin_target(target) {
                                            // 煌臨元に元々乗っているコア数を算出
                                            let inherited_cores = target.cores.normal() + target.cores.soul 
                                                - if src_id == &target.id { 1 } else { 0 };

                                            // プール（リザーブ ＋ 他のフィールドオブジェクト）の作成
                                            let mut pools = Vec::new();
                                            pools.push(CorePool {
                                                id: "Reserve".to_string(),
                                                normal: active_side.reserve.normal(),
                                                soul: active_side.reserve.soul,
                                            });
                                            for obj in &active_side.field {
                                                if &obj.id != &target.id {
                                                    pools.push(CorePool {
                                                        id: obj.id.clone(),
                                                        normal: obj.cores.normal(),
                                                        soul: obj.cores.soul,
                                                    });
                                                }
                                            }

                                            // ソウルコア支払い分のプール減少
                                            for p in &mut pools {
                                                if &p.id == src_id {
                                                    if p.soul > 0 {
                                                        p.soul -= 1;
                                                    }
                                                }
                                            }

                                            let total_available_normal: u8 = pools.iter().map(|p| p.normal).sum();

                                            // 煌臨スピリットのレベル維持コストごとに必要な追加コア数を算出
                                            let mut placement_options = Vec::new();
                                            for &cost in &card.lv_costs {
                                                let needed = cost.saturating_sub(inherited_cores);
                                                if needed <= total_available_normal && !placement_options.contains(&needed) {
                                                    placement_options.push(needed);
                                                }
                                            }

                                            for needed in placement_options {
                                                let placement_combinations = get_placement_combinations(&pools, needed, false);
                                                for placement_item in placement_combinations {
                                                    actions.push(Action::Kourin {
                                                        card_id: card.id.clone(),
                                                        target_id: target.id.clone(),
                                                        payment: vec![CoreSource {
                                                            source_id: src_id.clone(),
                                                            count: 0,
                                                        }],
                                                        use_soul_core: true,
                                                        placement: placement_item,
                                                    });
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    for card in &active_side.hand {
                        if card.card_type == CardType::Magic {
                            let reduction = calculate_reduction(card, &active_side.field);
                            let cost_to_pay = card.base_cost.saturating_sub(reduction);
                            if active_side.reserve.total >= cost_to_pay {
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
                    actions.push(Action::Pass);
                }
                AttackSubPhase::DeclareBlock => {
                    for obj in &state.opponent.field {
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
        Phase::ResolveFaraEffect { .. } => {
            actions.push(Action::ResolveFaraToken { summon: false });
            let token_needed = if !state.token_summoned_this_turn { 0 } else { 1 };
            let token_count = state.player.token_pool.iter().filter(|c| c.id == "BS76-T001").count();
            eprintln!("[DEBUG ResolveFara] token_pool(BS76-T001): {}, token_summoned_this_turn: {}, token_needed: {}",
                token_count, state.token_summoned_this_turn, token_needed);
            if state.player.token_pool.iter().any(|c| c.id == "BS76-T001") {
                let mut available = state.player.reserve.normal();
                for other in &state.player.field {
                    if other.card_type == CardType::Nexus {
                        available += other.cores.normal();
                    } else {
                        let lv1 = other.lv_costs.get(0).copied().unwrap_or(1);
                        if other.cores.normal() > lv1 {
                            available += other.cores.normal() - lv1;
                        }
                    }
                }
                eprintln!("[DEBUG ResolveFara] available_cores: {}, condition: {} >= {}", available, available, token_needed);
                if available >= token_needed {
                    actions.push(Action::ResolveFaraToken { summon: true });
                }
            }
        }
        Phase::ResolveBasiliskEffect { .. } => {
            actions.push(Action::ResolveBasilisk { use_effect: false, destroy_bug: false });
            actions.push(Action::ResolveBasilisk { use_effect: true, destroy_bug: false });
            let has_bug = state.player.field.iter().any(|o| o.name == "プラチナム・バグ");
            if has_bug {
                actions.push(Action::ResolveBasilisk { use_effect: true, destroy_bug: true });
            }
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
// 安全なカードおよびオブジェクト探索ヘルパー
// ----------------------------------------------------
fn find_card_in_state(state: &GameState, card_id: &str) -> Option<Card> {
    state.player.hand.iter().find(|c| &c.id == card_id)
        .or_else(|| state.opponent.hand.iter().find(|c| &c.id == card_id))
        .or_else(|| state.player.opened.iter().find(|c| &c.id == card_id))
        .or_else(|| state.opponent.opened.iter().find(|c| &c.id == card_id))
        .or_else(|| state.player.token_pool.iter().find(|c| &c.id == card_id))
        .or_else(|| state.opponent.token_pool.iter().find(|c| &c.id == card_id))
        .cloned()
}

fn find_field_object_in_state<'a>(state: &'a GameState, obj_id: &str) -> Option<&'a FieldObject> {
    state.player.field.iter().chain(&state.opponent.field)
        .find(|o| &o.id == obj_id)
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
    let mut systems = Vec::new();
    let mut in_systems = false;

    for line in content.lines() {
        let trimmed = line.trim();
        if trimmed.starts_with("systems:") {
            in_systems = true;
            in_lv_info = false;
        } else if in_systems && trimmed.starts_with("-") {
            let sys = trimmed["-".len()..].trim().trim_matches('"').to_string();
            systems.push(sys);
        } else if in_systems && !trimmed.starts_with("-") && !trimmed.is_empty() {
            in_systems = false;
        }
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
        systems,
    })
}

// ----------------------------------------------------
// デッキ定義ファイルからのカードロード機能
// ----------------------------------------------------
#[derive(Debug, Deserialize)]
struct YamlDeck {
    cards: std::collections::HashMap<String, usize>,
    #[serde(default)]
    tokens: std::collections::HashMap<String, usize>,
}

/// デッキファイルを読み込み、(deckカード一覧, tokenプール) を返す
pub fn load_deck_from_file(filename: &str) -> Result<(Vec<Card>, Vec<Card>), String> {
    let path = filename;
    let content = std::fs::read_to_string(path)
        .map_err(|e| format!("Failed to read deck file {}: {}", path, e))?;

    let yaml_deck: YamlDeck = serde_yaml::from_str(&content)
        .map_err(|e| format!("Failed to parse deck file {}: {}", path, e))?;

    let mut deck = Vec::new();
    for (card_id, count) in &yaml_deck.cards {
        if let Ok(card) = load_card_from_yaml(card_id) {
            for _ in 0..*count {
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
                systems: vec![],
            };
            for _ in 0..*count {
                deck.push(placeholder.clone());
            }
        }
    }

    let mut token_pool = Vec::new();
    for (card_id, count) in &yaml_deck.tokens {
        if let Ok(card) = load_card_from_yaml(card_id) {
            for _ in 0..*count {
                token_pool.push(card.clone());
            }
        } else {
            eprintln!("警告: トークンカード {} のYAMLが見つかりません。スキップします。", card_id);
        }
    }

    Ok((deck, token_pool))
}


// 疑似乱数シャッフル
pub fn pseudo_shuffle(deck: &mut Vec<Card>) {
    let mut seed: u64 = 123456789;
    for i in (1..deck.len()).rev() {
        seed = seed.wrapping_mul(6364136223846793005).wrapping_add(1442695040888963407);
        let j = (seed % (i as u64 + 1)) as usize;
        deck.swap(i, j);
    }
}

pub fn setup_initial_state(deck1_path: &str, deck2_path: &str) -> Result<GameState, String> {
    let (mut deck, token_pool1) = load_deck_from_file(deck1_path)?;
    let target_fixed_ids = vec![
        "BS76-CX03".to_string(),
        "BS75-CX03".to_string(), "BS75-042".to_string(), "BS75-043".to_string(), "BS75-068".to_string(),
        "BS49-X09".to_string(), "BS49-040".to_string(), "BS49-X04".to_string(), "SD56-RV009".to_string(),
        "26RSD03-X01".to_string(), "26RSD03-X02".to_string(), "26RSD03-001".to_string(), "26RSD03-003".to_string()
    ];

    let mut hand = Vec::new();
    for card_id in &target_fixed_ids {
        if hand.len() >= 4 { break; }
        if let Some(idx) = deck.iter().position(|c| &c.id == card_id) {
            let card = deck.remove(idx);
            hand.push(card);
        }
    }
    
    pseudo_shuffle(&mut deck);
    while hand.len() < 4 && !deck.is_empty() {
        hand.push(deck.remove(0));
    }

    let (mut opponent_deck, token_pool2) = load_deck_from_file(deck2_path)?;
    let mut opponent_hand = Vec::new();
    for card_id in &target_fixed_ids {
        if opponent_hand.len() >= 4 { break; }
        if let Some(idx) = opponent_deck.iter().position(|c| &c.id == card_id) {
            let card = opponent_deck.remove(idx);
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
        reserve: Cores::new(4, 1),
        field: vec![],
        hand,
        trash: vec![],
        trash_cores: Cores::new(0, 0),
        opened: deck,
        token_pool: token_pool1,
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
        token_pool: token_pool2,
        count: 0,
    };
    println!("【デッキ情報】プレイヤー1 デッキ総枚数: {}枚 (山札: {}枚, 手札: {}枚), トークンプール: {}枚", player.opened.len() + player.hand.len(), player.opened.len(), player.hand.len(), player.token_pool.len());
    println!("【デッキ情報】プレイヤー2 デッキ総枚数: {}枚 (山札: {}枚, 手札: {}枚), トークンプール: {}枚", opponent.opened.len() + opponent.hand.len(), opponent.opened.len(), opponent.hand.len(), opponent.token_pool.len());

    let mut state = GameState {
        player,
        opponent,
        phase: Phase::StartStep,
        turn_count: 1,
        active_attacker: None,
        active_blocker: None,
        token_summoned_this_turn: false,
        last_move_core: None,
        core_move_count_this_turn: 0,
    };

    process_automatic_steps(&mut state);
    Ok(state)
}

fn calculate_state_hash(state: &GameState) -> u64 {
    use std::collections::hash_map::DefaultHasher;
    use std::hash::{Hash, Hasher};
    let mut hasher = DefaultHasher::new();
    format!("{:?}", state.phase).hash(&mut hasher);
    state.turn_count.hash(&mut hasher);
    state.player.life.hash(&mut hasher);
    state.player.reserve.total.hash(&mut hasher);
    state.player.reserve.soul.hash(&mut hasher);
    state.player.trash_cores.total.hash(&mut hasher);
    state.player.trash_cores.soul.hash(&mut hasher);
    state.opponent.life.hash(&mut hasher);
    state.opponent.reserve.total.hash(&mut hasher);
    state.opponent.reserve.soul.hash(&mut hasher);
    state.opponent.trash_cores.total.hash(&mut hasher);
    state.opponent.trash_cores.soul.hash(&mut hasher);
    for obj in &state.player.field {
        obj.id.hash(&mut hasher);
        obj.cores.total.hash(&mut hasher);
        obj.cores.soul.hash(&mut hasher);
        obj.is_exhausted.hash(&mut hasher);
    }
    for obj in &state.opponent.field {
        obj.id.hash(&mut hasher);
        obj.cores.total.hash(&mut hasher);
        obj.cores.soul.hash(&mut hasher);
        obj.is_exhausted.hash(&mut hasher);
    }
    hasher.finish()
}

fn is_same_state(s1: &GameState, s2: &GameState) -> bool {
    s1.phase == s2.phase 
        && s1.turn_count == s2.turn_count
        && s1.player.life == s2.player.life
        && s1.player.reserve.total == s2.player.reserve.total
        && s1.player.reserve.soul == s2.player.reserve.soul
        && s1.player.trash_cores.total == s2.player.trash_cores.total
        && s1.player.trash_cores.soul == s2.player.trash_cores.soul
        && s1.opponent.life == s2.opponent.life
        && s1.opponent.reserve.total == s2.opponent.reserve.total
        && s1.opponent.reserve.soul == s2.opponent.reserve.soul
        && s1.opponent.trash_cores.total == s2.opponent.trash_cores.total
        && s1.opponent.trash_cores.soul == s2.opponent.trash_cores.soul
        && s1.player.field.len() == s2.player.field.len()
        && s1.player.field.iter().zip(&s2.player.field).all(|(a, b)| {
            a.id == b.id 
                && a.cores.total == b.cores.total 
                && a.cores.soul == b.cores.soul
                && a.is_exhausted == b.is_exhausted
                && a.under_cards.len() == b.under_cards.len()
                && a.under_cards.iter().zip(&b.under_cards).all(|(x, y)| x.id == y.id)
        })
        && s1.opponent.field.len() == s2.opponent.field.len()
        && s1.opponent.field.iter().zip(&s2.opponent.field).all(|(a, b)| {
            a.id == b.id 
                && a.cores.total == b.cores.total 
                && a.cores.soul == b.cores.soul
                && a.is_exhausted == b.is_exhausted
                && a.under_cards.len() == b.under_cards.len()
                && a.under_cards.iter().zip(&b.under_cards).all(|(x, y)| x.id == y.id)
        })
}

#[derive(Clone, PartialEq, Eq)]
pub enum ActionGroup {
    PlayCardGroup(String, String), // CardId, Name
    MoveCoreGroup,
    AttackGroup(String, String), // ObjectId, Name
    BlockGroup(String, String),  // ObjectId, Name
    ResolveFaraTokenGroup(bool), // summon
    ResolveBasiliskGroup(bool, bool), // use_effect, destroy_bug
    KourinGroup(String, String, String), // CardId, TargetId, TargetName
}

pub fn is_forbidden_action(action: &Action, state: &GameState, visited: &[(GameState, u64)]) -> bool {
    let mut next_state = state.clone();
    if apply_action(&mut next_state, action).is_ok() {
        let next_hash = calculate_state_hash(&next_state);
        visited.iter().any(|(prev_state, prev_hash)| {
            *prev_hash == next_hash && is_same_state(prev_state, &next_state)
        })
    } else {
        false
    }
}

pub fn is_group_forbidden(group: &ActionGroup, state: &GameState, visited: &[(GameState, u64)], actions: &[Action]) -> bool {
    let mut group_actions = Vec::new();
    for action in actions {
        let is_match = match (group, action) {
            (ActionGroup::PlayCardGroup(card_id, _), Action::PlayCard { card_id: action_card_id, .. }) => card_id == action_card_id,
            (ActionGroup::MoveCoreGroup, Action::MoveCore { .. }) => true,
            (ActionGroup::AttackGroup(id, _), Action::Attack { object_id, .. }) => id == object_id,
            (ActionGroup::BlockGroup(id, _), Action::Block { object_id }) => id == object_id,
            (ActionGroup::ResolveFaraTokenGroup(s1), Action::ResolveFaraToken { summon: s2 }) => s1 == s2,
            (ActionGroup::ResolveBasiliskGroup(u1, d1), Action::ResolveBasilisk { use_effect: u2, destroy_bug: d2 }) => u1 == u2 && d1 == d2,
            (ActionGroup::KourinGroup(c1, t1, _), Action::Kourin { card_id: c2, target_id: t2, .. }) => c1 == c2 && t1 == t2,
            _ => false
        };
        if is_match {
            group_actions.push(action);
        }
    }
    if group_actions.is_empty() {
        return false;
    }
    group_actions.iter().all(|act| is_forbidden_action(act, state, visited))
}

// ----------------------------------------------------
// 対話型CLIゲームループの実装 (二段階入力ウィザード)
// ----------------------------------------------------
fn run_interactive_loop(
    deck1_path: &str,
    deck2_path: &str,
    model: Option<&ai::model::BoardEvaluator<burn::backend::NdArray>>,
    device: &<burn::backend::NdArray as burn::tensor::backend::Backend>::Device,
) {
    println!("=== TCG BattleSpirits デッキシミュレータ (エターナルフォーマット) ===");

    let mut state = match setup_initial_state(deck1_path, deck2_path) {
        Ok(s) => s,
        Err(e) => {
            println!("初期状態の構築に失敗しました: {}", e);
            return;
        }
    };
    
    println!("プレイヤー1の初期手札を設定しました。山札残り: {}枚", state.player.opened.len());
    println!("プレイヤー2の初期手札を設定しました。山札残り: {}枚", state.opponent.opened.len());

    let mut game_history: Vec<LogEntry> = Vec::new();
    let mut visited_states: Vec<(GameState, u64)> = Vec::new();

    loop {
        let current_hash = calculate_state_hash(&state);
        visited_states.push((state.clone(), current_hash));
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
        print!("({}/5)  [相手ライフ: ", state.player.life);
        for _ in 0..state.opponent.life {
            print!("■ ");
        }
        println!("({}/5)]", state.opponent.life);
        println!("リザーブコア: {} (相手: {})", state.player.reserve.format(), state.opponent.reserve.format());
        println!("トラッシュコア: {} (相手: {})", state.player.trash_cores.format(), state.opponent.trash_cores.format());
        println!("フィールド (自分):");
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
        println!("フィールド (相手):");
        if state.opponent.field.is_empty() {
            println!("  (なし)");
        } else {
            for obj in &state.opponent.field {
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

        // 選択肢がない(n,qのみ)の場合は選択は行わずその選択肢を実施
        let has_end = actions.contains(&Action::EndStep);
        let has_pass = actions.contains(&Action::Pass);
        let has_category_actions = actions.iter().any(|act| matches!(act, 
            Action::PlayCard { .. } | Action::MoveCore { .. } | Action::Attack { .. } | Action::Block { .. }
        ));

        if !has_category_actions {
            if has_end {
                println!("選択可能なアクション（EndStep/Passを除く）がありません。自動的にステップ終了を実行します。");
                println!(">> アクションを実行: EndStep");
                if let Err(e) = apply_action(&mut state, &Action::EndStep) {
                    println!("自動遷移エラー: {}", e);
                }
                process_automatic_steps(&mut state);
                continue;
            } else if has_pass {
                println!("選択可能なアクション（EndStep/Passを除く）がありません。自動的にパス/スキップを実行します。");
                println!(">> アクションを実行: Pass");
                if let Err(e) = apply_action(&mut state, &Action::Pass) {
                    println!("自動遷移エラー: {}", e);
                }
                process_automatic_steps(&mut state);
                continue;
            }
        }

        // 選択可能なアクションを一括で列挙して表示（コア移動等もすべて個別の選択肢）
        let display_actions: Vec<Action> = actions.iter()
            .filter(|act| !matches!(act, Action::EndStep | Action::Pass))
            .cloned()
            .collect();

        let mut seen_labels = std::collections::HashSet::new();
        let mut unique_display_actions = Vec::new();
        for action in display_actions {
            let label = match &action {
                Action::PlayCard { card_id, payment, use_soul_core, placement, placement_soul_core } => {
                    let card = find_card_in_state(&state, card_id);
                    let card_name = card.as_ref().map(|c| c.name.as_str()).unwrap_or("不明なカード");
                    let pay_amount: u8 = payment.iter().map(|p| p.count).sum();
                    let pay_soul = if *use_soul_core { 1 } else { 0 };
                    let pay_cores = Cores::new(pay_amount + pay_soul, pay_soul);
                    
                    let place_amount: u8 = placement.iter().map(|p| p.count).sum();
                    let place_soul = if *placement_soul_core { 1 } else { 0 };
                    let place_cores = Cores::new(place_amount + place_soul, place_soul);
                    
                    let total_str = format_sources(&state, payment, *use_soul_core, placement, *placement_soul_core);
                    format!("PlayCard:{} pay:{} place:{} sources:{}", card_name, pay_cores.format(), place_cores.format(), total_str)
                }
                Action::Kourin { card_id, target_id, payment, use_soul_core: _, placement } => {
                    let card = find_card_in_state(&state, card_id);
                    let card_name = card.as_ref().map(|c| c.name.as_str()).unwrap_or("不明なカード");
                    let target = find_field_object_in_state(&state, target_id);
                    let target_name = target.as_ref().map(|o| o.name.as_str()).unwrap_or("不明な対象");
                    let pay_amount: u8 = payment.iter().map(|p| p.count).sum();
                    let pay_cores = Cores::new(pay_amount + 1, 1);
                    let place_amount: u8 = placement.iter().map(|p| p.count).sum();
                    let place_cores = Cores::new(place_amount, 0);
                    let total_str = format_sources(&state, payment, true, placement, false);
                    format!("Kourin:{} target:{} pay:{} place:{} sources:{}", card_name, target_name, pay_cores.format(), place_cores.format(), total_str)
                }
                Action::MoveCore { from, to, normal_cores, soul_core } => {
                    format!("MoveCore:{}->{} normal:{} soul:{}", from, to, normal_cores, soul_core)
                }
                Action::Attack { object_id } => {
                    format!("Attack:{}", object_id)
                }
                Action::Block { object_id } => {
                    format!("Block:{}", object_id)
                }
                Action::ResolveFaraToken { summon } => {
                    format!("ResolveFaraToken:{}", summon)
                }
                Action::ResolveBasilisk { use_effect, destroy_bug } => {
                    format!("ResolveBasilisk:use_effect={},destroy_bug={}", use_effect, destroy_bug)
                }
                _ => format!("{:?}", action),
            };

            if seen_labels.insert(label) {
                unique_display_actions.push(action);
            }
        }
        let display_actions = unique_display_actions;

        // 評価値を事前に計算
        let mut evaluated_actions: Vec<(Action, f32)> = display_actions.iter().map(|action| {
            let mut val = 0.0;
            if let Some(m) = model {
                if let Some(mut eval) = evaluate_action(m, &state, action, device) {
                    if is_forbidden_action(action, &state, &visited_states) {
                        eval = -2.0;
                    }
                    val = eval;
                }
            }
            (action.clone(), val)
        }).collect();

        // すべて評価値の降順（大きい順）にする
        evaluated_actions.sort_by(|a, b| {
            b.1.partial_cmp(&a.1).unwrap_or(std::cmp::Ordering::Equal)
        });

        let display_actions: Vec<Action> = evaluated_actions.iter().map(|(a, _)| a.clone()).collect();

        println!("選択可能なアクション:");
        for (idx, (action, eval_val)) in evaluated_actions.iter().enumerate() {
            let eval_str = if model.is_some() {
                format!(" [AI評価値: {:.3}]", eval_val)
            } else {
                "".to_string()
            };

            let is_forbidden = is_forbidden_action(action, &state, &visited_states);
            let forbidden_prefix = if is_forbidden { "🚫[同一盤面につき禁止] " } else { "" };

            match action {
                Action::PlayCard { card_id, payment, use_soul_core, placement, placement_soul_core } => {
                    let card = find_card_in_state(&state, card_id);
                    let card_name = card.as_ref().map(|c| c.name.as_str()).unwrap_or("不明なカード");
                    let card_type = card.as_ref().map(|c| c.card_type).unwrap_or(CardType::Magic);
                    let action_label = match card_type {
                        CardType::Spirit | CardType::Ultimate | CardType::Brave => "召喚",
                        CardType::Nexus => "配置",
                        CardType::Magic => "使用",
                    };
                    let pay_amount: u8 = payment.iter().map(|p| p.count).sum();
                    let pay_soul = if *use_soul_core { 1 } else { 0 };
                    let pay_cores = Cores::new(pay_amount + pay_soul, pay_soul);
                    
                    let place_amount: u8 = placement.iter().map(|p| p.count).sum();
                    let place_soul = if *placement_soul_core { 1 } else { 0 };
                    let place_cores = Cores::new(place_amount + place_soul, place_soul);
                    
                    let total_str = format_sources(&state, payment, *use_soul_core, placement, *placement_soul_core);
                    
                    println!("  {}: {}【手札から{}】 {} (コスト:{}, 配置コア:{}, {}){}", 
                        idx + 1, 
                        forbidden_prefix,
                        action_label, 
                        card_name,
                        pay_cores.format(), 
                        place_cores.format(), 
                        total_str,
                        eval_str
                    );
                }
                Action::Kourin { card_id, target_id, payment, use_soul_core: _, placement } => {
                    let card = find_card_in_state(&state, card_id);
                    let card_name = card.as_ref().map(|c| c.name.as_str()).unwrap_or("不明なカード");
                    let target = find_field_object_in_state(&state, target_id);
                    let target_name = target.as_ref().map(|o| o.name.as_str()).unwrap_or("不明な対象");
                    let pay_amount: u8 = payment.iter().map(|p| p.count).sum();
                    let pay_cores = Cores::new(pay_amount + 1, 1);
                    let place_amount: u8 = placement.iter().map(|p| p.count).sum();
                    let place_cores = Cores::new(place_amount, 0);
                    let total_str = format_sources(&state, payment, true, placement, false);
                    println!("  {}: {}【手札から煌臨】 {} を {} に重ねて煌臨 (コスト:{}, 配置コア:{}, {}){}", 
                        idx + 1, forbidden_prefix, card_name, target_name, pay_cores.format(), place_cores.format(), total_str, eval_str);
                }
                Action::MoveCore { from, to, normal_cores, soul_core } => {
                    let from_name = if from == "Reserve" {
                        "リザーブ".to_string()
                    } else {
                        find_field_object_in_state(&state, from)
                            .map(|o| o.name.clone())
                            .unwrap_or(from.clone())
                    };
                    let to_name = if to == "Reserve" {
                        "リザーブ".to_string()
                    } else {
                        find_field_object_in_state(&state, to)
                            .map(|o| o.name.clone())
                            .unwrap_or(to.clone())
                    };
                    let move_cores = Cores::new(*normal_cores + if *soul_core { 1 } else { 0 }, if *soul_core { 1 } else { 0 });
                    println!("  {}: {}【コア移動】 {} -> {} (コア:{}){}", idx + 1, forbidden_prefix, from_name, to_name, move_cores.format(), eval_str);
                }
                Action::Attack { object_id } => {
                    let obj = find_field_object_in_state(&state, object_id);
                    let obj_name = obj.map(|o| o.name.as_str()).unwrap_or("不明な対象");
                    println!("  {}: {}【アタック宣言】 {}{}", idx + 1, forbidden_prefix, obj_name, eval_str);
                }
                Action::Block { object_id } => {
                    let obj = find_field_object_in_state(&state, object_id);
                    let obj_name = obj.map(|o| o.name.as_str()).unwrap_or("不明な対象");
                    println!("  {}: {}【ブロック宣言】 {}{}", idx + 1, forbidden_prefix, obj_name, eval_str);
                }
                Action::ResolveFaraToken { summon } => {
                    let text = if *summon {
                        "ファラの効果：トークン「プラチナム・バグ」を召喚する"
                    } else {
                        "ファラの効果：トークンを召喚しない（パス）"
                    };
                    println!("  {}: {}【効果解決】 {}{}", idx + 1, forbidden_prefix, text, eval_str);
                }
                _ => {
                    println!("  {}: {}{:?}{}", idx + 1, forbidden_prefix, action, eval_str);
                }
            }
        }
        
        let has_end = actions.contains(&Action::EndStep);
        let has_pass = actions.contains(&Action::Pass);
        
        let mut end_eval_str = "".to_string();
        let mut pass_eval_str = "".to_string();
        if let Some(m) = model {
            if has_end {
                if let Some(mut val) = evaluate_action(m, &state, &Action::EndStep, device) {
                    let mut next_state = state.clone();
                    if apply_action(&mut next_state, &Action::EndStep).is_ok() {
                        let next_hash = calculate_state_hash(&next_state);
                        if visited_states.iter().any(|(prev_state, prev_hash)| {
                            *prev_hash == next_hash && is_same_state(prev_state, &next_state)
                        }) {
                            val = -2.0;
                        }
                    }
                    end_eval_str = format!(" [AI評価値: {:.3}]", val);
                }
            }
            if has_pass {
                if let Some(mut val) = evaluate_action(m, &state, &Action::Pass, device) {
                    let mut next_state = state.clone();
                    if apply_action(&mut next_state, &Action::Pass).is_ok() {
                        let next_hash = calculate_state_hash(&next_state);
                        if visited_states.iter().any(|(prev_state, prev_hash)| {
                            *prev_hash == next_hash && is_same_state(prev_state, &next_state)
                        }) {
                            val = -2.0;
                        }
                    }
                    pass_eval_str = format!(" [AI評価値: {:.3}]", val);
                }
            }
        }

        let is_end_forbidden = has_end && is_forbidden_action(&Action::EndStep, &state, &visited_states);
        let is_pass_forbidden = has_pass && is_forbidden_action(&Action::Pass, &state, &visited_states);
        let end_forbidden_suffix = if is_end_forbidden { " 🚫[同一盤面につき禁止]" } else { "" };
        let pass_forbidden_suffix = if is_pass_forbidden { " 🚫[同一盤面につき禁止]" } else { "" };

        if has_end {
            println!("  n: ステップ終了{}{}", end_eval_str, end_forbidden_suffix);
        } else if has_pass {
            println!("  n: パス / スキップ{}{}", pass_eval_str, pass_forbidden_suffix);
        }
        println!("  q: サレンダー（ゲームを終了する）");
        if model.is_some() {
            println!("  Enter/a: AI自動決定 (最善手を選択して実行)");
        }

        if has_end || has_pass {
            if model.is_some() {
                print!("\n選択してください [1-{}, n:終了, q:サレンダー, Enter:AI]: ", display_actions.len());
            } else {
                print!("\n選択してください [1-{}, n:終了, q:サレンダー]: ", display_actions.len());
            }
        } else {
            if model.is_some() {
                print!("\n選択してください [1-{}, q:サレンダー, Enter:AI]: ", display_actions.len());
            } else {
                print!("\n選択してください [1-{}, q:サレンダー]: ", display_actions.len());
            }
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

        if (trimmed.is_empty() || trimmed.to_lowercase() == "a") && model.is_some() {
            let m = model.unwrap();
            let mut best_action: Option<&Action> = None;
            let mut best_val = -9999.0;
            for action in &actions {
                if is_forbidden_action(action, &state, &visited_states) {
                    continue;
                }
                if let Some(val) = evaluate_action(m, &state, action, device) {
                    if val > best_val {
                        best_val = val;
                        best_action = Some(action);
                    }
                }
            }
            if let Some(act) = best_action {
                println!(">> AI自動決定（評価値: {:.3}）: {:?}", best_val, act);
                if let Err(e) = apply_action(&mut state, act) {
                    println!("自動遷移エラー: {}", e);
                }
                process_automatic_steps(&mut state);
                continue;
            } else {
                println!("AIによる選択肢（非禁止アクション）の評価に失敗しました。");
                continue;
            }
        }

        if trimmed.to_lowercase() == "q" {
            println!("サレンダーしました。ゲームオーバー。");
            break;
        }

        if trimmed.to_lowercase() == "n" {
            if actions.contains(&Action::EndStep) {
                if is_end_forbidden {
                    println!("🚫 このアクションは同一盤面に遷移するため選択できません。");
                    continue;
                }
                println!(">> アクションを実行: {:?}", Action::EndStep);
                if let Err(e) = apply_action(&mut state, &Action::EndStep) {
                    println!("エラー: {}", e);
                }
                process_automatic_steps(&mut state);
                continue;
            } else if actions.contains(&Action::Pass) {
                if is_pass_forbidden {
                    println!("🚫 このアクションは同一盤面に遷移するため選択できません。");
                    continue;
                }
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

        let choice = match trimmed.parse::<usize>() {
            Ok(num) if num >= 1 && num <= display_actions.len() => num - 1,
            _ => {
                println!("無効な入力です。");
                continue;
            }
        };

        let chosen_action = &display_actions[choice];
        if is_forbidden_action(chosen_action, &state, &visited_states) {
            println!("🚫 このアクションは同一盤面に遷移するため選択できません。");
            continue;
        }

        println!(">> アクションを実行: {:?}", chosen_action);
        if let Err(e) = apply_action(&mut state, chosen_action) {
            println!("エラー: {}", e);
        }
        process_automatic_steps(&mut state);

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
        Phase::ResolveFaraEffect { .. } => "resolveFaraEffect".to_string(),
        Phase::ResolveBasiliskEffect { .. } => "resolveBasiliskEffect".to_string(),
    }
}

fn main() {
    let args: Vec<String> = std::env::args().collect();
    
    if args.len() > 1 && args[1] == "train" {
        ai::train::train_model();
        return;
    }

    let deck_args: Vec<&str> = args.iter()
        .skip(1)
        .map(|s| s.as_str())
        .collect();

    let deck1_path = deck_args.get(0).unwrap_or(&"deck.yaml");
    let deck2_path = deck_args.get(1).unwrap_or(&"deck2.yaml");
    
    type MyBackend = burn::backend::NdArray;
    let device = Default::default();
    let model_path = "tmp/bs_model/checkpoint/model-10";
    let model = if std::path::Path::new("tmp/bs_model/checkpoint/model-10.mpk").exists() {
        println!("学習済みモデル weights をロードしています: {}.mpk", model_path);
        let record = CompactRecorder::new()
            .load(model_path.into(), &device)
            .expect("Failed to load model weights");
        let config = ai::model::BoardEvaluatorConfig::new();
        Some(config.init::<MyBackend>(&device).load_record(record))
    } else {
        println!("警告: 学習済みモデル weights が見つかりません。評価値は表示されません。");
        None
    };


    run_interactive_loop(deck1_path, deck2_path, model.as_ref(), &device);
}

#[cfg(test)]
mod flash_kourin_tests {
    use super::*;
    use crate::cards::CARD_REGISTRY;

    fn make_flash_test_state() -> GameState {
        let fara = FieldObject {
            id: "BS76-CX03_1".to_string(),
            name: "光虫の旗手ファラ".to_string(),
            colors: vec![Color::White],
            card_type: CardType::Nexus,
            cores: Cores::new(1, 0),
            is_exhausted: false,
            lv_costs: vec![0],
            base_symbols: vec![Color::White],
            systems: vec!["フラッグ".to_string(), "光契約".to_string(), "旗種".to_string()],
            under_cards: vec![],
        };

        let bug = FieldObject {
            id: "BS76-T001_2".to_string(),
            name: "プラチナム・バグ".to_string(),
            colors: vec![Color::White],
            card_type: CardType::Spirit,
            cores: Cores::new(1, 0),
            is_exhausted: true,
            lv_costs: vec![1],
            base_symbols: vec![Color::White],
            systems: vec!["フラッグ".to_string()],
            under_cards: vec![],
        };

        let ageha = Card {
            id: "BS76-038".to_string(),
            name: "プラチナム・アゲハ".to_string(),
            base_cost: 5,
            colors: vec![Color::White],
            reduction_symbols: vec![Color::White, Color::White, Color::White],
            card_type: CardType::Spirit,
            lv_costs: vec![1, 2, 4],
            symbols: vec![Color::White],
            systems: vec!["フラッグ".to_string()],
        };

        GameState {
            player: SideState {
                player_id: 1,
                life: 5,
                reserve: Cores::new(3, 1),
                field: vec![fara, bug],
                hand: vec![ageha],
                trash: vec![],
                trash_cores: Cores::new(0, 0),
                opened: vec![],
                token_pool: vec![],
                count: 1,
            },
            opponent: SideState {
                player_id: 2,
                life: 5,
                reserve: Cores::new(4, 1),
                field: vec![],
                hand: vec![],
                trash: vec![],
                trash_cores: Cores::new(0, 0),
                opened: vec![],
                token_pool: vec![],
                count: 0,
            },
            phase: Phase::AttackStep(AttackSubPhase::AttackFlash {
                priority: Priority::Attacker,
                consecutive_passes: 0,
            }),
            turn_count: 3,
            active_attacker: Some("BS76-T001_2".to_string()),
            active_blocker: None,
            token_summoned_this_turn: false,
            last_move_core: None,
            core_move_count_this_turn: 0,
        }
    }

    #[test]
    fn test_ageha_kourin_in_attack_flash() {
        let state = make_flash_test_state();
        let actions = generate_legal_actions(&state);
        let has_kourin = actions.iter().any(|a| {
            matches!(a, Action::Kourin { card_id, target_id, .. }
                if card_id == "BS76-038" && target_id.starts_with("BS76-CX03"))
        });
        assert!(has_kourin, "フラッシュでアゲハ→ファラへの煌臨が選択肢に含まれるべき");
    }

    #[test]
    fn test_ageha_kourin_blocked_at_count0() {
        let mut state = make_flash_test_state();
        state.player.count = 0;
        let actions = generate_legal_actions(&state);
        let has_kourin = actions.iter().any(|a| {
            matches!(a, Action::Kourin { card_id, .. } if card_id == "BS76-038")
        });
        assert!(!has_kourin, "カウント0では煌臨不可");
    }

    #[test]
    fn test_ageha_kourin_blocked_without_soul() {
        let mut state = make_flash_test_state();
        state.player.reserve = Cores::new(3, 0);
        let actions = generate_legal_actions(&state);
        let has_kourin = actions.iter().any(|a| {
            matches!(a, Action::Kourin { card_id, .. } if card_id == "BS76-038")
        });
        assert!(!has_kourin, "ソウルコアなしでは煌臨不可");
    }

    #[test]
    fn test_registry_has_ageha() {
        let effect = CARD_REGISTRY.get("BS76-038");
        assert!(effect.is_some(), "BS76-038がCardRegistryに登録済み");
        assert_eq!(effect.unwrap().card_id(), "BS76-038");
    }

    #[test]
    fn test_attack_flash_defender_play_magic_no_panic() {
        let fara = FieldObject {
            id: "BS76-CX03_1".to_string(),
            name: "光虫の旗手ファラ".to_string(),
            colors: vec![Color::White],
            card_type: CardType::Nexus,
            cores: Cores::new(1, 0),
            is_exhausted: false,
            lv_costs: vec![0],
            base_symbols: vec![Color::White],
            systems: vec!["フラッグ".to_string(), "光契約".to_string(), "旗種".to_string()],
            under_cards: vec![],
        };

        let bug = FieldObject {
            id: "BS76-T001_2".to_string(),
            name: "プラチナム・バグ".to_string(),
            colors: vec![Color::White],
            card_type: CardType::Spirit,
            cores: Cores::new(1, 0),
            is_exhausted: true,
            lv_costs: vec![1],
            base_symbols: vec![Color::White],
            systems: vec!["フラッグ".to_string()],
            under_cards: vec![],
        };

        let magic = Card {
            id: "BS76-086".to_string(),
            name: "ラージスクアッシュ".to_string(),
            base_cost: 4,
            colors: vec![Color::White],
            reduction_symbols: vec![Color::White, Color::White],
            card_type: CardType::Magic,
            lv_costs: vec![],
            symbols: vec![],
            systems: vec![],
        };

        let state = GameState {
            player: SideState {
                player_id: 1,
                life: 5,
                reserve: Cores::new(5, 1),
                field: vec![fara, bug],
                hand: vec![],
                trash: vec![],
                trash_cores: Cores::new(0, 0),
                opened: vec![],
                token_pool: vec![],
                count: 1,
            },
            opponent: SideState {
                player_id: 2,
                life: 4,
                reserve: Cores::new(6, 1),
                field: vec![],
                hand: vec![magic],
                trash: vec![],
                trash_cores: Cores::new(0, 0),
                opened: vec![],
                token_pool: vec![],
                count: 0,
            },
            phase: Phase::AttackStep(AttackSubPhase::AttackFlash {
                priority: Priority::Defender,
                consecutive_passes: 0,
            }),
            turn_count: 5,
            active_attacker: Some("BS76-T001_2".to_string()),
            active_blocker: None,
            token_summoned_this_turn: false,
            last_move_core: None,
            core_move_count_this_turn: 0,
        };

        let actions = generate_legal_actions(&state);
        let has_play_card = actions.iter().any(|a| matches!(a, Action::PlayCard { card_id, .. } if card_id == "BS76-086"));
        assert!(has_play_card);

        for action in &actions {
            match action {
                Action::PlayCard { card_id, payment, use_soul_core, placement, placement_soul_core } => {
                    let card = find_card_in_state(&state, card_id);
                    assert!(card.is_some());
                    assert_eq!(card.as_ref().unwrap().id, "BS76-086");
                    
                    let card_name = card.as_ref().map(|c| c.name.as_str()).unwrap_or("不明なカード");
                    let pay_amount: u8 = payment.iter().map(|p| p.count).sum();
                    let pay_soul = if *use_soul_core { 1 } else { 0 };
                    let pay_cores = Cores::new(pay_amount + pay_soul, pay_soul);
                    
                    let place_amount: u8 = placement.iter().map(|p| p.count).sum();
                    let place_soul = if *placement_soul_core { 1 } else { 0 };
                    let place_cores = Cores::new(place_amount + place_soul, place_soul);
                    
                    let total_str = format_sources(&state, payment, *use_soul_core, placement, *placement_soul_core);
                    let label = format!("PlayCard:{} pay:{} place:{} sources:{}", card_name, pay_cores.format(), place_cores.format(), total_str);
                    assert!(label.contains("ラージスクアッシュ"));
                }
                _ => {}
            }
        }
    }

    #[test]
    fn test_no_symbol_attacker_no_damage() {
        let bug = FieldObject {
            id: "BS76-T001_1".to_string(),
            name: "プラチナム・バグ".to_string(),
            colors: vec![Color::White],
            card_type: CardType::Spirit,
            cores: Cores::new(1, 0),
            is_exhausted: true,
            lv_costs: vec![1],
            base_symbols: vec![],
            systems: vec!["フラッグ".to_string()],
            under_cards: vec![],
        };

        let basilisk = FieldObject {
            id: "BS76-035_2".to_string(),
            name: "プラチナム・バシリスク".to_string(),
            colors: vec![Color::White],
            card_type: CardType::Spirit,
            cores: Cores::new(1, 0),
            is_exhausted: true,
            lv_costs: vec![1, 2, 5],
            base_symbols: vec![Color::White],
            systems: vec![],
            under_cards: vec![],
        };

        let mut state = GameState {
            player: SideState {
                player_id: 1,
                life: 5,
                reserve: Cores::new(0, 0),
                field: vec![bug.clone()],
                hand: vec![],
                trash: vec![],
                trash_cores: Cores::new(0, 0),
                opened: vec![],
                token_pool: vec![],
                count: 1,
            },
            opponent: SideState {
                player_id: 2,
                life: 5,
                reserve: Cores::new(0, 0),
                field: vec![],
                hand: vec![],
                trash: vec![],
                trash_cores: Cores::new(0, 0),
                opened: vec![],
                token_pool: vec![],
                count: 0,
            },
            phase: Phase::AttackStep(AttackSubPhase::BattleResolution),
            turn_count: 3,
            active_attacker: Some("BS76-T001_1".to_string()),
            active_blocker: None,
            token_summoned_this_turn: false,
            last_move_core: None,
            core_move_count_this_turn: 0,
        };

        let res = apply_action(&mut state, &Action::Pass);
        assert!(res.is_ok());
        assert_eq!(state.opponent.life, 5);
        assert_eq!(state.opponent.reserve.total, 0);

        let mut state_basilisk = GameState {
            player: SideState {
                player_id: 1,
                life: 5,
                reserve: Cores::new(0, 0),
                field: vec![basilisk.clone()],
                hand: vec![],
                trash: vec![],
                trash_cores: Cores::new(0, 0),
                opened: vec![],
                token_pool: vec![],
                count: 1,
            },
            opponent: SideState {
                player_id: 2,
                life: 5,
                reserve: Cores::new(0, 0),
                field: vec![],
                hand: vec![],
                trash: vec![],
                trash_cores: Cores::new(0, 0),
                opened: vec![],
                token_pool: vec![],
                count: 0,
            },
            phase: Phase::AttackStep(AttackSubPhase::BattleResolution),
            turn_count: 3,
            active_attacker: Some("BS76-035_2".to_string()),
            active_blocker: None,
            token_summoned_this_turn: false,
            last_move_core: None,
            core_move_count_this_turn: 0,
        };

        let res_basilisk = apply_action(&mut state_basilisk, &Action::Pass);
        assert!(res_basilisk.is_ok());
        assert_eq!(state_basilisk.opponent.life, 4);
        assert_eq!(state_basilisk.opponent.reserve.total, 1);
    }
}
