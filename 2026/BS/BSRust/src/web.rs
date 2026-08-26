//! Webサービスモード: 対話型シミュレータをJSON API + WebUIとして提供する
//! 起動: cargo run -- web [port]  (デフォルト: 8080)

use std::collections::HashMap;
use std::sync::{Arc, Mutex};

use axum::extract::State;
use axum::response::Html;
use axum::routing::{get, post};
use axum::{Json, Router};
use burn::module::Module;
use burn::record::{CompactRecorder, Recorder};
use serde::{Deserialize, Serialize};

use chrono::Local;

use crate::ai::model::{BoardEvaluator, BoardEvaluatorConfig};
use crate::{
    apply_action, build_action_infos, calculate_state_hash, check_game_end, describe_action,
    format_symbols, generate_legal_actions, group_action_infos, is_forbidden_action,
    process_automatic_steps, Action, ActionInfo, AttackSubPhase, Card, Cores,
    GameState, LogEntry, LogSideState, Phase, Priority, SideState,
};

type MyBackend = burn::backend::NdArray;
type MyDevice = <MyBackend as burn::tensor::backend::Backend>::Device;

// ---------- リプレイログ（LLM学習用 JSONフォーマット） ----------

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ReplayLog {
    pub metadata: ReplayMetadata,
    pub initial_state: ReplayInitialState,
    pub steps: Vec<ReplayStep>,
    pub result: Option<ReplayResult>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ReplayMetadata {
    pub version: String,
    pub recorded_at: String,
    pub deck1_name: String,
    pub deck2_name: String,
    pub format: String,
    pub total_turns: u32,
    pub total_steps: usize,
    pub seed: u64,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ReplayInitialState {
    pub seed: u64,
    pub player1: ReplaySideInitial,
    pub player2: ReplaySideInitial,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ReplaySideInitial {
    pub player_id: u8,
    pub life: u8,
    pub reserve: Cores,
    /// 初期デッキ（40枚、カードID昇順ソート）
    pub initial_deck: Vec<ReplayCardInfo>,
    pub token_pool: Vec<ReplayCardInfo>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ReplayCardInfo {
    pub id: String,
    pub name: String,
    pub card_type: String,
    pub base_cost: u8,
    pub colors: Vec<String>,
    pub reduction_symbols: Vec<String>,
    pub symbols: Vec<String>,
    pub systems: Vec<String>,
}

impl From<&Card> for ReplayCardInfo {
    fn from(c: &Card) -> Self {
        Self {
            id: c.id.clone(),
            name: c.name.clone(),
            card_type: format!("{:?}", c.card_type),
            base_cost: c.base_cost,
            colors: c.colors.iter().map(|col| format!("{:?}", col)).collect(),
            reduction_symbols: c.reduction_symbols.iter().map(|col| format!("{:?}", col)).collect(),
            symbols: c.symbols.iter().map(|col| format!("{:?}", col)).collect(),
            systems: c.systems.clone(),
        }
    }
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ReplayStep {
    pub step_index: usize,
    pub turn: u32,
    pub active_player: u8,
    pub phase: String,
    pub state_snapshot: ReplayStateSnapshot,
    pub available_action_count: usize,
    pub chosen_action_index: usize,
    pub chosen_action_category: String,
    pub chosen_action_detail: String,
    pub chosen_action_kind: String,
    pub chosen_action_eval: Option<f32>,
    pub result_messages: Vec<String>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ReplayStateSnapshot {
    pub p1_life: u8,
    pub p1_reserve: String,
    pub p1_trash_cores: String,
    pub p1_count: u8,
    pub p1_hand: Vec<String>,
    pub p1_field: Vec<ReplayFieldObjectSnapshot>,
    pub p1_trash: Vec<String>,
    pub p1_deck_count: usize,
    pub p2_life: u8,
    pub p2_reserve: String,
    pub p2_trash_cores: String,
    pub p2_count: u8,
    pub p2_hand: Vec<String>,
    pub p2_field: Vec<ReplayFieldObjectSnapshot>,
    pub p2_trash: Vec<String>,
    pub p2_deck_count: usize,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ReplayFieldObjectSnapshot {
    pub id: String,
    pub name: String,
    pub cores: String,
    pub lv: u8,
    pub is_exhausted: bool,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ReplayResult {
    pub winner: u8,
    pub reason: String,
    pub saved_file: Option<String>,
}

#[derive(Clone)]
struct HistoryEntry {
    state: GameState,
    #[allow(dead_code)]
    actions: Vec<Action>,
    visited_states: Vec<(GameState, u64)>,
    game_history: Vec<LogEntry>,
    #[allow(dead_code)]
    messages: Vec<String>,
}

struct GameSession {
    seed: u64,
    state: GameState,
    initial_state: GameState,
    format_label: String,
    deck1: String,
    deck2: String,
    replay_log: ReplayLog,
    /// 現局面の合法手（レスポンスの index はこのVecへの添字）
    actions: Vec<Action>,
    visited_states: Vec<(GameState, u64)>,
    game_history: Vec<LogEntry>,
    winner: Option<u8>,
    history: Vec<HistoryEntry>,
    history_idx: usize,
    replay_file_path: String,
}

struct Inner {
    sessions: HashMap<String, GameSession>,
    model: Option<BoardEvaluator<MyBackend>>,
    device: MyDevice,
    next_id: u64,
}

type AppState = Arc<Mutex<Inner>>;

// ---------- リクエスト/レスポンス ----------

#[derive(Deserialize)]
struct NewGameReq {
    deck1: Option<String>,
    deck2: Option<String>,
    format: Option<String>,
    seed: Option<u64>,
}

#[derive(Deserialize)]
struct ActReq {
    session: String,
    index: usize,
}

#[derive(Deserialize)]
struct SessionReq {
    session: String,
}

#[derive(Serialize)]
struct FieldView {
    id: String,
    card_id: String,
    image_url: String,
    name: String,
    symbols: String,
    cores: String,
    exhausted: bool,
    lv: u8,
    bp: u32,
}

#[derive(Serialize)]
struct HandView {
    id: String,
    image_url: String,
    name: String,
    cost: u8,
    reduction: String,
}

#[derive(Serialize)]
struct SideView {
    player_id: u8,
    life: u8,
    reserve: String,
    trash_cores: String,
    count: u8,
    field: Vec<FieldView>,
    hand: Vec<HandView>,
    hand_count: usize,
    deck_count: usize,
    trash: Vec<String>,
}

#[derive(Serialize)]
struct OptionView {
    index: usize,
    detail: String,
    eval: Option<f32>,
    forbidden: bool,
    forbidden_reason: Option<String>,
}

#[derive(Serialize)]
struct GroupView {
    category: String,
    kind: String,
    best_eval: Option<f32>,
    all_forbidden: bool,
    forbidden_reason: Option<String>,
    options: Vec<OptionView>,
}

#[derive(Serialize)]
struct GameResponse {
    session: String,
    seed: u64,
    winner: Option<u8>,
    turn: u32,
    phase: String,
    active_player: u8,
    player: SideView,
    opponent: SideView,
    groups: Vec<GroupView>,
    /// n キー相当（ステップ終了/パス）。存在すれば actions への添字
    n_index: Option<usize>,
    n_label: String,
    n_eval: Option<f32>,
    n_forbidden: bool,
    n_forbidden_reason: Option<String>,
    ai_available: bool,
    can_undo: bool,
    can_redo: bool,
    messages: Vec<String>,
    replay_steps: Vec<ReplayStep>,
    replay_file: Option<String>,
}

#[derive(Serialize)]
struct ErrorResponse {
    error: String,
}

// ---------- 局面ビュー構築 ----------

fn phase_label(phase: &Phase) -> String {
    match phase {
        Phase::StartStep => "スタートステップ".to_string(),
        Phase::CoreStep => "コアステップ".to_string(),
        Phase::DrawStep => "ドローステップ".to_string(),
        Phase::RefreshStep => "リフレッシュステップ".to_string(),
        Phase::MainStep => "メインステップ".to_string(),
        Phase::AttackStep(sub) => {
            let sub_label = match sub {
                AttackSubPhase::DeclareAttack => "アタック宣言",
                AttackSubPhase::AttackFlash { priority, .. } => match priority {
                    Priority::Attacker => "フラッシュ(攻撃側)",
                    Priority::Defender => "フラッシュ(防御側)",
                },
                AttackSubPhase::DeclareBlock => "ブロック宣言",
                AttackSubPhase::BlockFlash { priority, .. } => match priority {
                    Priority::Attacker => "ブロック後フラッシュ(攻撃側)",
                    Priority::Defender => "ブロック後フラッシュ(防御側)",
                },
                AttackSubPhase::BattleResolution => "バトル解決",
                AttackSubPhase::End => "バトル終了",
            };
            format!("アタックステップ - {}", sub_label)
        }
        Phase::EndStep => "エンドステップ".to_string(),
        Phase::ResolveFaraEffect { .. } => "効果解決（ファラ）".to_string(),
        Phase::ResolveBasiliskEffect { .. } => "効果解決（バシリスク）".to_string(),
        Phase::ChooseEffectOrder => "効果解決順の選択".to_string(),
    }
}

/// 非公開領域（デッキ、相手の手札）は数のみを返す
fn side_view(side: &SideState, reveal_hand: bool) -> SideView {
    SideView {
        player_id: side.player_id,
        life: side.life,
        reserve: side.reserve.format(),
        trash_cores: side.trash_cores.format(),
        count: side.count,
        field: side
            .field
            .iter()
            .map(|o| {
                let card_id = o.current_card_id.clone();
                let image_url = format!("https://www.battlespirits.com/images/cardlist/{}.webp", card_id);
                let display_name = crate::get_field_object_display_name(&o.id, &side.field);
                FieldView {
                    id: o.id.clone(),
                    card_id,
                    image_url,
                    name: display_name,
                    symbols: format_symbols(&o.base_symbols),
                    cores: o.cores.format(),
                    exhausted: o.is_exhausted,
                    lv: o.current_lv(),
                    bp: o.current_bp(),
                }
            })
            .collect(),
        hand: if reveal_hand {
            side.hand
                .iter()
                .map(|c| {
                    let card_id = c.id.clone();
                    let image_url = format!("https://www.battlespirits.com/images/cardlist/{}.webp", card_id);
                    HandView {
                        id: card_id,
                        image_url,
                        name: c.name.clone(),
                        cost: c.base_cost,
                        reduction: format_symbols(&c.reduction_symbols),
                    }
                })
                .collect()
        } else {
            vec![]
        },
        hand_count: side.hand.len(),
        deck_count: side.opened.len(),
        trash: side.trash.iter().map(|c| c.name.clone()).collect(),
    }
}

pub fn make_state_snapshot(state: &GameState) -> ReplayStateSnapshot {
    let (p1, p2) = if state.player.player_id == 1 {
        (&state.player, &state.opponent)
    } else {
        (&state.opponent, &state.player)
    };
    ReplayStateSnapshot {
        p1_life: p1.life,
        p1_reserve: p1.reserve.format(),
        p1_trash_cores: p1.trash_cores.format(),
        p1_count: p1.count,
        p1_hand: p1.hand.iter().map(|c| c.name.clone()).collect(),
        p1_field: p1.field.iter().map(|o| ReplayFieldObjectSnapshot {
            id: o.id.clone(),
            name: o.name.clone(),
            cores: o.cores.format(),
            lv: o.current_lv(),
            is_exhausted: o.is_exhausted,
        }).collect(),
        p1_trash: p1.trash.iter().map(|c| c.name.clone()).collect(),
        p1_deck_count: p1.opened.len(),

        p2_life: p2.life,
        p2_reserve: p2.reserve.format(),
        p2_trash_cores: p2.trash_cores.format(),
        p2_count: p2.count,
        p2_hand: p2.hand.iter().map(|c| c.name.clone()).collect(),
        p2_field: p2.field.iter().map(|o| ReplayFieldObjectSnapshot {
            id: o.id.clone(),
            name: o.name.clone(),
            cores: o.cores.format(),
            lv: o.current_lv(),
            is_exhausted: o.is_exhausted,
        }).collect(),
        p2_trash: p2.trash.iter().map(|c| c.name.clone()).collect(),
        p2_deck_count: p2.opened.len(),
    }
}

pub fn create_initial_replay_log(
    initial_state: &GameState,
    format_label: &str,
    deck1: &str,
    deck2: &str,
    seed: u64,
) -> ReplayLog {
    let now = Local::now();
    let (deck_cards1, tokens1) = crate::load_deck_from_file(deck1).unwrap_or_default();
    let (deck_cards2, tokens2) = crate::load_deck_from_file(deck2).unwrap_or_default();

    let (p1, p2) = if initial_state.player.player_id == 1 {
        (&initial_state.player, &initial_state.opponent)
    } else {
        (&initial_state.opponent, &initial_state.player)
    };

    let p1_initial = ReplaySideInitial {
        player_id: 1,
        life: p1.life,
        reserve: p1.reserve,
        initial_deck: deck_cards1.iter().map(ReplayCardInfo::from).collect(),
        token_pool: tokens1.iter().map(ReplayCardInfo::from).collect(),
    };

    let p2_initial = ReplaySideInitial {
        player_id: 2,
        life: p2.life,
        reserve: p2.reserve,
        initial_deck: deck_cards2.iter().map(ReplayCardInfo::from).collect(),
        token_pool: tokens2.iter().map(ReplayCardInfo::from).collect(),
    };

    ReplayLog {
        metadata: ReplayMetadata {
            version: "1.0".to_string(),
            recorded_at: now.to_rfc3339(),
            deck1_name: deck1.to_string(),
            deck2_name: deck2.to_string(),
            format: format_label.to_string(),
            total_turns: 1,
            total_steps: 0,
            seed,
        },
        initial_state: ReplayInitialState {
            seed,
            player1: p1_initial,
            player2: p2_initial,
        },
        steps: Vec::new(),
        result: None,
    }
}

fn record_replay_step(
    session: &mut GameSession,
    action_idx: usize,
    action: &Action,
    eval: Option<f32>,
) {
    let snapshot = make_state_snapshot(&session.state);
    let (kind, category, detail, _, _) = describe_action(&session.state, action);
    let step = ReplayStep {
        step_index: session.replay_log.steps.len() + 1,
        turn: session.state.turn_count,
        active_player: session.state.player.player_id,
        phase: phase_label(&session.state.phase),
        state_snapshot: snapshot,
        available_action_count: session.actions.len(),
        chosen_action_index: action_idx,
        chosen_action_category: category,
        chosen_action_detail: detail,
        chosen_action_kind: kind,
        chosen_action_eval: eval,
        result_messages: Vec::new(),
    };
    session.replay_log.steps.push(step);
    save_replay_log(session);
}

fn generate_replay_filename(deck1: &str, deck2: &str) -> String {
    let now = Local::now();
    let d1_stem = std::path::Path::new(deck1)
        .file_stem()
        .and_then(|s| s.to_str())
        .unwrap_or("deck1");
    let d2_stem = std::path::Path::new(deck2)
        .file_stem()
        .and_then(|s| s.to_str())
        .unwrap_or("deck2");

    let filename = format!("{}-{}.{}.json", d1_stem, d2_stem, now.format("%y%m%d-%H%M"));
    let temp_dir = if std::path::Path::new("../temp").exists() {
        std::path::PathBuf::from("../temp")
    } else {
        let p = std::path::PathBuf::from("temp");
        let _ = std::fs::create_dir_all(&p);
        p
    };
    temp_dir.join(&filename).to_string_lossy().to_string()
}

fn save_replay_log(session: &mut GameSession) -> Option<String> {
    session.replay_log.metadata.total_turns = session.state.turn_count;
    session.replay_log.metadata.total_steps = session.replay_log.steps.len();

    let full_path = std::path::PathBuf::from(&session.replay_file_path);
    if let Some(parent) = full_path.parent() {
        let _ = std::fs::create_dir_all(parent);
    }

    if let Ok(json_str) = serde_json::to_string_pretty(&session.replay_log) {
        if let Ok(_) = std::fs::write(&full_path, json_str) {
            return Some(session.replay_file_path.clone());
        }
    }
    None
}

fn record_log(session: &mut GameSession) {
    let (p1, p2) = if session.state.player.player_id == 1 {
        (&session.state.player, &session.state.opponent)
    } else {
        (&session.state.opponent, &session.state.player)
    };
    session.game_history.push(LogEntry {
        tuen: session.state.turn_count,
        phase: crate::format_phase_camel(&session.state.phase),
        action: None,
        player1: LogSideState::from(p1),
        player2: LogSideState::from(p2),
    });
    if let Ok(yaml_content) = serde_yaml::to_string(&session.game_history) {
        let _ = std::fs::write("bs-log.yaml", yaml_content);
    }
}

/// CLIループと同じ自動進行: 合法手なし→強制EndStep、EndStep/Passのみ→自動実行
fn advance(session: &mut GameSession, messages: &mut Vec<String>) {
    loop {
        record_log(session);

        if let Some(winner) = check_game_end(&session.state) {
            session.winner = Some(winner);
            session.actions.clear();
            session.replay_log.result = Some(ReplayResult {
                winner,
                reason: "opponent_life_zero".to_string(),
                saved_file: None,
            });
            let saved_path = save_replay_log(session);
            if let Some(ref mut res) = session.replay_log.result {
                res.saved_file = saved_path.clone();
            }
            messages.push(format!(
                "ゲーム終了: プレイヤー{} の勝利！（相手のライフが0になりました）",
                winner
            ));
            if let Some(path) = saved_path {
                messages.push(format!("📄 リプレイログ保存完了: {}", path));
            }
            return;
        }

        let actions = generate_legal_actions(&session.state);
        if actions.is_empty() {
            messages.push("実行可能なアクションがないため自動的にステップ終了します".to_string());
            session.state.phase = Phase::EndStep;
            let _ = apply_action(&mut session.state, &Action::EndStep);
            process_automatic_steps(&mut session.state);
            continue;
        }

        let has_category = actions.iter().any(|act| {
            matches!(
                act,
                Action::PlayCard { .. }
                    | Action::MoveCore { .. }
                    | Action::Attack { .. }
                    | Action::Block { .. }
            )
        });
        if !has_category {
            if actions.contains(&Action::EndStep) {
                messages.push("選択肢がないため自動的にステップ終了を実行しました".to_string());
                let _ = apply_action(&mut session.state, &Action::EndStep);
                process_automatic_steps(&mut session.state);
                continue;
            } else if actions.contains(&Action::Pass) {
                messages.push("選択肢がないため自動的にパスを実行しました".to_string());
                let _ = apply_action(&mut session.state, &Action::Pass);
                process_automatic_steps(&mut session.state);
                continue;
            }
        }

        let hash = calculate_state_hash(&session.state);
        session.visited_states.push((session.state.clone(), hash));
        session.actions = actions;
        return;
    }
}

fn build_response(inner: &Inner, session_id: &str, messages: Vec<String>) -> GameResponse {
    let session = &inner.sessions[session_id];
    let state = &session.state;
    let model = inner.model.as_ref();

    let mut groups: Vec<GroupView> = Vec::new();
    let mut n_index = None;
    let mut n_label = String::new();
    let mut n_eval = None;
    let mut n_forbidden = false;
    let mut n_forbidden_reason = None;

    if session.winner.is_none() {
        let mut infos =
            build_action_infos(state, &session.actions, model, &inner.device, &session.visited_states);

        // n キー相当（EndStep優先、なければPass）
        if let Some(info) = infos
            .iter()
            .find(|i| i.kind == "end")
            .or_else(|| infos.iter().find(|i| i.kind == "pass"))
        {
            n_index = Some(info.index);
            n_label = if info.kind == "end" {
                "ステップ終了".to_string()
            } else {
                "パス / スキップ".to_string()
            };
            n_eval = info.eval;
            n_forbidden = info.forbidden;
            n_forbidden_reason = info.forbidden_reason.clone();
        }

        infos.retain(|i| i.kind != "end" && i.kind != "pass");
        // 同一表示のアクションを除去（重複アクションの完全排除）
        let mut seen = std::collections::HashSet::new();
        infos.retain(|i| seen.insert((i.category.clone(), i.detail.clone())));

        let grouped: Vec<(String, Vec<ActionInfo>)> = group_action_infos(infos, model.is_some());
        groups = grouped
            .into_iter()
            .map(|(category, items)| {
                let best = items.iter().filter_map(|i| i.eval).fold(f32::NEG_INFINITY, f32::max);
                let all_forbidden = items.iter().all(|i| i.forbidden);
                let group_reason = if all_forbidden {
                    items[0].forbidden_reason.clone()
                } else {
                    None
                };
                GroupView {
                    category,
                    kind: items[0].kind.clone(),
                    best_eval: if best > f32::NEG_INFINITY { Some(best) } else { None },
                    all_forbidden,
                    forbidden_reason: group_reason,
                    options: items
                        .into_iter()
                        .map(|i| OptionView {
                            index: i.index,
                            detail: i.detail,
                            eval: i.eval,
                            forbidden: i.forbidden,
                            forbidden_reason: i.forbidden_reason,
                        })
                        .collect(),
                }
            })
            .collect();
    }

    let can_undo = session.history_idx > 0;
    let can_redo = session.history_idx + 1 < session.history.len();
    let replay_steps = session.replay_log.steps.clone();
    let replay_file = Some(session.replay_file_path.clone());

    GameResponse {
        session: session_id.to_string(),
        seed: session.seed,
        winner: session.winner,
        turn: state.turn_count,
        phase: phase_label(&state.phase),
        active_player: state.player.player_id,
        player: side_view(&state.player, true),
        opponent: side_view(&state.opponent, false),
        groups,
        n_index,
        n_label,
        n_eval,
        n_forbidden,
        n_forbidden_reason,
        ai_available: model.is_some(),
        can_undo,
        can_redo,
        messages,
        replay_steps,
        replay_file,
    }
}

// ---------- ハンドラ ----------

async fn index() -> Html<&'static str> {
    Html(include_str!("../static/index.html"))
}

async fn new_game(
    State(app): State<AppState>,
    Json(req): Json<NewGameReq>,
) -> Result<Json<GameResponse>, Json<ErrorResponse>> {
    let deck1 = req.deck1.filter(|s| !s.is_empty()).unwrap_or_else(|| "deck-fara.yaml".to_string());
    let deck2 = req.deck2.filter(|s| !s.is_empty()).unwrap_or_else(|| "deck-kogyo.yaml".to_string());
    let seed = req.seed.unwrap_or_else(|| rand::random::<u64>());

    let state = crate::setup_initial_state_with_seed(&deck1, &deck2, seed).map_err(|e| {
        Json(ErrorResponse { error: format!("初期状態の構築に失敗しました: {}", e) })
    })?;

    let mut inner = app.lock().unwrap();
    inner.next_id += 1;
    let session_id = format!("g{:08x}-{:04x}", inner.next_id, rand::random::<u16>());

    let format_label = match req.format.as_deref() {
        Some("standard") => "スタンダード",
        _ => "エターナル",
    };
    let replay_file_path = generate_replay_filename(&deck1, &deck2);
    let replay_log = create_initial_replay_log(&state, format_label, &deck1, &deck2, seed);
    let mut session = GameSession {
        seed,
        initial_state: state.clone(),
        format_label: format_label.to_string(),
        deck1: deck1.clone(),
        deck2: deck2.clone(),
        replay_log,
        state,
        actions: Vec::new(),
        visited_states: Vec::new(),
        game_history: Vec::new(),
        winner: None,
        history: Vec::new(),
        history_idx: 0,
        replay_file_path,
    };
    let mut messages = vec![format!("新規ゲームを開始しました [フォーマット: {}] (Seed: {}, deck1: {}, deck2: {})", format_label, seed, deck1, deck2)];
    advance(&mut session, &mut messages);

    session.history = vec![HistoryEntry {
        state: session.state.clone(),
        actions: session.actions.clone(),
        visited_states: session.visited_states.clone(),
        game_history: session.game_history.clone(),
        messages: messages.clone(),
    }];
    session.history_idx = 0;
    save_replay_log(&mut session);

    inner.sessions.insert(session_id.clone(), session);

    Ok(Json(build_response(&inner, &session_id, messages)))
}

async fn act(
    State(app): State<AppState>,
    Json(req): Json<ActReq>,
) -> Result<Json<GameResponse>, Json<ErrorResponse>> {
    let mut inner_guard = app.lock().unwrap();
    let Inner {
        ref mut sessions,
        ref model,
        ref device,
        ..
    } = *inner_guard;

    let session = sessions.get_mut(&req.session).ok_or_else(|| {
        Json(ErrorResponse { error: "セッションが見つかりません".to_string() })
    })?;
    if session.winner.is_some() {
        return Err(Json(ErrorResponse { error: "ゲームは終了しています".to_string() }));
    }
    let action = match session.actions.get(req.index).cloned() {
        Some(a) => a,
        None => {
            return Err(Json(ErrorResponse { error: "無効なアクションです".to_string() }));
        }
    };
    if is_forbidden_action(&action, &session.state, &session.visited_states) {
        return Err(Json(ErrorResponse {
            error: "🚫 このアクションは同一盤面に遷移するため選択できません".to_string(),
        }));
    }

    let eval = model.as_ref().and_then(|m| crate::ai::decision::evaluate_action(m, &session.state, &action, device));

    // リプレイログへステップを記録
    record_replay_step(session, req.index, &action, eval);

    // 学習用アクション選択ログを記録
    crate::log_action_choice(
        &req.session,
        &session.state,
        &session.actions,
        req.index,
        model.as_ref(),
        device,
    );

    let mut messages = Vec::new();
    if let Err(e) = apply_action(&mut session.state, &action) {
        return Err(Json(ErrorResponse { error: format!("エラー: {}", e) }));
    }
    process_automatic_steps(&mut session.state);
    advance(session, &mut messages);

    if session.history_idx < session.history.len().saturating_sub(1) {
        session.history.truncate(session.history_idx + 1);
    }
    session.history.push(HistoryEntry {
        state: session.state.clone(),
        actions: session.actions.clone(),
        visited_states: session.visited_states.clone(),
        game_history: session.game_history.clone(),
        messages: messages.clone(),
    });
    session.history_idx = session.history.len() - 1;

    save_replay_log(session);

    Ok(Json(build_response(&inner_guard, &req.session, messages)))
}

async fn auto(
    State(app): State<AppState>,
    Json(req): Json<SessionReq>,
) -> Result<Json<GameResponse>, Json<ErrorResponse>> {
    let mut inner_guard = app.lock().unwrap();
    if inner_guard.model.is_none() {
        return Err(Json(ErrorResponse {
            error: "学習済みモデルがないためAI自動決定は利用できません".to_string(),
        }));
    }
    let Inner {
        ref mut sessions,
        ref model,
        ref device,
        ..
    } = *inner_guard;

    let session = sessions.get_mut(&req.session).ok_or_else(|| {
        Json(ErrorResponse { error: "セッションが見つかりません".to_string() })
    })?;

    if session.winner.is_some() {
        return Err(Json(ErrorResponse { error: "ゲームは終了しています".to_string() }));
    }
    let model_ref = model.as_ref().unwrap();
    let mut best: Option<(usize, f32)> = None;
    for (i, action) in session.actions.iter().enumerate() {
        if is_forbidden_action(action, &session.state, &session.visited_states) {
            continue;
        }
        if let Some(val) =
            crate::ai::decision::evaluate_action(model_ref, &session.state, action, device)
        {
            if best.map_or(true, |(_, bv)| val > bv) {
                best = Some((i, val));
            }
        }
    }
    let (idx, val) = best.ok_or_else(|| {
        Json(ErrorResponse { error: "AIによる選択肢の評価に失敗しました".to_string() })
    })?;

    let action = session.actions[idx].clone();

    // リプレイログへステップを記録
    record_replay_step(session, idx, &action, Some(val));

    // 学習用アクション選択ログを記録
    crate::log_action_choice(
        &req.session,
        &session.state,
        &session.actions,
        idx,
        model.as_ref(),
        device,
    );
    let mut messages =
        vec![format!("AI自動決定（評価値: {:.3}）: {:?}", val, action)];
    let _ = apply_action(&mut session.state, &action);
    process_automatic_steps(&mut session.state);
    advance(session, &mut messages);

    if session.history_idx < session.history.len().saturating_sub(1) {
        session.history.truncate(session.history_idx + 1);
    }
    session.history.push(HistoryEntry {
        state: session.state.clone(),
        actions: session.actions.clone(),
        visited_states: session.visited_states.clone(),
        game_history: session.game_history.clone(),
        messages: messages.clone(),
    });
    session.history_idx = session.history.len() - 1;

    save_replay_log(session);

    Ok(Json(build_response(&inner_guard, &req.session, messages)))
}

async fn restart_game(
    State(app): State<AppState>,
    Json(req): Json<SessionReq>,
) -> Result<Json<GameResponse>, Json<ErrorResponse>> {
    let mut inner = app.lock().unwrap();
    let session = inner.sessions.get_mut(&req.session).ok_or_else(|| {
        Json(ErrorResponse { error: "セッションが見つかりません".to_string() })
    })?;
    session.state = session.initial_state.clone();
    session.actions.clear();
    session.visited_states.clear();
    session.game_history.clear();
    session.winner = None;
    session.replay_log = create_initial_replay_log(&session.initial_state, &session.format_label, &session.deck1, &session.deck2, session.seed);
    let mut messages = vec![format!(
        "🔄 同じ初期条件でゲームをリスタートしました [フォーマット: {}] (Seed: {}, deck1: {}, deck2: {})",
        session.format_label, session.seed, session.deck1, session.deck2
    )];
    advance(session, &mut messages);

    session.history = vec![HistoryEntry {
        state: session.state.clone(),
        actions: session.actions.clone(),
        visited_states: session.visited_states.clone(),
        game_history: session.game_history.clone(),
        messages: messages.clone(),
    }];
    session.history_idx = 0;

    save_replay_log(session);

    Ok(Json(build_response(&inner, &req.session, messages)))
}

async fn undo(
    State(app): State<AppState>,
    Json(req): Json<SessionReq>,
) -> Result<Json<GameResponse>, Json<ErrorResponse>> {
    let mut inner = app.lock().unwrap();
    let session = inner.sessions.get_mut(&req.session).ok_or_else(|| {
        Json(ErrorResponse { error: "セッションが見つかりません".to_string() })
    })?;
    if session.history_idx == 0 {
        return Err(Json(ErrorResponse { error: "これ以上戻れません".to_string() }));
    }
    session.history_idx -= 1;
    let entry = session.history[session.history_idx].clone();
    session.state = entry.state;
    session.visited_states = entry.visited_states;
    session.game_history = entry.game_history;
    session.winner = None;

    session.replay_log.steps.truncate(session.history_idx);
    session.replay_log.result = None;

    let mut messages = vec!["◀ 1手前の局面に巻き戻しました".to_string()];
    advance(session, &mut messages);

    save_replay_log(session);

    Ok(Json(build_response(&inner, &req.session, messages)))
}

async fn redo(
    State(app): State<AppState>,
    Json(req): Json<SessionReq>,
) -> Result<Json<GameResponse>, Json<ErrorResponse>> {
    let mut inner = app.lock().unwrap();
    let session = inner.sessions.get_mut(&req.session).ok_or_else(|| {
        Json(ErrorResponse { error: "セッションが見つかりません".to_string() })
    })?;
    if session.history_idx + 1 >= session.history.len() {
        return Err(Json(ErrorResponse { error: "これ以上進めません".to_string() }));
    }
    session.history_idx += 1;
    let entry = session.history[session.history_idx].clone();
    session.state = entry.state;
    session.visited_states = entry.visited_states;
    session.game_history = entry.game_history;

    let mut messages = vec!["進む ▶ 1手先の局面に進めました".to_string()];
    advance(session, &mut messages);

    save_replay_log(session);

    Ok(Json(build_response(&inner, &req.session, messages)))
}

async fn surrender(
    State(app): State<AppState>,
    Json(req): Json<SessionReq>,
) -> Result<Json<GameResponse>, Json<ErrorResponse>> {
    let mut inner = app.lock().unwrap();
    let session = inner.sessions.get_mut(&req.session).ok_or_else(|| {
        Json(ErrorResponse { error: "セッションが見つかりません".to_string() })
    })?;
    let winner = session.state.opponent.player_id;
    session.winner = Some(winner);
    session.actions.clear();
    session.replay_log.result = Some(ReplayResult {
        winner,
        reason: "surrender".to_string(),
        saved_file: Some(session.replay_file_path.clone()),
    });
    let saved_path = save_replay_log(session);
    let mut messages = vec![format!(
        "プレイヤー{} がサレンダーしました。プレイヤー{} の勝利！",
        session.state.player.player_id, winner
    )];
    if let Some(path) = saved_path {
        messages.push(format!("📄 リプレイログ保存完了: {}", path));
    }
    Ok(Json(build_response(&inner, &req.session, messages)))
}

async fn get_replay(
    State(app): State<AppState>,
    Json(req): Json<SessionReq>,
) -> Result<Json<ReplayLog>, Json<ErrorResponse>> {
    let inner = app.lock().unwrap();
    let session = inner.sessions.get(&req.session).ok_or_else(|| {
        Json(ErrorResponse { error: "セッションが見つかりません".to_string() })
    })?;
    Ok(Json(session.replay_log.clone()))
}

// ---------- 起動 ----------

pub fn run_server(port: u16) {
    let device: MyDevice = Default::default();
    let model_path = "tmp/bs_model/checkpoint/model-10";
    let model = if std::path::Path::new("tmp/bs_model/checkpoint/model-10.mpk").exists() {
        println!("学習済みモデル weights をロードしています: {}.mpk", model_path);
        let record = CompactRecorder::new()
            .load(model_path.into(), &device)
            .expect("Failed to load model weights");
        let config = BoardEvaluatorConfig::new();
        Some(config.init::<MyBackend>(&device).load_record(record))
    } else {
        println!("初期ニューラルネットワーク評価モデル (520 -> 256 -> 256 -> 1) を初期化しました。");
        let config = BoardEvaluatorConfig::new();
        Some(config.init::<MyBackend>(&device))
    };

    let app_state: AppState = Arc::new(Mutex::new(Inner {
        sessions: HashMap::new(),
        model,
        device,
        next_id: 0,
    }));

    let rt = tokio::runtime::Builder::new_multi_thread()
        .enable_all()
        .build()
        .expect("tokioランタイムの構築に失敗しました");

    rt.block_on(async move {
        let app = Router::new()
            .route("/", get(index))
            .route("/api/new", post(new_game))
            .route("/api/restart", post(restart_game))
            .route("/api/undo", post(undo))
            .route("/api/redo", post(redo))
            .route("/api/act", post(act))
            .route("/api/auto", post(auto))
            .route("/api/surrender", post(surrender))
            .route("/api/replay", post(get_replay))
            .with_state(app_state);

        let listener = tokio::net::TcpListener::bind(("127.0.0.1", port))
            .await
            .expect("ポートのバインドに失敗しました");
        println!("=== TCG BattleSpirits デッキシミュレータ Webサービス ===");
        println!("http://localhost:{}/ をブラウザで開いてください", port);
        axum::serve(listener, app).await.unwrap();
    });
}
