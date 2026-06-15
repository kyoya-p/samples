use burn::tensor::backend::Backend;
use burn::tensor::{Tensor, TensorData};
use crate::{GameState, Action, apply_action, generate_legal_actions, process_automatic_steps};
use super::model::BoardEvaluator;
use super::encoder::encode_state;

/// 現在のGameStateから可能な行動を全列挙し、
/// それぞれ遷移した後の盤面評価値が最も高いアクションを選択する。
pub fn select_best_action<B: Backend>(
    model: &BoardEvaluator<B>,
    state: &GameState,
    device: &B::Device,
) -> Option<Action> {
    let actions = generate_legal_actions(state);
    if actions.is_empty() {
        return None;
    }
    if actions.len() == 1 {
        return Some(actions[0].clone());
    }

    let mut best_action = None;
    let mut best_val = -f32::INFINITY;

    for action in &actions {
        if let Some(val) = evaluate_action(model, state, action, device) {
            if val > best_val {
                best_val = val;
                best_action = Some(action.clone());
            }
        }
    }

    best_action
}

/// あるアクションを適用した後の評価値を、現在手番プレイヤー視点で算出する
pub fn evaluate_action<B: Backend>(
    model: &BoardEvaluator<B>,
    state: &GameState,
    action: &Action,
    device: &B::Device,
) -> Option<f32> {
    let mut next_state = state.clone();
    if apply_action(&mut next_state, action).is_ok() {
        process_automatic_steps(&mut next_state);
        
        // 評価用にフェイズを正規化（フェイズ差でNN出力が歪むのを防止）
        next_state.phase = crate::Phase::MainStep;
        
        let encoded = encode_state(&next_state);
        let input_data = TensorData::from(encoded.as_slice());
        let input_tensor = Tensor::<B, 1>::from_data(input_data, device)
            .reshape([1, 520]);
        
        let output = model.forward(input_tensor);
        let val = output.into_data().as_slice::<f32>().unwrap()[0];
        
        // 手番が相手に移った場合は、相手視点での評価値を反転して自分視点にする（ネガマックス法）
        let adjusted_val = if next_state.player.player_id == state.player.player_id {
            val
        } else {
            -val
        };
        Some(adjusted_val)
    } else {
        None
    }
}

