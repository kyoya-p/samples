import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds

@Serializable
data class Todo(val userId: Int, val id: Int, val title: String, val completed: Boolean)

val client = HttpClient(CIO) { install(ContentNegotiation) { json() } }
suspend fun appMain() = runCatching{
    println("start:")//TODO
    (1..5).forEach {
        val client = HttpClient(CIO) { install(ContentNegotiation) { json() } }
        val res = client.get("http://127.0.0.1:8080/index.html").bodyAsText()
        println("localhost=")
        println(res)
//        val client = HttpClient(CIO) { install(ContentNegotiation) { json() } }
//        val res = client.get("http://jsonplaceholder.typicode.com/todos/$it").body<Todo>()
//        println(res) //TODO
        delay(500.milliseconds)
    }
}.onFailure { it.printStackTrace() }

