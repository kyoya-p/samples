@file:OptIn(ExperimentalUnsignedTypes::class)

package jp.wjg.shokkaa.snmp.jvm

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.mp.MessageProcessingModel
import org.snmp4j.smi.Address
import org.snmp4j.smi.Integer32
import org.snmp4j.smi.Null
import org.snmp4j.smi.OID
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.Variable
import org.snmp4j.smi.VariableBinding
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import kotlin.onFailure
import kotlin.time.Clock.System.now
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

typealias SecurityModel = Int
typealias SecurityLevel = Int

interface MessageProcessingModel : MessageProcessingModel {
    typealias MPv1 = org.snmp4j.mp.MPv1
    typealias MPv2c = org.snmp4j.mp.MPv2c
    typealias MPv3 = org.snmp4j.mp.MPv3
    typealias Code = Int

    companion object {
        val v1: Code = 0
        val v2c: Code = 1
        val v3: Code = 3
    }
}

val SecurityModel.ANY get() = 0
val SecurityModel.SNMPv1 get() = 1
val SecurityModel.SNMPv2c get() = 2
val SecurityModel.USM get() = 3
val SecurityModel.TSM get() = 4

val SecurityLevel.NOAUTH_NOPRIV get() = 1
val SecurityLevel.AUTH_NOPRIV get() = 2
val SecurityLevel.AUTH_PRIV get() = 3

class SnmpImpl(val snmp: Snmp) : jp.wjg.shokkaa.snmp.Snmp {
    override fun send(req: jp.wjg.shokkaa.snmp.Request, onResult: (jp.wjg.shokkaa.snmp.Outcome) -> Unit) =
        sendImpl(req, onResult)

    override fun cancel(req: jp.wjg.shokkaa.snmp.Request) = TODO("Not yet implemented")
}
//
//fun Request(
//    strAdr: String = "127.0.0.1",
//    port: Int = 161,
//    udpAdr: UdpAddress = UdpAddress(InetAddress.getByName(strAdr), port),
//
//    reqType: Int = SnmpPDU.GETNEXT,
//
//    commStrV1: String = "public",
//    comm: OctetString = OctetString(commStrV1),
//
//    nRetry: Int = 5,
//    interval: Duration = 5.seconds,
//    intervalMs: Long = interval.inWholeMilliseconds,
//    target: SnmpTarget = SnmpTarget(udpAdr, comm).apply { retries = nRetry; timeout = intervalMs },
//
//    strOid: String = ".1.3.6",
//    strOids: List<String> = listOf(strOid),
//    oids: Array<OID> = strOids.map { OID(it) }.toTypedArray(),
//    vbl: List<VariableBinding> = oids.map { VariableBinding(it) },
//    reqId: UInt? = null,
//    pdu: SnmpPDU = SnmpPDU(reqType, vbl).apply { reqId?.let { reqId -> requestID = Integer32(reqId.toInt()) } },
//
//    userData: Any? = null
//) = Request(target = target, pdu = pdu, userData = userData)
//
//fun createSnmpPDU(
//    type: Int = SnmpPDU.GETNEXT,
//    strOid: String? = null,
//    strOids: List<String> = listOfNotNull(strOid),
//    oids: Array<OID> = strOids.map { OID(it) }.toTypedArray(),
//    vbl: List<VariableBinding> = oids.map { VariableBinding(it) },
//    reqId: Int? = null,
//    ei: Int = 0, es: Int = 0,
//) = SnmpPDU(type, vbl).apply {
//    reqId?.let { reqId -> requestID = Integer32(reqId) }
//    errorIndex = ei
//    errorStatus = es
//}


//sealed class Result(val request: Request) {
//    class Response(request: Request, val received: SnmpEvent) : Result(request)
//    class Timeout(request: Request) : Result(request)
//
//    fun onResponse(op: (Response) -> Any?): Result = also { if (it is Response) op(it) }
//    fun getResponseOrNull() = if (this is Response) received else null
//    fun onTimeout(op: (Timeout) -> Any?): Result = also { if (it is Timeout) op(it) }
//}

//suspend fun snmpUnicast(
//    strAdr: String = "127.0.0.1",
//    reqType: Int = SnmpPDU.GETNEXT,
//    strOid: String = ".1.3.6",
//    strOids: List<String> = listOf(strOid),
//    oids: Array<OID> = strOids.map { OID(it) }.toTypedArray(),
//
//    port: Int = 161,
//    udpAdr: UdpAddress = UdpAddress(InetAddress.getByName(strAdr), port),
//    commStrV1: String = "public",
//    retries: Int = 5,
//    interval: Duration = 5.seconds,
//    intervalMs: Long = interval.inWholeMilliseconds,
//    target: SnmpTarget = SnmpTarget(udpAdr, OctetString(commStrV1)).apply {
//        this.retries = retries
//        timeout = intervalMs
//    },
//    vbl: List<VariableBinding> = oids.map { VariableBinding(it) },
//    pdu: SnmpPDU = SnmpPDU(reqType, vbl),
//    req: Request = Request(target, pdu),
//    snmp: Snmp = defaultSenderSnmp,
//) = suspendCoroutine { conti ->
//    val listener: ResponseListener = object : ResponseListener {
//        override fun <A : Address?> onResponse(event: ResponseEvent<A>) {
//            (event.source as Snmp).cancel(event.request, this)
//            val response = event.response
//            if (response == null) {
//                conti.resume(Result.Timeout(req))
//            } else {
//                @Suppress("UNCHECKED_CAST")
//                conti.resume(Result.Response(req, event as SnmpEvent))
//            }
//        }
//    }
//    snmp.send(req.pdu, req.target, null, listener)
//}

fun String.toIpV4UInt(): UInt = split(".").fold(0u) { a, e -> a * 0x100u + e.toUInt() }
fun UInt.toIpV4String(): String = (3 downTo 0).map { (this shr (it * 8)) and 0xffu }.joinToString(".")
fun ByteArray.toIpV4Adr(): InetAddress = InetAddress.getByAddress(this)
fun String.toIpV4Adr(): InetAddress = InetAddress.getByName(this)

@OptIn(ExperimentalTime::class)
class RateLimiter @OptIn(ExperimentalTime::class) constructor(
    val interval: Duration,
    val amount: Int = 1,
    val origin: Instant = now(),
) {
    private val tokenChannel = Channel<Unit>(Channel.RENDEZVOUS)

    init {
        CoroutineScope(Dispatchers.Default).launch {
            var nextExecutionTime = origin
            while (isActive) {
                val currentTime = now()
                val passedIntervals = ((currentTime - origin) / interval + 1.0).toInt()
                nextExecutionTime = origin + interval * passedIntervals
                if (currentTime < nextExecutionTime) delay(nextExecutionTime - currentTime)
                tokenChannel.send(Unit)
            }
        }
    }

    suspend fun <T> runRateLimited(block: suspend () -> T): T {
        tokenChannel.receive()
        return block()
    }
}

fun <T> Flow<T>.rateLimited(rateLimiter: RateLimiter): Flow<T> = flow {
    collect { v -> rateLimiter.runRateLimited { emit(v) } }
}.cancellable()

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.throttle(rateLimiter: RateLimiter): Flow<T> =
    flow { chunked(rateLimiter.amount).collect { rateLimiter.runRateLimited { it.forEach { emit(it) } } } }

//@OptIn(ExperimentalTime::class, ExperimentalUnsignedTypes::class, ExperimentalCoroutinesApi::class)
//fun snmpSendFlow(
//    ipRange: IpV4RangeSet,
//    snmp: Snmp = defaultSenderSnmp,
//    rps: Int,
//    scrambleBlock: Int,
//    requestBuilder: (ip: UInt) -> Request = { ip -> Request(ip.toIpV4String(), reqId = ip.toUInt()) },
//): Flow<Result> = callbackFlow {
//    fun Flow<UInt>.scrambled(nBits: Int): Flow<UInt> = flow {
//        val mask = (1U shl nBits) - 1U
//        val rndTable = (0U..mask).shuffled()
//        for (w in 0U..mask) collect { ip -> if (ip and mask == rndTable[w.toInt()]) emit(ip) }
//    }
//
//
//    var count = 0UL
//    val total = ipRange.totalLength().toULong()
//    val listener = object : ResponseListener {
//        override fun <A : Address?> onResponse(r: ResponseEvent<A>) {
//            ++count
//            runCatching {
//                val reqAdr = r.response?.requestID?.value?.toUInt()
//                val resAdr = r.peerAddress?.toByteArray()?.toUByteArray()?.toIpV4ULong()?.toUInt()
//
//                @Suppress("UNCHECKED_CAST")
//                val res = when {
//                    resAdr != null && resAdr == reqAdr -> Result.Response(r.userObject as Request, r as SnmpEvent)
//                    else -> Result.Timeout(r.userObject as Request)
//                }
//                trySendBlocking(res) // 受信スレッドをブロックする、send制限が必要
//                snmp.cancel(r.request, this)
//            }.onFailure { it.printStackTrace() }
//            if (count >= total) close()
//        }
//    }
//    ipRange.asFlatSequence().asFlow().scrambled(scrambleBlock).chunked(rps).rateLimited(RateLimiter(1.seconds))
//        .collect { ips ->
//            ips.forEachIndexed { i, ip ->
//                val req = requestBuilder(ip)
//                snmp.send(req.pdu, req.target, req, listener)
//            }
//        }
//    awaitClose {}
//}.cancellable()


//fun org.snmp4j.smi.Address.toUdpAddress(): UdpAddress {
//    val sa = socketAddress
//    if (sa is java.net.InetSocketAddress) {
//        when (val a = sa.address) {
//            is java.net.Inet4Address -> return UdpAddress(jp.wjg.shokkaa.snmp.Inet4Address(a.address), sa.port)
//            is java.net.Inet6Address -> return UdpAddress(jp.wjg.shokkaa.snmp.Inet6Address(a.address), sa.port)
//            else -> throw IllegalArgumentException("Unknown address type ${a.address}")
//        }
//    } else throw kotlin.IllegalArgumentException("Unknown address: $this")
//}

fun Address.toUdpAddress() = when (val sa = this) {
    is UdpAddress -> when (val a = sa.inetAddress) {
        is Inet4Address -> jp.wjg.shokkaa.snmp.UdpAddress(jp.wjg.shokkaa.snmp.Inet4Address(a.address), sa.port)
        is Inet6Address -> jp.wjg.shokkaa.snmp.UdpAddress(jp.wjg.shokkaa.snmp.Inet6Address(a.address), sa.port)
        else -> throw IllegalArgumentException("Unknown address type ${a.address}")
    }

    else -> throw kotlin.IllegalArgumentException("Unknown address: $this")
}

fun jp.wjg.shokkaa.snmp.Variable.toRaw(): Variable = when (val me = this) {
    is jp.wjg.shokkaa.snmp.OID -> me.toRaw()
    is jp.wjg.shokkaa.snmp.OctetString -> me.toRaw()
    is jp.wjg.shokkaa.snmp.Integer32 -> me.toRaw()
    is jp.wjg.shokkaa.snmp.Null -> me.toRaw()
}

fun jp.wjg.shokkaa.snmp.OID.toRaw(): org.snmp4j.smi.OID = org.snmp4j.smi.OID(value.toIntArray())
fun jp.wjg.shokkaa.snmp.OctetString.toRaw(): org.snmp4j.smi.OctetString = org.snmp4j.smi.OctetString(value)
fun jp.wjg.shokkaa.snmp.Integer32.toRaw(): org.snmp4j.smi.Integer32 = org.snmp4j.smi.Integer32(value)
fun jp.wjg.shokkaa.snmp.Null.toRaw(): org.snmp4j.smi.Null = org.snmp4j.smi.Null()

fun jp.wjg.shokkaa.snmp.PDU.toRaw() = PDU(
    pduType,
    vbl.map { VariableBinding(it.oid.toRaw(), it.value.toRaw()) }
)

fun jp.wjg.shokkaa.snmp.InetAddress.toRaw() = InetAddress.getByAddress(address)
fun jp.wjg.shokkaa.snmp.UdpAddress.toRaw() = UdpAddress(address.toRaw(), port)
fun jp.wjg.shokkaa.snmp.Target.toRaw() = CommunityTarget<UdpAddress>(
    udpAddress.toRaw(),
    communityTarget.toRaw()
)

fun SnmpImpl.sendImpl(req: jp.wjg.shokkaa.snmp.Request, onResult: (jp.wjg.shokkaa.snmp.Outcome) -> Unit) {
    println("IP:${req.target.udpAddress.address.address.toList()}:${req.target.udpAddress.port} comm:${String(req.target.communityTarget.value)}") //todo

    val listener = object : ResponseListener {
        override fun <A : Address?> onResponse(r: ResponseEvent<A>) {
            runCatching {
                when (val res = r.response) {
                    null -> onResult(jp.wjg.shokkaa.snmp.Timeout(r.userObject as jp.wjg.shokkaa.snmp.Request))
                    else if r.peerAddress != null -> onResult(
                        jp.wjg.shokkaa.snmp.Response(
                            r.userObject as jp.wjg.shokkaa.snmp.Request,
                            r.peerAddress?.toUdpAddress()!!,
                            res.toPDU()
                        )
                    )

                    else -> onResult(
                        jp.wjg.shokkaa.snmp.Exception(
                            r.userObject as jp.wjg.shokkaa.snmp.Request,
                            IllegalStateException()
                        )
                    )
                }
//                rawSnmp.cancel(res.request, this)
            }.onFailure { onResult(jp.wjg.shokkaa.snmp.Exception(r.userObject as jp.wjg.shokkaa.snmp.Request, it)) }
        }
    }
    val t = req.target.toRaw()
    println("IP:${t.address?.inetAddress} port:${t.address?.port}") //todo

    snmp.send(req.pdu.toRaw(), req.target.toRaw(), req, listener)
}

fun Variable.toVariable() = when (this) {
    is OID -> jp.wjg.shokkaa.snmp.OID(value.toList())
    is OctetString -> jp.wjg.shokkaa.snmp.OctetString(value)
    is Integer32 -> jp.wjg.shokkaa.snmp.Integer32(value)
    is Null -> jp.wjg.shokkaa.snmp.Null
    else -> throw IllegalStateException("Unknown variable $this")
}

fun VariableBinding.toVariable() = jp.wjg.shokkaa.snmp.VariableBinding(
    oid = jp.wjg.shokkaa.snmp.OID(oid.value.toList()),
    value = variable.toVariable()
)

fun PDU.toPDU() = jp.wjg.shokkaa.snmp.PDU(
    pduType = type,
    vbl = variableBindings.map {
        jp.wjg.shokkaa.snmp.VariableBinding(it.oid.toVariable() as jp.wjg.shokkaa.snmp.OID, it.variable.toVariable())
    },
    reqId = requestID.value,
    errorIndex = errorIndex,
    errorStatus = errorStatus,
)