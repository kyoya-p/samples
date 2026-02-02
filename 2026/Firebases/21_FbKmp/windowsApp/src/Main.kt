import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NativeInputProvider : InputProvider {
    override suspend fun nextCommand(): String? = withContext(Dispatchers.Default) {
        // Dispatchers.IO not strictly available in all Native targets the same way, 
        // but readlnOrNull blocks. In KMP Native main thread is often needed for UI.
        // Mosaic runs in runBlocking.
        // For simple TUI, blocking readln might pause UI updates if not careful.
        // But since we are in a coroutine launch in App.kt, it should be fine if dispatcher allows.
        readlnOrNull()
    }
}

fun main() {
    mainApp(NativeInputProvider())
}
