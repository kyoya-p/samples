import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath

expect val fileSystem: FileSystem

// {"contents":[{"parts":[{"text":"説明: AIはどのように動くのか?"}]}]
@Serializable
data class Contents(val contents: List<Parts>)

@Serializable
data class Parts(val parts: List<Text>)

@Serializable
data class Text(val text: String)

val request = Contents(listOf(Parts(listOf(Text("説明: AIはどのように動くのか?")))))

@OptIn(DelicateCoroutinesApi::class)
suspend fun appMain() {
//    val path = ".".toPath() / "test.txt"
//    fileSystem.write(path) { writeUtf8("test") }
//    val f = flow {
//        (1..5).forEach {
//            emit(it)
//            delay(1000)
//        }
//    }
//    GlobalScope.launch { f.collect { println(it) } }
//    f.collect { println(it) }
//    println(fileSystem.read(path) { readUtf8() })
    ktorTest()
}


suspend fun ktorTest() {
    val r = HttpClient(CIO).get("http://jsonplaceholder.typicode.com/todos/1").bodyAsText()
    println(r)
}

