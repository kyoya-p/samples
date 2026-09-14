import bstools.model.*
import kotlinx.cinterop.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

/**
 * HTTPリクエストを Content-Length 分まで読み切る。
 * POST された GameState JSON は1回の recv に収まらないことがあるため、ヘッダを見て残りを読み続ける。
 */
@OptIn(ExperimentalForeignApi::class)
fun readHttpRequest(sock: platform.posix.SOCKET, buffer: CArrayPointer<ByteVar>, bufSize: Int): String? {
    var acc = ByteArray(0)
    while (true) {
        val n = recv(sock, buffer, (bufSize - 1).convert(), 0)
        if (n <= 0) break
        acc += ByteArray(n) { buffer[it] }

        val headerEnd = indexOfHeaderEnd(acc)
        if (headerEnd < 0) continue

        val header = acc.copyOfRange(0, headerEnd).decodeToString()
        val contentLength = header.lines()
            .firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
            ?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
        if (acc.size - (headerEnd + 4) >= contentLength) break
    }
    return if (acc.isEmpty()) null else acc.decodeToString()
}

/** ヘッダ終端 (CRLF CRLF) のバイト位置。Content-Length はバイト数なので文字列ではなくバイト列で探す */
fun indexOfHeaderEnd(data: ByteArray): Int {
    for (i in 0..data.size - 4) {
        if (data[i] == 13.toByte() && data[i + 1] == 10.toByte() &&
            data[i + 2] == 13.toByte() && data[i + 3] == 10.toByte()
        ) return i
    }
    return -1
}

@OptIn(ExperimentalForeignApi::class)
fun runGameServerHttp(port: Int = 8081) {
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
        println("=== GameServer started at http://localhost:$port/ ===")
        println("=================================================================")

        // デモ局面。Playmats と同じ共有初期化ロジックを使う (以前はここだけ別の簡易初期化だった)
        val state = createInitialGameState()
        val bufSize = 65536
        val buffer = allocArray<ByteVar>(bufSize)

        while (true) {
            val clientSocket = accept(serverSocket, null, null)
            if (clientSocket == platform.posix.INVALID_SOCKET) continue

            val requestText = readHttpRequest(clientSocket, buffer, bufSize)
            if (requestText != null) {
                val firstLine = requestText.lines().firstOrNull() ?: ""
                val parts = firstLine.split(" ")
                val method = parts.getOrNull(0) ?: "GET"
                val path = parts.getOrNull(1) ?: "/"
                val body = requestText.substringAfter("\r\n\r\n", "")

                val responseBody: String
                val contentType: String
                var statusCode = "200 OK"

                when {
                    path == "/" || path == "/api/status" -> {
                        responseBody = """{"status":"ok","service":"GameServer (Kotlin/Native)","turn":${state.turn},"step":"${state.step.displayName}","evalMode":"${evalMode.name}"}"""
                        contentType = "application/json"
                    }
                    path == "/api/eval-config" -> {
                        if (method == "POST" && body.isNotBlank()) {
                            val mode = body.substringAfter("\"mode\"", "").substringAfter(":", "")
                                .trim().removePrefix("\"").substringBefore("\"").trim()
                            fun extract(field: String): String? = if (body.contains("\"$field\"")) {
                                body.substringAfter("\"$field\"").substringAfter(":")
                                    .substringAfter("\"").substringBefore("\"").trim()
                            } else null
                            val weightsPathP1 = extract("weightsPathP1")
                            val weightsPathP2 = extract("weightsPathP2")
                            val result = if (mode.isBlank()) "modeが指定されていません" else applyEvalConfig(mode, weightsPathP1, weightsPathP2)
                            if (result != "ok") statusCode = "400 Bad Request"
                            responseBody = """{"status":"${if (result == "ok") "ok" else "error"}","message":"$result","evalMode":"${evalMode.name}"}"""
                        } else {
                            responseBody = """{"evalMode":"${evalMode.name}"}"""
                        }
                        contentType = "application/json"
                    }
                    path == "/api/actions" -> {
                        // POST で GameState を受け取ればその局面を、GET なら自前のデモ局面を列挙する
                        val target = if (method == "POST" && body.isNotBlank()) {
                            try {
                                laxJson.decodeFromString<GameState>(body)
                            } catch (e: Exception) {
                                null
                            }
                        } else state

                        if (target == null) {
                            statusCode = "400 Bad Request"
                            responseBody = """{"error":"invalid GameState payload"}"""
                        } else {
                            responseBody = compactJson.encodeToString(enumerateActions(target))
                        }
                        contentType = "application/json"
                    }
                    else -> {
                        statusCode = "404 Not Found"
                        responseBody = """{"error":"Not Found"}"""
                        contentType = "application/json"
                    }
                }

                val bodyBytes = responseBody.encodeToByteArray()
                val header = "HTTP/1.1 $statusCode\r\n" +
                        "Content-Type: $contentType\r\n" +
                        "Content-Length: ${bodyBytes.size}\r\n" +
                        "Connection: close\r\n" +
                        "Access-Control-Allow-Origin: *\r\n\r\n"
                val headerBytes = header.encodeToByteArray()

                headerBytes.usePinned { hPinned ->
                    send(clientSocket, hPinned.addressOf(0), headerBytes.size, 0)
                }
                bodyBytes.usePinned { bPinned ->
                    send(clientSocket, bPinned.addressOf(0), bodyBytes.size, 0)
                }
            }
            closesocket(clientSocket)
        }
    }
}

fun main(args: Array<String>) {
    println("=== GameServer: バトスピ局面選択肢列挙エンジン (Kotlin/Native) ===")
    
    val state = createInitialGameState()

    if (args.contains("--tune")) {
        runWeightTuning(args)
        return
    }

    if (args.contains("--rnn-debug")) {
        debugRnnGame(seed = 1L)
        return
    }

    if (args.contains("--kann-debug")) {
        println(kannSmokeTest())
        return
    }

    if (args.contains("--kann-train")) {
        runKannRandomTraining(args)
        return
    }

    if (args.contains("--kann-es-train")) {
        runKannEsTraining(args)
        return
    }

    if (args.contains("--kann-match")) {
        runKannMatch(args)
        return
    }

    if (args.contains("--kann-rnn-debug")) {
        val net = KannValueNetwork()
        val v1 = evaluateStateWithKann(net, state, 1)
        val v2 = evaluateStateWithKann(net, state, 2)
        println("KANN GRU value network (未学習): V(state, player1)=$v1 V(state, player2)=$v2")
        net.delete()
        return
    }

    if (args.contains("--kann-inspect")) {
        val path = args.indexOf("--kann-inspect").let { idx ->
            if (idx >= 0 && idx + 1 < args.size) args[idx + 1] else null
        }
        if (path == null) {
            println("使い方: --kann-inspect <重みファイルパス>")
            return
        }
        val net = KannValueNetwork.load(path)
        if (net == null) {
            println("重みファイルを読み込めません: $path")
            return
        }
        val w = net.getWeights()
        val mean = w.average()
        val variance = w.sumOf { (it - mean) * (it - mean) } / w.size
        println("=== $path ===")
        println("パラメータ数: ${w.size}")
        println("最小値: ${w.min()}  最大値: ${w.max()}")
        println("平均: $mean  標準偏差: ${kotlin.math.sqrt(variance)}")
        println("先頭16件: ${w.take(16)}")
        net.delete()
        return
    }

    if (args.contains("--kann-eval-spread")) {
        val net = KannValueNetwork()
        val actions = enumerateActions(state)
        val candidates = actions.filterNot { it.forbidden }
        println("初期局面: 選択肢${candidates.size}件、各アクション後の評価値V(state')を表示")
        val endStep = GameAction(index = 999, type = "END_STEP", category = "", detail = "", eval = 0.0)
        for (a in (candidates + endStep)) {
            val trial = state.snapshot()
            val messages = mutableListOf<String>()
            if (a.type == "END_STEP") advanceStep(trial, messages) else applyAction(trial, a, messages)
            val tp = if (state.choosingPlayerId == 1) trial.player1 else trial.player2
            val topp = if (state.choosingPlayerId == 1) trial.player2 else trial.player1
            val v = net.evaluate(buildTokenSequence(tp, topp), buildGlobalFeatures(trial, tp, topp))
            println("  [${a.index}] ${a.type}/${a.category}: ${a.detail} -> V=$v")
        }
        net.delete()
        return
    }

    // 評価値の算出方式。Playmats が選択肢の列挙をこのプロセスへ委譲するため、
    // 両者で evalMode を揃えないと Playmats 側の --eval-rnn が効かない
    // (evalMode/rnnParams はプロセスごとのグローバル変数のため)。
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

    // KANN(cinterop連携したC言語のNNライブラリ)のGRU価値ネットワークを評価に使う。
    // --eval-rnn と同様、実行中に GUI(Playmats) の設定ダイアログから /api/eval-config で
    // 切り替えることもできる。--kann-weights-p1/-p2 で player1/2 に別の重みを指定でき、
    // 片方だけの指定なら --kann-weights がその既定値になる。
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

    if (args.contains("--json") || args.contains("--cli-only")) {
        val actions = enumerateActions(state)
        println(jsonFormat.encodeToString(actions))
        if (args.contains("--cli-only")) {
            println("【列挙された選択肢 (${actions.size}件)】")
            for (action in actions) {
                println("  [${action.index}] ${action.category}: ${action.detail} (eval=${action.eval})")
            }
        }
        return
    }

    val port = args.indexOf("--port").let { idx ->
        if (idx >= 0 && idx + 1 < args.size) args[idx + 1].toIntOrNull() ?: 8081 else 8081
    }

    runGameServerHttp(port)
}
