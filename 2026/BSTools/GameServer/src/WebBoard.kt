import bstools.model.*
import kotlinx.cinterop.*
import kotlinx.serialization.Serializable
import platform.posix.*

@Serializable
data class CardVisualPosition(
    val id: String,
    val name: String,
    val x: Double,
    val y: Double,
    val width: Double = 80.0,
    val height: Double = 112.0,
    val zone: String,
    val isExhausted: Boolean = false,
    val coreCount: Int = 0,
    val isSoulCore: Boolean = false,
    val level: Int = 1
)

@Serializable
data class PlaymatLayout(
    val boardWidth: Double = 1280.0,
    val boardHeight: Double = 720.0,
    val player1Cards: List<CardVisualPosition>,
    val player2Cards: List<CardVisualPosition>,
    val turnInfo: String,
    val activeZone: String
)

@Serializable
data class RuleValidationRequest(
    val actionType: String,
    val sourceCardNo: String,
    val currentCores: Int,
    val costRequired: Int,
    val targetInstanceId: String? = null
)

@Serializable
data class RuleValidationResponse(
    val isValid: Boolean,
    val reason: String,
    val resultingCores: Int = 0
)

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
    val image_url: String = ""
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
data class CoreEffectDto(
    val source_id: String,
    val label: String,
    val normal: Int,
    val soul: Int,
    val direction: String // "from" or "to"
)

@Serializable
data class WebActionOptionDto(
    val index: Int,
    val detail: String,
    val eval: Double? = 0.5,
    val forbidden: Boolean = false,
    val forbidden_reason: String? = null,
    val repeats_position: Boolean = false,
    val core_effects: List<CoreEffectDto> = emptyList()
)

@Serializable
data class WebActionGroupDto(
    val category: String,
    val best_eval: Double? = 0.5,
    val all_forbidden: Boolean = false,
    val forbidden_reason: String? = null,
    val options: List<WebActionOptionDto>,
    val card_no: String? = null
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
    val n_label: String? = "ステップ終了",
    val n_eval: Double? = 0.5,
    val n_forbidden: Boolean = false,
    val n_forbidden_reason: String? = null,
    val can_undo: Boolean = false,
    val can_redo: Boolean = false,
    val winner: Int? = null,
    val messages: List<String> = emptyList(),
    val seed: Long? = null
)

fun coreEffectsOf(action: GameAction): List<CoreEffectDto> {
    val merged = mutableMapOf<String, CoreEffectDto>()

    fun add(id: String, label: String, normal: Int, soul: Int, direction: String) {
        if (normal == 0 && soul == 0) return
        val key = "$direction:$id"
        val prev = merged[key]
        merged[key] = if (prev == null) {
            CoreEffectDto(id, label, normal, soul, direction)
        } else {
            prev.copy(normal = prev.normal + normal, soul = prev.soul + soul)
        }
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

fun buildWebGameStateDto(
    state: GameState,
    session: String = "session_1",
    extraMessages: List<String> = emptyList(),
    viewer: Int = VIEWER_GOD
): WebGameStateDto {
    val isGod = viewer == VIEWER_GOD

    val active = when {
        isGod -> state.activePlayer
        viewer == 1 -> state.player1
        else -> state.player2
    }
    val opp = if (active.playerId == 1) state.player2 else state.player1

    val oppHandDto = if (!isGod) emptyList() else opp.hand.mapIndexed { idx, c ->
        WebCardDto(
            id = "opp_hand_${c.cardNo}_$idx",
            card_no = c.cardNo,
            name = c.name,
            cost = c.cost,
            reduction = c.reductionSymbols.joinToString("") { it.displayName },
            symbols = c.symbols.joinToString("") { it.displayName },
            cores = "0",
            image_url = "https://www.battlespirits.com/images/cardlist/${c.cardNo}.webp"
        )
    }

    val canAct = state.winner == null && (isGod || viewer == state.choosingPlayerId)

    fun trashDto(p: PlayerState) = p.trashCards.mapIndexed { idx, c ->
        WebCardDto(
            id = "trash_${p.playerId}_${c.cardNo}_$idx",
            card_no = c.cardNo,
            name = c.name,
            cost = c.cost,
            symbols = c.symbols.joinToString("") { it.displayName },
            image_url = "https://www.battlespirits.com/images/cardlist/${c.cardNo}.webp"
        )
    }

    val handDto = active.hand.mapIndexed { idx, c ->
        WebCardDto(
            id = "hand_${c.cardNo}_$idx",
            card_no = c.cardNo,
            name = c.name,
            cost = c.cost,
            reduction = c.reductionSymbols.joinToString("") { it.displayName },
            symbols = c.symbols.joinToString("") { it.displayName },
            cores = "0",
            image_url = "https://www.battlespirits.com/images/cardlist/${c.cardNo}.webp"
        )
    }

    val myFieldDto = active.field.map { f ->
        WebCardDto(
            id = f.instanceId,
            card_no = f.cardNo,
            name = f.name,
            cost = 3,
            symbols = f.symbols.joinToString("") { it.displayName },
            cores = f.cores.format(),
            lv = f.level,
            bp = f.currentBp,
            exhausted = f.isExhausted,
            image_url = "https://www.battlespirits.com/images/cardlist/${f.cardNo}.webp"
        )
    }

    val oppFieldDto = opp.field.map { f ->
        WebCardDto(
            id = f.instanceId,
            card_no = f.cardNo,
            name = f.name,
            cost = 3,
            symbols = f.symbols.joinToString("") { it.displayName },
            cores = f.cores.format(),
            lv = f.level,
            bp = f.currentBp,
            exhausted = f.isExhausted,
            image_url = "https://www.battlespirits.com/images/cardlist/${f.cardNo}.webp"
        )
    }

    val groups = (if (canAct) enumerateActions(state) else emptyList())
        .groupBy { it.category }
        .map { (category, actions) ->
            val selectable = actions.filter { !it.forbidden }
            val cardNo = actions.firstNotNullOfOrNull { a ->
                a.handIndex.takeIf { it >= 0 && it < active.hand.size }?.let { active.hand[it].cardNo }
            }
            WebActionGroupDto(
                category = category,
                best_eval = selectable.maxOfOrNull { it.eval } ?: actions.maxOfOrNull { it.eval } ?: 0.5,
                all_forbidden = selectable.isEmpty(),
                forbidden_reason = if (selectable.isEmpty()) actions.firstOrNull()?.forbiddenReason else null,
                options = actions.map {
                    WebActionOptionDto(
                        index = it.index,
                        detail = it.detail,
                        eval = it.eval,
                        forbidden = it.forbidden,
                        forbidden_reason = it.forbiddenReason,
                        repeats_position = it.repeatsPosition,
                        core_effects = coreEffectsOf(it)
                    )
                },
                card_no = cardNo
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
            player_id = active.playerId,
            life = active.life,
            reserve = active.reserve.format(),
            trash_cores = active.trash.format(),
            count = 0,
            deck_count = active.deck.size,
            hand_count = active.hand.size,
            hand = handDto,
            field = myFieldDto,
            trash_cards = trashDto(active)
        ),
        opponent = WebPlayerDto(
            player_id = opp.playerId,
            life = opp.life,
            reserve = opp.reserve.format(),
            trash_cores = opp.trash.format(),
            count = 0,
            deck_count = opp.deck.size,
            hand_count = opp.hand.size,
            hand = oppHandDto,
            field = oppFieldDto,
            trash_cards = trashDto(opp)
        ),
        groups = groups,
        view_mode = if (isGod) "god" else "play",
        viewer = viewer,
        can_act = canAct,
        n_index = if (!canAct || state.step == Step.BLOCK_DECLARATION) null else 999,
        n_label = nLabel,
        n_eval = if (state.winner != null || state.step == Step.BLOCK_DECLARATION) null else stepEndEval(state),
        can_undo = false,
        can_redo = false,
        winner = state.winner,
        messages = extraMessages.ifEmpty {
            listOf("【ターン ${state.turn}】 手番: プレイヤー${state.activePlayerId} (${state.step.displayName})")
        },
        seed = state.seed
    )
}

@OptIn(ExperimentalForeignApi::class)
fun loadHtmlFile(): String {
    val candidates = listOf(
        "Playmats/index.html",
        "Playmats/src/index.html",
        "../Playmats/index.html",
        "../Playmats/src/index.html",
        "index.html"
    )
    for (p in candidates) {
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
    return "<html><body><h1>BS GameServer</h1><p>Playmats index.html not found</p></body></html>"
}
