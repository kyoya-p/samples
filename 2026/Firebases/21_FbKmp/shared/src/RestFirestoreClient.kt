import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

class RestFirestoreClient(
    private val apiKey: String,
    private val projectId: String
) : FirestoreClient {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                encodeDefaults = true
            })
        }
    }

    private var idToken: String? = null

    override suspend fun signIn(email: String, password: String): String {
        val response: SignInResponse = client.post("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$apiKey") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "email" to email,
                "password" to password,
                "returnSecureToken" to true
            ))
        }.body()
        idToken = response.idToken
        return response.localId
    }

    override suspend fun createDocument(collection: String, data: Map<String, JsonElement>) {
        val document = FirestoreDocument(fields = data.toFields())
        client.post("https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collection") {
            contentType(ContentType.Application.Json)
            bearerAuth(idToken ?: "")
            setBody(document)
        }
    }

    override suspend fun getDocumentsSorted(collection: String, orderByField: String, direction: String): List<Map<String, String>> {
        val query = JsonObject(mapOf(
            "structuredQuery" to JsonObject(mapOf(
                "from" to JsonArray(listOf(JsonObject(mapOf("collectionId" to JsonPrimitive(collection))))),
                "orderBy" to JsonArray(listOf(JsonObject(mapOf(
                    "field" to JsonObject(mapOf("fieldPath" to JsonPrimitive(orderByField))),
                    "direction" to JsonPrimitive(direction)
                ))))
            ))
        ))

        val response: List<RunQueryResponse> = client.post("https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents:runQuery") {
            contentType(ContentType.Application.Json)
            bearerAuth(idToken ?: "")
            setBody(query)
        }.body()

        return response.mapNotNull { it.document?.fields?.toSimpleMap() }
    }

    override fun close() {
        client.close()
    }
}

@Serializable
data class SignInResponse(val idToken: String, val localId: String, val expiresIn: String)

@Serializable
data class FirestoreDocument(
    val name: String? = null,
    val fields: Map<String, FirestoreValue>? = null
)

@Serializable
data class RunQueryResponse(val document: FirestoreDocument? = null, val readTime: String? = null)

@Serializable
data class FirestoreValue(
    val stringValue: String? = null,
    val integerValue: String? = null,
    val doubleValue: Double? = null,
    val booleanValue: Boolean? = null,
    val timestampValue: String? = null
)

// Helper functions
fun Map<String, JsonElement>.toFields(): Map<String, FirestoreValue> {
    return this.mapValues { (_, value) ->
        when {
            value is JsonPrimitive && value.isString -> FirestoreValue(stringValue = value.content)
            value is JsonPrimitive -> {
                 if (value.booleanOrNull != null) FirestoreValue(booleanValue = value.boolean)
                 else if (value.content.contains(".")) FirestoreValue(doubleValue = value.double)
                 else FirestoreValue(integerValue = value.content)
            }
            else -> FirestoreValue(stringValue = value.toString())
        }
    }
}

fun Map<String, FirestoreValue>.toSimpleMap(): Map<String, String> {
    return this.mapValues { (_, value) ->
        value.stringValue
            ?: value.integerValue
            ?: value.doubleValue?.toString()
            ?: value.booleanValue?.toString()
            ?: value.timestampValue
            ?: "null"
    }
}
