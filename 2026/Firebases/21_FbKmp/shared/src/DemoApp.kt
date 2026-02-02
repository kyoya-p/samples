import com.jakewharton.mosaic.runMosaic
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.layout.padding
import kotlinx.coroutines.delay
import androidx.compose.runtime.*
import kotlinx.serialization.json.JsonPrimitive
import kotlin.random.Random

// API Config (Replace with valid keys for real usage)
// export FIREBASE_API_KEY="AIza..."
// export FIREBASE_PROJECT_ID="my-project"
// Or set here directly
val API_KEY = getEnv("FIREBASE_API_KEY") ?: "YOUR_API_KEY_HERE"
val PROJECT_ID = getEnv("FIREBASE_PROJECT_ID") ?: "YOUR_PROJECT_ID_HERE"
const val TEST_EMAIL = "test@example.com"
const val TEST_PASSWORD = "password123"

suspend fun runDemoApp(client: FirestoreClient) = runMosaic {
    var status by remember { mutableStateOf("Initializing...") }
    var logs by remember { mutableStateOf(listOf<String>()) }
    var items by remember { mutableStateOf(listOf<Map<String, String>>()) }

    Column(modifier = Modifier.padding(1)) {
        Text("Firestore KMP Demo", color = Color.Green)
        Text("-------------------")
        Text("Status: $status")
        Text("")
        Text("Logs:")
        logs.takeLast(8).forEach { Text(" - $it") }
        Text("")
        if (items.isNotEmpty()) {
            Text("Items (Sorted by Value):")
            items.forEach { item ->
                Text(" - ${item["name"]}: ${item["value"]}", color = Color.Yellow)
            }
        }
    }

    LaunchedEffect(Unit) {
        // client is passed in
        try {
            if (API_KEY == "YOUR_API_KEY_HERE") {
                logs = logs + "WARN: API_KEY not set. Running in Dummy Mode."
                delay(2000)
            }

            status = "Signing In..."
            logs = logs + "Signing in as $TEST_EMAIL..."
            
            if (API_KEY != "YOUR_API_KEY_HERE") {
                 try {
                    val uid = client.signIn(TEST_EMAIL, TEST_PASSWORD)
                    logs = logs + "Signed in. UID: $uid"
                 } catch (e: Exception) {
                    logs = logs + "Sign In Failed: ${e.message}. Fallback to dummy."
                 }
            }
            delay(500)

            status = "Creating Documents..."
            val collection = "demo_items"
            repeat(5) { i ->
                val value = Random.nextInt(1, 100)
                val data = mapOf(
                    "name" to JsonPrimitive("Item $i"),
                    "value" to JsonPrimitive(value)
                )
                if (API_KEY != "YOUR_API_KEY_HERE") {
                    try {
                        client.createDocument(collection, data)
                        logs = logs + "Created Item $i with value $value"
                    } catch (e: Exception) {
                        logs = logs + "Create Failed: ${e.message}"
                    }
                } else {
                    logs = logs + "Simulating Create Item $i with value $value..."
                    delay(200)
                }
            }

            status = "Fetching Sorted Documents..."
            logs = logs + "Querying $collection sorted by 'value'..."
            
            if (API_KEY != "YOUR_API_KEY_HERE") {
                try {
                    val results = client.getDocumentsSorted(collection, "value")
                    items = results
                    logs = logs + "Fetched ${items.size} documents."
                } catch (e: Exception) {
                    logs = logs + "Fetch Failed: ${e.message}"
                }
            } else {
                // Dummy Data for visual verification without API Key
                delay(500)
                items = listOf(
                    mapOf("name" to "Item 3", "value" to "12"),
                    mapOf("name" to "Item 1", "value" to "45"),
                    mapOf("name" to "Item 0", "value" to "67"),
                    mapOf("name" to "Item 4", "value" to "89"),
                    mapOf("name" to "Item 2", "value" to "92")
                )
                logs = logs + "Fetched ${items.size} dummy documents."
            }

            status = "Complete"
        } catch (e: Exception) {
            status = "Error"
            logs = logs + "Error: ${e.message}"
            e.printStackTrace()
        } finally {
            client.close()
        }
    }
}