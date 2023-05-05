import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

var testData = mutableListOf(
    Customer(1, "John", "Doe"),
    Customer(2, "Jane", "Smith"),
    Customer(3, "Bob", "Brown"),
    Customer(4, "Alice", "Green"),
    Customer(5, "Tom", "Jones")
)

fun Application.sampleModule() {
    install(ContentNegotiation) { json() }
    routing {
        get("/") {
            val sId = call.request.queryParameters["id"] ?: return@get call.respond(testData)
            val id = sId.toIntOrNull() ?: return@get call.respond(listOf<Customer>())
            call.respond(testData.filter { it.id == id })
        }
        get("/{id}") {
            val sId = call.parameters["id"] ?: return@get call.respond(testData)
            val id = sId.toIntOrNull() ?: return@get call.respond(listOf<Customer>())
            call.respond(testData.filter { it.id == id })
        }
        post("/") {
            val customer: Customer = call.receive()
            testData.add(customer)
            call.respond(Created)
        }
    }
}

fun main() {
    embeddedServer(CIO, port = 8080) {
        sampleModule()
    }.start(wait = true)
}
