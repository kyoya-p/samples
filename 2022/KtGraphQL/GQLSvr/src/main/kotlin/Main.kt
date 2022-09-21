import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        json()
    }

    routing {
        suspend fun PipelineContext<Unit, ApplicationCall>.gqlFunc() {
            val stringRequest = call.receive<String>()
            val mapper = com.fasterxml.jackson.databind.ObjectMapper()
            val request = mapper.readValue(stringRequest, GraphQLRequest::class.java)
            //TODO
        }
        get("/graphql") { gqlFunc()
        }
        post("/graphql") { gqlFunc() }
    }
}

data class GraphQLRequest(
    val query: String = "",
    val operationName: String? = "",
    val variables: Map<String, Any>? = mapOf()
)
