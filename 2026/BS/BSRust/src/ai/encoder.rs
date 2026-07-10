use crate::{CardType, GameState, Phase, SideState, Color};

pub const MAX_FIELD_OBJECTS: usize = 10;
pub const FIELD_OBJ_DIM: usize = 25; // 10 (base) + 1 (cost) + 7 (colors) + 1 (symbols_count) + 6 (systems)
pub const SIDE_STATE_DIM: usize = 6 + MAX_FIELD_OBJECTS * FIELD_OBJ_DIM; // 6 + 250 = 256
pub const STATE_DIM: usize = 8 + SIDE_STATE_DIM * 2; // 8 + 256*2 = 520

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
        Phase::ResolveFaraEffect { is_placement } => {
            if is_placement {
                phase_oh[4] = 1.0;
            } else {
                phase_oh[5] = 1.0;
            }
        }
        Phase::ResolveBasiliskEffect { is_main } => {
            if is_main {
                phase_oh[4] = 1.0;
            } else {
                phase_oh[5] = 1.0;
            }
        }
        Phase::ChooseEffectOrder => {
            // 待機中の効果がMainStepへ戻る文脈かどうかで丸め込む
            let returns_to_main = state.pending_effects.first().map_or(true, |pe| {
                matches!(
                    pe.target_phase,
                    Phase::ResolveFaraEffect { is_placement: true } | Phase::ResolveBasiliskEffect { is_main: true }
                )
            });
            if returns_to_main {
                phase_oh[4] = 1.0;
            } else {
                phase_oh[5] = 1.0;
            }
        }
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

pub fn get_card_cost(card_id: &str) -> u8 {
    let base_id = if let Some(idx) = card_id.find('_') {
        &card_id[..idx]
    } else {
        card_id
    };

    if let Ok(user_home) = std::env::var("USERPROFILE") {
        let path = format!("{}\\.bscards\\yaml\\{}.yaml", user_home, base_id);
        if let Ok(content) = std::fs::read_to_string(&path) {
            for line in content.lines() {
                let trimmed = line.trim();
                if trimmed.starts_with("cost:") {
                    if let Ok(c) = trimmed["cost:".len()..].trim().parse::<u8>() {
                        return c;
                    }
                }
            }
        }
    }
    
    if base_id == "BS76-T001" {
        return 0; // プラチナム・バグ (トークン)
    }
    3 // デフォルト
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

            // Card Cost (1 dim)
            vec.push(get_card_cost(&obj.id) as f32 / 10.0);

            // -- NEW FEATURES --
            // Colors (One-Hot / Multi-Hot, 7 dims)
            let mut colors_oh = vec![0.0; 7];
            for col in &obj.colors {
                match col {
                    Color::Red => colors_oh[0] = 1.0,
                    Color::Purple => colors_oh[1] = 1.0,
                    Color::Green => colors_oh[2] = 1.0,
                    Color::White => colors_oh[3] = 1.0,
                    Color::Yellow => colors_oh[4] = 1.0,
                    Color::Blue => colors_oh[5] = 1.0,
                    Color::None => colors_oh[6] = 1.0,
                }
            }
            vec.extend(colors_oh);

            // Symbols Count (1 dim)
            vec.push(obj.base_symbols.len() as f32 / 3.0);

            // Systems (Multi-Hot, 6 dims: フラッグ, 光契約, 旗種, 翆海, 甲獣, その他)
            let mut sys_oh = vec![0.0; 6];
            let mut matched = false;
            for sys in &obj.systems {
                match sys.as_str() {
                    "フラッグ" => { sys_oh[0] = 1.0; matched = true; },
                    "光契約" => { sys_oh[1] = 1.0; matched = true; },
                    "旗種" => { sys_oh[2] = 1.0; matched = true; },
                    "翆海" => { sys_oh[3] = 1.0; matched = true; },
                    "甲獣" => { sys_oh[4] = 1.0; matched = true; },
                    _ => {}
                }
            }
            if !obj.systems.is_empty() && !matched {
                sys_oh[5] = 1.0;
            }
            vec.extend(sys_oh);
        } else {
            // Padding for empty slots
            vec.extend(vec![0.0; FIELD_OBJ_DIM]);
        }
    }
}
