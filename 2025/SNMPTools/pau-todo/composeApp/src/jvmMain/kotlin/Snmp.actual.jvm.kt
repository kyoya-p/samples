package jp.wjg.shokkaa.snmp

import jp.wjg.shokkaa.snmp.jvm.SnmpImpl
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.transport.DefaultUdpTransportMapping

actual fun createSnmp(bufferSizeByte: Int): jp.wjg.shokkaa.snmp.Snmp {
    val trasport = DefaultUdpTransportMapping()
    trasport.receiveBufferSize = bufferSizeByte
    return SnmpImpl(SnmpBuilder().udp(org.snmp4j.smi.UdpAddress()).tm(trasport).v1().v3().build()!!)
}