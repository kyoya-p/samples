import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow

@OptIn(DelicateCoroutinesApi::class)
fun main() {
    GlobalScope.launch {
        val jA = launch { scan("192.168.11.1","192.168.11.254" }
        launch { scan("192.168.11.1","192.168.11.254" }
        delay(300)
        jA.cancelAndJoin()
    }
}

suspend fun scan(ipStart: String, ipEnd: String) {
    val s = ipStart.ipV4ToLong()
    val e = ipEnd.ipV4ToLong()
    flow {
        for (i in s..e) {
            emit(i)
            delay(200)
        }
    }.collect {
    }
}