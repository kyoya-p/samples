import jp.wjg.shokkaa.snmp4jutils.async.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock.System.now
import org.junit.jupiter.api.Test
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.mp.SnmpConstants
import org.snmp4j.smi.OID
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding
import java.net.InetAddress
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CommonKtTest {

    @Test
    fun uniCast_Test() = runTest(timeout = 20.seconds) {
        val ag = launch { snmpAgent(sampleMibList) }
        createDefaultSenderSnmpAsync().use { snmp ->
            val adr = UdpAddress(InetAddress.getByName("127.0.0.1"), 161)
            val tg = CommunityTarget(adr, OctetString("public")).apply { timeout = 1000; retries = 1 }
            val pdu = PDU(PDU.GET, listOf(VariableBinding(OID(SampleOID.sysDescr.oid))))
            val res = snmp.uniCast(Request(tg, pdu))
            when (res) {
                is Result.Timeout -> assert(false)
                is Result.Response -> {
                    println("res:${res.received.response}")
                    assert(res.received.response[0].variable == sampleMibMap[OID(SampleOID.sysDescr.oid)])
                }
            }
        }
        ag.cancelAndJoin()
    }

    @Test
    fun getAsync(): Unit = runTest {
        val snmp = createDefaultSenderSnmpAsync()

        val jobAg = launch { snmpAgent(sampleMibList) }
        delay(1000)

        val pdu = PDU(PDU.GETNEXT, listOf(VariableBinding(OID("1.3.6"))))
        val target = CommunityTarget(UdpAddress(snmp.getInetAddressByName("127.0.0.1"), 161), OctetString("public"))
        val res = snmp.sendAsync(pdu, target)
        println("${res.response.variableBindings}:${sampleMibList[0]}")
        assert(res.response.variableBindings[0] == sampleMibList[0])
        jobAg.cancelAndJoin()
        snmp.close()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Test
    fun getNext_Timeout() = runTest(timeout = 60.seconds) {
        val snmp = SnmpBuilder().udp().v1().threads(1).build().async()

        val pdu = PDU(PDU.GET, listOf(VariableBinding(OID(SampleOID.sysDescr.oid))))
        val tg = CommunityTarget(UdpAddress(InetAddress.getByName("1.2.3.4"), 161), OctetString("public"))
        val r1 = snmp.sendAsync(pdu, tg)
        assert(r1.peerAddress == null)
        assert(r1.response == null)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Test
    fun getNext() = runBlocking(Dispatchers.Default) {
        val jobAg = launch { snmpAgent(sampleMibList) }
        delay(1000)
        val snmpAsync = createDefaultSenderSnmpAsync()
        val pdu = PDU(PDU.GET, listOf(VariableBinding(OID(SampleOID.sysDescr.oid))))
        val tg = CommunityTarget(UdpAddress(InetAddress.getByName("localhost"), 161), OctetString("public"))
        val resAsync = snmpAsync.sendAsync(pdu, tg)
        val resSync = snmpAsync.snmp.send(pdu, tg)

        assert(resAsync.peerAddress != null)
        assert(resAsync.response?.variableBindings != null)
        assert(resSync.peerAddress == resAsync.peerAddress)
        assert(resSync.response.variableBindings == resAsync.response?.variableBindings)

        jobAg.cancelAndJoin()
        snmpAsync.close()
    }

    @Test
    fun uniCast2_1() = runTest {
        createDefaultSenderSnmpAsync().uniCast2(Request.from("127.0.0.1"))
            .onResponse { println(it) }
            .onTimeout { println("timeout: $it") }
    }

    @Test
    fun uniCast2_2() = runTest(timeout = 60.seconds) {
        launch { snmpAgent(sampleMibList) }
        delay(100.milliseconds)
        createDefaultSenderSnmpAsync().uniCast2(Request.from("127.0.0.1"))
            .onResponse { println(it) }
            .onTimeout { println("timeout: $it") }
    }

    @Test
    fun uniCast2_3() = runTest(context = Dispatchers.Default, timeout = 1.hours) {
        var ci = 0
        var co = 0
        val total = 1_000_000

        launch {
            var nResponse = 0
            var nTimeout = 0

            val snmpAsync = createDefaultSenderSnmpAsync()
            val sem = Semaphore(50_000)
            (0 until 10_000_000).asFlow().map {
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

                ++ci
                sem.acquire()
                snmpAsync.uniCast2(Request(target, pdu))
            }.collect {
                when (it) {
                    is Result.Response -> ++nResponse
                    else -> ++nTimeout
                }
                ++co
                sem.release()
            }
        }
        fun p() = println("${now()} $ci $co(${co * 100 / total}%) [${ci - co} sess] ")
        while (total > co) {
            p()
            delay(1.seconds)
        }
    }
}

