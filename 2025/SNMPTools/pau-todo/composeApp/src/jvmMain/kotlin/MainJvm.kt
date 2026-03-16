package jp.wjg.shokkaa.snmp


import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.snmp4j.smi.UdpAddress
import org.snmp4j.transport.DefaultUdpTransportMapping
import org.snmp4j.transport.UdpTransportMapping


fun main() = application {
    val alwaysOnTop = System.getProperty("app.alwaysOnTop", "false").toBoolean()
    Window(
        alwaysOnTop = alwaysOnTop,
        onCloseRequest = ::exitApplication,
        title = "",
    ) {
        App()
    }
}


fun createUdpTransport() : UdpTransportMapping {
    val receiveBufferSize = 1024 * 1024
    val transport = DefaultUdpTransportMapping()
    transport.setReceiveBufferSize(receiveBufferSize)
    val listenAddress = UdpAddress()
    return DefaultUdpTransportMapping(listenAddress)
}