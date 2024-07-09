import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.Timestamp.Companion.now
import kotlinx.serialization.Serializable

@Serializable
data class SpawnResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

@Serializable
data class Request(
    val cmd: String,
    val time: Timestamp = now(),
    val isComplete: Boolean = false,
    val result: SpawnResult? = null,
    val exception: String? = null,
)
