package netSnmp

/*
 https://github.com/markabrahams/node-net-snmp
 */
external fun require(module: String): dynamic
val snmp = require("net-snmp")

class Snmp() {
    companion object {
        fun createSession(host: String, community: String) =
            Session(snmp.createSession(host = host, community = community))
    }
}

data class VarBind(val oid: String, val type: Int, val value: Any) {
    companion object {
        fun from(vb: dynamic) = VarBind(vb.oid, vb.type, vb.value)
    }
}

class Session(private val session: dynamic) {
    fun getNext(oids: Array<String>, callback: (error: Int?, varbinds: Array<VarBind>) -> Unit) =
        session.getNext(oids, callback)

    fun get(oids: Array<String>, callback: (error: Int?, varbinds: Array<VarBind>) -> Unit) =
        session.get(oids, callback)
}

