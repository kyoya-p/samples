package jp.wjg.shokkaa.container

import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.*
import java.net.InetAddress
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


suspend fun snmpUnicast(
    strAdr: String = "127.0.0.1",
    port: Int = 161,
    reqType: Int = PDU.GETNEXT,
    strOid: String = ".1.3.6",
    oid: List<Int> = strOid.split(".").map { it.toInt() },
    variable: SnmpVariable = SnmpVariable.Null(),
    vbl: List<SnmpVarBind> = listOf(SnmpVarBind(oid = oid, variable = variable)),
    target: SnmpTargetV1 = SnmpTargetV1(strAdr, port, community = "public", retry = 5, intervalMs = 5000),
    req: SnmpRequest = SnmpRequest(target = target, pduType = reqType, vbl = vbl),
    snmp: Snmp = SnmpBuilder().udp().v1().v3().build(),
) = suspendCoroutine { conti ->
    val listener: ResponseListener = object : ResponseListener {
        override fun <A : Address?> onResponse(event: ResponseEvent<A>) {
            (event.source as Snmp).cancel(event.request, this)
            val response = event.response
            if (response == null) {
                conti.resume(SnmpResult.Timeout(req))
            } else {
                @Suppress("UNCHECKED_CAST")
                conti.resume(SnmpResult.Response(req))
            }
        }
    }

    val pdu = PDU(req.pduType, req.vbl.toSnmp4jVbl())
    val adr = UdpAddress(InetAddress.getByName(req.target.adr), req.target.port)
    val target = CommunityTarget(adr, OctetString(req.target.community)).apply {
        retries = req.target.retry
        timeout = req.target.intervalMs
    }
    snmp.send(pdu, target, null, listener)
}

fun List<SnmpVarBind>.toSnmp4jVbl() =
    map { VariableBinding(OID(it.oid.joinToString(".")), it.variable.toSnmp4jVariable()) }

fun SnmpVariable?.toSnmp4jVariable() = when (this?.syntax) {
    null -> Null.instance
    Null.instance.syntax -> Null.instance
    else -> throw NotImplementedError()
}!!
