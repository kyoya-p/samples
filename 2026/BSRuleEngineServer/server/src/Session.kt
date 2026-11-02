import bstools.model.*
import kotlinx.cinterop.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import platform.posix.*

/**
 * 盤面保存用データ構造
 */
@Serializable
data class SavedBoardDto(
    val version: Int = 1,
    val session: String = "default",
    val deck1: String = "deck-kogyo.yaml",
    val deck2: String = "deck-kogyo.yaml",
    val savedAt: String = "",
    val state: GameState
)

/**
 * ブラウザのタブごとのゲーム。タブはそれぞれ固有のセッションIDを持ち、別々の盤面で遊べる
 * (同じIDを URL の ?session= で指定すれば、複数タブで1つのゲームを共有・観戦できる)。
 * 戻る/進む用の局面履歴と、選択ログ (ゲーム終了時に temp/ へ保存) もセッションごとに持つ。
 */
class GameSession(val id: String) {
    var state: GameState = newGame("deck-kogyo.yaml", "deck-kogyo.yaml", kotlin.random.Random.nextLong(1, 1_000_000), GameFormat.STANDARD)
    var deck1 = "deck-kogyo.yaml"
    var deck2 = "deck-kogyo.yaml"
    private val undoStack = ArrayDeque<Pair<GameState, Int>>() // 局面と、その時点の選択ログ件数
    private val redoStack = ArrayDeque<Pair<GameState, List<ReplayStepDto>>>()
    val steps = mutableListOf<ReplayStepDto>()
    var replayFile: String? = null
    /** SSE 購読中のソケット → 視点 */
    val subscribers = mutableMapOf<SOCKET, Int>()

    val canUndo get() = undoStack.isNotEmpty()
    val canRedo get() = redoStack.isNotEmpty()

    fun reset(d1: String, d2: String, seed: Long, format: GameFormat) {
        deck1 = d1; deck2 = d2
        state = newGame(d1, d2, seed, format)
        undoStack.clear(); redoStack.clear(); steps.clear(); replayFile = null
    }

    /** 手を指す直前に呼ぶ: 戻る用に局面を保存し、進む用の履歴は捨てる */
    fun beforeChange() {
        undoStack.addLast(state.deepCopy() to steps.size)
        if (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(): Boolean {
        val (prev, stepCount) = undoStack.removeLastOrNull() ?: return false
        redoStack.addLast(state to steps.drop(stepCount))
        while (steps.size > stepCount) steps.removeAt(steps.size - 1)
        state = prev
        replayFile = null
        return true
    }

    fun redo(): Boolean {
        val (next, redoneSteps) = redoStack.removeLastOrNull() ?: return false
        undoStack.addLast(state to steps.size)
        state = next
        steps.addAll(redoneSteps)
        return true
    }

    fun dto(viewer: Int, messages: List<String> = emptyList()): WebGameStateDto {
        evaluating = this
        try {
            return buildWebGameStateDto(
                state, session = id, extraMessages = messages, viewer = viewer, canUndo = canUndo, canRedo = canRedo,
                replaySteps = steps.takeLast(100), replayFile = replayFile
            )
        } finally {
            evaluating = null
        }
    }

    /**
     * ゲームが終わったら選択ログを temp/<デッキ1>-<デッキ2>.YYMMDD-HHMMSS.<セッション>.json に保存する (1回だけ)。
     * 秒とセッションIDを含めるので、同じ分に終わった別のゲームのログを上書きしない。
     */
    @OptIn(ExperimentalForeignApi::class)
    fun saveReplayIfFinished() {
        if (state.winner == null || replayFile != null) return
        mkdir("temp")
        val name = "temp/${deck1.substringBeforeLast('.')}-${deck2.substringBeforeLast('.')}.${timestamp()}.$id.json"
        val body = jsonFormat.encodeToString(ReplayFileDto(id, deck1, deck2, state.seed, state.winner,
            PlaymatEval.nameP1, PlaymatEval.nameP2 ?: PlaymatEval.nameP1, steps))
        val f = fopen(name, "wb") ?: return
        val bytes = body.encodeToByteArray()
        bytes.usePinned { fwrite(it.addressOf(0), 1u, bytes.size.convert(), f) }
        fclose(f)
        replayFile = "./$name"
    }

    /**
     * 現在の盤面 (GameState) を temp/board-<deck1>-<deck2>.<timestamp>.<id>.json に保存する。
     * @return 保存したファイルパスと JSON 文字列
     */
    @OptIn(ExperimentalForeignApi::class)
    fun saveBoardToFile(customFilename: String? = null): Pair<String, String> {
        mkdir("temp")
        val filename = if (!customFilename.isNullOrBlank()) {
            val safe = customFilename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            if (safe.endsWith(".json")) safe else "$safe.json"
        } else {
            "board-${deck1.substringBeforeLast('.')}-${deck2.substringBeforeLast('.')}.${timestamp()}.$id.json"
        }
        val path = "temp/$filename"
        val dto = SavedBoardDto(
            version = 1,
            session = id,
            deck1 = deck1,
            deck2 = deck2,
            savedAt = timestamp(),
            state = state
        )
        val json = jsonFormat.encodeToString(dto)
        val f = fopen(path, "wb")
        if (f != null) {
            val bytes = json.encodeToByteArray()
            bytes.usePinned { fwrite(it.addressOf(0), 1u, bytes.size.convert(), f) }
            fclose(f)
        }
        return Pair(path, json)
    }

    /**
     * JSON文字列から GameState を復元する。
     * SavedBoardDto または GameState 直接のどちらの形式にも対応する。
     */
    fun loadBoardFromJson(jsonStr: String): Boolean {
        val loadedState = try {
            val dto = laxJson.decodeFromString<SavedBoardDto>(jsonStr)
            dto.deck1?.let { if (it.isNotBlank()) deck1 = it }
            dto.deck2?.let { if (it.isNotBlank()) deck2 = it }
            dto.state
        } catch (_: Exception) {
            try {
                laxJson.decodeFromString<GameState>(jsonStr)
            } catch (e: Exception) {
                println("⚠️ JSONパース失敗: ${e.message}")
                return false
            }
        }

        beforeChange()
        state = loadedState
        state.visitedPositions.add(positionHash(state))
        replayFile = null
        return true
    }

    /**
     * ファイルパスから盤面を読み込む。
     */
    @OptIn(ExperimentalForeignApi::class)
    fun loadBoardFromFile(path: String): Boolean {
        val safePath = if (path.startsWith("temp/") || path.startsWith("temp\\")) path else "temp/$path"
        val f = fopen(safePath, "rb") ?: return false
        fseek(f, 0, SEEK_END)
        val size = ftell(f).toLong()
        fseek(f, 0, SEEK_SET)
        if (size <= 0) { fclose(f); return false }
        val bytes = ByteArray(size.toInt())
        val read = bytes.usePinned { p -> fread(p.addressOf(0), 1u, size.toULong().convert(), f) }
        fclose(f)
        if (read.toInt() <= 0) return false
        val content = bytes.copyOfRange(0, read.toInt()).decodeToString()
        return loadBoardFromJson(content)
    }

    companion object {
        const val MAX_HISTORY = 500

        /** いま盤面 (と選択肢の評価) を作っているセッション。評価の途中経過の送り先に使う */
        var evaluating: GameSession? = null

        fun newGame(d1: String, d2: String, seed: Long, format: GameFormat): GameState =
            createInitialGameState(d1, d2, seed, format).also { it.visitedPositions.add(positionHash(it)) }

        @OptIn(ExperimentalForeignApi::class)
        fun timestamp(): String = memScoped {
            val t = alloc<time_tVar>()
            t.value = time(null)
            val tm = localtime(t.ptr)?.pointed ?: return@memScoped "000000-000000"
            fun two(n: Int) = n.toString().padStart(2, '0')
            "${two(tm.tm_year % 100)}${two(tm.tm_mon + 1)}${two(tm.tm_mday)}-${two(tm.tm_hour)}${two(tm.tm_min)}${two(tm.tm_sec)}"
        }
    }
}

/** 保存された盤面ファイル一覧を取得する */
@OptIn(ExperimentalForeignApi::class)
fun listSavedBoardFiles(): List<String> = memScoped {
    val dir = opendir("temp") ?: return emptyList()
    val list = mutableListOf<String>()
    try {
        while (true) {
            val e = readdir(dir) ?: break
            val name = e.pointed.d_name.toKString()
            if (name.startsWith("board-") && name.endsWith(".json")) {
                list.add(name)
            }
        }
    } finally {
        closedir(dir)
    }
    list.sortedDescending()
}

/** 戻る/進む用の完全なコピー (到達局面の記録も複製する。snapshot は手の試行用で共有している) */
fun GameState.deepCopy(): GameState = snapshot().also { it.visitedPositions = visitedPositions.toMutableSet() }

/** 選択ログ1件を作る (手を指す直前の局面で呼ぶ) */
fun replayStepOf(s: GameSession, action: GameAction?, eval: Double?, available: Int): ReplayStepDto {
    val st = s.state
    val p1 = st.player1; val p2 = st.player2
    return ReplayStepDto(
        step_index = s.steps.size + 1,
        turn = st.turn,
        active_player = st.choosingPlayerId,
        phase = st.step.displayName,
        chosen_action_category = action?.category ?: "ステップ終了",
        chosen_action_kind = action?.type ?: "END_STEP",
        chosen_action_detail = action?.detail ?: "ステップ終了",
        chosen_action_eval = eval,
        available_action_count = available,
        state_snapshot = "P1 ライフ${p1.life} R${p1.reserve.format()} 手札${p1.hand.size} 場${p1.field.size} / " +
            "P2 ライフ${p2.life} R${p2.reserve.format()} 手札${p2.hand.size} 場${p2.field.size}"
    )
}

private val QUERY_ROOM = Regex("[?&]room=([A-Za-z0-9_-]{1,64})")
private val QUERY_SESSION = Regex("[?&]session=([A-Za-z0-9_-]{1,64})")
private val FIELD_ROOM = Regex("\"room\"\\s*:\\s*\"([A-Za-z0-9_-]{1,64})\"")
private val FIELD_SESSION = Regex("\"session\"\\s*:\\s*\"([A-Za-z0-9_-]{1,64})\"")

/** リクエストのルーム/セッションID (クエリ ?room= / ?session= か本文の "room" / "session")。不正・未指定なら "default" */
fun parseSessionId(path: String, body: String): String {
    val q = QUERY_ROOM.find(path)?.groupValues?.get(1)
        ?: QUERY_SESSION.find(path)?.groupValues?.get(1)
    if (!q.isNullOrEmpty()) return q
    val b = FIELD_ROOM.find(body)?.groupValues?.get(1)
        ?: FIELD_SESSION.find(body)?.groupValues?.get(1)
    if (!b.isNullOrEmpty()) return b
    return "default"
}
