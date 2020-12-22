package firebaseInterOp

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
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
        fun initializeApp(apiKey: String, authDomain: String, projectId: String, name: String? = null): Firebase {
            val firebase = require("firebase/app")
            require("firebase/auth")
            require("firebase/firestore")
            data class Config(val apiKey: String, val authDomain: String, val projectId: String)
            name ?: return Firebase(firebase.initializeApp(Config(apiKey, authDomain, projectId)))
            return Firebase(firebase.initializeApp(Config(apiKey, authDomain, projectId), name))
        }
    }

    fun app(appName: String) = App(raw.app(appName))
    fun auth() = Auth(raw.auth())
    fun auth(app: App) = Auth(raw.auth(app.raw))
    fun firestore() = Firestore(raw.firestore())
    fun firestore(app: App) = Firestore(raw.firestore(app.raw))

    class App(val raw: dynamic)

    class Auth(val raw: dynamic) {
        fun signInWithEmailAndPassword(email: String, password: String): Unit =
            raw.signInWithEmailAndPassword(email, password)

        fun signInWithCustomToken(token: String): Unit =
            raw.signInWithCustomToken(token)

        fun onAuthStateChanged(op: (User?) -> Unit): Unit =
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

    open class Query {}

    class ListenerRegistration(val raw: dynamic) {
        fun remove(): Unit = raw()
    }

    class CollectionReference(val raw: dynamic) : Query() {
        fun document(path: String) = DocumentReference(raw.doc(path))
        fun doc(path: String) = document(path)
        fun document() = DocumentReference(raw.doc())
        fun doc() = document()

    }

    class DocumentReference(val raw: dynamic) : Query() {
        fun collection(id: String) = CollectionReference(raw.collection(id))

        fun get(): Promise<DocumentSnapshot> = raw.get()
        fun get(id: String): Promise<DocumentSnapshot> = raw.get(id)
        fun set(doc: Any): Promise<Unit> = raw.set(doc)
        fun addSnapshotListener(listener: EventListener<DocumentSnapshot>): ListenerRegistration =
            ListenerRegistration(raw.onSnapshot { doc -> listener.onEvent(DocumentSnapshot(doc)) })

        fun addSnapshotListener(listener: (DocumentSnapshot?) -> Unit): ListenerRegistration =
            ListenerRegistration(raw.onSnapshot { doc -> listener(DocumentSnapshot(doc)) })
    }

    fun interface EventListener<T> {
        fun onEvent(snapshot: T?): Unit
    }

    class DocumentSnapshot(val raw: dynamic) {
        val data: JsonObject?
            get() {
                raw?.data ?: return null
                val j = JSON.stringify(raw.data())
                return Json {}.decodeFromString(j)
            }
    }
}


@Suppress("UNCHECKED_CAST")
fun Any?.toJsonElement(): JsonElement {
    return when {
        this == null -> JsonNull
        this is JsonPrimitive -> this
        this is JsonObject -> this
        this is JsonArray -> this
        this is Map<*, *> -> (this as Map<String, Any>).toJsonObject()
        this is List<*> -> (this as List<Any>).toJsonArray()
        this is Boolean -> JsonPrimitive(this)
        this is Number -> JsonPrimitive(this)
        this is String -> JsonPrimitive(this)
        else -> throw IllegalStateException("in toJsonElement(): Type missmatch: ${this}")
    }
}

fun Any.toJsonObject(): JsonObject {
    val t = this
    if (t is JsonObject) return t
    if (t is Map<*, *>) {
        t as Map<String, Any>
        return buildJsonObject {
            t.forEach { (k, v) -> put(k, v.toJsonElement()) }
        }
    }
    throw IllegalStateException("Any.toJsonObject() this=$this")
}

fun Any.toJsonArray(): JsonArray {
    val t = this
    if (t is JsonArray) return t
    if (t is List<*>) {
        t as List<Any>
        return buildJsonArray {
            t.forEach { add(it.toJsonElement()) }
        }
    }
    throw IllegalStateException("Any.toJsonArray() this=$this")
}

