import kotlinx.datetime.Clock.System.now
import org.junit.jupiter.api.Test
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.Address
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import java.lang.Thread.sleep
import java.net.InetAddress
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread
import kotlin.time.measureTime

fun main()=Snmp4jTest().snmp4j()

class Snmp4jTest {

    @Test
    fun snmp4j() {
        val snmp = SnmpBuilder().udp().v1().v3().build()!!
        val tg = CommunityTarget(UdpAddress(InetAddress.getByName("127.0.0.1")!!, 161), OctetString("public")).apply {
            timeout = 5000
            retries = 5
        }
        val nSess=10_000*30
        val sem = Semaphore(nSess)
        val nTotal = 0x01_00_00_00
        var nReq = 0
        var nRes = 0
        val listener = object : ResponseListener {
            override fun <A : Address?> onResponse(event: ResponseEvent<A>?) {
                ++nRes
                sem.release()
            }
        }
        val monitor = thread {
            val ts = now()
            var t0 = now()
            var nRes0 = 0
            while (true) {
                sleep(5000)
                val t1 = now()
                if (t1 > t0) println("${(now() - ts)}, $nReq->$nRes(${nRes * 100 / nTotal}%) res, ${nReq - nRes} sess, ${(nRes - nRes0) * 1000 / (t1 - t0).inWholeMilliseconds}/s")
                t0 = t1
                nRes0 = nRes
                if (nRes >= nTotal) break
            }
        }
        snmp.listen()
        val td = measureTime {
            repeat(nTotal) {
//                delay(1000.milliseconds)
                sem.acquire()
                snmp.send(PDU(PDU.GETNEXT, listOf()), tg, null, listener)
                ++nReq
            }
            repeat(nSess) { sem.acquire() }
        }
        monitor.join()
        println("$td ${nTotal / td.inWholeSeconds}/s")
    }
}