package firebaseInterOp

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow


external fun require(module: String): dynamic //javascriptのrequire()を呼ぶ

class Firebase(apiKey: String, authDomain: String, projectId: String) {
    val _firebase: dynamic = initializeApp(apiKey, authDomain, projectId)

    fun initializeApp(apiKey: String, authDomain: String, projectId: String): dynamic {
        val firebase = require("firebase/app")
        require("firebase/auth")
        require("firebase/firestore")
        class Config(val apiKey: String, val authDomain: String, val projectId: String)
        return firebase.initializeApp(Config(apiKey, authDomain, projectId))
    }

    class Auth(val _auth: dynamic) {
        fun signInWithEmailAndPassword(email: String, password: String) =
            _auth.signInWithEmailAndPassword(email, password)

        @ExperimentalCoroutinesApi
        fun onAuthStateChanged() = callbackFlow {
            _auth.onAuthStateChanged { user ->
                println("Callbacked")
                if (user) offer(user)
            }
            awaitClose {} // 一生closeしないけど..
        }
    }

    class Firestore(val _firestore: dynamic) {
        fun collection(path: String) = CollectionReference(_firestore.collect(path))
    }

    class CollectionReference(val _collectionRef: dynamic) {
        fun document(path: String) = DocumentReference(_collectionRef.document(path))
    }

    class DocumentReference(val _documentRef: dynamic) {
        suspend fun get() = callbackFlow {
            _documentRef.get().then { doc -> offer(DocumentSnapshot(doc)) }
            awaitClose { }
        }

        fun snapshotFlow() = callbackFlow<DocumentSnapshot> {
//TODO
        }
    }

    class DocumentSnapshot(val _documentSnapshot: dynamic) {
        val data get() :Map<String, Any> = _documentSnapshot.data()
    }

    val auth = Auth(_firebase.auth())
    val firestore = Firestore(_firebase.firestore())
}