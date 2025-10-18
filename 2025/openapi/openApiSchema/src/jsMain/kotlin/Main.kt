import okio.FileSystem
import okio.NodeJsFileSystem

suspend fun main() {
    appMain()
}

actual val fileSystem: FileSystem = NodeJsFileSystem