package modbusdump.modbusdump.v3

import com.ghgande.j2mod.modbus.procimg.SimpleInputRegister
import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage
import com.ghgande.j2mod.modbus.slave.ModbusSlave
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import modbusdump.ModbusDevice
import kotlin.coroutines.resume

fun main() {
    val slave: ModbusSlave = ModbusSlaveFactory.createTCPSlave(502, 5)
    val spi = SimpleProcessImage(1)
    spi.addRegister(0, SimpleInputRegister(1234))
    slave.addProcessImage(1, spi)
    slave.open()
    Thread.sleep(10_000)
    slave.close()
}

suspend fun ModbusDevice.startModbusDevice() = suspendCancellableCoroutine { continuation ->
    val slave: ModbusSlave = ModbusSlaveFactory.createTCPSlave(listenPort, 5)
    val spi = SimpleProcessImage(1)
    spi.addRegister(0, SimpleInputRegister(1234)) //TODO
    slave.addProcessImage(1, spi)
    continuation.invokeOnCancellation {
        println("Canceled!!!") //TODO
        slave.close()
    }
    slave.open()
    continuation.resume(Unit)
}
