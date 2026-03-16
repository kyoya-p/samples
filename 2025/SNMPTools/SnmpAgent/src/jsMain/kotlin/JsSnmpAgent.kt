import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.json

external class NetSnmp {
    fun createSession(host: String, port: Int, community: String): Session
}

external class Session {
    fun get(oids: Array<String>, callback: (error: dynamic, varbinds: dynamic) -> Unit)
    fun set(oids: Array<dynamic>, callback: (error: dynamic, varbinds: dynamic) -> Unit)
    fun getNext(oids: Array<String>, callback: (error: dynamic, varbinds: dynamic) -> Unit)
    fun close()
}

class JsSnmpAgent : SnmpAgent {
    private val mibData = mutableMapOf<String, String>()
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var session: Session? = null
    private val netSnmp = js("require('net-snmp')") as NetSnmp

    override suspend fun start(port: Int) {
        _isRunning.value = true
        session = netSnmp.createSession("127.0.0.1", port, "public")
        println("SNMP Agent started on port $port")
    }

    override suspend fun stop() {
        _isRunning.value = false
        session?.close()
        session = null
        println("SNMP Agent stopped")
    }

    override suspend fun setValue(oid: String, value: String) {
        val session = session ?: throw IllegalStateException("Session not initialized")
        val result = suspendCancellableCoroutine { continuation ->
            session.set(arrayOf(json(
                "oid" to oid,
                "type" to 2, // OctetString
                "value" to value
            ))) { error, varbinds ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.toString()))
                } else {
                    mibData[oid] = value
                    continuation.resume(Unit)
                }
            }
        }
        println("Set OID: $oid = $value")
    }

    override suspend fun getValue(oid: String): String? {
        val session = session ?: throw IllegalStateException("Session not initialized")
        return suspendCancellableCoroutine { continuation ->
            session.get(arrayOf(oid)) { error, varbinds ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.toString()))
                } else {
                    val value = varbinds[0].value.toString()
                    mibData[oid] = value
                    continuation.resume(value)
                }
            }
        }
    }

    override suspend fun getNextValue(oid: String): Pair<String, String>? {
        val session = session ?: throw IllegalStateException("Session not initialized")
        return suspendCancellableCoroutine { continuation ->
            session.getNext(arrayOf(oid)) { error, varbinds ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.toString()))
                } else {
                    val nextOid = varbinds[0].oid.toString()
                    val value = varbinds[0].value.toString()
                    mibData[nextOid] = value
                    continuation.resume(nextOid to value)
                }
            }
        }
    }
} 