package snmp

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.JsName

class Variable(val type: Int, val value: Array<UByte>)
class VariableBind(val oid: OID, val variable: Variable)
class PDU(val requestType: Int, val varbindList: List<VariableBind>)
class Target(val adr: String, val community: String)
class Request(val target: Target, val pdu: PDU)
class Response(val request: Request, response: PDU)
typealias OID = List<Int>


fun OID.oidString() = joinToString(".")
fun String.toOid() = split(".").map { it.toInt() }

//expect fun snmpRequest(request: Request): Response
//expect suspend fun snmpGet(session: Session, oids: Array<String>): Array<Varbind> {
