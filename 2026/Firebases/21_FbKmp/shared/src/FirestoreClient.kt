import kotlinx.serialization.json.JsonElement

interface FirestoreClient {
    suspend fun signIn(email: String, password: String): String
    suspend fun createDocument(collection: String, data: Map<String, JsonElement>)
    suspend fun getDocumentsSorted(collection: String, orderByField: String, direction: String = "ASCENDING"): List<Map<String, String>>
    fun close()
}