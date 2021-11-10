package jp.`live-on`.shokkaa

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File

fun main() {
    val a = ByteArray(256) { it.toByte() }
    println(a.joinToString())
    val b = a.escaped()
    val c = b.unescaped()
    println(c.joinToString())
}


@ExperimentalSerializationApi
fun main2() {
    val devs = jsonSnmp4j.decodeFromStream<List<Device>>(File("samples/res.json").inputStream())
    devs.forEach { d ->
        println((listOf(d.ip) + d.vbl.map { jsonSnmp4j.encodeToString(it.variable) }).joinToString())
    }
}