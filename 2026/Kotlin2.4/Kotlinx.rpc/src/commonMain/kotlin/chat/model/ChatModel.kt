package chat.model

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.grpc.annotations.Grpc
import kotlinx.serialization.Serializable

/**
 * チャットメッセージのデータモデル
 * 動的コード生成 (.proto) を使わず、Kotlin の @Serializable で定義。
 */
@Serializable
data class ChatMessage(
    val sender: String,
    val text: String
)

/**
 * 連続チャット用 gRPC サービスインターフェース
 * 双方向ストリーミング (Bidirectional Streaming RPC) を使用し、
 * クライアントとサーバー間で Flow を通じてメッセージをリアルタイムに相互送信。
 */
@Grpc
interface ChatService {
    fun chat(incoming: Flow<ChatMessage>): Flow<ChatMessage>
}
