@JsModule("firebase/app")
external val firebase: JsAny

fun aaa() {
    val app = firebase.initializeApp("my-app")
}
