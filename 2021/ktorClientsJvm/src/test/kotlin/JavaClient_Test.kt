import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class JavaClient_Test {
    @Test
    fun test() {
        val client = JavaClient().httplClient
        val req = HttpRequest.newBuilder().uri(URI.create("https://localhost"))
            .build()
        val r = client.send(req, HttpResponse.BodyHandlers.ofString())
        println(r.body())
    }

    @Test
    fun t22() {
        println(System.getProperty("jdk.tls.disabledAlgorithm"))
    }
}
