import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc
import kotlin.time.Duration

@Rpc
interface ACertainService : RemoteService {
    suspend fun timer(interval: Duration): Flow<Int>
}
