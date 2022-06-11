import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow

@OptIn(DelicateCoroutinesApi::class)
fun main() {
    GlobalScope.launch {
        val jA = launch { scan("192.168.11.1", "192.168.11.254") }
        launch { scan("192.168.11.1", "192.168.11.254") }
        delay(30)
        jA.cancelAndJoin()
    }
}

suspend fun scan(ipStart: String, ipEnd: String) {
    val s = ipStart.ipv4ToLong()
    val e = ipEnd.ipv4ToLong()
    flow {
        for (i in s..e) {
            emit(i)
            delay(200)
        }
    }.collect {
        println(it.longToIpv4())
    }
}

private fun String.ipv4ToLong() = split(".").map { it.toLong() }.fold(0L) { a, e -> a * 256 + e }
private fun Long.longToIpv4() = (3 downTo 0).map { (this shr (it * 8)).mod(256) }.joinToString(".")
