import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

@Serializable
data class Todo(
    val userId: Int,
    val id: Int,
    val title: String,
    val completed: Boolean
)

suspend fun appMain() {
    val res = HttpClient(CIO).get("http://jsonplaceholder.typicode.com/todos/1").body<Todo>()
    println(res)

}

