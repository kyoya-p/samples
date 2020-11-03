import firebaseInterOp.Firebase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect

data class X(val a: Int)

@ExperimentalCoroutinesApi
suspend fun main() {
    val firebase = Firebase(
        apiKey = "AIzaSyDrO7W7Sb6RCpHTsY3GaP-zODRP_HtY4nI",
        authDomain = "road-to-iot.firebaseapp.com",
        projectId = "road-to-iot"
    )
    firebase.auth.signInWithEmailAndPassword("kyoya.p4@gmail.com", "kyoyap4")
    firebase.auth.onAuthStateChanged().collect {
        println("User: ${it.email}")
        val db = firebase.firestore
        val v = db.collection("device").document("agent1").get()
        v.collect {
            println("Data: ${it.data}")
        }
    }
}


