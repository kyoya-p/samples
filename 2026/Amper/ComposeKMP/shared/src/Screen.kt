import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class UserData(val name: String, val content: String)

@Composable
fun Screen() {
    val dataList = remember {
        listOf(
            UserData("User1", "Text1"),
            UserData("User2", "Text2"),
            UserData("User3", "Text3"),
            UserData("User4", "Text4")
        )
    }

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dataList) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TableCell(text = item.name, modifier = Modifier.weight(1f))
                        TableCell(text = item.content, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun TableCell(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(1.dp, Color.Black)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text)
    }
}
