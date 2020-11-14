package firebaseInterOp

import kotlin.js.Json

external fun require(module: String): dynamic //javascriptのrequire()を呼ぶ

class Firebase(private val fb: dynamic) {

    companion object {
        fun initializeApp(apiKey: String, authDomain: String, projectId: String): Firebase {
            val firebase = require("firebase/app")
            require("firebase/auth")
            require("firebase/firestore")
            data class Config(val apiKey: String, val authDomain: String, val projectId: String)
            return Firebase(firebase.initializeApp(Config(apiKey, authDomain, projectId)))
        }
    }

    val auth get() = Auth(fb.auth())
    val firestore get() = Firestore(fb.firestore())

    class Auth(private val auth: dynamic) {
        fun signInWithEmailAndPassword(email: String, password: String): Unit =
            auth.signInWithEmailAndPassword(email, password)

        fun signInWithCustomToken(token: String): Unit =
            auth.signInWithCustomToken(token)

        fun onAuthStateChanged(op: (User?) -> Unit) =
            auth.onAuthStateChanged { user -> op(if (user != null) User(user) else null) }

        val currentUser = User(auth.currentUser)
    }

    // https://firebase.google.com/docs/reference/js/firebase.User
    data class User(private val user: dynamic) {
        val uid: String get() = user.uid
    }
}

class Firestore(private val firestore: dynamic) {
    fun collection(path: String) = CollectionReference(firestore.collection(path))

    open class Query

    class CollectionReference(private val collectionRef: dynamic) : Query() {
        fun document(path: String) = DocumentReference(collectionRef.doc(path))
        fun document() = DocumentReference(collectionRef.doc())
    }

    class DocumentReference(private val documentRef: dynamic) : Query() {
        fun get(op: (documentSnapshot: DocumentSnapshot) -> Any?): Unit =
            documentRef.get().then({ v: dynamic -> op(DocumentSnapshot(v)) })
    }

    class DocumentSnapshot(private val documentSnapshot: dynamic) {
        val data get() = documentSnapshot.data()
    }
}