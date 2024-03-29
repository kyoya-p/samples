import kotlinx.cinterop.*
import platform.posix.WSADATA
import platform.windows.*

// https://github-com.translate.goog/JetBrains/kotlin/blob/master/kotlin-native/samples/win32/src/win32Main/kotlin/MessageBox.kt?_x_tr_sl=en&_x_tr_tl=ja&_x_tr_hl=ja&_x_tr_pto=op,sc

fun main() = commonMain()

fun messageBox() {
    val message = StringBuilder()
    memScoped {
        val buffer = allocArray<UShortVar>(MAX_PATH)
        GetModuleFileNameW(null, buffer, MAX_PATH)
        val path = buffer.toKString().split("\\").dropLast(1).joinToString("\\")
        message.append("ファイルパス $path\n")
    }
    MessageBoxW(
        null, "ウィンドウズAPIとKotlin/Nativeのテストです\n$message",
        "ウィンドウズAPIとKotlin/Nativeのテストです", (MB_YESNOCANCEL or MB_ICONQUESTION).convert()
    )
}

fun ip() {
    var wsaData: WSADATA? = null
    var iResult = WSAStartup(0x0202.toUShort(), wsaData?.ptr)
    if (iResult != 0) {
        println("WSAStartup failed: $iResult")
    }
    //TODO
}