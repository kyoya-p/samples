suspend fun main() {
    val client = RestFirestoreClient(API_KEY, PROJECT_ID)
    runDemoApp(client)
}