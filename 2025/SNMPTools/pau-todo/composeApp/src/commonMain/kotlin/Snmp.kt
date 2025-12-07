package jp.wjg.shokkaa.snmp

sealed class InetAddress(val address: ByteArray)
class Inet4Address(a: ByteArray = ByteArray(4) { 0 }) : InetAddress(a.also { assert(it.size == 4) })
class Inet6Address(a: ByteArray = ByteArray(16) { 0 }) : InetAddress(a.also { assert(it.size == 16) })

data class UdpAddress(val address: InetAddress = Inet4Address(), val port: Int = 0)
data class VariableBinding(val oid: OID, val value: Variable = Null)

sealed class Variable(val syntax: Int)
class OID(val value: List<Int>) : Variable(6)
class OctetString(val value: ByteArray) : Variable(4)
class Integer32(val value: Int) : Variable(2)
object Null : Variable(5)
//class Counter32(val value: Long) : Variable(65)
//class Gauge32(val value: Long) : Variable(66)
//class TimeTicks(val value: Long) : Variable(67)
//class Opaque(val value: ByteArray) : Variable(68)
//class Counter64(val value: Long) : Variable(70)
//class IpAddress(val value: InetAddress) : Variable(64)

data class PDU(
    val pduType: Type = GETNEXT,
    val vbl: List<VariableBinding> = listOf(),
    val reqId: Int = 0,
    val errorIndex: Int = 0,
    val errorStatus: Status = noError,
) {
    typealias Type = Int
    typealias Status = Int

    companion object {

        const val GET: Type = 0
        const val GETNEXT: Type = 1
        const val RESPONSE: Type = 2
        const val SET: Type = 3
        const val V1TRAP: Type = 4
        const val GETBULK: Type = 5
        const val INFORM: Type = 6
        const val TRAP: Type = 7
        const val NOTIFICATION: Type = 7
        const val REPORT: Type = 8

        const val noError: Status = 0
        const val tooBig: Status = 1
        const val noSuchName: Status = 2
        const val badValue: Status = 3
        const val readOnly: Status = 4
        const val genErr: Status = 5
        const val noAccess: Status = 6
        const val wrongType: Status = 7
        const val wrongLength: Status = 8
        const val wrongValue: Status = 9
        const val wrongEncoding: Status = 10
        const val noCreation: Status = 11
        const val inconsistentValue: Status = 12
        const val resourceUnavailable: Status = 13
        const val commitFailed: Status = 14
        const val undoFailed: Status = 15
        const val authorizationError: Status = 16
        const val notWritable: Status = 17
        const val inconsistentName: Status = 18
    }
}

data class Target(val udpAddress: UdpAddress, val communityTarget: OctetString)
data class Request(val target: Target, val pdu: PDU, val userData: Any? = null)

sealed class Outcome(val request: Request)
class Response(request: Request, val peerAddress: UdpAddress, val pdu: PDU) : Outcome(request)
class Timeout(request: Request) : Outcome(request)
class Exception(request: Request, val exception: Throwable) : Outcome(request)

data class SnmpReceived(
    val peerAddress: UdpAddress,
    val mpModelCode: Int,//todo
    val secModel: OctetString,
    val secName: OctetString,
    val secLevel: Int,//todo
    val pdu: PDU,
    val maxSizeResponseScopedPDU: Int,
    val stateReference: Int, //todo
)

interface Snmp {
    fun send(req: Request, onResult: (Outcome) -> Unit)
    fun cancel(req: Request)
}


expect fun createSnmp(bufferSizeByte: Int): Snmp
