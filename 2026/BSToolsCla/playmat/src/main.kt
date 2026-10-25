import bstools.model.*
import bstools.valuerl.searchSettingsError
import kotlinx.cinterop.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.posix.*

/**
 * BSToolsCla Playmat: 盤面 Web UI (web/index.html) と、それに状態を配信するサーバー。
 * BSTools の GameServer + Playmats と同じ API・画面で、評価だけを BSToolsCla の GRU 価値ネットワークに替えたもの。
 *
 *   playmat [--port 8090] [--weights f] [--weights-p1 f] [--weights-p2 f] [--actions level|all|compact] [--depth N] [--width k]
 *     --actions: level (既定) = Lv単位の正規形列挙 (学習の既定と同じ) / all = すべての合法手 (終盤は件数が爆発) / compact = 圧縮列挙
 *     --depth: 選択肢の評価に使うミニマックス探索の深さ (1〜4、既定1)。画面の評価設定からも変更できる
 *     --width: 探索の幅 (既定1、0=制限なし)。深さ2以上で、各局面で深く読む手を1手先読みの上位 k 手に絞る
 *
 * 既定ポートは 8090 (BSTools の 8080 / 8082 と同時に起動できるように)。
 */

val jsonFormat = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
val compactJson = Json { prettyPrint = false; ignoreUnknownKeys = true; encodeDefaults = true }

/** ソケットに受信データが来るまで最大 [ms] ミリ秒待つ */
@OptIn(ExperimentalForeignApi::class)
private fun waitReadable(sock: SOCKET, ms: Int): Boolean = memScoped {
    val fds = alloc<fd_set>()
    fds.fd_count = 1u
    fds.fd_array[0] = sock
    val tv = alloc<timeval>()
    tv.tv_sec = ms / 1000
    tv.tv_usec = (ms % 1000) * 1000
    platform.windows.select(0, fds.ptr, null, null, tv.ptr) > 0
}

/**
 * HTTPリクエストをヘッダ終端と Content-Length 分まで読み切る。
 * ノンブロッキングソケットでは、ブラウザがヘッダと本文を別々のパケットで送ると途中で
 * WSAEWOULDBLOCK になるため、そこで打ち切らず残りが届くまで待つ (最大2秒)。
 */
@OptIn(ExperimentalForeignApi::class)
fun readHttpRequest(sock: SOCKET, buffer: CArrayPointer<ByteVar>, bufSize: Int): String? {
    var acc = ByteArray(0)
    var waits = 0
    while (true) {
        val n = recv(sock, buffer, (bufSize - 1).convert(), 0)
        if (n == 0) break // 相手が切断
        if (n < 0) {
            if (WSAGetLastError() != WSAEWOULDBLOCK || waits >= 20) break
            waits++
            waitReadable(sock, 100)
            continue
        }
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

/** 全バイトを送り切る。送信バッファが一杯 (WSAEWOULDBLOCK) なら少し待って続きを送る (最大約5秒) */
@OptIn(ExperimentalForeignApi::class)
fun sendAll(sock: SOCKET, bytes: ByteArray): Boolean {
    var off = 0
    var waits = 0
    bytes.usePinned { p ->
        while (off < bytes.size) {
            val n = send(sock, p.addressOf(off), bytes.size - off, 0)
            if (n > 0) { off += n; continue }
            if (WSAGetLastError() != WSAEWOULDBLOCK || waits >= 500) return false
            waits++
            platform.windows.Sleep(10u)
        }
    }
    return true
}

fun indexOfHeaderEnd(data: ByteArray): Int {
    for (i in 0..data.size - 4) {
        if (data[i] == 13.toByte() && data[i + 1] == 10.toByte() && data[i + 2] == 13.toByte() && data[i + 3] == 10.toByte()) return i
    }
    return -1
}

private fun jsonField(body: String, field: String): String? =
    if (body.contains("\"$field\"")) body.substringAfter("\"$field\"").substringAfter(":").substringAfter("\"").substringBefore("\"").trim() else null

/** 数値のフィールド (引用符あり・なしの両方を受け付ける) */
private fun jsonInt(body: String, field: String): Int? =
    if (body.contains("\"$field\"")) body.substringAfter("\"$field\"").substringAfter(":").trim().removePrefix("\"")
        .takeWhile { it.isDigit() }.toIntOrNull() else null

/** Playmat で指定できる探索の深さの上限 (深さ4で1手数秒〜数十秒かかるため) */
const val MAX_DEPTH = 4

@OptIn(ExperimentalForeignApi::class)
fun runPlaymatServer(port: Int) {
    memScoped {
        val wsaData = alloc<WSADATA>()
        if (WSAStartup(0x0202u, wsaData.ptr) != 0) { println("WSAStartup failed"); return }
        val serverSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)
        if (serverSocket == INVALID_SOCKET) { println("Socket creation failed"); WSACleanup(); return }
        val nonBlocking = alloc<UIntVar>(); nonBlocking.value = 1u
        platform.windows.ioctlsocket(serverSocket, platform.windows.FIONBIO.convert(), nonBlocking.ptr)
        val serverAddr = alloc<sockaddr_in>()
        serverAddr.sin_family = AF_INET.convert()
        serverAddr.sin_port = htons(port.toShort()).toUShort()
        serverAddr.sin_addr.S_un.S_addr = INADDR_ANY
        if (bind(serverSocket, serverAddr.ptr.reinterpret(), sizeOf<sockaddr_in>().convert()) != 0) {
            println("Bind failed on port $port"); closesocket(serverSocket); WSACleanup(); return
        }
        if (listen(serverSocket, SOMAXCONN) != 0) { println("Listen failed"); closesocket(serverSocket); WSACleanup(); return }

        println("=== BSToolsCla Playmat: http://localhost:$port/ ===")
        println("評価: ${PlaymatEval.mode} P1=${PlaymatEval.nameP1 ?: "-"} P2=${PlaymatEval.nameP2 ?: "(P1と共有)"} 候補=${PlaymatEval.actions} 深さ=${PlaymatEval.depth} 幅=${PlaymatEval.width}")

        val sessions = mutableMapOf<String, GameSession>()
        fun session(id: String) = sessions.getOrPut(id) { GameSession(id) }
        // 評価の途中経過 (評価済み数/全選択肢数) を、評価中のセッションを見ているタブへ SSE の progress イベントで送る
        PlaymatEval.progress = { done, total ->
            GameSession.evaluating?.let { s ->
                val bytes = "event: progress\ndata: {\"done\":$done,\"total\":$total,\"player\":${s.state.choosingPlayerId}}\n\n".encodeToByteArray()
                for (sock in s.subscribers.keys) sendAll(sock, bytes)
            }
        }
        val clientSockets = mutableListOf<SOCKET>()
        val bufSize = 65536
        val buffer = allocArray<ByteVar>(bufSize)

        /** そのセッションを見ているタブ (SSE) だけに最新の盤面を送る */
        fun broadcast(s: GameSession) {
            val payloads = mutableMapOf<Int, ByteArray>()
            val dead = mutableListOf<SOCKET>()
            for ((sock, viewer) in s.subscribers) {
                val bytes = payloads.getOrPut(viewer) { "data: ${compactJson.encodeToString(s.dto(viewer))}\n\n".encodeToByteArray() }
                if (!sendAll(sock, bytes)) dead.add(sock)
            }
            for (d in dead) { s.subscribers.remove(d); clientSockets.remove(d); closesocket(d) }
        }
        fun broadcastAll() { for (s in sessions.values) broadcast(s) }
        fun isSubscriber(sock: SOCKET) = sessions.values.any { it.subscribers.containsKey(sock) }

        val readFds = alloc<fd_set>()
        val tv = alloc<timeval>()
        while (true) {
            readFds.fd_count = 0u
            readFds.fd_array[readFds.fd_count.toInt()] = serverSocket
            readFds.fd_count++
            for (cs in clientSockets) {
                if (readFds.fd_count.toInt() < 64) { readFds.fd_array[readFds.fd_count.toInt()] = cs; readFds.fd_count++ }
            }
            tv.tv_sec = 0; tv.tv_usec = 10000
            if (platform.windows.select(0, readFds.ptr, null, null, tv.ptr) <= 0) continue

            fun ready(s: SOCKET): Boolean { for (i in 0 until readFds.fd_count.toInt()) if (readFds.fd_array[i] == s) return true; return false }

            if (ready(serverSocket)) {
                val newSock = accept(serverSocket, null, null)
                if (newSock != INVALID_SOCKET) {
                    val nb = alloc<UIntVar>(); nb.value = 1u
                    platform.windows.ioctlsocket(newSock, platform.windows.FIONBIO.convert(), nb.ptr)
                    clientSockets.add(newSock)
                }
            }

            for (cs in clientSockets.toList()) {
                if (isSubscriber(cs) || !ready(cs)) continue
                val requestText = readHttpRequest(cs, buffer, bufSize)
                if (requestText == null) { clientSockets.remove(cs); closesocket(cs); continue }

                val parts = (requestText.lines().firstOrNull() ?: "").split(" ")
                val method = parts.getOrNull(0) ?: "GET"
                val path = parts.getOrNull(1) ?: "/"
                val body = requestText.substringAfter("\r\n\r\n", "")
                var keepAlive = false
                var responseBody = ""
                var contentType = "application/json"
                var statusCode = "200 OK"
                val viewer = parseViewer(path, requestText)
                val sid = parseSessionId(path, body)

                when {
                    path == "/" || path.startsWith("/?") || path == "/index.html" -> { responseBody = loadHtmlFile(); contentType = "text/html; charset=UTF-8" }
                    path == "/favicon.ico" -> { statusCode = "204 No Content"; contentType = "" }
                    path.startsWith("/api/events") || path.startsWith("/api/subscribe") -> {
                        keepAlive = true
                        val s = session(sid)
                        val header = "HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\nCache-Control: no-cache\r\n" +
                            "Connection: keep-alive\r\nAccess-Control-Allow-Origin: *\r\n\r\n"
                        sendAll(cs, header.encodeToByteArray())
                        sendAll(cs, "data: ${compactJson.encodeToString(s.dto(viewer))}\n\n".encodeToByteArray())
                        s.subscribers[cs] = viewer
                    }
                    path.startsWith("/api/state") -> responseBody = jsonFormat.encodeToString(session(sid).dto(viewer))
                    path == "/api/act" -> {
                        val s = session(sid)
                        val st = s.state
                        val idx = body.substringAfter("\"index\"", "").substringAfter(":", "").trim()
                            .takeWhile { it.isDigit() || it == '-' }.toIntOrNull()
                        val msgs = mutableListOf<String>()
                        when {
                            st.winner != null -> msgs.add("ℹ️ ゲームは終了しています")
                            viewer != VIEWER_GOD && viewer != st.choosingPlayerId -> msgs.add("⚠️ いまはプレイヤー${st.choosingPlayerId}の選択中です")
                            idx == 999 -> {
                                val step = replayStepOf(s, null, PlaymatEval.stepEndValue(st), PlaymatEval.rawActions(st).size + 1)
                                s.beforeChange()
                                advanceStep(st, msgs); st.visitedPositions.add(positionHash(st))
                                s.steps.add(step)
                            }
                            idx != null -> {
                                val listed = PlaymatEval.listActions(st)
                                val a = listed.find { it.index == idx }
                                if (a == null) msgs.add("⚠️ 選択肢 #$idx は現在の局面では選べません")
                                else {
                                    val step = replayStepOf(s, a, a.eval, listed.size + 1)
                                    s.beforeChange()
                                    applyActionAndRecord(st, a, msgs)
                                    s.steps.add(step)
                                }
                            }
                            else -> msgs.add("⚠️ 不正なリクエストです")
                        }
                        s.saveReplayIfFinished()
                        responseBody = jsonFormat.encodeToString(s.dto(viewer, msgs))
                        broadcast(s)
                    }
                    path == "/api/new" || path == "/api/restart" -> {
                        val s = session(sid)
                        val restart = path == "/api/restart"
                        val d1 = if (restart) s.deck1 else (jsonField(body, "deck1") ?: "").ifBlank { "deck-kogyo.yaml" }
                        val d2 = if (restart) s.deck2 else (jsonField(body, "deck2") ?: "").ifBlank { "deck-kogyo.yaml" }
                        val seed = if (restart) s.state.seed else {
                            body.substringAfter("\"seed\"", "").substringAfter(":", "").trim().removePrefix("\"")
                                .takeWhile { it.isDigit() || it == '-' }.toLongOrNull() ?: kotlin.random.Random.nextLong(1, 1_000_000)
                        }
                        val format = if (restart) s.state.format else GameFormat.fromString(jsonField(body, "format") ?: "STANDARD")
                        s.reset(d1, d2, seed, format)
                        responseBody = jsonFormat.encodeToString(s.dto(viewer))
                        broadcast(s)
                    }
                    path == "/api/undo" || path == "/api/redo" -> {
                        val s = session(sid)
                        val ok = if (path == "/api/undo") s.undo() else s.redo()
                        val msg = if (ok) (if (path == "/api/undo") "◀ 1手戻しました" else "進む ▶ 1手進めました")
                                  else (if (path == "/api/undo") "これ以上戻れません" else "これ以上進めません")
                        responseBody = jsonFormat.encodeToString(s.dto(viewer, listOf(msg)))
                        broadcast(s)
                    }
                    path == "/api/replay" -> {
                        val s = session(sid)
                        responseBody = jsonFormat.encodeToString(ReplayFileDto(s.id, s.deck1, s.deck2, s.state.seed, s.state.winner,
                            PlaymatEval.nameP1, PlaymatEval.nameP2 ?: PlaymatEval.nameP1, s.steps))
                    }
                    path == "/api/status" -> responseBody = """{"status":"ok","service":"BSToolsCla Playmat","sessions":${sessions.size},"subscribers":${sessions.values.sumOf { it.subscribers.size }},"evalMode":"${PlaymatEval.mode}"}"""
                    path == "/api/weight-files" -> responseBody = compactJson.encodeToString(PlaymatEval.listWeightFiles())
                    path == "/api/training-status" -> responseBody = """{"active":false}"""
                    path == "/api/config" -> responseBody = """{"status":"ok"}"""
                    path == "/api/eval-config" -> {
                        if (method == "POST" && body.isNotBlank()) {
                            val mode = (jsonField(body, "mode") ?: "").ifBlank { "GRU" }
                            val newDepth = (jsonInt(body, "depth") ?: PlaymatEval.depth).coerceIn(1, MAX_DEPTH)
                            val newWidth = maxOf(0, jsonInt(body, "width") ?: PlaymatEval.width)
                            // 幅1で深さ1以外は選ぶ手が変わらず無意味なので、設定を受け付けずにエラーを返す
                            val searchError = searchSettingsError(newDepth, newWidth)
                            if (searchError == null) { PlaymatEval.depth = newDepth; PlaymatEval.width = newWidth }
                            val result = searchError ?: PlaymatEval.configure(mode, jsonField(body, "weightsPathP1"), jsonField(body, "weightsPathP2"))
                            if (result != "ok") statusCode = "400 Bad Request"
                            responseBody = """{"status":"${if (result == "ok") "ok" else "error"}","message":"$result","evalMode":"${PlaymatEval.mode}","depth":${PlaymatEval.depth},"width":${PlaymatEval.width},"gameServerReached":true}"""
                            broadcastAll() // 評価器は全セッション共通
                        } else responseBody = """{"evalMode":"${PlaymatEval.mode}","depth":${PlaymatEval.depth},"width":${PlaymatEval.width},"maxDepth":$MAX_DEPTH}"""
                    }
                    path == "/api/actions" -> responseBody = compactJson.encodeToString(PlaymatEval.listActions(session(sid).state))
                    path == "/api/formats" -> responseBody = """["STANDARD","ETERNAL"]"""
                    else -> { statusCode = "404 Not Found"; responseBody = """{"error":"Not Found"}""" }
                }

                if (!keepAlive) {
                    val bodyBytes = responseBody.encodeToByteArray()
                    val header = "HTTP/1.1 $statusCode\r\nContent-Type: $contentType\r\nContent-Length: ${bodyBytes.size}\r\n" +
                        "Connection: close\r\nAccess-Control-Allow-Origin: *\r\n\r\n"
                    sendAll(cs, header.encodeToByteArray())
                    if (bodyBytes.isNotEmpty()) sendAll(cs, bodyBytes)
                    clientSockets.remove(cs)
                    closesocket(cs)
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    fun arg(flag: String): String? = args.indexOf(flag).let { if (it >= 0 && it + 1 < args.size) args[it + 1] else null }
    PlaymatEval.actions = when (arg("--actions")) { "all" -> "all"; "compact" -> "compact"; else -> "level" }
    PlaymatEval.depth = (arg("--depth")?.toIntOrNull() ?: 1).coerceIn(1, MAX_DEPTH)
    PlaymatEval.width = maxOf(0, arg("--width")?.toIntOrNull() ?: 1)
    searchSettingsError(PlaymatEval.depth, PlaymatEval.width)?.let { println("エラー: $it"); exit(2) }
    val both = arg("--weights")
    val p1 = arg("--weights-p1") ?: both
    val p2 = arg("--weights-p2") ?: both
    val r = PlaymatEval.configure("GRU", p1, p2)
    if (r != "ok") println("⚠️ $r (評価なしで起動します。画面の設定から重みを指定できます)")
    runPlaymatServer(arg("--port")?.toIntOrNull() ?: 8090)
}
