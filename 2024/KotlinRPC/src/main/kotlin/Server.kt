import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc

@Rpc
interface AwesomeService : RemoteService {
    suspend fun getNews(city: String): Flow<String>
}