package snmp


import org.khronos.webgl.Uint8Array
import snmp.netsnmp.js.Session
import snmp.netsnmp.js.SessionOptions
import snmp.netsnmp.js.Varbind
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

val TARGET_IP = "127.0.0.1" // SNMPエージェントが動作しているホストIP
val COMMUNITY = "public"    // SNMPコミュニティ文字列
val TARGET_OID = "1.3.6.1.2.1.1.1.0" // sysDescr.0 (システム記述)
