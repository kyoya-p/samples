package jp.wjg.shokkaa.mcp

import ai.koog.agents.core.agent.AIAgent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

data class Chat(val msg: String, val from: String)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() = MaterialTheme {
    val scope = rememberCoroutineScope()
    var agent by remember { mutableStateOf<AIAgent?>(null) }
    LaunchedEffect(Unit) { agent = createAgent() }
    var query by remember { mutableStateOf("") }
    val chatLogs = remember { mutableStateListOf<Chat>() }
    val lazyListState = rememberLazyListState()

    @Composable
    fun queryButton() = IconButton(onClick = {
        chatLogs.add(Chat(query, "Answer"))
        scope.launch {
            val res = agent?.runAndGetResult(query)
            chatLogs.add(Chat(res ?: "no answer", "Order"))
        }
        query = ""
    }) {
        if (agent == null) CircularProgressIndicator()
        else Icon(Icons.AutoMirrored.Filled.Send, "Send")
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MCP Box") },
                actions = { IconButton(onClick = {}) { Icon(Icons.Default.Settings, "Setting") } })
        },
//        floatingActionButton = { queryButton() }
    ) {
        LaunchedEffect(chatLogs.size) {
            if (chatLogs.isNotEmpty()) lazyListState.animateScrollToItem(chatLogs.lastIndex)
        }
        Column(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxWidth().weight(1f), state = lazyListState) {
                items(chatLogs) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(it.msg)
                        Text(it.from)
                    }
                }
            }
            TextField(
                query,
                trailingIcon = { queryButton() },
                modifier = Modifier.fillMaxWidth().onKeyEvent {
                    if (it.key == Key.Enter && it.isCtrlPressed) {
                        chatLogs.add(Chat(query, "Order"))
                        query = ""
                        true
                    } else false
                }, onValueChange = { query = it })
        }
    }
}
