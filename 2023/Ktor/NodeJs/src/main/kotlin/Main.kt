
import io.ktor.client.*
fun mainX() {
    println(greeting("NodeJs"))
}

fun greeting(name: String) =
    "Hello, $name"



fun main(args: Array<String>) {
    // Create a client with the CIO engine.
    val client = HttpClient(CIO)

    // Make a GET request to the Google homepage.
    val response = client.get("https://www.google.com")

    // Check the status code of the response.
    if (response.status == 200) {
        // The request was successful.
        val body = response.content
        console.log(body)
    } else {
        // The request failed.
        console.log("Request failed with status code:", response.status)
    }
}
