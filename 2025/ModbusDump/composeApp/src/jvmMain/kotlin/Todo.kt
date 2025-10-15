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
import kotlinx.serialization.Serializable
import read
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
hostAdr: $hostAdr
unitId: $unitId
mode: ${mode.face}
registerAdr: $regAdr
registerCount: $regCount
bulkSize: $nAcq

"""
                runCatching {
                    val master = ModbusTCPMaster(hostAdr)
                    master.connect()
                    master.read(params).forEach { result += "$it\n" }
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
    DropdownMenu(mode, MBMode.entries, "Mode", { onChange(copy(mode = it)) }) { _, e -> e.face }
    IntField(regAdr, label = "Data address", onValueChange = { onChange(copy(regAdr = it)) })
    IntField(regCount, label = "# of Data items", onValueChange = { onChange(copy(regCount = it)) })
    IntField(nAcq, label = "Data acquisition quantity", onValueChange = { onChange(copy(nAcq = it)) })
}

/*

fun ModbusTCPMaster.read(params: AppData, cb: (MBRes) -> Unit): SummaryResult = with(params) {
    val nAcqAligned = if (mode2 is ReadMode.REGISTERS) (nAcq + 1) / mode2.nWord * mode2.nWord else nAcq
    for (ofs in regAdr..<regAdr + regCount step nAcqAligned) {
        runCatching {
            when (mode2) {
                is ReadMode.BIT_VECTORS -> when (mode2) {
                    is ReadMode.BIT_VECTORS.COILS -> readCoils(unitId, ofs, nAcq)
                    is ReadMode.BIT_VECTORS.INPUT_DISCRETES -> readInputDiscretes(unitId, ofs, nAcq)
                }.forEachIndexed { i, m -> cb(MBRes.OK(ofs, m)) }

                is ReadMode.REGISTERS -> when (mode2) {
                    is ReadMode.REGISTERS.HOLDING_REGISTERS -> readMultipleRegisters(unitId, ofs, nAcqAligned)
                    is ReadMode.REGISTERS.INPUT_REGISTERS -> readInputRegisters(unitId, ofs, nAcqAligned)
                }.asSequence().chunked(2).forEachIndexed { i, e ->
                    val v = e.fold(0U) { a, e -> a * 0x10000U + e.value.toUInt() }.toInt()
                    cb(MBRes.OK(ofs + i * 2, v.toText()))
                }
            }
        }.onFailure { ex -> cb(MBRes.Error(ofs, "${ofs.toAddress()}, ${ex.message}", ex)) }
    }
    return@with SummaryResult(0, 0, 0)
}

*/
@Serializable
sealed class ReadMode() {
    @Serializable
    sealed class BIT_VECTORS : ReadMode() {
        @Serializable
        class COILS : BIT_VECTORS()

        @Serializable
        class INPUT_DISCRETES : BIT_VECTORS()
    }

    @Serializable
    sealed class REGISTERS() : ReadMode() {
        @Serializable
        class HOLDING_REGISTERS() : REGISTERS()

        @Serializable
        class INPUT_REGISTERS() : REGISTERS()
    }
    @Serializable
    sealed class REGISTERSx2() : ReadMode() {
        @Serializable
        class HOLDING_REGISTERS() : REGISTERSx2()

        @Serializable
        class INPUT_REGISTERS() : REGISTERSx2()
    }

    companion object {
        val items: Map<ReadMode, String> = mapOf(
            BIT_VECTORS.COILS() to "1.Read Coils",
            BIT_VECTORS.INPUT_DISCRETES() to "2.Read Input Discretes",
            REGISTERS.HOLDING_REGISTERS() to "3.Read Holding Registers",
            REGISTERS.INPUT_REGISTERS() to "4.Read Input Registers",
            REGISTERS.HOLDING_REGISTERS() to "3.Read Holding Registers - x2 word",
            REGISTERS.INPUT_REGISTERS() to "4.Read Input Registers - x2 word"
        )
    }

    val face get() = items[this]!!
}
