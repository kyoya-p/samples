import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*


suspend fun clientSample1(client: HttpClient): List<Customer> = client.get("/").body<List<Customer>>()

suspend fun clientSample2(client: HttpClient, id: Int) = client.get("/${id}").body<List<Customer>>()

suspend fun clientSample3(client: HttpClient, id: Int) = client.get("/?id=${id}").body<List<Customer>>()

suspend fun clientSample4(client: HttpClient, customer: Customer) = client.post("/") {
    contentType(ContentType.Application.Json)
    setBody(customer)
}.status

fun sampleClient(engine: HttpClientEngine) = HttpClient(engine) {
    install(ContentNegotiation) { json() }
}

suspend fun main() {
    val customer = Customer(1, "Alice", "Smith")
    val response = sampleClient(CIO.create()).post("http://localhost:8082") {
        contentType(ContentType.Application.Json)
        setBody(customer)
    }
    println(response.status)
}

