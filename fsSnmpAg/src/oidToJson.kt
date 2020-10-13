package mibtool.snmp4jWrapper

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mibtool.toVB
import java.io.File

fun main(args: Array<String>) {
    val vbl = File(args[0]).readLines().drop(1).map { it.toVB() }
    val format = Json { prettyPrint = true }
    println(format.encodeToString(vbl))
}
