//! BS76-T001 プラチナム・バグ
//! トークンスピリット / 白 / 系統: フラッグ
//!
//! トークン: 破壊時トークンプールに戻る

use super::CardEffect;
use crate::{SideState, FieldObject};

pub struct Bs76T001;

impl CardEffect for Bs76T001 {
    fn card_id(&self) -> &str {
        "BS76-T001"
    }
    
    fn is_token(&self) -> bool {
        true
    }
    
    fn returns_to_token_pool(&self) -> bool {
        true
    }
    
    fn kourin_condition(&self, _side: &SideState) -> Option<u8> {
        None
    }
    
    fn is_valid_kourin_target(&self, _target: &FieldObject) -> bool {
        false
    }
}
