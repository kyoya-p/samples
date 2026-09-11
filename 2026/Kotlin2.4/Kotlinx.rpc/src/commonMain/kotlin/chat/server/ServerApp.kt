package chat.server

import chat.model.ChatMessage
import chat.model.ChatService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.grpc.marshaller.kotlinx.serialization.asMarshallerResolver
import kotlinx.rpc.grpc.server.GrpcServer
import kotlinx.rpc.registerService
import kotlinx.serialization.json.Json

/**
 * 連続チャット サーバー実装
 * 全接続クライアント間でメッセージをブロードキャストする。
 */
class ChatServiceImpl : ChatService {
    // 全クライアント共有のブロードキャストフロー
    private val broadcastFlow = MutableSharedFlow<ChatMessage>(
        extraBufferCapacity = 128
    )
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun chat(incoming: Flow<ChatMessage>): Flow<ChatMessage> {
        // クライアントからのメッセージを受信してブロードキャストへ流す
        scope.launch {
            try {
                incoming.collect { message ->
                    println("[SERVER] [${message.sender}]: ${message.text}")
                    broadcastFlow.emit(message)
                }
            } catch (e: Exception) {
                println("[SERVER] Client stream ended: ${e.message}")
            }
        }

        // ブロードキャストフローをそのままクライアントへの送出ストリームとして返却
        return broadcastFlow.asSharedFlow()
    }
}

fun main(args: Array<String>): Unit = runBlocking {
    val port = args.firstOrNull()?.toIntOrNull() ?: 50055
    val jsonResolver = Json.asMarshallerResolver()

    println("==================================================")
    println("  gRPC Chat Server (Proto-less / KMP)")
    println("  Listening on port: $port")
    println("==================================================")

    val server = GrpcServer(port) {
        messageMarshallerResolver = jsonResolver
        services {
            registerService<ChatService> { ChatServiceImpl() }
        }
    }

    server.start()
    println(">>> Server started. Waiting for chat clients...")
    server.awaitTermination()
}
