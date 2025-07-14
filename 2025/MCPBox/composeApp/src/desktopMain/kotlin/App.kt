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
import androidx.compose.ui.Modifier
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.head
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import rmmx.AppDialog
import rmmx.DynamicForm
import kotlin.reflect.full.createType
import kotlin.time.Duration.Companion.seconds

data class Chat(val msg: String, val from: String)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() = MaterialTheme {
    val chatLogs = remember { mutableStateListOf<Chat>() }
    val scope = rememberCoroutineScope()
//    var agent by remember { mutableStateOf<AIAgent?>(null) }
    var mcpConnected by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        chatLogs.add(Chat("Node.js Environment: Downloading...", "Application"))
        runCatching {
            setupNodejsEnvironment()
            chatLogs.add(Chat("Node.js Environment: Complete.", "Application"))
        }.onFailure {
            it.printStackTrace()
            chatLogs.add(Chat("Node.js Environment: Failed. ${it.stackTraceToString()}", "Application"))
        }

        // serviceUrlが接続可能(TCP接続可能)であることをチェック(Ktor HttpClient使用)
        while (true) {
            runCatching {
                HttpClient(CIO).head(appSettings.mcpServices[0].serviceUrl)
                chatLogs.add(Chat("MCP Service: Connection successful.", "Application"))
                mcpConnected = true
            }.onFailure {
                chatLogs.add(Chat("MCP Service: Connection failed. ${it.message}", "Application"))
                mcpConnected = false
            }
            delay(30.seconds)
        }
    }
    var query by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()

    @Composable
    fun queryButton() = IconButton(
        onClick = {
            scope.launch {
                chatLogs.add(Chat(query, "Query"))
//                val res = agent?.runAndGetResult(query)
                val res = runAIAgent(query)
                chatLogs.add(Chat(res ?: "AIAgent disabled", "Answer"))
            }
        },
//        enabled = agent != null,
        enabled = mcpConnected,
    ) { Icon(Icons.AutoMirrored.Filled.Send, "Send") }

    Scaffold {
        LaunchedEffect(chatLogs.size) {
            if (chatLogs.isNotEmpty()) lazyListState.animateScrollToItem(chatLogs.lastIndex)
        }
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(),
                title = { Text("MCP Box") },
                actions = {
                    McpEnableSlider(appSettings.mcpServices[0], onMsg = { chatLogs.add(Chat(it, "Application")) }) {
                        chatLogs.add(Chat(if (it) "Succeeded" else "Failed", "Application"))
                    }
                    IconButton(onClick = SettingsDialog()) { Icon(Icons.Default.Settings, "Setting") }
                })
            LazyColumn(Modifier.fillMaxWidth().weight(1f), state = lazyListState) {
                items(chatLogs) {
                    Text("${it.from}: ${it.msg}")
                }
            }
            TextField(
                query,
                trailingIcon = { queryButton() },
                modifier = Modifier.fillMaxWidth()
//                    .onKeyEvent {
//                        if (it.key == Key.Enter && it.isCtrlPressed) {
//                            chatLogs.add(Chat(query, "Order"))
//                            query = ""
//                            false
//                        } else false
//                    },
                , onValueChange = { query = it })
        }
    }
}

@Composable
fun McpEnableSlider(mcp: Mcp, onMsg: (msg: String) -> Unit = {}, onResult: (succeeded: Boolean) -> Unit) {
    var enabled by remember { mutableStateOf(false) }
    var process: Process? by remember { mutableStateOf(null) }
//    var agent: AIAgent? = null
    LaunchedEffect(enabled) {
        if (enabled && process == null) {
            val cmd = mcp.command.split(" ").run { listOf("$nodeJsDir\\${first()}") + drop(1) }
            onMsg("MCP Service starting.. :$cmd")
            process = startNodeProcess(cmd)
            launch { process?.await { onMsg(it) } }
            onMsg("MCP Service started. {PID:${process?.pid()}}")
//            repeat(20) {
//                createAIAgent()?.let {
//                    onMsg("MCP Agent created :$it")
//                    return@LaunchedEffect onResult(it)
//                }
//                delay(1.seconds)
//            }
//            onMsg("MCP Agent creating failed. timeout")
            onResult(true)
        } else if (!enabled && process != null) {
            onMsg("MCP Service terminated. {PID:${process?.pid()}}")
            process?.descendants()?.forEach { it.destroyForcibly() }
            process = null
            onResult(false)
        }
    }
    Switch(
        checked = enabled,
        onCheckedChange = { enabled = it }
    )
}

@Composable
fun SettingsDialog() = AppDialog {
    DynamicForm(
        v = appSettings,
        kType = AppSettings::class.createType(),
        label = "Settings",
        isNullable = false,
//    labelWidget = TODO()
    ) { appSettings = it as AppSettings }
}

@Composable
inline fun <T> MutableState<T?>.prepare(crossinline f: suspend () -> T): MutableState<T?> =
    apply { LaunchedEffect(Unit) { value = f() } }

@Suppress("UNCHECKED_CAST")
inline fun <T : Any> MutableState<T?>.ifNull(onBreak: () -> Unit): MutableState<T> =
    apply { value ?: onBreak() } as MutableState<T>