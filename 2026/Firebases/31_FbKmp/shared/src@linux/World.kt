
import ftxui.*
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
actual fun getWorld(): String {
    hello_ftxui()
    return "Linux World with FTXUI"
}
