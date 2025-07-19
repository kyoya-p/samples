package jp.wjg.shokkaa.mcp

import ai.koog.agents.core.agent.AIAgent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.input.key.Key.Companion.Enter
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.reflect.full.createType

data class Log(val msg: String, val from: String)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() = MaterialTheme {
    val chatLogs_ = remember { mutableStateListOf<Log>() }
    with(Logger { a, b -> chatLogs_.add(Log(a, b)) }) {
        val scope = rememberCoroutineScope()
        var process by remember { mutableStateOf<Process?>(null) }
        var aiAgent by remember { mutableStateOf<AIAgent<String, String>?>(null) }
        var query by remember { mutableStateOf("") }
        val lazyListState = rememberLazyListState()

        fun runQuery() = scope.launch {
            log(query, "Query")
            val res = aiAgent?.run(query)
            log(res ?: "AIAgent disabled", "Answer")
        }

        @Composable
        fun queryButton() = IconButton(
            onClick = ::runQuery,
            enabled = aiAgent != null,
        ) { Icon(Icons.AutoMirrored.Filled.Send, "Send") }

        Scaffold {
            LaunchedEffect(chatLogs_.size) { if (chatLogs_.isNotEmpty()) lazyListState.animateScrollToItem(chatLogs_.lastIndex) }
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = { Text("MCP Box") },
                    actions = {
                        McpEnableSlider(
                            appSettings.mcpServices[0],
                            process = process,
                        ) { newProcess ->
                            process = newProcess
                            scope.launch { aiAgent = if (newProcess != null) createAiAgent(newProcess) else null }
                        }
                        IconButton(onClick = SettingsDialog()) { Icon(Icons.Default.Settings, "Setting") }
                    })
                LazyColumn(Modifier.fillMaxWidth().weight(1f), state = lazyListState) {
                    items(chatLogs_) {
                        SelectionContainer { Text("${it.from}: ${it.msg}") }
                    }
                }
                TextField(
                    query,
                    trailingIcon = { queryButton() },
                    modifier = Modifier.fillMaxWidth().onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp && keyEvent.key == Enter) runQuery()
                        false
                    },
                    onValueChange = { query = it },
                )
            }
        }
    }
}

@Composable
context(logger: Logger)
fun McpEnableSlider(mcp: McpService, process: Process?, onResult: (process: Process?) -> Unit) {
    var enabled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(enabled) {
        if (enabled) {
            logger.log("McpEnableSlider: Power On", "Application")
            onResult(mcp.startWithEnvironment())
        } else if (process != null) {
            logger.log("McpEnableSlider: Power Off", "Application")
            process.descendants()?.forEach { it.destroyForcibly() }
            onResult(null)
        }
    }
    Switch(
        checked = process != null,
        onCheckedChange = {
            enabled = it
        }
    )
}

@Composable
fun SettingsDialog() = AppDialog {
    DynamicForm(
        v = appSettings,
        kType = AppSettings::class.createType(),
        label = "Settings",
        isNullable = false,
    ) { appSettings = it as AppSettings }
}

@Composable
inline fun <T> MutableState<T?>.prepare(crossinline f: suspend () -> T): MutableState<T?> =
    apply { LaunchedEffect(Unit) { value = f() } }

@Suppress("UNCHECKED_CAST")
inline fun <T : Any> MutableState<T?>.ifNull(onBreak: () -> Unit): MutableState<T> =
    apply { value ?: onBreak() } as MutableState<T>