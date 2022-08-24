import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    val url = args.getOrNull(0) ?: "http://127.0.0.1:8080/"
    val userId = args.getOrNull(1) ?: "userName"
    val password = args.getOrNull(2) ?: "secret"

    val client = HttpClient{
//        install(Auth) {
//            basic {
//                credentials { BasicAuthCredentials(userId, password) }
//            }
//        }
    }.use{client->
        val res = client.get(url).bodyAsText()
        println(res)
    }
}