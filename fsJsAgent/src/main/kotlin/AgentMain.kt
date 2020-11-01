external fun require(module: String): dynamic //javascriptのrequire()を呼ぶ

fun main() {
    firestoreTest()
}

fun firestoreTest() {

    println("Start Firestore Client.")
    // Firestore接続
    // See: https://firebase.google.com/docs/firestore/quickstart?hl=ja
    val firebase = require("firebase/app")
    require("firebase/auth")
    require("firebase/firestore")
    firebase.initializeApp(
        mapOf(
            "apiKey" to "AIzaSyDrO7W7Sb6RCpHTsY3GaP-zODRP_HtY4nI",
            "authDomain" to "road-to-iot.firebaseapp.com",
            "projectId" to "road-to-iot"
        )
    )
    val db = firebase.firestore()

    // Firebase認証
    // See: https://firebase.google.com/docs/auth/web/password-auth?hl=ja
    try {
        firebase.auth().signInWithEmailAndPassword("kyoya.p4@gmail.com", "kyoyap4")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun webServerTest() {
    val exp = require("express")
    val svr = exp()
    svr.get("/") { _, res ->
        res.send("Kotlin/JS Web Server.")
    }
    svr.listen(8080)
}

