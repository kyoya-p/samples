import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.*
import kotlinx.serialization.*

@Serializable
data class Customer(val id: Int, val firstName: String, val lastName: String)

val customerStorage = listOf(
    Customer(1, "one", "Shokkaa"),
    Customer(2, "two", "Shokkaa"),
)

fun main() {
    embeddedServer(Netty, port = 8082) {
        serverApp()
    }.start(wait = true)
}

fun Application.serverApp() {
    routing {
        get("/customer") {
            val id = call.parameters["id"]
            println(id)
//            val customer: Customer = customerStorage.find { it.id == id!!.toInt() }!!
            call.respond(customerStorage[1])
        }
        post("/") {
            val req = call.receive<String>()
            println(req)
            call.respond(OK)
        }
    }
}

interface ContentConverter {
    suspend fun serialize(contentType: ContentType, charset: Charset, typeInfo: TypeInfo, value: Any): OutgoingContent?
    suspend fun deserialize(charset: Charset, typeInfo: TypeInfo, content: ByteReadChannel): Any?
}