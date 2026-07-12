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

use crate::ai::model::{BoardEvaluator, BoardEvaluatorConfig};
use crate::{
    apply_action, build_action_infos, calculate_state_hash, check_game_end, format_symbols,
    generate_legal_actions, group_action_infos, is_forbidden_action, process_automatic_steps,
    setup_initial_state, Action, ActionInfo, AttackSubPhase, GameState, LogEntry, LogSideState,
    Phase, Priority, SideState,
};

type MyBackend = burn::backend::NdArray;
type MyDevice = <MyBackend as burn::tensor::backend::Backend>::Device;

struct GameSession {
    state: GameState,
    /// 現局面の合法手（レスポンスの index はこのVecへの添字）
    actions: Vec<Action>,
    visited_states: Vec<(GameState, u64)>,
    game_history: Vec<LogEntry>,
    winner: Option<u8>,
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
    name: String,
    symbols: String,
    cores: String,
    exhausted: bool,
    lv: u8,
}

#[derive(Serialize)]
struct HandView {
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
    messages: Vec<String>,
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
            .map(|o| FieldView {
                id: o.id.clone(),
                name: o.name.clone(),
                symbols: format_symbols(&o.base_symbols),
                cores: o.cores.format(),
                exhausted: o.is_exhausted,
                lv: o.current_lv(),
            })
            .collect(),
        hand: if reveal_hand {
            side.hand
                .iter()
                .map(|c| HandView {
                    name: c.name.clone(),
                    cost: c.base_cost,
                    reduction: format_symbols(&c.reduction_symbols),
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

fn record_log(session: &mut GameSession) {
    let (p1, p2) = if session.state.player.player_id == 1 {
        (&session.state.player, &session.state.opponent)
    } else {
        (&session.state.opponent, &session.state.player)
    };
    session.game_history.push(LogEntry {
        tuen: session.state.turn_count,
        phase: crate::format_phase_camel(&session.state.phase),
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
            messages.push(format!(
                "ゲーム終了: プレイヤー{} の勝利！（相手のライフが0になりました）",
                winner
            ));
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

    GameResponse {
        session: session_id.to_string(),
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
        messages,
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

    let state = setup_initial_state(&deck1, &deck2).map_err(|e| {
        Json(ErrorResponse { error: format!("初期状態の構築に失敗しました: {}", e) })
    })?;

    let mut inner = app.lock().unwrap();
    inner.next_id += 1;
    let session_id = format!("g{:08x}-{:04x}", inner.next_id, rand::random::<u16>());

    let mut session = GameSession {
        state,
        actions: Vec::new(),
        visited_states: Vec::new(),
        game_history: Vec::new(),
        winner: None,
    };
    let mut messages = vec![format!("新規ゲームを開始しました (deck1: {}, deck2: {})", deck1, deck2)];
    advance(&mut session, &mut messages);
    inner.sessions.insert(session_id.clone(), session);

    Ok(Json(build_response(&inner, &session_id, messages)))
}

async fn act(
    State(app): State<AppState>,
    Json(req): Json<ActReq>,
) -> Result<Json<GameResponse>, Json<ErrorResponse>> {
    let mut inner = app.lock().unwrap();
    let session = inner.sessions.get_mut(&req.session).ok_or_else(|| {
        Json(ErrorResponse { error: "セッションが見つかりません".to_string() })
    })?;
    if session.winner.is_some() {
        return Err(Json(ErrorResponse { error: "ゲームは終了しています".to_string() }));
    }
    let action = session.actions.get(req.index).cloned().ok_or_else(|| {
        Json(ErrorResponse { error: "無効なアクションです".to_string() })
    })?;
    if is_forbidden_action(&action, &session.state, &session.visited_states) {
        return Err(Json(ErrorResponse {
            error: "🚫 このアクションは同一盤面に遷移するため選択できません".to_string(),
        }));
    }

    let mut messages = Vec::new();
    if let Err(e) = apply_action(&mut session.state, &action) {
        return Err(Json(ErrorResponse { error: format!("エラー: {}", e) }));
    }
    process_automatic_steps(&mut session.state);
    advance(session, &mut messages);

    Ok(Json(build_response(&inner, &req.session, messages)))
}

async fn auto(
    State(app): State<AppState>,
    Json(req): Json<SessionReq>,
) -> Result<Json<GameResponse>, Json<ErrorResponse>> {
    let mut inner = app.lock().unwrap();
    if inner.model.is_none() {
        return Err(Json(ErrorResponse {
            error: "学習済みモデルがないためAI自動決定は利用できません".to_string(),
        }));
    }
    // borrow分割のため一旦セッションを取り出す
    let mut session = inner.sessions.remove(&req.session).ok_or_else(|| {
        Json(ErrorResponse { error: "セッションが見つかりません".to_string() })
    })?;

    let result = (|| {
        if session.winner.is_some() {
            return Err("ゲームは終了しています".to_string());
        }
        let model = inner.model.as_ref().unwrap();
        let mut best: Option<(usize, f32)> = None;
        for (i, action) in session.actions.iter().enumerate() {
            if is_forbidden_action(action, &session.state, &session.visited_states) {
                continue;
            }
            if let Some(val) =
                crate::ai::decision::evaluate_action(model, &session.state, action, &inner.device)
            {
                if best.map_or(true, |(_, bv)| val > bv) {
                    best = Some((i, val));
                }
            }
        }
        best.ok_or_else(|| "AIによる選択肢の評価に失敗しました".to_string())
    })();

    match result {
        Ok((idx, val)) => {
            let action = session.actions[idx].clone();
            let mut messages =
                vec![format!("AI自動決定（評価値: {:.3}）: {:?}", val, action)];
            let _ = apply_action(&mut session.state, &action);
            process_automatic_steps(&mut session.state);
            advance(&mut session, &mut messages);
            inner.sessions.insert(req.session.clone(), session);
            Ok(Json(build_response(&inner, &req.session, messages)))
        }
        Err(e) => {
            inner.sessions.insert(req.session.clone(), session);
            Err(Json(ErrorResponse { error: e }))
        }
    }
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
    let messages = vec![format!(
        "プレイヤー{} がサレンダーしました。プレイヤー{} の勝利！",
        session.state.player.player_id, winner
    )];
    Ok(Json(build_response(&inner, &req.session, messages)))
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
        println!("警告: 学習済みモデル weights が見つかりません。評価値は表示されません。");
        None
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
            .route("/api/act", post(act))
            .route("/api/auto", post(auto))
            .route("/api/surrender", post(surrender))
            .with_state(app_state);

        let listener = tokio::net::TcpListener::bind(("127.0.0.1", port))
            .await
            .expect("ポートのバインドに失敗しました");
        println!("=== TCG BattleSpirits デッキシミュレータ Webサービス ===");
        println!("http://localhost:{}/ をブラウザで開いてください", port);
        axum::serve(listener, app).await.unwrap();
    });
}
