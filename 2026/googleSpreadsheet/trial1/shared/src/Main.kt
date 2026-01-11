import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

expect fun getEnv(key: String): String
expect val client: HttpClient

@Serializable
data class ValueRange(
    val range: String? = null,
    val majorDimension: String? = null,
    val values: List<List<String>>? = null
)

fun main(args: Array<String>) = runBlocking {
    val apiKey = getEnv("GOOGLE_API_KEY")
    if(apiKey.isBlank()){
        login()
    }

    val spreadsheetId = getEnv("SPREADSHEET_ID")
    val range = "A1:C5"

    val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/$range?key=$apiKey"
    val response = client.get(url).body<ValueRange>()
    println(response)
}
