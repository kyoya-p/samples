import kotlinx.coroutines.runBlocking
import okio.FileSystem

fun main() = runBlocking {
    appMain()
}

actual val fileSystem = FileSystem.SYSTEM
