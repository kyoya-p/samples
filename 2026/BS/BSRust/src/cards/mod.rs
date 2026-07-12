use crate::{GameState, SideState, FieldObject};
use std::collections::HashMap;
use std::sync::LazyLock;

/// グローバルカードレジストリ
pub static CARD_REGISTRY: LazyLock<CardRegistry> = LazyLock::new(|| CardRegistry::new());

// カード効果トレイト
pub trait CardEffect: Send + Sync {
    /// カードID (例: "BS76-CX03")
    fn card_id(&self) -> &str;
    
    /// 配置/召喚/煌臨時効果。追加アクション候補を返す。
    /// PhaseをResolveFaraEffect等に変更する必要がある場合はSome(phase変更指示)を返す。
    /// return_to_main: 効果解決後にMainStepへ戻る文脈か(true)、AttackFlashへ戻る文脈か(false)。
    /// （煌臨はMainStep中とフラッシュタイミングの双方で起こり得るため、呼び出し側から伝える）
    fn on_placement(&self, _state: &mut GameState, _return_to_main: bool) -> Option<crate::Phase> {
        None
    }
    
    /// アタック時効果。
    fn on_attack(&self, _state: &mut GameState, _object_id: &str) -> Option<crate::Phase> {
        None
    }
    
    /// 煌臨条件: Some(必要カウント数)を返す。Noneなら煌臨不可。
    fn kourin_condition(&self, _side: &SideState) -> Option<u8> {
        None
    }
    
    /// 煌臨先の条件判定
    fn is_valid_kourin_target(&self, target: &FieldObject) -> bool {
        // デフォルト: 光契約系統を持つか
        target.systems.contains(&"光契約".to_string()) || target.name == "光虫の旗手ファラ"
    }
    
    /// トークンカードか
    fn is_token(&self) -> bool {
        false
    }
    
    /// トークン消滅時にトークンプールに戻すか
    fn returns_to_token_pool(&self) -> bool {
        self.is_token()
    }
    
    /// MainStepでの煌臨可能か (フラッシュタイミング以外)
    fn can_kourin_main_step(&self) -> bool {
        false
    }

    /// 煌臨可能なタイミングが『自分のターン』に限られるか
    /// （falseなら『お互いのアタックステップ』＝相手ターンのフラッシュでも煌臨可能）
    fn kourin_own_turn_only(&self) -> bool {
        false
    }

    /// 【契約煌臨元】等、煌臨で下敷きになった後も自身の配置/アタック時効果が
    /// 持続して発揮されるか（trueの場合、このカードが under_cards にあれば
    /// 現在の一番上のカードの効果と同時に発揮する対象として扱う）
    fn persists_through_kourin(&self) -> bool {
        false
    }
}

// レジストリ
pub struct CardRegistry {
    effects: HashMap<String, Box<dyn CardEffect>>,
}

impl CardRegistry {
    pub fn new() -> Self {
        let mut registry = Self { effects: HashMap::new() };
        registry.register_all();
        registry
    }
    
    fn register(&mut self, effect: Box<dyn CardEffect>) {
        let id = effect.card_id().to_string();
        self.effects.insert(id, effect);
    }
    
    fn register_all(&mut self) {
        self.register(Box::new(bs76_cx03::Bs76Cx03));
        self.register(Box::new(bs76_x07::Bs76X07));
        self.register(Box::new(bs76_035::Bs76035));
        self.register(Box::new(bs76_038::Bs76038));
        self.register(Box::new(bs76_041::Bs76041));
        self.register(Box::new(bs76_t001::Bs76T001));
    }
    
    pub fn get(&self, card_id: &str) -> Option<&dyn CardEffect> {
        // IDの "_数字" サフィックスを除去して検索
        let base_id = if let Some(idx) = card_id.find('_') {
            &card_id[..idx]
        } else {
            card_id
        };
        self.effects.get(base_id).map(|e| e.as_ref())
    }
}

pub mod bs76_cx03;
pub mod bs76_x07;
pub mod bs76_035;
pub mod bs76_038;
pub mod bs76_041;
pub mod bs76_t001;
