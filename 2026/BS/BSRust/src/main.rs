use serde::{Deserialize, Serialize};

/// コアの構成 (合計数, ソウルコア数)
#[derive(Debug, Serialize, Deserialize, Clone, Copy, PartialEq)]
struct Cores {
    total: u8,
    soul: u8, // 0 or 1
}

impl Cores {
    fn new(total: u8, soul: u8) -> Self {
        Self { total, soul }
    }
    fn normal(&self) -> u8 {
        self.total - self.soul
    }
}

/// プレイヤーごとの状態（自分・相手共通）
#[derive(Debug, Serialize, Deserialize, Clone)]
struct SideState {
    player_id: u8,
    life: u8,
    reserve: Cores,
    hand: Vec<Card>,
    field: Vec<FieldObject>,
    trash: Vec<Card>,      // トラッシュも追加
    opened: Vec<Card>,     // オープンされたカード等
    count: u8,             // カウント領域
}

/// ゲームステート (S)
#[derive(Debug, Serialize, Deserialize, Clone)]
struct GameState {
    player: SideState,      // 自分サイド
    opponent: SideState,    // 相手サイド
    phase: Phase,
    turn_count: u32,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
enum Phase {
    MainStep,
    AttackStep(AttackSubPhase),
}


#[derive(Debug, Serialize, Deserialize, Clone)]
enum AttackSubPhase {
    Start,
    Flash(Priority),
    BlockSelection,
    BattleResolution,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
enum Priority {
    Attacker,
    Defender,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
struct Card {
    id: String,
    name: String,
    base_cost: u8,
    reduction_symbols: u8,
    card_type: String, // Spirit, Nexus, etc.
}

#[derive(Debug, Serialize, Deserialize, Clone)]
struct FieldObject {
    id: String,
    name: String,
    lv: u8,
    cores: Cores,
    is_exhausted: bool,
    symbols: u8,
}

/// リーガルアクション (a)
#[derive(Debug, Serialize, Deserialize, PartialEq)]
enum Action {
    PlayCard { card_id: String, cost_paid: u8, soul_core_used: bool },
    Attack { object_id: String },
    Block { object_id: String },
    Pass,    // フラッシュのパス、またはブロックのスルー
    EndStep, // ステップの終了（何もしないで次へ）
}

/// リーガルアクション生成関数 f(S) -> A
fn generate_legal_actions(state: &GameState) -> Vec<Action> {
    let mut actions = Vec::new();

    // 仕様書 第7章「否定優先の原則」に基づき、強制・禁止効果を走査
    let must_attack = false;

    match &state.phase {
        Phase::MainStep => {
            // 1. カードのプレイ可能性判定 (コスト計算)
            for card in &state.player.hand {
                let available_total = state.player.reserve.total;
                let my_symbols: u8 = state.player.field.iter().map(|o| o.symbols).sum();
                let actual_reduction = my_symbols.min(card.reduction_symbols);
                let cost_to_pay = card.base_cost.saturating_sub(actual_reduction);

                if available_total >= cost_to_pay {
                    // ソウルコアを使用するかどうかの分岐を生成
                    if state.player.reserve.soul > 0 && cost_to_pay > 0 {
                        actions.push(Action::PlayCard {
                            card_id: card.id.clone(),
                            cost_paid: cost_to_pay,
                            soul_core_used: true,
                        });
                    }
                    // 通常コアのみで支払える場合、またはソウルコアを使わない選択
                    if state.player.reserve.normal() >= cost_to_pay || (state.player.reserve.soul > 0 && cost_to_pay > 0) {
                         actions.push(Action::PlayCard {
                            card_id: card.id.clone(),
                            cost_paid: cost_to_pay,
                            soul_core_used: false,
                        });
                    }
                }
            }
            actions.push(Action::EndStep);
        }
        Phase::AttackStep(sub_phase) => {
            match sub_phase {
                AttackSubPhase::Start => {
                    for obj in &state.player.field {
                        if !obj.is_exhausted {
                            actions.push(Action::Attack { object_id: obj.id.clone() });
                        }
                    }
                    // 「強制アタック」がなければ「何もしない（終了）」が可能
                    if !must_attack {
                        actions.push(Action::EndStep);
                    }
                }
                AttackSubPhase::Flash(_) => {
                    actions.push(Action::Pass); // フラッシュのパスという「何もしない」選択肢
                }
                AttackSubPhase::BlockSelection => {
                    // 防御側のターン
                    for obj in &state.player.field {
                        if !obj.is_exhausted {
                            actions.push(Action::Block { object_id: obj.id.clone() });
                        }
                    }
                    // 「ブロックしなければならない」効果がなければスルーが可能
                    let must_block = false; // TODO
                    if !must_block {
                        actions.push(Action::Pass);
                    }
                }
                _ => {}
            }
        }
    }
    actions
}

fn main() {
    let empty_side = SideState {
        player_id: 2,
        life: 5,
        reserve: Cores::new(0, 0),
        hand: vec![],
        field: vec![],
        trash: vec![],
        opened: vec![],
        count: 0,
    };

    let sample_state = GameState {
        player: SideState {
            player_id: 1,
            life: 5,
            reserve: Cores::new(4, 1),
            hand: vec![Card {
                           id: "BS75-041".to_string(),
                           name: "Spirit-1".to_string(),
                           base_cost: 2,
                           reduction_symbols: 1,
                           card_type: "Spirit".to_string(),
                       }],
            field: vec![],
            trash: vec![],
            opened: vec![],
            count: 0,
        },
        opponent: empty_side.clone(),
        phase: Phase::MainStep,
        turn_count: 1,
    };

    println!("--- Input (GameState) ---");
    println!("{}", serde_json::to_string_pretty(&sample_state).unwrap());

    let legal_actions = generate_legal_actions(&sample_state);

    println!("\n--- Output (Legal Actions) ---");
    for action in legal_actions {
        println!("{:?}", action);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn create_empty_side(id: u8) -> SideState {
        SideState {
            player_id: id,
            life: 5,
            reserve: Cores::new(0, 0),
            hand: vec![],
            field: vec![],
            trash: vec![],
            opened: vec![],
            count: 0,
        }
    }

    #[test]
    fn test_main_step_play_card() {
        let mut player = create_empty_side(1);
        player.reserve = Cores::new(2, 0);
        player.hand = vec![Card {
            id: "C1".to_string(),
            name: "Test Card".to_string(),
            base_cost: 3,
            reduction_symbols: 1,
            card_type: "Spirit".to_string(),
        }];
        player.field = vec![FieldObject {
            id: "F1".to_string(),
            name: "Sym".to_string(),
            lv: 1,
            cores: Cores::new(1, 0),
            is_exhausted: false,
            symbols: 1,
        }];

        let state = GameState {
            player,
            opponent: create_empty_side(2),
            phase: Phase::MainStep,
            turn_count: 1,
        };
        
        let actions = generate_legal_actions(&state);
        assert!(actions.iter().any(|a| matches!(a, Action::PlayCard { cost_paid: 2, .. })));
    }

    #[test]
    fn test_attack_step_exhaustion() {
        let mut player = create_empty_side(1);
        player.field = vec![
            FieldObject { id: "S1".to_string(), name: "Ready".to_string(), lv: 1, cores: Cores::new(1, 0), is_exhausted: false, symbols: 1 },
            FieldObject { id: "S2".to_string(), name: "Exhausted".to_string(), lv: 1, cores: Cores::new(1, 0), is_exhausted: true, symbols: 1 },
        ];

        let state = GameState {
            player,
            opponent: create_empty_side(2),
            phase: Phase::AttackStep(AttackSubPhase::Start),
            turn_count: 1,
        };

        let actions = generate_legal_actions(&state);
        assert!(actions.iter().any(|a| matches!(a, Action::Attack { object_id } if object_id == "S1")));
        assert!(!actions.iter().any(|a| matches!(a, Action::Attack { object_id } if object_id == "S2")));
    }

    #[test]
    fn test_block_selection_options() {
        let mut player = create_empty_side(1);
        player.field = vec![FieldObject { id: "B1".to_string(), name: "Blocker".to_string(), lv: 1, cores: Cores::new(1, 0), is_exhausted: false, symbols: 1 }];

        let state = GameState {
            player,
            opponent: create_empty_side(2),
            phase: Phase::AttackStep(AttackSubPhase::BlockSelection),
            turn_count: 1,
        };

        let actions = generate_legal_actions(&state);
        assert!(actions.iter().any(|a| matches!(a, Action::Block { .. })));
        assert!(actions.iter().any(|a| matches!(a, Action::Pass)));
    }
}
