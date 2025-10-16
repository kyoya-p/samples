package v2

import AppData
import DropdownMenu
import IntField
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import appHome
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import config
import forEachIndexed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone.Companion.currentSystemDefault
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import toAddress
import toText
import v2.ReadType.*
import kotlin.collections.fold
import kotlin.sequences.chunked
import kotlin.sequences.forEachIndexed
import kotlin.stackTraceToString
import kotlin.time.Clock.System.now
import kotlin.time.ExperimentalTime
import kotlin.use


@OptIn(ExperimentalTime::class)
@Composable
fun UI() = MaterialTheme {
    val x = config.copy(result = "")
    var params: AppData by remember { mutableStateOf(x) }
    var result by remember { mutableStateOf(params.result) }
    var run by remember { mutableStateOf(false) }
    Row {
        Column {
            params.ParameterField { params = it }
            Row {
                Button(onClick = { run = !run }) { if (!run) Text("Dump") else Text("Stop") }
                if (run) CircularProgressIndicator()
            }
        }
        Spacer(Modifier.width(4.dp))
        SelectionContainer {
            Box(modifier = Modifier.fillMaxSize()) {
                val scrollState = rememberScrollState()
                Text(
                    text = result,
                    modifier = Modifier.verticalScroll(state = scrollState).fillMaxSize(),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace)
                )
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(scrollState = scrollState)
                )
            }
        }
    }
    LaunchedEffect(run) {
        if (run) with(params) {
            launch(Dispatchers.Default) {
                result = """date: ${now().toLocalDateTime(currentSystemDefault())}
host-adr: $hostAdr
unit-id: $unitId
read-mode: ${mode2.face}
register-adr: $regAdr
register-count: $regCount
bulk-size: $nAcq

"""
                runCatching {
                    val master = ModbusTCPMaster(hostAdr)
                    master.connect()
//                    master.read(params).forEach { result += "$it\n" }
                    master.read(params) { result += "${it.message}\n" }
                    master.disconnect()
                }.onFailure { result += "${it.message}\n${it.stackTraceToString()}" }
                val dt = now().toLocalDateTime(currentSystemDefault())
                    .format(LocalDateTime.Format { year(); monthNumber(); day(); hour(); minute(); second() })
                SystemFileSystem.sink(
                    Path(
                        appHome,
                        "moddump-$dt-${hostAdr.filter { it.isDigit() || it == '.' }}-$unitId.log"
                    )
                ).buffered()
                    .use { it.writeString(result) }
                run = false
            }
        }
    }
    config = params
}


@Composable
fun AppData.ParameterField(onChange: (AppData) -> Unit) = Column {
    TextField(
        hostAdr,
        label = { Text("Device address") },
        singleLine = true,
        isError = !hostAdr.trim().all { it.isDigit() || it == '.' },
        onValueChange = { onChange(copy(hostAdr = it)) })
    IntField(unitId, label = "Unit ID", onValueChange = { onChange(copy(unitId = it)) })
    DropdownMenu<ReadType>(
        selectedOption = mode2,
        options = ReadType.entries.map { it },
        label = "Read Mode",
        onChange = { onChange(copy(mode2 = it)) },
    ) { _, e -> e.face }
    IntField(regAdr, label = "Data address", onValueChange = { onChange(copy(regAdr = it)) })
    IntField(regCount, label = "# of Data items", onValueChange = { onChange(copy(regCount = it)) })
    IntField(nAcq, label = "Data acquisition quantity", onValueChange = { onChange(copy(nAcq = it)) })
}

sealed class MBRes(val adr: Int, val message: String) {
    class OK(adr: Int, message: String) : MBRes(adr, message)
    class Error(adr: Int, message: String, val ex: Throwable) : MBRes(adr, message)
}

class SummaryResult(val success: Int, val error: Int, val total: Int)

fun ModbusTCPMaster.read(params: AppData, cb: (MBRes) -> Unit): SummaryResult = with(params) {
    val chunk = when (mode2) {
        HOLDING_REGISTERS_X2, INPUT_REGISTERS_X2 -> 2
        else -> 1
    }
    val nAcqAligned = (nAcq + (chunk - 1)) / chunk * chunk
    for (ofs in regAdr..<regAdr + regCount step nAcqAligned) {
        runCatching {
            when (mode2) {
                COILS -> readCoils(unitId, ofs, nAcq)
                INPUT_DISCRETES -> readInputDiscretes(unitId, ofs, nAcq)
                else -> null
            }?.forEachIndexed { i, m -> cb(MBRes.OK(ofs, m)) }
            when (mode2) {
                HOLDING_REGISTERS, HOLDING_REGISTERS_X2 -> readMultipleRegisters(unitId, ofs, nAcqAligned)
                INPUT_REGISTERS, INPUT_REGISTERS_X2 -> readInputRegisters(unitId, ofs, nAcqAligned)
                else -> null
            }?.run {
                asSequence().chunked(chunk).forEachIndexed { i, e ->
                    val v = e.fold(0U) { a, e -> a * 0x10000U + e.value.toUInt() }.toInt()
                    cb(MBRes.OK(ofs + i * chunk, v.toText(chunk)))
                }
            }
        }.onFailure { ex -> cb(MBRes.Error(ofs, "${ofs.toAddress()}, ${ex.message}", ex)) }
    }
    return@with SummaryResult(0, 0, 0)
}

enum class ReadType(val id: Int, val face: String) {
    COILS(1, "1.Read Coils"),
    INPUT_DISCRETES(2, "2.Read Input Discretes"),
    HOLDING_REGISTERS(3, "3.Read Holding Registers"),
    HOLDING_REGISTERS_X2(13, "3.Read Holding Registers x2"),
    INPUT_REGISTERS(4, "4.Read Input Registers"),
    INPUT_REGISTERS_X2(14, "4.Read Input Registers x2"),
}
