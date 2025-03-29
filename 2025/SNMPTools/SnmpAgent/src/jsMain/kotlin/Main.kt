import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel

fun main() {
    val agent = JsSnmpAgent()
    val scope = CoroutineScope(Dispatchers.Default + Job())

    try {
        // エージェントを起動
        scope.launch {
            agent.start(161)
            
            // テスト用のデータを設定
            agent.setValue("1.3.6.1.2.1.1.1.0", "Test Device")
            agent.setValue("1.3.6.1.2.1.1.2.0", "1.3.6.1.4.1.999")
            
            // エージェントを実行し続ける
            while (true) {
                delay(1000)
            }
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
        scope.cancel()
    }
} 