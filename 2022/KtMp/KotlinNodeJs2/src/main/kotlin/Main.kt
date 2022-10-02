import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import netSnmp.SampleOID
import netSnmp.Snmp

@OptIn(DelicateCoroutinesApi::class)
fun main() {
    GlobalScope.launch {
        


//        val jA = launch { scan("192.168.11.1", "192.168.11.254") }
//        launch { scan("192.168.11.1", "192.168.11.254") }
//        delay(300)
//        jA.cancelAndJoin()
    }
}

private fun String.ipv4ToLong() = split(".").map { it.toLong() }.fold(0L) { a, e -> a * 256 + e }
private fun Long.longToIpv4() = (3 downTo 0).map { (this shr (it * 8)).mod(256) }.joinToString(".")

suspend fun scan(ipStart: String, ipEnd: String) {
    val s = ipStart.ipv4ToLong()
    val e = ipEnd.ipv4ToLong()
    flow {
        for (i in s..e) {
            emit(i)
            delay(30)
        }
    }.collect {
        println(it.longToIpv4())
        val adr = it.longToIpv4()
        val session = Snmp.createSession(adr, "public")
        session.getNext(arrayOf(SampleOID.hrDeviceDescr.oid)) { err, vbs ->
            println("Error=$err,VBS=$vbs")
        }
    }
}

fun greeting(name: String) =
    "Hello, $name"