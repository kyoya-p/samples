use crate::{CardType, GameState, Phase, SideState};

pub const MAX_FIELD_OBJECTS: usize = 10;
pub const FIELD_OBJ_DIM: usize = 10;
pub const SIDE_STATE_DIM: usize = 6 + MAX_FIELD_OBJECTS * FIELD_OBJ_DIM; // 6 + 100 = 106
pub const STATE_DIM: usize = 8 + SIDE_STATE_DIM * 2; // 8 + 106*2 = 220

pub fn encode_state(state: &GameState) -> Vec<f32> {
    let mut vec = Vec::with_capacity(STATE_DIM);

    // 1. Phase (One-Hot, 7 dims)
    let mut phase_oh = vec![0.0; 7];
    match state.phase {
        Phase::StartStep => phase_oh[0] = 1.0,
        Phase::CoreStep => phase_oh[1] = 1.0,
        Phase::DrawStep => phase_oh[2] = 1.0,
        Phase::RefreshStep => phase_oh[3] = 1.0,
        Phase::MainStep => phase_oh[4] = 1.0,
        Phase::AttackStep(_) => phase_oh[5] = 1.0,
        Phase::EndStep => phase_oh[6] = 1.0,
    }
    vec.extend(phase_oh);

    // 2. Turn count (normalized, assuming max 50 turns)
    vec.push((state.turn_count as f32) / 50.0);

    // 3. Player state
    encode_side_state(&state.player, &mut vec);

    // 4. Opponent state
    encode_side_state(&state.opponent, &mut vec);

    // Pad if slightly short (safety)
    while vec.len() < STATE_DIM {
        vec.push(0.0);
    }
    // Truncate if too long
    vec.truncate(STATE_DIM);

    vec
}

fn encode_side_state(side: &SideState, vec: &mut Vec<f32>) {
    vec.push(side.life as f32 / 5.0); // Assuming 5 is starting life
    vec.push(side.reserve.total as f32 / 10.0); // Arbitrary normalization
    vec.push(side.reserve.soul as f32);
    vec.push(side.trash_cores.total as f32 / 10.0);
    vec.push(side.trash_cores.soul as f32);
    vec.push(side.hand.len() as f32 / 10.0);

    for i in 0..MAX_FIELD_OBJECTS {
        if i < side.field.len() {
            let obj = &side.field[i];
            vec.push(1.0); // Exists
            
            // CardType (One-Hot 5)
            let mut ct_oh = vec![0.0; 5];
            match obj.card_type {
                CardType::Spirit => ct_oh[0] = 1.0,
                CardType::Nexus => ct_oh[1] = 1.0,
                CardType::Magic => ct_oh[2] = 1.0,
                CardType::Brave => ct_oh[3] = 1.0,
                CardType::Ultimate => ct_oh[4] = 1.0,
            }
            vec.extend(ct_oh);

            vec.push(obj.cores.total as f32 / 5.0);
            vec.push(obj.cores.soul as f32);
            vec.push(if obj.is_exhausted { 1.0 } else { 0.0 });
            vec.push(obj.current_lv() as f32 / 3.0);
        } else {
            // Padding for empty slots
            vec.extend(vec![0.0; FIELD_OBJ_DIM]);
        }
    }
}
