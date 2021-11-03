import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.snmp4j.CommunityTarget
import org.snmp4j.Target
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.*
import java.net.InetAddress


@ExperimentalCoroutinesApi
suspend fun main(args: Array<String>) {
    if (args.size == 0) {
        println("Syntax: java -jar walker.jar addr [topOid]")
        return
    }
    val addr = InetAddress.getByName(args[0])!!
    val topOid = OID(args.getOrNull(1) ?: "1")

    val snmp = SnmpBuilder().udp().v1().threads(2).build()!!
    snmp.listen()


    val module = SerializersModule {
        contextual(VariableBindingAsStringSerializer)
    }

    val json = Json {
        prettyPrint = true
        serializersModule = module
    }

    runCatching {
        val topPdu = PDU().apply { variableBindings = listOf(VariableBinding(topOid)) }
        val target = CommunityTarget(UdpAddress(addr, 161), OctetString("public"))
        val res = snmp.walkFlow(topPdu, target).toList()
        println(json.encodeToString(res))
    }.onFailure { it.printStackTrace() }
    snmp.close()
}

@ExperimentalCoroutinesApi
fun Snmp.walkFlow(topPdu: PDU, target: Target<UdpAddress>) = callbackFlow<VariableBinding> {
    generateSequence(topPdu) { reqPdu ->
        reqPdu.type = PDU.GETNEXT
        this@walkFlow.send(reqPdu, target).response
    }.drop(1).takeWhile { pdu ->
        pdu.errorStatus == PDU.noError && pdu.variableBindings[0].oid.startsWith(topPdu.variableBindings[0].oid)
    }.forEach {
        trySend(it.variableBindings[0]).isSuccess
    }
    close()
    awaitClose {}
}

