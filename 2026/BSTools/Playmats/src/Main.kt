import bstools.model.*
import kotlinx.cinterop.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import platform.posix.*

val jsonFormat = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

val compactJson = Json {
    prettyPrint = false
    ignoreUnknownKeys = true
    encodeDefaults = true
}

val laxJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

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
    /** トラッシュのカード。公開領域なので両者がいつでも見られる (総合ルール 4-4-2) */
    val trash_cards: List<WebCardDto> = emptyList()
)

/**
 * その選択肢で increase/decrease するコア。WebUIがホバー時に移動元・移動先をマークするために使う。
 * source_id は "Reserve" または FieldCard.instanceId。
 */
@Serializable
data class CoreEffectDto(
    val source_id: String,
    val label: String,
    val normal: Int,
    val soul: Int,
    /** "from" = ここからコアが出る / "to" = ここへコアが入る */
    val direction: String
)

@Serializable
data class WebActionOptionDto(
    val index: Int,
    val detail: String,
    val eval: Double? = 0.5,
    val forbidden: Boolean = false,
    val forbidden_reason: String? = null,
    /** 選択は可能だが、選ぶと既訪問の局面に戻り盤面が進まない手であることを示す */
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
    /**
     * このグループが手札の特定カードに由来する(SUMMON/USE_MAGIC)場合のカード番号。
     * フロントエンドが手札カードとグループを紐付ける際、カード名の部分文字列一致
     * (`g.category.includes(c.name)`) では同名カードが手札とフィールドに同時に存在するとき
     * 無関係なグループ(フィールド側のコア移動など)まで誤って拾ってしまうバグが実際にあった。
     * `card_no` による一致に限定することで解消する。手札に由来しないグループ(コア移動・
     * アタック・ブロック等)は null。
     */
    val card_no: String? = null
)

@Serializable
data class WebGameStateDto(
    val session: String,
    val turn: Int,
    val active_player: Int,
    /** いま選択を行うプレイヤー。ブロック宣言だけはアタックされている側になる */
    val choosing_player: Int = 0,
    val phase: String,
    val player: WebPlayerDto,
    val opponent: WebPlayerDto,
    val groups: List<WebActionGroupDto>,
    /** "god" = 全情報を参照・操作できる / "play" = 公開情報と自分の非公開情報のみ */
    val view_mode: String = "god",
    /** この盤面を見ている視点。0 = GodMode、1/2 = そのプレイヤー */
    val viewer: Int = 0,
    /** この視点でいま操作できるか。PlayMode では自分が選択者のときだけ true */
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

class PlaymatService {

    fun generatePlaymatLayout(state: GameState): PlaymatLayout {
        val p1Cards = mutableListOf<CardVisualPosition>()
        val p2Cards = mutableListOf<CardVisualPosition>()

        state.player1.hand.forEachIndexed { idx, card ->
            p1Cards.add(
                CardVisualPosition(
                    id = "p1_hand_$idx",
                    name = card.name,
                    x = 200.0 + (idx * 90.0),
                    y = 580.0,
                    zone = "HAND"
                )
            )
        }

        state.player1.field.forEachIndexed { idx, fc ->
            p1Cards.add(
                CardVisualPosition(
                    id = fc.instanceId,
                    name = fc.name,
                    x = 250.0 + (idx * 110.0),
                    y = 400.0,
                    zone = "FIELD",
                    isExhausted = fc.isExhausted,
                    coreCount = fc.cores.normal,
                    isSoulCore = fc.cores.soul > 0,
                    level = fc.level
                )
            )
        }

        state.player2.hand.forEachIndexed { idx, card ->
            p2Cards.add(
                CardVisualPosition(
                    id = "p2_hand_$idx",
                    name = card.name,
                    x = 200.0 + (idx * 90.0),
                    y = 20.0,
                    zone = "HAND"
                )
            )
        }

        state.player2.field.forEachIndexed { idx, fc ->
            p2Cards.add(
                CardVisualPosition(
                    id = fc.instanceId,
                    name = fc.name,
                    x = 250.0 + (idx * 110.0),
                    y = 180.0,
                    zone = "FIELD",
                    isExhausted = fc.isExhausted,
                    coreCount = fc.cores.normal,
                    isSoulCore = fc.cores.soul > 0,
                    level = fc.level
                )
            )
        }

        return PlaymatLayout(
            player1Cards = p1Cards,
            player2Cards = p2Cards,
            turnInfo = "ターン ${state.turn} - ${state.activePlayer.name} (${state.step.displayName})",
            activeZone = if (state.activePlayerId == 1) "PLAYER_1" else "PLAYER_2"
        )
    }

    fun validateRule(request: RuleValidationRequest): RuleValidationResponse {
        return when (request.actionType) {
            "SUMMON" -> {
                if (request.currentCores >= request.costRequired + 1) {
                    RuleValidationResponse(true, "召喚可能 (コア十分)", request.currentCores - request.costRequired)
                } else {
                    RuleValidationResponse(false, "コア不足: 必要コスト ${request.costRequired} + 維持コア1 に対して保有コア ${request.currentCores}")
                }
            }
            "ATTACK" -> RuleValidationResponse(true, "アタック宣言可能")
            "BLOCK" -> RuleValidationResponse(true, "ブロック宣言可能")
            else -> RuleValidationResponse(false, "未知のアクションタイプ: ${request.actionType}")
        }
    }
}


// =====================================================================
// GameServer 連携 (選択肢の列挙を GameServer に委譲する)
// =====================================================================

var gameServerUrl: String = "http://localhost:8081"
var gameServerOnline: Boolean = false

/** ソケットが読み書き可能になるまで待つ。タイムアウトしたら false */
@OptIn(ExperimentalForeignApi::class)
fun waitSocketReady(sock: platform.posix.SOCKET, timeoutMs: Int, forWrite: Boolean): Boolean = memScoped {
    val fds = alloc<fd_set>()
    fds.fd_count = 1u
    fds.fd_array[0] = sock
    val tv = alloc<timeval>()
    tv.tv_sec = timeoutMs / 1000
    tv.tv_usec = (timeoutMs % 1000) * 1000
    val ready = if (forWrite) {
        platform.windows.select(0, null, fds.ptr, null, tv.ptr)
    } else {
        platform.windows.select(0, fds.ptr, null, null, tv.ptr)
    }
    return ready > 0
}

/**
 * GameServer へ JSON を POST する最小HTTPクライアント。到達不能・タイムアウト時は null。
 * 盤面更新のたびに同期的に呼ぶため、GameServerが応答しなくてもUIが固まらないよう
 * ノンブロッキングソケット + select でタイムアウトを強制する。
 */
@OptIn(ExperimentalForeignApi::class)
fun httpPostJson(baseUrl: String, path: String, body: String, timeoutMs: Int = 1500): String? {
    val hostPort = baseUrl.substringAfter("://").trimEnd('/')
    val host = hostPort.substringBefore(":")
    val port = hostPort.substringAfter(":", "80").toIntOrNull() ?: 80
    val ip = if (host == "localhost") "127.0.0.1" else host

    memScoped {
        val sock = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)
        if (sock == platform.posix.INVALID_SOCKET) return null

        val nonBlocking = alloc<UIntVar>()
        nonBlocking.value = 1u
        platform.windows.ioctlsocket(sock, platform.windows.FIONBIO.convert(), nonBlocking.ptr)

        val addr = alloc<sockaddr_in>()
        addr.sin_family = AF_INET.convert()
        addr.sin_port = htons(port.toShort()).toUShort()
        addr.sin_addr.S_un.S_addr = inet_addr(ip)

        connect(sock, addr.ptr.reinterpret(), sizeOf<sockaddr_in>().convert())
        // ノンブロッキングなので connect は即時復帰する。接続完了は書き込み可能で判定する
        if (!waitSocketReady(sock, timeoutMs, forWrite = true)) {
            closesocket(sock)
            return null
        }

        val bodyBytes = body.encodeToByteArray()
        val header = "POST $path HTTP/1.1\r\n" +
                "Host: $host:$port\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: ${bodyBytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        val payload = header.encodeToByteArray() + bodyBytes

        var sent = 0
        payload.usePinned { pinned ->
            while (sent < payload.size) {
                val n = send(sock, pinned.addressOf(sent), payload.size - sent, 0)
                if (n > 0) {
                    sent += n
                } else if (!waitSocketReady(sock, timeoutMs, forWrite = true)) {
                    break
                }
            }
        }
        if (sent < payload.size) {
            closesocket(sock)
            return null
        }

        val bufSize = 65536
        val buf = allocArray<ByteVar>(bufSize)
        var acc = ByteArray(0)
        while (true) {
            if (!waitSocketReady(sock, timeoutMs, forWrite = false)) break
            val n = recv(sock, buf, (bufSize - 1).convert(), 0)
            if (n <= 0) break
            acc += ByteArray(n) { buf[it] }
        }
        closesocket(sock)

        if (acc.isEmpty()) return null
        val text = acc.decodeToString()
        val sep = text.indexOf("\r\n\r\n")
        return if (sep >= 0) text.substring(sep + 4) else null
    }
}

private var cachedActions: List<GameAction>? = null
private var gameServerRetryAt: Long = 0

/** 局面を変更したら必ず呼ぶ。次回の列挙で GameServer に問い合わせ直す */
fun invalidateActionCache() {
    cachedActions = null
}

/**
 * 選択肢の列挙を GameServer に委譲する (Readme の役割分担どおりの構成)。
 * GameServer と Playmats は model/src/Rules.kt の同一関数を使うため、
 * 到達できないときにローカルで列挙しても結果は完全に一致する。
 *
 * 1リクエスト中に複数回（DTO生成・SSE配信・index引き直し）呼ばれるので結果をキャッシュし、
 * GameServerが落ちている間は毎回タイムアウトを待たないよう再試行を間引く。
 */
@OptIn(ExperimentalForeignApi::class)
fun enumerateActionsForState(state: GameState): List<GameAction> {
    cachedActions?.let { return it }

    val now = time(null)
    val skipRemote = !gameServerOnline && now < gameServerRetryAt
    if (!skipRemote) {
        // デッキは列挙に不要で、JSONが数十KBに膨らむため送らない
        val lean = state.copy(
            player1 = state.player1.copy(deck = mutableListOf()),
            player2 = state.player2.copy(deck = mutableListOf())
        )
        val response = httpPostJson(gameServerUrl, "/api/actions", compactJson.encodeToString(lean))
        val remote = response?.let {
            try {
                laxJson.decodeFromString<List<GameAction>>(it)
            } catch (e: Exception) {
                null // フォーマット不一致時はローカル列挙にフォールバックする
            }
        }
        if (remote != null) {
            gameServerOnline = true
            cachedActions = remote
            return remote
        }
        if (gameServerOnline) println("GameServer unreachable at $gameServerUrl - falling back to local enumeration")
        gameServerOnline = false
        gameServerRetryAt = now + 10
    }

    val local = enumerateActions(state)
    cachedActions = local
    return local
}


/**
 * 選択肢が動かすコアを、場所ごとに集計して返す。
 * 召喚・マジック使用は支払い分と配置分の両方がその場所から出ていくので合算する。
 * コア移動は移動元(from)と移動先(to)の両方を返し、WebUIが向きを区別して表示できるようにする。
 */
fun coreEffectsOf(action: GameAction): List<CoreEffectDto> {
    // (source_id, direction) をキーにまとめる。Map の順序は挿入順なので表示順も安定する
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

/** 観戦者。0 = GodMode (全情報を見て両者を操作できる) / 1,2 = そのプレイヤー視点の PlayMode */
const val VIEWER_GOD = 0

/** リクエストから視点を取り出す。クエリ `?viewer=N` と POST本文の `"viewer":N` の両方に対応する */
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

    // GodMode は盤面をターンプレイヤー基準で見る。
    // PlayMode は常に自分を下、相手を上に固定して視点が動かないようにする。
    val active = when {
        isGod -> state.activePlayer
        viewer == 1 -> state.player1
        else -> state.player2
    }
    val opp = if (active.playerId == 1) state.player2 else state.player1

    // 相手の手札は非公開情報。GodMode のときだけ中身も渡す (枚数は常に公開)
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

    // PlayMode では自分が選択者のときだけ操作できる
    val canAct = state.winner == null && (isGod || viewer == state.choosingPlayerId)

    // トラッシュは公開領域。どちらのモードでも両者の内容を渡す (総合ルール 4-4-2)
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

    // 列挙ロジックは enumerateActions に一本化し、同じ index で applyAction が再現できるようにする。
    // 操作できない観戦者には選択肢を渡さない (見えてしまうと相手の手が読めてしまう)
    val groups = (if (canAct) enumerateActionsForState(state) else emptyList())
        .groupBy { it.category }
        .map { (category, actions) ->
            val selectable = actions.filter { !it.forbidden }
            // SUMMON/USE_MAGIC は手札カード由来なので、その handIndex から cardNo を引ける。
            // choosingPlayer の手札を参照する (SUMMON/USE_MAGIC はメインステップでのみ発生し、
            // メインステップでは choosing_player は常に active と一致するため active.hand でよい)
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
        // ブロック宣言はブロックするかしないかを必ず選ぶので、ステップ終了ボタンは出さない
        n_index = if (!canAct || state.step == Step.BLOCK_DECLARATION) null else 999,
        n_label = nLabel,
        // ステップ終了が選べない局面では評価値も出さない (選択肢と比較されて誤判定されるため)
        n_eval = if (state.winner != null || state.step == Step.BLOCK_DECLARATION) null else stepEndEval(state),
        // Undo/Redo は未実装のため、UI側のボタンを有効に見せない
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
        "Playmats/src/index.html",
        "src/index.html",
        "../BS/BSRust/static/index.html",
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
    return "<html><body><h1>BS Kogyo Board</h1><p>index.html not found</p></body></html>"
}

@OptIn(ExperimentalForeignApi::class)
fun runHttpServer(port: Int = 8080, initialGameServerUrl: String = "http://localhost:8081") {
    gameServerUrl = initialGameServerUrl
    memScoped {
        val wsaData = alloc<WSADATA>()
        if (WSAStartup(0x0202u, wsaData.ptr) != 0) {
            println("WSAStartup failed")
            return
        }

        val serverSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)
        if (serverSocket == platform.posix.INVALID_SOCKET) {
            println("Socket creation failed")
            WSACleanup()
            return
        }

        // ノンブロッキングモード設定
        val nonBlocking = alloc<UIntVar>()
        nonBlocking.value = 1u
        platform.windows.ioctlsocket(serverSocket, platform.windows.FIONBIO.convert(), nonBlocking.ptr)

        val serverAddr = alloc<sockaddr_in>()
        serverAddr.sin_family = AF_INET.convert()
        serverAddr.sin_port = htons(port.toShort()).toUShort()
        serverAddr.sin_addr.S_un.S_addr = INADDR_ANY

        if (bind(serverSocket, serverAddr.ptr.reinterpret(), sizeOf<sockaddr_in>().convert()) != 0) {
            println("Bind failed on port $port")
            closesocket(serverSocket)
            WSACleanup()
            return
        }

        if (listen(serverSocket, SOMAXCONN) != 0) {
            println("Listen failed")
            closesocket(serverSocket)
            WSACleanup()
            return
        }

        println("=================================================================")
        println("=== Playmats Web Server started at http://localhost:$port/ ===")
        println("=== GameServer URL: $gameServerUrl ===")
        println("=== Non-blocking SSE Real-time Subscription Engine Enabled ===")
        println("=================================================================")

        val service = PlaymatService()
        val gameState = createInitialGameState()
        val htmlContent = loadHtmlFile()
        // 観戦者ごとに見える情報が違うため、SSE接続とその視点を対応付けて保持する
        val subscribers = mutableMapOf<platform.posix.SOCKET, Int>()
        val clientSockets = mutableListOf<platform.posix.SOCKET>()
        val buffer = allocArray<ByteVar>(8192)

        fun broadcastState() {
            // 視点ごとにDTOは1回だけ作り、同じ視点の接続で使い回す
            val payloads = mutableMapOf<Int, ByteArray>()
            val deadList = mutableListOf<platform.posix.SOCKET>()

            for ((sock, viewer) in subscribers) {
                val bytes = payloads.getOrPut(viewer) {
                    val dto = buildWebGameStateDto(gameState, viewer = viewer)
                    "data: ${compactJson.encodeToString(dto)}\n\n".encodeToByteArray()
                }
                val sent = bytes.usePinned { p -> send(sock, p.addressOf(0), bytes.size, 0) }
                if (sent <= 0) deadList.add(sock)
            }
            for (dead in deadList) {
                subscribers.remove(dead)
                closesocket(dead)
            }
        }

        val readFds = alloc<fd_set>()
        val tv = alloc<timeval>()

        while (true) {
            // fd_set の初期化
            readFds.fd_count = 0u
            readFds.fd_array[readFds.fd_count.toInt()] = serverSocket
            readFds.fd_count++

            for (cs in clientSockets) {
                if (readFds.fd_count.toInt() < 64) {
                    readFds.fd_array[readFds.fd_count.toInt()] = cs
                    readFds.fd_count++
                }
            }

            tv.tv_sec = 0
            tv.tv_usec = 10000 // 10ms

            val selRes = platform.windows.select(0, readFds.ptr, null, null, tv.ptr)
            if (selRes <= 0) continue

            // 1. 新規接続受け入れ
            var isServerReady = false
            for (i in 0 until readFds.fd_count.toInt()) {
                if (readFds.fd_array[i] == serverSocket) {
                    isServerReady = true
                    break
                }
            }

            if (isServerReady) {
                val newSock = accept(serverSocket, null, null)
                if (newSock != platform.posix.INVALID_SOCKET) {
                    val nb = alloc<UIntVar>()
                    nb.value = 1u
                    platform.windows.ioctlsocket(newSock, platform.windows.FIONBIO.convert(), nb.ptr)
                    clientSockets.add(newSock)
                }
            }

            // 2. 既存クライアントからのリクエスト受信
            val closedSockets = mutableListOf<platform.posix.SOCKET>()

            for (cs in clientSockets.toList()) {
                var isCsReady = false
                for (i in 0 until readFds.fd_count.toInt()) {
                    if (readFds.fd_array[i] == cs) {
                        isCsReady = true
                        break
                    }
                }
                if (!isCsReady) continue

                val bytesRead = recv(cs, buffer, 8191, 0)
                if (bytesRead > 0) {
                    buffer[bytesRead] = 0.toByte()
                    val requestText = buffer.toKString()
                    val firstLine = requestText.lines().firstOrNull() ?: ""
                    val parts = firstLine.split(" ")
                    val method = parts.getOrNull(0) ?: "GET"
                    val path = parts.getOrNull(1) ?: "/"

                    var keepAlive = false
                    val responseBody: String
                    val contentType: String
                    var statusCode = "200 OK"

                    // 視点は ?viewer=0|1|2 (クエリ) または POST本文の "viewer" で指定する
                    val requestViewer = parseViewer(path, requestText)

                    when {
                        path == "/" || path == "/index.html" -> {
                            responseBody = htmlContent
                            contentType = "text/html; charset=UTF-8"
                        }
                        path.startsWith("/api/events") || path.startsWith("/api/subscribe") -> {
                            keepAlive = true
                            val header = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: text/event-stream\r\n" +
                                    "Cache-Control: no-cache\r\n" +
                                    "Connection: keep-alive\r\n" +
                                    "Access-Control-Allow-Origin: *\r\n\r\n"
                            val hBytes = header.encodeToByteArray()
                            hBytes.usePinned { hPinned ->
                                send(cs, hPinned.addressOf(0), hBytes.size, 0)
                            }

                            val initDto = buildWebGameStateDto(gameState, viewer = requestViewer)
                            val initSse = "data: ${compactJson.encodeToString(initDto)}\n\n"
                            val initBytes = initSse.encodeToByteArray()
                            initBytes.usePinned { iPinned ->
                                send(cs, iPinned.addressOf(0), initBytes.size, 0)
                            }

                            subscribers[cs] = requestViewer
                            responseBody = ""
                            contentType = ""
                        }
                        path.startsWith("/api/state") -> {
                            val dto = buildWebGameStateDto(gameState, viewer = requestViewer)
                            responseBody = jsonFormat.encodeToString(dto)
                            contentType = "application/json"
                        }
                        path == "/api/config" -> {
                            if (method == "POST" && requestText.contains("\"gameServerUrl\"")) {
                                val newUrl = requestText.substringAfter("\"gameServerUrl\"").substringAfter(":").substringAfter("\"").substringBefore("\"").trim()
                                if (newUrl.isNotEmpty()) {
                                    gameServerUrl = newUrl
                                    println("Updated GameServer URL to: $gameServerUrl")
                                }
                            }
                            responseBody = """{"status":"ok","gameServerUrl":"$gameServerUrl","port":$port}"""
                            contentType = "application/json"
                        }
                        path == "/api/eval-config" -> {
                            // 選択肢の列挙は基本的に GameServer に委譲する ([enumerateActionsForState]) ため、
                            // GUIからの切り替えはこのプロセス自身(ローカルフォールバック用)と GameServer の
                            // 両方に適用しないと、GameServerが生きている間は切り替えが反映されない。
                            val evalConfigBody = requestText.substringAfter("\r\n\r\n", "")
                            if (method == "POST" && evalConfigBody.isNotBlank()) {
                                val mode = evalConfigBody.substringAfter("\"mode\"", "").substringAfter(":", "")
                                    .trim().removePrefix("\"").substringBefore("\"").trim()
                                fun extract(field: String): String? = if (evalConfigBody.contains("\"$field\"")) {
                                    evalConfigBody.substringAfter("\"$field\"").substringAfter(":")
                                        .substringAfter("\"").substringBefore("\"").trim()
                                } else null
                                val weightsPathP1 = extract("weightsPathP1")
                                val weightsPathP2 = extract("weightsPathP2")

                                val localResult = if (mode.isBlank()) "modeが指定されていません" else applyEvalConfig(mode, weightsPathP1, weightsPathP2)
                                // GameServer側にも同じ設定を中継する。到達不能でもローカル評価は既に切り替わっているので
                                // フォールバック動作には影響しない(gameServerOnline=false時はローカル評価が使われる)。
                                val remoteResponse = httpPostJson(gameServerUrl, "/api/eval-config", evalConfigBody)

                                if (localResult != "ok") statusCode = "400 Bad Request"
                                responseBody = """{"status":"${if (localResult == "ok") "ok" else "error"}","message":"$localResult","evalMode":"${evalMode.name}","gameServerReached":${remoteResponse != null}}"""
                            } else {
                                responseBody = """{"evalMode":"${evalMode.name}"}"""
                            }
                            contentType = "application/json"
                        }
                        path == "/api/new" || path == "/api/restart" -> {
                            val d1Name = if (requestText.contains("\"deck1\"")) {
                                requestText.substringAfter("\"deck1\"").substringAfter(":").substringAfter("\"").substringBefore("\"").trim()
                            } else "deck-kogyo.yaml"
                            val d2Name = if (requestText.contains("\"deck2\"")) {
                                requestText.substringAfter("\"deck2\"").substringAfter(":").substringAfter("\"").substringBefore("\"").trim()
                            } else "deck-kogyo.yaml"

                            val seedToUse = if (path == "/api/restart") {
                                gameState.seed
                            } else {
                                if (requestText.contains("\"seed\"")) {
                                    // 値がJSON文字列("42")として送られてもバレ数値(42)として送られても解析できるよう、
                                    // 先頭の引用符を許容してから数字を取り出す。引用符を考慮せず takeWhile するだけだと
                                    // `"seed":"42"` の場合に最初の文字が `"` で即座に空文字列になり、
                                    // 指定したシードが無視されてランダムシードにすり替わってしまう(再現性テストで実際に踏んだ)。
                                    val raw = requestText.substringAfter("\"seed\"").substringAfter(":").trim()
                                        .removePrefix("\"")
                                        .takeWhile { it.isDigit() || it == '-' }
                                    raw.toLongOrNull() ?: kotlin.random.Random.nextLong(1, 1_000_000)
                                } else {
                                    kotlin.random.Random.nextLong(1, 1_000_000)
                                }
                            }

                            // リスタート時は同じフォーマットを維持する
                            val formatToUse = if (path == "/api/restart") {
                                gameState.format
                            } else if (requestText.contains("\"format\"")) {
                                GameFormat.fromString(
                                    requestText.substringAfter("\"format\"").substringAfter(":")
                                        .substringAfter("\"").substringBefore("\"").trim()
                                )
                            } else GameFormat.STANDARD

                            val initial = createInitialGameState(
                                d1Name.ifBlank { "deck-kogyo.yaml" },
                                d2Name.ifBlank { "deck-kogyo.yaml" },
                                seedToUse,
                                formatToUse
                            )
                            gameState.turn = initial.turn
                            gameState.activePlayerId = initial.activePlayerId
                            gameState.step = initial.step
                            gameState.format = initial.format
                            gameState.winner = null
                            for ((dst, src) in listOf(
                                gameState.player1 to initial.player1,
                                gameState.player2 to initial.player2
                            )) {
                                dst.name = src.name
                                dst.life = src.life
                                dst.reserve = src.reserve
                                dst.trash = src.trash
                                dst.field.clear()
                                dst.hand = src.hand
                                dst.deck = src.deck
                                dst.deckCount = src.deckCount
                            }
                            gameState.seed = initial.seed
                            gameState.visitedPositions = mutableSetOf(positionHash(gameState))
                            invalidateActionCache()

                            val dto = buildWebGameStateDto(gameState, viewer = requestViewer)
                            responseBody = jsonFormat.encodeToString(dto)
                            contentType = "application/json"

                            broadcastState()
                        }
                        path == "/api/act" -> {
                            val requestedIndex = requestText
                                .substringAfter("\"index\"", "")
                                .substringAfter(":", "")
                                .trim()
                                .takeWhile { it.isDigit() || it == '-' }
                                .toIntOrNull()

                            val actMessages = mutableListOf<String>()
                            // 局面が変わるので、この後の再列挙はキャッシュを使わない
                            invalidateActionCache()
                            when {
                                gameState.winner != null ->
                                    actMessages.add("ℹ️ ゲームは終了しています")
                                // PlayMode では自分が選択者のときしか操作できない
                                requestViewer != VIEWER_GOD && requestViewer != gameState.choosingPlayerId ->
                                    actMessages.add("⚠️ いまはプレイヤー${gameState.choosingPlayerId}の選択中です")
                                requestedIndex == 999 -> {
                                    advanceStep(gameState, actMessages)
                                    gameState.visitedPositions.add(positionHash(gameState))
                                }
                                requestedIndex != null -> {
                                    // 提示した選択肢を index で引き直し、その選択肢どおりの処理を適用する
                                    val action = enumerateActionsForState(gameState).find { it.index == requestedIndex }
                                    if (action == null) {
                                        actMessages.add("⚠️ 選択肢 #$requestedIndex は現在の局面では選べません")
                                    } else {
                                        applyActionAndRecord(gameState, action, actMessages)
                                    }
                                }
                                else -> actMessages.add("⚠️ 不正なリクエストです")
                            }
                            // 局面が進んだので、返答用のDTOは新しい局面で列挙し直す
                            invalidateActionCache()

                            val dto = buildWebGameStateDto(gameState, extraMessages = actMessages, viewer = requestViewer)
                            responseBody = jsonFormat.encodeToString(dto)
                            contentType = "application/json"

                            broadcastState()
                        }
                        path == "/api/undo" || path == "/api/redo" -> {
                            val dto = buildWebGameStateDto(gameState, viewer = requestViewer)
                            responseBody = jsonFormat.encodeToString(dto)
                            contentType = "application/json"
                            broadcastState()
                        }
                        path == "/api/replay" -> {
                            responseBody = """{"steps":[]}"""
                            contentType = "application/json"
                        }
                        path == "/api/status" -> {
                            responseBody = """{"status":"ok","engine":"Playmats (Kotlin/Native)","ui":"BS Kogyo Board","subscribers":${subscribers.size},"gameServerUrl":"$gameServerUrl","gameServerOnline":$gameServerOnline,"evalMode":"${evalMode.name}"}"""
                            contentType = "application/json"
                        }
                        path == "/api/layout" -> {
                            responseBody = jsonFormat.encodeToString(service.generatePlaymatLayout(gameState))
                            contentType = "application/json"
                        }
                        path.startsWith("/api/validate") -> {
                            val testRes = service.validateRule(RuleValidationRequest("SUMMON", "26RSD03-001", 4, 3))
                            responseBody = jsonFormat.encodeToString(testRes)
                            contentType = "application/json"
                        }
                        else -> {
                            statusCode = "404 Not Found"
                            responseBody = "Not Found"
                            contentType = "text/plain"
                        }
                    }

                    if (!keepAlive) {
                        val bodyBytes = responseBody.encodeToByteArray()
                        val header = "HTTP/1.1 $statusCode\r\n" +
                                "Content-Type: $contentType\r\n" +
                                "Content-Length: ${bodyBytes.size}\r\n" +
                                "Connection: close\r\n" +
                                "Access-Control-Allow-Origin: *\r\n\r\n"
                        val headerBytes = header.encodeToByteArray()

                        headerBytes.usePinned { hPinned ->
                            send(cs, hPinned.addressOf(0), headerBytes.size, 0)
                        }
                        bodyBytes.usePinned { bPinned ->
                            send(cs, bPinned.addressOf(0), bodyBytes.size, 0)
                        }
                        closedSockets.add(cs)
                    }
                } else if (bytesRead == 0 || (bytesRead < 0 && !subscribers.containsKey(cs))) {
                    closedSockets.add(cs)
                }
            }

            for (cs in closedSockets) {
                clientSockets.remove(cs)
                subscribers.remove(cs)
                closesocket(cs)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
fun main(args: Array<String>) {
    println("=== Playmats: Game WebUI Layout & Rule API Engine (Kotlin/Native) ===")

    val service = PlaymatService()
    val state = createInitialGameState()

    val layout = service.generatePlaymatLayout(state)
    println("【WebUI Playmat 座標レイアウト生成】")
    println(jsonFormat.encodeToString(layout))

    if (args.contains("--cli-only")) {
        return
    }

    val port = args.indexOf("--port").let { idx ->
        if (idx >= 0 && idx + 1 < args.size) args[idx + 1].toIntOrNull() ?: 8080 else 8080
    }

    val gameUrl = args.indexOf("--game-url").let { idx ->
        if (idx >= 0 && idx + 1 < args.size) args[idx + 1]
        else args.indexOf("--game-server").let { sIdx ->
            if (sIdx >= 0 && sIdx + 1 < args.size) args[sIdx + 1]
            else getenv("GAME_SERVER_URL")?.toKString() ?: "http://localhost:8081"
        }
    }

    // 評価値の算出方式。既定は従来の線形モデル(EvalWeights)。--eval-rnn を付けると
    // RNN(GRU)による局面評価器 (RnnEvaluator.kt) に切り替わる。
    // 注意: ここで作る重みは学習済みではなく乱数初期化のままなので、判断の質は保証しない。
    if (args.contains("--eval-rnn")) {
        val rnnSeed = args.indexOf("--eval-rnn-seed").let { idx ->
            if (idx >= 0 && idx + 1 < args.size) args[idx + 1].toLongOrNull() ?: 42L else 42L
        }
        val weightsPath = args.indexOf("--rnn-weights").let { idx ->
            if (idx >= 0 && idx + 1 < args.size) args[idx + 1] else RNN_WEIGHTS_DEFAULT_PATH
        }
        evalMode = EvalMode.RNN
        val loaded = loadRnnParams(weightsPath)
        if (loaded != null) {
            rnnParams = loaded
            println("=== 評価方式: RNN (GRU) / 学習済み重みを読み込み: $weightsPath (パラメータ数=${RnnParams.paramCount}) ===")
        } else {
            rnnParams = RnnParams.random(kotlin.random.Random(rnnSeed))
            println("=== 評価方式: RNN (GRU) / 乱数シード=$rnnSeed / パラメータ数=${RnnParams.paramCount} (未学習、$weightsPath が見つからないため) ===")
        }
    }

    // KANN(cinterop連携したC言語のNNライブラリ)のGRU価値ネットワーク。--eval-rnn と同様、
    // 実行中に GUI の設定ダイアログから /api/eval-config で切り替えることもできる
    // (GameServer側にも中継される)。--kann-weights-p1/-p2 で player1/2 に別の重みを指定できる。
    if (args.contains("--eval-kann")) {
        fun weightsArg(flag: String): String? = args.indexOf(flag).let { idx ->
            if (idx >= 0 && idx + 1 < args.size) args[idx + 1] else null
        }
        val defaultWeights = weightsArg("--kann-weights")
        val weightsPathP1 = weightsArg("--kann-weights-p1") ?: defaultWeights
        val weightsPathP2 = weightsArg("--kann-weights-p2") ?: defaultWeights
        val result = applyEvalConfig("KANN", weightsPathP1, weightsPathP2)
        if (result == "ok") {
            println("=== 評価方式: KANN (GRU) / player1重み: ${weightsPathP1 ?: "(未学習の新規グラフ)"} / player2重み: ${weightsPathP2 ?: "(未学習の新規グラフ)"} (パラメータ数=${kannNet?.nVar}) ===")
        } else {
            println("=== KANN評価方式の初期化に失敗: $result ===")
        }
    }

    runHttpServer(port, gameUrl)
}
