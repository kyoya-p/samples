//! BS76-041 プラチナム・マンティス
//! スピリット / 白 / 系統: フラッグ
//!
//! F契約煌臨: カウント6以上

use super::CardEffect;
use crate::{SideState, FieldObject};

pub struct Bs76041;

impl CardEffect for Bs76041 {
    fn card_id(&self) -> &str {
        "BS76-041"
    }
    
    fn kourin_condition(&self, side: &SideState) -> Option<u8> {
        if side.count >= 6 {
            Some(6)
        } else {
            None
        }
    }
    
    fn is_valid_kourin_target(&self, target: &FieldObject) -> bool {
        target.systems.contains(&"光契約".to_string()) || target.name == "光虫の旗手ファラ"
    }
}
