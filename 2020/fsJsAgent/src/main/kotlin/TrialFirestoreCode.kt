package test

external fun require(module: String): dynamic //javascriptのrequire()を呼ぶ

fun firesoreTest() {
    println("Loading firebase modules.")
    // Firestore接続
    // See: https://firebase.google.com/docs/firestore/quickstart?hl=ja
    val firebase = require("firebase/app")
    require("firebase/auth")
    require("firebase/firestore")

    println("Initiate Firebase application instance.")
    class FirebaseAppConfig(val apiKey: String, val authDomain: String, val projectId: String)
    firebase.initializeApp(
        FirebaseAppConfig(
            apiKey = "AIzaSyDrO7W7Sb6RCpHTsY3GaP-zODRP_HtY4nI",
            authDomain = "road-to-iot.firebaseapp.com",
            projectId = "road-to-iot"
        )
    )

    println("Making Firestore database instance.")
    val db = firebase.firestore()

    // Firebase認証
    // See: https://firebase.google.com/docs/auth/web/password-auth?hl=ja

    println("Authenticate user account for Cloud Firestore database.")
    firebase.auth().signInWithEmailAndPassword("kyoya.p4@gmail.com", "kyoyap4")
    firebase.auth().onAuthStateChanged { user ->
        if (user) {
            println("Signed-in: ${user.email}")
            val docRef = db.collection("device").doc("agent1")
            docRef.get().then { doc ->
                if (doc.exists) {
                    println("Document data.deviceId= ${doc.data().deviceId}")
                } else {
                    println("No such document!")
                }
            }
        } else {
            console.log("Not signed-in")
        }
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
