import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = RestFirestoreClient(API_KEY, PROJECT_ID)
    runDemoApp(client)
}
