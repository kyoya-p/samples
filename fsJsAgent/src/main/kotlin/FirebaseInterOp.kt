package firebaseInterOp

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise


external fun require(module: String): dynamic //javascriptのrequire()を呼ぶ

suspend fun <T> Promise<T>.await(): T = suspendCoroutine { cont ->
    then({ cont.resume(it) }, { cont.resumeWithException(it) })
}

class Firebase(val raw: dynamic) {

    companion object {
        fun initializeApp(apiKey: String, authDomain: String, projectId: String): Firebase {
            val firebase = require("firebase/app")
            require("firebase/auth")
            require("firebase/firestore")
            data class Config(val apiKey: String, val authDomain: String, val projectId: String)
            return Firebase(firebase.initializeApp(Config(apiKey, authDomain, projectId)))
        }
    }

    val auth get() = Auth(raw.auth())
    val firestore get() = Firestore(raw.firestore())

    class Auth(val raw: dynamic) {
        fun signInWithEmailAndPassword(email: String, password: String): Unit =
            raw.signInWithEmailAndPassword(email, password)

        fun signInWithCustomToken(token: String): Unit =
            raw.signInWithCustomToken(token)

        fun onAuthStateChanged(op: (User?) -> Unit) =
            raw.onAuthStateChanged { user -> op(if (user != null) User(user) else null) }

        val currentUser = User(raw.currentUser)
    }

    // https://firebase.google.com/docs/reference/js/firebase.User
    data class User(private val raw: dynamic) {
        val uid: String get() = raw.uid
    }
}

class Firestore(val raw: dynamic) {
    fun collection(path: String) = CollectionReference(raw.collection(path))

    open class Query {
        fun onSnapshot(observer:)
    }

    class CollectionReference(val raw: dynamic) : Query() {
        fun doc(path: String) = DocumentReference(raw.doc(path))
        fun doc() = DocumentReference(raw.doc())
    }

    class DocumentReference(val raw: dynamic) : Query() {
        fun get(): Promise<DocumentSnapshot> = raw.get()
        fun get(id: String): Promise<DocumentSnapshot> = raw.get(id)
        fun set(doc: Any): Promise<Unit> = raw.set(doc)
        fun collection(id: String) = CollectionReference(raw.collection(id))
    }

    class DocumentSnapshot(val raw: dynamic) {
        fun data(): Map<String, Any> = raw.data()
    }
}