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
