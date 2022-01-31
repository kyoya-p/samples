import kotlinx.cinterop.*
import platform.windows.*


fun main() {
    commonMain()
}

actual fun winDialog_SAMPLE() {
    val message = StringBuilder()
    memScoped {
        val buffer = allocArray<UShortVar>(MAX_PATH)
        GetModuleFileNameW(null, buffer, MAX_PATH)
        val path = buffer.toKString().split("\\").dropLast(1).joinToString("\\")
        message.append("PATH $path\n")
    }
    MessageBoxW(
        null,
        "Text\n$message",
        "Dialog", (MB_YESNOCANCEL or MB_ICONQUESTION).convert()
    )
}
