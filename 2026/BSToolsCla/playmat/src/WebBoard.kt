import bstools.model.*
import kotlinx.cinterop.*
import kotlinx.serialization.Serializable
import platform.posix.*

// 盤面 → Playmats (web/index.html) が受け取る JSON。形式は BSTools GameServer の WebBoard.kt と同じ

@Serializable
data class WebCardDto(
    val id: String,
    val card_no: String,
    val name: String,
    val cost: Int,
    val reduction: String = "",
    val symbols: String = "",
    val cores: String = "0",
    val lv: Int = 1,
    val bp: Int? = null,
    val exhausted: Boolean = false,
    val image_url: String = "",
    val is_attacking: Boolean = false,
    val seq: Int? = null
)

@Serializable
data class WebPlayerDto(
    val player_id: Int,
    val life: Int,
    val reserve: String,
    val trash_cores: String,
    val count: Int = 0,
    val deck_count: Int,
    val hand_count: Int,
    val hand: List<WebCardDto> = emptyList(),
    val field: List<WebCardDto> = emptyList(),
    val trash_cards: List<WebCardDto> = emptyList()
)

@Serializable
data class CoreEffectDto(val source_id: String, val label: String, val normal: Int, val soul: Int, val direction: String)

@Serializable
data class WebActionOptionDto(
    val index: Int,
    val detail: String,
    val eval: Double? = 0.0,
    val forbidden: Boolean = false,
    val forbidden_reason: String? = null,
    val repeats_position: Boolean = false,
    val core_effects: List<CoreEffectDto> = emptyList(),
    /** コアの一括再配置の最終配置 (場所ごと)。再配置以外の手は空 */
    val core_target: List<CoreTargetDto> = emptyList()
)

@Serializable
data class CoreTargetDto(val source_id: String, val label: String, val normal: Int, val soul: Int)

@Serializable
data class WebActionGroupDto(
    val category: String,
    val best_eval: Double? = 0.0,
    val all_forbidden: Boolean = false,
    val forbidden_reason: String? = null,
    val options: List<WebActionOptionDto>,
    val card_no: String? = null,
    val target_instance_id: String? = null
)

@Serializable
data class WebGameStateDto(
    val session: String,
    val turn: Int,
    val active_player: Int,
    val choosing_player: Int = 0,
    val phase: String,
    val player: WebPlayerDto,
    val opponent: WebPlayerDto,
    val groups: List<WebActionGroupDto>,
    val view_mode: String = "god",
    val viewer: Int = 0,
    val can_act: Boolean = true,
    val n_index: Int? = null,
    /** 選択中のプレイヤーの手番で評価する選択肢の数 (同一局面に戻る手以外 + ステップ終了)。視点に関係なく常に出す */
    val eval_option_count: Int = 0,
    val n_label: String? = "ステップ終了",
    val n_eval: Double? = 0.0,
    val n_forbidden: Boolean = false,
    val n_forbidden_reason: String? = null,
    val can_undo: Boolean = false,
    val can_redo: Boolean = false,
    val winner: Int? = null,
    val messages: List<String> = emptyList(),
    val seed: Long? = null,
    val attacking_card_id: String? = null,
    val evaluated_count: Long = 0L,
    val visited_count: Int = 0,
    val replay_steps: List<ReplayStepDto> = emptyList(),
    val replay_file: String? = null
)

/** 選択ログ1件 (画面の「選択ログ (JSON)」タブ、ゲーム終了時の保存ファイル) */
@Serializable
data class ReplayStepDto(
    val step_index: Int,
    val turn: Int,
    val active_player: Int,
    val phase: String,
    val chosen_action_category: String,
    val chosen_action_kind: String,
    val chosen_action_detail: String,
    val chosen_action_eval: Double?,
    val available_action_count: Int,
    val state_snapshot: String
)

/** 保存するリプレイファイルの中身 */
@Serializable
data class ReplayFileDto(
    val session: String,
    val deck1: String,
    val deck2: String,
    val seed: Long,
    val winner: Int?,
    val weights_p1: String?,
    val weights_p2: String?,
    val steps: List<ReplayStepDto>
)

fun coreEffectsOf(action: GameAction): List<CoreEffectDto> {
    val merged = mutableMapOf<String, CoreEffectDto>()
    fun add(id: String, label: String, normal: Int, soul: Int, direction: String) {
        if (normal == 0 && soul == 0) return
        val key = "$direction:$id"
        val prev = merged[key]
        merged[key] = prev?.copy(normal = prev.normal + normal, soul = prev.soul + soul)
            ?: CoreEffectDto(id, label, normal, soul, direction)
    }
    for (m in action.moves) {
        add(m.fromId, m.fromLabel, m.normal, m.soul, "from")
        add(m.toId, m.toLabel, m.normal, m.soul, "to")
    }
    for (c in action.payFrom) add(c.sourceId, c.label, c.normal, c.soul, "from")
    for (c in action.placeFrom) add(c.sourceId, c.label, c.normal, c.soul, "from")
    return merged.values.toList()
}

const val VIEWER_GOD = 0

fun parseViewer(path: String, requestText: String): Int {
    val fromQuery = path.substringAfter("viewer=", "").takeWhile { it.isDigit() }
    if (fromQuery.isNotEmpty()) return fromQuery.toIntOrNull()?.coerceIn(0, 2) ?: VIEWER_GOD
    val body = requestText.substringAfter("\r\n\r\n", "")
    if (body.contains("\"viewer\"")) {
        val raw = body.substringAfter("\"viewer\"").substringAfter(":").trim().takeWhile { it.isDigit() }
        return raw.toIntOrNull()?.coerceIn(0, 2) ?: VIEWER_GOD
    }
    return VIEWER_GOD
}

private fun imageUrl(cardNo: String) = "https://www.battlespirits.com/images/cardlist/$cardNo.webp"

private fun cardDto(id: String, c: CardData) = WebCardDto(
    id = id, card_no = c.cardNo, name = c.name, cost = c.cost,
    reduction = c.reductionSymbols.joinToString("") { it.displayName },
    symbols = c.symbols.joinToString("") { it.displayName }, cores = "0", image_url = imageUrl(c.cardNo)
)

private fun fieldDto(f: FieldCard, state: GameState) = WebCardDto(
    id = f.instanceId, card_no = f.cardNo, name = f.name, cost = f.cost,
    symbols = f.symbols.joinToString("") { it.displayName }, cores = f.cores.format(), lv = f.level, bp = f.currentBp,
    exhausted = f.isExhausted, image_url = imageUrl(f.cardNo), is_attacking = f.instanceId == state.attackingCardId, seq = f.seq
)

fun buildWebGameStateDto(
    state: GameState,
    session: String = "default",
    extraMessages: List<String> = emptyList(),
    viewer: Int = VIEWER_GOD,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    replaySteps: List<ReplayStepDto> = emptyList(),
    replayFile: String? = null
): WebGameStateDto {
    val isGod = viewer == VIEWER_GOD
    val active = when {
        isGod -> state.activePlayer
        viewer == 1 -> state.player1
        else -> state.player2
    }
    val opp = if (active.playerId == 1) state.player2 else state.player1
    val canAct = state.winner == null && (isGod || viewer == state.choosingPlayerId)

    fun trashDto(p: PlayerState) = p.trashCards.mapIndexed { idx, c -> cardDto("trash_${p.playerId}_${c.cardNo}_$idx", c) }

    val groups = (if (canAct) PlaymatEval.listActions(state) else emptyList())
        .groupBy { it.category }
        .map { (category, actions) ->
            val selectable = actions.filter { !it.forbidden }
            val chooser = state.choosingPlayer
            WebActionGroupDto(
                category = category,
                best_eval = selectable.maxOfOrNull { it.eval } ?: actions.maxOfOrNull { it.eval } ?: 0.0,
                all_forbidden = selectable.isEmpty(),
                forbidden_reason = if (selectable.isEmpty()) actions.firstOrNull()?.forbiddenReason else null,
                options = actions.map {
                    WebActionOptionDto(
                        index = it.index, detail = it.detail, eval = it.eval, forbidden = it.forbidden,
                        forbidden_reason = it.forbiddenReason, repeats_position = it.repeatsPosition,
                        core_effects = coreEffectsOf(it),
                        core_target = it.targetConfig.map { c -> CoreTargetDto(c.sourceId, c.label, c.normal, c.soul) }
                    )
                },
                card_no = actions.firstNotNullOfOrNull { a ->
                    a.handIndex.takeIf { it >= 0 && it < chooser.hand.size }?.let { chooser.hand[it].cardNo }
                },
                target_instance_id = actions.firstNotNullOfOrNull { it.targetInstanceId }
            )
        }

    val nLabel = when (state.step) {
        Step.MAIN -> "メインステップ終了"
        Step.ATTACK_DECLARATION -> "アタックステップ終了"
        Step.BLOCK_DECLARATION -> "ブロックを選択してください"
        Step.MAIN_2 -> "第2メインステップ終了"
        Step.END -> "ターン終了"
        else -> "ステップ終了"
    }

    return WebGameStateDto(
        session = session,
        turn = state.turn,
        active_player = state.activePlayerId,
        choosing_player = state.choosingPlayerId,
        phase = "${state.step.displayName} [${state.format.displayName}]",
        player = WebPlayerDto(
            player_id = active.playerId, life = active.life, reserve = active.reserve.format(),
            trash_cores = active.trash.format(), deck_count = active.deck.size, hand_count = active.hand.size,
            hand = active.hand.mapIndexed { idx, c -> cardDto("hand_${c.cardNo}_$idx", c) },
            field = active.field.map { fieldDto(it, state) }, trash_cards = trashDto(active)
        ),
        opponent = WebPlayerDto(
            player_id = opp.playerId, life = opp.life, reserve = opp.reserve.format(),
            trash_cores = opp.trash.format(), deck_count = opp.deck.size, hand_count = opp.hand.size,
            hand = if (!isGod) emptyList() else opp.hand.mapIndexed { idx, c -> cardDto("opp_hand_${c.cardNo}_$idx", c) },
            field = opp.field.map { fieldDto(it, state) }, trash_cards = trashDto(opp)
        ),
        groups = groups,
        view_mode = if (isGod) "god" else "play",
        viewer = viewer,
        can_act = canAct,
        n_index = if (!canAct || state.step == Step.BLOCK_DECLARATION) null else 999,
        eval_option_count = PlaymatEval.evaluatedOptionCount(state),
        n_label = nLabel,
        n_eval = if (state.winner != null || state.step == Step.BLOCK_DECLARATION) null else PlaymatEval.stepEndValue(state),
        can_undo = canUndo,
        can_redo = canRedo,
        winner = state.winner,
        messages = extraMessages.ifEmpty {
            listOf("【ターン ${state.turn}】 手番: プレイヤー${state.activePlayerId} (${state.step.displayName})")
        },
        seed = state.seed,
        attacking_card_id = state.attackingCardId,
        evaluated_count = state.evaluatedCount,
        visited_count = state.visitedPositions.size,
        replay_steps = replaySteps,
        replay_file = replayFile
    )
}

@OptIn(ExperimentalForeignApi::class)
fun loadHtmlFile(): String {
    for (p in listOf("playmat/web/index.html", "web/index.html", "../playmat/web/index.html")) {
        val f = fopen(p, "rb") ?: continue
        fseek(f, 0, SEEK_END)
        val size = ftell(f)
        fseek(f, 0, SEEK_SET)
        if (size > 0) {
            memScoped {
                val buf = allocArray<ByteVar>(size + 1)
                fread(buf, 1u, size.convert(), f)
                buf[size] = 0.toByte()
                fclose(f)
                return buf.toKString()
            }
        }
        fclose(f)
    }
    return "<html><body><h1>BSToolsCla Playmat</h1><p>playmat/web/index.html が見つかりません</p></body></html>"
}
