import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.rpc.RpcServer
import kotlinx.rpc.krpc.rpcServerConfig
import kotlinx.rpc.krpc.server.KrpcServer
import kotlinx.rpc.registerService
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

class ACertainServiceImpl(override val coroutineContext: CoroutineContext) : ACertainService {
    override suspend fun timer(interval: Duration): Flow<Int> = flow {
        repeat(1000) {
            emit(it)
            delay(interval)
        }
    }
}

fun appMain() {
    embeddedServer(CIO, port = 8080) {
        module()
        println("Server running")
    }.start(wait = true)
}

fun Application.module() {
    install(Krpc)

    routing {
        rpc("/pizza") {
            rpcConfig {
                serialization {
                    json()
                }
            }

            registerService<PizzaShop> { ctx -> PizzaShopImpl(ctx) }
        }
    }
}


//fun appMain() {
//    server.registerService<ACertainService> { ctx: CoroutineContext -> ACertainServiceImpl(ctx) }
//}

// same MySimpleRpcTransport as in the client example above
class MySimpleRpcServer : KrpcServer(rpcServerConfig(), MySimpleRpcTransport())

val server = MySimpleRpcServer()
server.registerService<MyService> { ctx -> MyServiceImpl(ctx) }