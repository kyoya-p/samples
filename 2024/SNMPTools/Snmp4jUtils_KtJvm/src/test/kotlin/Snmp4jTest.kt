import kotlinx.datetime.Clock.System.now
import org.junit.jupiter.api.Test
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.mp.SnmpConstants
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.lang.Thread.sleep
import java.net.InetAddress
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread
import kotlin.time.measureTime


fun main() = Snmp4jTest().snmp4j()
class Snmp4jTest {
    val nClassC = 0x100
    val nClassB = nClassC * 0x100
    val nClassA = nClassB * 0x100

    @Test
    fun snmp4j() {
        val nTotal = nClassA * 1

        val snmp = SnmpBuilder().udp().v1().v3().build()!!
        val tg = CommunityTarget(UdpAddress(InetAddress.getByName("127.0.0.1")!!, 161), OctetString("public")).apply {
            timeout = 5000
            retries = 5
        }
        val nSess = 12_000 * 30
        val sem = Semaphore(nSess)
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
        //snmp.listen()
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

    @Test
    fun snmp4jSample() {

        val snmp = Snmp(DefaultUdpTransportMapping())
        snmp.listen()

        var ci = 0
        var co = 0
        val sem = Semaphore(500_000)

        val listener: ResponseListener = object : ResponseListener {
            override fun <A : Address?> onResponse(event: ResponseEvent<A>) {
                ++co
                sem.release()
                (event.source as Snmp).cancel(event.request, this)
                val response = event.response
                val request = event.request
                if (response == null) {
//                    println("Request $request timed out")
                } else {
//                    println("Received response " + response + " on request " + request)
                }
            }
        }

        val total = 10_000_000
        thread {
            repeat(total) {
                val pdu = PDU(PDU.GETNEXT, listOf(VariableBinding(OID("1.3.6.1.2.1.1.1"))))
                val target: CommunityTarget<UdpAddress> = CommunityTarget<UdpAddress>(
//                    UdpAddress(InetAddress.getByName("127.0.0.1")!!, 161),
                    UdpAddress(InetAddress.getByName("10.255.255.255")!!, 161),
                    OctetString("public")
                ).apply {
                    version = SnmpConstants.version1
                    retries = 5
                    timeout = 5000
                }

                sem.acquire()
                ++ci
                snmp.send(pdu, target, null, listener)
            }
        }

        fun p() = println("${now()} $ci $co (${ci - co} sess)  ")
        while (total > co) {
            p()
            sleep(1000)
        }
        p()
    }
}