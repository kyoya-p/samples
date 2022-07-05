import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    val client = HttpClient(Apache)

    val r1 = client.get<String>("http://localhost:8080")
    println(r1)

    client.close()
}