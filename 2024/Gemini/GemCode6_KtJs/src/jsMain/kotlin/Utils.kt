import okio.FileSystem
import okio.NodeJsFileSystem

actual fun getApiKey() = js("process.env.GOOGLE_API_KEY") as? String ?: throw IllegalArgumentException("No GOOGLE_API_KEY.")
actual val fileSystem: FileSystem = NodeJsFileSystem
