//! BS76-035 プラチナム・バシリスク
//! スピリット / 白 / 系統: フラッグ, 光契約
//!
//! F契約煌臨: MainStep時のみ、光契約持ちに煌臨可能

use super::CardEffect;
use crate::{SideState, FieldObject, GameState, Phase};

pub struct Bs76035;

impl CardEffect for Bs76035 {
    fn card_id(&self) -> &str {
        "BS76-035"
    }
    
    fn on_placement(&self, _state: &mut GameState, return_to_main: bool) -> Option<Phase> {
        // 煌臨（または召喚）時: 煌臨時効果の解決フェイズへ移行
        Some(Phase::ResolveBasiliskEffect { is_main: return_to_main })
    }
    
    fn on_attack(&self, _state: &mut GameState, _object_id: &str) -> Option<Phase> {
        // アタック時: 効果解決フェイズへ移行
        Some(Phase::ResolveBasiliskEffect { is_main: false })
    }
    
    fn kourin_condition(&self, _side: &SideState) -> Option<u8> {
        // カウント制限なし（MainStep煌臨）
        Some(0)
    }
    
    fn is_valid_kourin_target(&self, target: &FieldObject) -> bool {
        target.systems.contains(&"光契約".to_string()) || target.name == "光虫の旗手ファラ"
    }
    
    fn can_kourin_main_step(&self) -> bool {
        true
    }

    fn kourin_own_turn_only(&self) -> bool {
        // 《F契約煌臨：光契約》『自分のターン』のため、相手ターンには煌臨できない
        true
    }
}
