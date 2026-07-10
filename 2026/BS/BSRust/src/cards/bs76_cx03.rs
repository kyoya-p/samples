//! BS76-CX03 光虫の旗手ファラ
//! 契約ネクサス / コスト3 / 白 / 系統: フラッグ, 光契約, 旗種
//!
//! 効果:
//! - 配置時: トークン「プラチナム・バグ」1体を出せる (ターンに1回ボイドからコア)
//! - アタック時: トークン「プラチナム・バグ」1体を出せる (ターンに1回ボイドからコア)
//! - F契約煌臨の対象となる
//! - カウント+1 (F契約煌臨時またはプラチナム・バグが出たとき)

use super::CardEffect;
use crate::{GameState, SideState, FieldObject, Phase};

pub struct Bs76Cx03;

impl CardEffect for Bs76Cx03 {
    fn card_id(&self) -> &str {
        "BS76-CX03"
    }
    
    fn on_placement(&self, _state: &mut GameState, return_to_main: bool) -> Option<Phase> {
        // 配置/煌臨時: トークン召喚の選択フェイズへ移行
        Some(Phase::ResolveFaraEffect { is_placement: return_to_main })
    }
    
    fn on_attack(&self, _state: &mut GameState, _object_id: &str) -> Option<Phase> {
        // アタック時: トークン召喚の選択フェイズへ移行
        Some(Phase::ResolveFaraEffect { is_placement: false })
    }
    
    fn kourin_condition(&self, _side: &SideState) -> Option<u8> {
        None // ファラ自身は煌臨しない
    }
    
    fn is_valid_kourin_target(&self, _target: &FieldObject) -> bool {
        false // 煌臨カードではない
    }

    fn persists_through_kourin(&self) -> bool {
        // 【契約煌臨元】: 煌臨で下敷きになった後も配置/アタック時効果が持続する
        true
    }
}
