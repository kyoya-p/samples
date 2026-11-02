package chat.client

import chat.model.ChatMessage
import chat.model.ChatService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.rpc.grpc.client.GrpcClient
import kotlinx.rpc.grpc.marshaller.kotlinx.serialization.asMarshallerResolver
import kotlinx.rpc.withService
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * 連続チャット クライアント実装
 * サーバーと双方向ストリーミング接続し、メッセージのリアルタイム送受信を行う。
 */
fun main(args: Array<String>): Unit = runBlocking {
    val isAutoMode = args.contains("--auto")
    val nonFlagArgs = args.filter { !it.startsWith("--") }
    val host = nonFlagArgs.getOrNull(0) ?: "localhost"
    val port = nonFlagArgs.getOrNull(1)?.toIntOrNull() ?: 50055
    val userName = nonFlagArgs.getOrNull(2) ?: "User_${Random.nextInt(100, 999)}"

    println("==================================================")
    println("  gRPC Chat Client (Proto-less / KMP)")
    println("  User: $userName")
    println("  Server: $host:$port")
    println("  Mode: ${if (isAutoMode) "Automated Demo" else "Interactive"}")
    println("==================================================")

    val client = GrpcClient(host, port) {
        messageMarshallerResolver = Json.asMarshallerResolver()
        credentials = plaintext()
    }

    val chatService = client.withService<ChatService>()
    val outgoingFlow = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)

    coroutineScope {
        // 1. サーバーからの連続メッセージを受信するコルーチン
        val receiverJob = launch {
            try {
                chatService.chat(outgoingFlow).collect { message ->
                    val tag = if (message.sender == userName) "YOU" else message.sender
                    println("\n[RECV $tag]: ${message.text}")
                    if (!isAutoMode) {
                        print("[$userName] > ")
                    }
                }
            } catch (e: Exception) {
                println("\n>>> Connection closed: ${e.message}")
            }
        }

        // 参加通知の送信
        outgoingFlow.emit(ChatMessage(sender = userName, text = ">>> Joined the chat room <<<"))

        // 2. メッセージ送信処理
        if (isAutoMode) {
            // 自動モード: 連続でメッセージを送信
            println(">>> Sending automated stream of messages...")
            val sampleMessages = listOf(
                "Hello, everyone!",
                "Testing continuous gRPC streaming.",
                "Flow to Flow bidirectional chat works smoothly.",
                "No code generation or proto files needed!",
                "Goodbye!"
            )
            for (msg in sampleMessages) {
                delay(400)
                println(">>> [AUTO SEND]: $msg")
                outgoingFlow.emit(ChatMessage(sender = userName, text = msg))
            }
            delay(1000)
            println(">>> Auto test completed.")
        } else {
            // 対話型モード: 標準入力から連続入力
            println("Type your message and press Enter (Type 'exit' or 'quit' to leave):")
            withContext(Dispatchers.Default) {
                while (true) {
                    print("[$userName] > ")
                    val line = readlnOrNull() ?: break
                    val trimmed = line.trim()
                    if (trimmed.equals("exit", ignoreCase = true) || trimmed.equals("quit", ignoreCase = true)) {
                        println(">>> Leaving chat...")
                        outgoingFlow.emit(ChatMessage(sender = userName, text = ">>> Left the chat room <<<"))
                        break
                    }
                    if (trimmed.isNotEmpty()) {
                        outgoingFlow.emit(ChatMessage(sender = userName, text = trimmed))
                    }
                }
            }
        }

        receiverJob.cancel()
    }

    client.shutdown()
    println(">>> Chat Client finished.")
}
