package sc

import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.Target
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.mp.SnmpConstants
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.net.InetAddress

class SNMPManager(
        private val snmp: Snmp = Snmp(DefaultUdpTransportMapping())
) {
    enum class ResultCode { RECEIVED, COMPLETE, TIMEOUT, ILLEGAL_RESPONSE, CONTINUE, ABORT }

    fun listen(): SNMPManager {
        snmp.listen()
        return this
    }

    fun close(): SNMPManager {
        snmp.close()
        return this
    }

    // Test
    fun getNext(addr: String, oid: Array<Int>): List<String> {
        val a = IntArray(oid.size)
        oid.forEachIndexed { i, x -> a[i] = x }
        val target = CommunityTarget().apply {
            community = OctetString("public")
            address = UdpAddress(InetAddress.getByName(addr), 161)
            version = SnmpConstants.version1
            timeout = 5000
            retries = 5
        }
        val pdu = PDU().apply {
            add(VariableBinding(OID(a)))
            type = PDU.GETNEXT
        }
        val response = snmp.send(pdu, target)
        if (response.response == null) return listOf<String>()
        return response.response.variableBindings.map { it.toString() }
    }

    // Test
    fun getBulk(addr: String, oid: IntArray): List<String> {
        val target = CommunityTarget().apply {
            community = OctetString("public")
            address = UdpAddress(InetAddress.getByName(addr), 161)
            version = SnmpConstants.version2c
            timeout = 5000
            retries = 5
            version = SnmpConstants.version2c
        }
        val pdu = PDU().apply {
            add(VariableBinding(OID(oid)))
            type = PDU.GETBULK
            maxRepetitions = 100
            nonRepeaters = 0
        }
        val response = snmp.send(pdu, target)
        if (response.response == null) return listOf<String>()
        return response.response.variableBindings.map { it.toString() }

    }

    //　usage: snmp.sendAsync(pdu,target){/*応答処理*/}
    private fun sendAsync(pdu: PDU, target: Target, callback: (ResponseEvent) -> Unit = {}) {
        val listener = object : ResponseListener {
            override fun onResponse(event: ResponseEvent) {
                (event.source as Snmp).cancel(event.request, this)
                callback(event)
            }
        }
        snmp.send(pdu, target, null, listener)
    }

    infix fun OID.includes(target: OID): Boolean {
        val sOid = java.lang.Math.min(this.size(), target.size())
        for (i in 0 until sOid) {
            if (target[i] != this[i]) return false
        }
        return true
    }

    private fun walkEachCallback(initOid: OID, target: Target, event: ResponseEvent
                                 , cbTerminate: (ResultCode, ResponseEvent) -> Any = { _, _ -> }
                                 , cbEachResponse: (ResponseEvent) -> Any = { }
                                 , cbEachResult: (VariableBinding) -> Any = { }

    ): Unit {
        if (cbEachResponse(event) == ResultCode.ABORT) {
            cbTerminate(ResultCode.ABORT, event)
            return
        }
        when {
            (event.response == null) -> cbTerminate(ResultCode.TIMEOUT, event)
            (event.response.variableBindings.size < 1) -> cbTerminate(ResultCode.ILLEGAL_RESPONSE, event)
            (event.response.errorStatus == PDU.noSuchName) -> cbTerminate(ResultCode.COMPLETE, event)
            else -> {
                event.response.variableBindings.forEach {
                    val nextOid = it.oid
                    if (!(initOid includes nextOid) or (it.variable == Null.endOfMibView)) {
                        cbTerminate(ResultCode.COMPLETE, event)
                        return
                    }
                    if (cbEachResult(it) == ResultCode.ABORT) {
                        cbTerminate(ResultCode.ABORT, event)
                        return
                    }
                }
                val nextPdu = PDU()
                nextPdu.add(VariableBinding(event.response.variableBindings.last().oid))
                nextPdu.type = event.request.type
                if (event.request.type == PDU.GETBULK) {
                    nextPdu.maxRepetitions = 100
                    nextPdu.nonRepeaters = 0
                }
                sendAsync(nextPdu, target) { walkEachCallback(initOid, target, it, cbTerminate, cbEachResponse, cbEachResult) }
            }
        }
    }

    // usage: snmp.walk("192.169.0.1"){/* getNextResponse毎の受信処理 */}
    fun walk(addr: String, initOid: List<Int> = listOf(1)
             , cbTerminate: (ResultCode, ResponseEvent) -> Any = { _, _ -> }
             , cbEachResponse: (ResponseEvent) -> Any = { ResultCode.CONTINUE }
             , cbEachResult: (VariableBinding) -> Any = { ResultCode.CONTINUE }
    ) {
        val oid = OID(initOid.toIntArray())
        val target = CommunityTarget().apply {
            community = OctetString("public")
            address = UdpAddress(InetAddress.getByName(addr), 161)
            version = SnmpConstants.version1
            timeout = 1000
            retries = 5
        }
        val pdu = PDU().apply {
            type = PDU.GETNEXT
            add(VariableBinding(oid))
        }
        sendAsync(pdu, target) { walkEachCallback(oid, target, it, cbTerminate, cbEachResponse, cbEachResult) }
    }

    fun walkBulk(addr: String, initOid: List<Int> = listOf(1)
                 , cbTerminate: (ResultCode, ResponseEvent) -> Any = { _, _ -> }
                 , cbEachResponse: (ResponseEvent) -> Any = { ResultCode.CONTINUE }
                 , cbEachResult: (VariableBinding) -> Any = { ResultCode.CONTINUE }
    ) {
        val oid = OID(initOid.toIntArray())
        val target = CommunityTarget().apply {
            community = OctetString("public")
            address = UdpAddress(InetAddress.getByName(addr), 161)
            version = SnmpConstants.version2c
            timeout = 1000
            retries = 5
        }
        val pdu = PDU().apply {
            type = PDU.GETBULK
            maxRepetitions = 100
            nonRepeaters = 0
            add(VariableBinding(oid))
        }
        sendAsync(pdu, target) { walkEachCallback(oid, target, it, cbTerminate, cbEachResponse, cbEachResult) }
    }
}
