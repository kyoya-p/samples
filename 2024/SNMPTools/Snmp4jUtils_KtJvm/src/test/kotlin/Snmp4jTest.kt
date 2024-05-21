import jp.wjg.shokkaa.snmp4jutils.async.OID
import jp.wjg.shokkaa.snmp4jutils.async.defaultScanTarget
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
import org.snmp4j.smi.VariableBinding
import java.lang.Thread.sleep
import java.net.InetAddress
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread
import kotlin.time.measureTime

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
    fun loadtest_timeout_OOM() {
        val snmp = SnmpBuilder().udp().v1().v3().build()
        val noTaget = InetAddress.getByName("192.168.11.99")
        val tg = defaultScanTarget(noTaget).apply { timeout = 5000; retries = 1 }

        var ci = 0
        var co = 0
        val t0 = now()
        thread {
            while (true) {
                sleep(1000)
                println("${now() - t0}: $ci->$co [${ci - co}]")
            }
        }
        val t = 16_777_216  // class-A
        val s = 100_000
        val sem = Semaphore(s)
        for (i in 0 until t) {
            sem.acquire()
            snmp.send(
                PDU(PDU.GETNEXT, listOf(VariableBinding(OID(1, 3, 6)))),
                tg,
                null,
                object : ResponseListener {
                    override fun <A : Address?> onResponse(p0: ResponseEvent<A>?) {
                        ++co
                        sem.release()
                    }
                })
            ++ci
        }
        sem.acquire(s)
    }

    @Test
    fun loadtest_timeout2_OOM() {
        val snmp = SnmpBuilder().udp().v1().v3().build()
        val noTaget = InetAddress.getByName("192.168.11.99")
        val tg = defaultScanTarget(noTaget).apply { timeout = 5000; retries = 1 }

        var ci = 0
        var co = 0
        val t0 = now()
        thread {
            while (true) {
                sleep(1000)
                println("${now() - t0}: $ci->$co [${ci - co}]")
            }
        }
        val t = 16_777_216  // class-A
        val s = 100_000
        val sem = Semaphore(s)
        val listener = object : ResponseListener {
            override fun <A : Address?> onResponse(p0: ResponseEvent<A>?) {
                ++co
                sem.release()
            }
        }
        for (i in 0 until t) {
            sem.acquire()
            snmp.send(PDU(PDU.GETNEXT, listOf(VariableBinding(OID(1, 3, 6)))), tg, null, listener)
            ++ci
        }
        sem.acquire(s)
    }

    @Test
    // SNMP投入スループットを適切に制限し、SNMPスタック内で要求完了(解放)処理時間を確保することでOoM回避できる
    fun loadtest_timeout3() {
        val snmp = SnmpBuilder().udp().v1().v3().build()
        val noTaget = InetAddress.getByName("192.168.11.99")
        val tg = defaultScanTarget(noTaget).apply { timeout = 500; retries = 1 }

        var ci = 0
        var co = 0
        val t0 = now()
        val n = 16_777_216  // class-A
        thread {
            while (true) {
                sleep(1000)
                println("${now() - t0}: $ci->$co [${ci - co}]  ${(co * 1000L / n) / 10.0}%")
            }
        }
        val s = 700_000
        val sem = Semaphore(s)
        val listener = object : ResponseListener {
            override fun <A : Address?> onResponse(res: ResponseEvent<A>?) {
                ++co
                snmp.cancel(res!!.request, this)
                sem.release()
            }
        }
        for (i in 0 until n) {
            sem.acquire()
            snmp.send(PDU(PDU.GETNEXT, listOf(VariableBinding(OID(1, 3, 6)))), tg, null, listener)
            ++ci
        }
        sem.acquire(s)
    }
}