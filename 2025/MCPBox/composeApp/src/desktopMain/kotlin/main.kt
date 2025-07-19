package jp.wjg.shokkaa.mcp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlin.reflect.KClass

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MCPBox",
//        size = DpSize(width = 800.dp, height = 600.dp)
    ) {
        App()
    }
}

class Logger(val facility: String = "", val log: (msg: String, facility: String) -> Unit) {
    @Composable
    fun <R> guard(
        n: String,
        op: @Composable () -> R
    ) {
        log("start", n)
        runCatching {
            op()
            log("end", n)
        }.onFailure {
            log("failed", n)
        }.getOrThrow()
    }
}

context(logger: Logger)
fun <R> guard(
    n: String,
    op: () -> R
): R {
    logger.log("start", n)
    return runCatching {
        op().apply { logger.log("end", n) }
    }.onFailure {
        logger.log("failed", n)
    }.getOrThrow()
}
