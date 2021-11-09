package jp.`live-on`.shokkaa

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromStream
import java.io.File

fun main() {
    val devs = jsonSnmp4j.decodeFromStream<List<Device>>(File("res.json").inputStream())
    devs.forEach { d ->
        println((listOf(d.ip) + d.vbl.map { jsonSnmp4j.encodeToString(it.variable) }).joinToString())
    }
}