//! BS76-X07 光翅の導き手ファラ・ルクス
//! スピリット / 白 / 系統: フラッグ, 光契約
//!
//! F契約煌臨: カウント4以上

use super::CardEffect;
use crate::{SideState, FieldObject};

pub struct Bs76X07;

impl CardEffect for Bs76X07 {
    fn card_id(&self) -> &str {
        "BS76-X07"
    }
    
    fn kourin_condition(&self, side: &SideState) -> Option<u8> {
        // カウント4以上で煌臨可能
        if side.count >= 4 {
            Some(4)
        } else {
            None
        }
    }
    
    fn is_valid_kourin_target(&self, target: &FieldObject) -> bool {
        target.systems.contains(&"光契約".to_string()) || target.name == "光虫の旗手ファラ"
    }
}
