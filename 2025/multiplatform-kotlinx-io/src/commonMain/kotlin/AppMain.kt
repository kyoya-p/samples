import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
//import okio.FileSystem

//expect val fileSystem: FileSystem

fun appMain() = with(SystemFileSystem) {
    val path = Path("./tmp.txt")
    sink(path).buffered().use { sink ->
        sink.writeInt(1)
        sink.writeString("test.")
    }
    source(path).buffered().use { src ->
        println(src.readInt())
        println(src.readString())
    }
}
