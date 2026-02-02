import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay

@Serializable
data class FirestoreDoc(
    val name: String,
    val fields: Map<String, FirestoreField>,
    val createTime: String? = null,
    val updateTime: String? = null
)

@Serializable
data class FirestoreField(
    val stringValue: String? = null,
    val timestampValue: String? = null
)

@Serializable
data class Contact(
    val id: String,
    val name: String,
    val email: String,
    val timestamp: String
)

@Serializable
data class RunQueryRequest(
    val structuredQuery: StructuredQuery
)

@Serializable
data class StructuredQuery(
    val from: List<CollectionSelector>,
    val orderBy: List<Order>? = null
)

@Serializable
data class CollectionSelector(
    val collectionId: String
)

@Serializable
data class Order(
    val field: FieldReference,
    val direction: String = "DESCENDING"
)

@Serializable
data class FieldReference(
    val fieldPath: String
)

@Serializable
data class RunQueryResponse(
    val document: FirestoreDoc? = null,
    val readTime: String? = null
)

class FirestoreRepository(private val apiKey: String, private val projectId: String) {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val v1beta1BaseUrl = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"

    // Real-time Listen via RunQuery Polling (Fallback for API Key limitation on JVM)
    fun listenToContacts(): Flow<List<Contact>> = flow {
        val requestBody = RunQueryRequest(
            structuredQuery = StructuredQuery(
                from = listOf(CollectionSelector("addressbook")),
                orderBy = listOf(Order(FieldReference("timestamp"), "DESCENDING"))
            )
        )

        while (true) {
            try {
                // Using v1 runQuery (POST)
                val response: List<RunQueryResponse> = client.post("$v1beta1BaseUrl:runQuery") {
                    parameter("key", apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }.body()

                val contacts = response.mapNotNull { it.document }.map { parseFirestoreDoc(it) }
                emit(contacts)
                
            } catch (e: Exception) {
                System.err.println("FATAL: Sync error (RunQuery): $e")
                throw e // Cause App to exit
            }
            delay(3000) // Poll interval
        }
    }

    private fun parseFirestoreDoc(doc: FirestoreDoc): Contact {
        val id = doc.name.substringAfterLast("/")
        return Contact(
            id = id,
            name = doc.fields["name"]?.stringValue ?: "",
            email = doc.fields["email"]?.stringValue ?: "",
            timestamp = doc.fields["timestamp"]?.timestampValue ?: doc.createTime ?: ""
        )
    }

    suspend fun addContact(name: String, email: String) {
        val body = JsonObject(mapOf(
            "fields" to JsonObject(mapOf(
                "name" to JsonObject(mapOf("stringValue" to JsonPrimitive(name))),
                "email" to JsonObject(mapOf("stringValue" to JsonPrimitive(email))),
                "timestamp" to JsonObject(mapOf("timestampValue" to JsonPrimitive("2026-02-03T12:00:00Z")))
            ))
        ))
        client.patch("$v1beta1BaseUrl/addressbook/$name") {
            parameter("key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun removeContact(id: String) {
        client.delete("$v1beta1BaseUrl/addressbook/$id") {
            parameter("key", apiKey)
        }
    }
}