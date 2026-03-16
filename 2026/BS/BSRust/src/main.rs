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

/// ゲームステート (S)
#[derive(Debug, Serialize, Deserialize, Clone)]
struct GameState {
    player_id: u8,
    life: u8,
    reserve: Cores,
    hand: Vec<Card>,
    field: Vec<FieldObject>,
    opponent_field: Vec<FieldObject>,
    phase: Phase,
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
            for card in &state.hand {
                let available_total = state.reserve.total;
                let my_symbols: u8 = state.field.iter().map(|o| o.symbols).sum();
                let actual_reduction = my_symbols.min(card.reduction_symbols);
                let cost_to_pay = card.base_cost.saturating_sub(actual_reduction);

                if available_total >= cost_to_pay {
                    // ソウルコアを使用するかどうかの分岐を生成
                    if state.reserve.soul > 0 && cost_to_pay > 0 {
                        actions.push(Action::PlayCard {
                            card_id: card.id.clone(),
                            cost_paid: cost_to_pay,
                            soul_core_used: true,
                        });
                    }
                    // 通常コアのみで支払える場合、またはソウルコアを使わない選択
                    if state.reserve.normal() >= cost_to_pay || (state.reserve.soul > 0 && cost_to_pay > 0) {
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
                    for obj in &state.field {
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
                    for obj in &state.field {
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
    // 標準入力からGameStateを読み込む (簡易的なサンプル)
    let sample_state = GameState {
        player_id: 1,
        life: 5,
        reserve: Cores::new(4, 1), //コア4個、うち1個がソウルコア
        hand: vec![Card {
                       id: "BS75-041".to_string(),
                       name: "Spirit-1".to_string(),
                       base_cost: 2,
                       reduction_symbols: 1,
                       card_type: "Spirit".to_string(),
                   },
                   Card {
                               id: "BS75-042".to_string(),
                               name: "Spirit-2".to_string(),
                               base_cost: 2,
                               reduction_symbols: 1,
                               card_type: "Spirit".to_string(),
                           }],
        field: vec![],
        opponent_field: vec![],
        phase: Phase::MainStep,
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

    #[test]
    fn test_main_step_play_card() {
        let state = GameState {
            player_id: 1,
            life: 5,
            reserve: Cores::new(2, 0),
            hand: vec![Card {
                id: "C1".to_string(),
                name: "Test Card".to_string(),
                base_cost: 3,
                reduction_symbols: 1,
                card_type: "Spirit".to_string(),
            }],
            field: vec![FieldObject {
                id: "F1".to_string(),
                name: "Sym".to_string(),
                lv: 1,
                cores: Cores::new(1, 0),
                is_exhausted: false,
                symbols: 1,
            }],
            opponent_field: vec![],
            phase: Phase::MainStep,
        };
        
        let actions = generate_legal_actions(&state);
        // コスト3 - 軽減1 = 2。リザーブ2なのでプレイ可能。
        assert!(actions.iter().any(|a| matches!(a, Action::PlayCard { cost_paid: 2, .. })));
    }

    #[test]
    fn test_attack_step_exhaustion() {
        let state = GameState {
            player_id: 1,
            life: 5,
            reserve: Cores::new(0, 0),
            hand: vec![],
            field: vec![
                FieldObject { id: "S1".to_string(), name: "Ready".to_string(), lv: 1, cores: Cores::new(1, 0), is_exhausted: false, symbols: 1 },
                FieldObject { id: "S2".to_string(), name: "Exhausted".to_string(), lv: 1, cores: Cores::new(1, 0), is_exhausted: true, symbols: 1 },
            ],
            opponent_field: vec![],
            phase: Phase::AttackStep(AttackSubPhase::Start),
        };

        let actions = generate_legal_actions(&state);
        // 回復状態のS1のみアタック可能
        assert!(actions.iter().any(|a| matches!(a, Action::Attack { object_id } if object_id == "S1")));
        assert!(!actions.iter().any(|a| matches!(a, Action::Attack { object_id } if object_id == "S2")));
    }

    #[test]
    fn test_block_selection_options() {
        let state = GameState {
            player_id: 1,
            life: 5,
            reserve: Cores::new(0, 0),
            hand: vec![],
            field: vec![FieldObject { id: "B1".to_string(), name: "Blocker".to_string(), lv: 1, cores: Cores::new(1, 0), is_exhausted: false, symbols: 1 }],
            opponent_field: vec![],
            phase: Phase::AttackStep(AttackSubPhase::BlockSelection),
        };

        let actions = generate_legal_actions(&state);
        // ブロックするか、スルー（Pass）するかの選択肢がある
        assert!(actions.iter().any(|a| matches!(a, Action::Block { .. })));
        assert!(actions.iter().any(|a| matches!(a, Action::Pass)));
    }
}
