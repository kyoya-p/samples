import io.ktor.client.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = HttpClient() {
        install(Auth) {
            basic {
                credentials { BasicAuthCredentials("aaa", "aaa") }
            }
        }
    }
    val res = client.get<String>("http://127.0.0.1:8080/")
    println(res)
}