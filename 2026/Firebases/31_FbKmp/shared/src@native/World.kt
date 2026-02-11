
import ftxui.*
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
actual fun getWorld(): String = "Native World with FTXUI"

@OptIn(ExperimentalForeignApi::class)
actual fun startFtxuiLoop(renderer: () -> String) {
    println("Native: Entering FTXUI Loop via screen.Loop(renderer)")
    // シミュレーション: Kotlinのレンダラーを3回呼ぶ
    repeat(3) { i ->
        println("Frame $i: ${renderer()}")
    }
}
