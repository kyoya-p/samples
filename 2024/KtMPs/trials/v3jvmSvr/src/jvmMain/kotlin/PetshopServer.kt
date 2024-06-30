import org.openapitools.client.apis.PetApi
import org.openapitools.client.models.Category
import org.openapitools.client.models.Pet

fun main() = embeddedServer(CIO, port = 8000) {
    routing {
        get ("/") {
            call.respondText("Hello, world!")
        }
    }
}.start(wait = true)
