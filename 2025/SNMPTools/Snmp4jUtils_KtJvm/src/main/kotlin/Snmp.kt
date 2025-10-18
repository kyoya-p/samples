import jp.wjg.shokkaa.snmp4jutils.async.*
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.smi.*
import java.net.InetAddress
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun snmpUnicast(
    strAdr: String = "127.0.0.1",
    reqType: Int = PDU.GETNEXT,
    strOid: String = ".1.3.6",

    port: Int = 161,
    udpAdr: UdpAddress = UdpAddress(InetAddress.getByName(strAdr), port),
    commStrV1: String = "public",
    retries: Int = 5,
    interval: Long = 5000,
    target: SnmpTarget = SnmpTarget(udpAdr, OctetString(commStrV1)).apply {
        this.retries = retries
        timeout = interval
    },
    oid: OID = OID(strOid),
    variable: Variable = Null(),
    vbl: List<VariableBinding> = listOf(VariableBinding(oid, variable)),
    pdu: PDU = PDU(reqType, vbl),
    req: Request = Request(target, pdu),
    snmp: Snmp = createDefaultSenderSnmp()
) = suspendCoroutine { conti ->
    val listener: ResponseListener = object : ResponseListener {
        override fun <A : Address?> onResponse(event: ResponseEvent<A>) {
            (event.source as Snmp).cancel(event.request, this)
            val response = event.response
            if (response == null) {
                conti.resume(Result.Timeout(req))
            } else {
                @Suppress("UNCHECKED_CAST")
                conti.resume(Result.Response(req, event as SnmpEvent))
            }
        }
    }
    snmp.send(req.pdu, req.target, null, listener)
}
