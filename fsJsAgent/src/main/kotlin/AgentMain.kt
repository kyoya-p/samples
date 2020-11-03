import firebaseInterOp.Firebase
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
suspend fun main() = GlobalScope.launch {
    println("Start SampleAgent/NodeJS. (Ctrl-C to Terminate)")

    val customAuth = true

    val firebase = Firebase(
        apiKey = "AIzaSyDrO7W7Sb6RCpHTsY3GaP-zODRP_HtY4nI",
        authDomain = "road-to-iot.firebaseapp.com",
        projectId = "road-to-iot"
    )

    if (customAuth) {
        val customToken = requestCustomToken("agent1", "1234")
        println("aquired token: $customToken")
        firebase.auth.signInWithCustomToken(customToken)
    } else {
        firebase.auth.signInWithEmailAndPassword("kyoya.p4@gmail.com", "kyoyap4")
    }
    println("Start access DB")
    firebase.auth.onAuthStateChangedFlow().collect { user ->
        if (user != null) {
            println("Logged-in.  User: ${user.uid}")
            val db = firebase.firestore
            db.collection("device").document("agent1").get()
                .collect { deviceAgent ->
                    val r1 = deviceAgent.data().deviceId
                    val r2 = deviceAgent.data()["deviceId"]
                    println("data.deviceId: ${r1}")
                    println("data.deviceId: ${r2}")
                }
        }
    }
}.join()

val httpClient = HttpClient {
    install(HttpTimeout) {
        requestTimeoutMillis = 5000
    }
}

suspend fun requestCustomToken(agentId: String, password: String) =
    httpClient.get<String>("http://localhost:8080/customToken?id=${agentId}&pw=${password}")

