import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.xxfast.kstore.file.storeOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import okio.Path.Companion.toPath
import java.io.File
import kotlin.test.Test

class Tests {
    @Test
    fun storeTest(): Unit = runTest { t1() }
    suspend fun t1() {
        @Serializable
        data class WinSize(val width: Int, val height: Int)

        val store = storeOf<WinSize>(System.getenv("USERPROFILE").toPath().resolve(".data.json"))
        store.set(WinSize(100, 200))
        val size = store.get()!!
        println(System.getenv("USERPROFILE").toPath().resolve(".data.json").toFile().readText())
    }

    @Test
    fun runProcess() {
        ProcessBuilder("cmd.exe", "/c", "cd")
            .inheritIO()
            .start()
            .waitFor()
    }

    @Test
    fun app() = application {
        ProcessBuilder("cmd.exe", "/c", "cd")
//            .redirectOutput(File("aaa.res"))
            .inheritIO()
            .start()
            .waitFor()

        Window(
            title = "Mongo Query",
            state = rememberWindowState(width = 680.dp, height = 800.dp),
            onCloseRequest = ::exitApplication
        ) { App() }

    }
}
