import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.Timestamp.Companion.now
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val email: String,
    val status: String,
    val time: Timestamp = now(),
    val uid: String = auth.currentUser!!.uid
)

@Serializable
data class SpawnResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val uid: String = auth.currentUser!!.uid,
)

@Serializable
data class Request(
    val cmd: String,
    val time: Timestamp = now(),
    val isComplete: Boolean = false,
    val result: SpawnResult? = null,
    val exception: String? = null,
    val uid: String = auth.currentUser!!.uid,
)

@Serializable
data class Image(
    val imageName: String,
    val time: Timestamp = now(),
    val uid: String = auth.currentUser!!.uid,
)

@Serializable
data class Container(
    val id: String,
    val imageName: String,
    val time: Timestamp = now(),
    val uid: String = auth.currentUser!!.uid,
)

@Serializable
data class Task(
    val id: String,
    val pid: String,
    val status: String,
    val time: Timestamp = now(),
    val uid: String = auth.currentUser!!.uid,
)

fun queryParameters(s: String) = s.replaceFirst("?", "").split("&")
    .map { it.split("=") }.associate { it[0] to (it.getOrElse(1) { "" }) }

