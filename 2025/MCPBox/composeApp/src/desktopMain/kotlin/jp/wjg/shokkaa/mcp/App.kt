package jp.wjg.shokkaa.mcp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import org.jetbrains.compose.ui.tooling.preview.Preview

data class Chat(val query: String, val Answer: String)

@Composable
@Preview
fun App() = MaterialTheme {
    var query by remember { mutableStateOf("") }
    val charList = remember { mutableStateListOf<Chat>() }
    val lazyListState = rememberLazyListState()

    @Composable
    fun queryButton() = IconButton(onClick = {
        charList.add(Chat(query, "Answer:"))
        query = ""
    }) {
        Icon(Icons.AutoMirrored.Filled.Send, "Send")
    }
    Scaffold(
//        floatingActionButton = { queryButton() }
    ) {
        LaunchedEffect(charList.size) {
            if (charList.isNotEmpty()) lazyListState.animateScrollToItem(charList.lastIndex)
        }
        Column(Modifier.fillMaxSize()) {
            // LazyColumnのラストにスクロールしたい
            LazyColumn(Modifier.fillMaxWidth().weight(1f), state = lazyListState) {
                items(charList) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(it.query)
                        Text(it.Answer)
                    }
                }
            }
            TextField(
                query,
                trailingIcon = { queryButton() },
                modifier = Modifier.fillMaxWidth().onKeyEvent {
                    if (it.key == Key.Enter && it.isCtrlPressed) {
                        charList.add(Chat(query, "Answer:"))
                        query = ""
                        true
                    } else false
                }, onValueChange = { query = it })
        }
    }
}
