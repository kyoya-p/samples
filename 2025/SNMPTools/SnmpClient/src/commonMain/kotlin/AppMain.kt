import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath

expect val fileSystem: FileSystem

@OptIn(DelicateCoroutinesApi::class)
suspend fun appMain() {

    val path = ".".toPath() / "test.txt"
    fileSystem.write(path) { writeUtf8("test") }
    val f = flow {
        (1..5).forEach {
            emit(it)
            delay(1000)
        }
    }
    GlobalScope.launch { f.collect { println(it) } }
    f.collect { println(it) }
    println(fileSystem.read(path) { readUtf8() })
}



