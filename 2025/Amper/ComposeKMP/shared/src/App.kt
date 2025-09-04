import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp


val textStyle = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    fontFamily = FontFamily.Monospace,
)

@Composable
fun App() = MaterialTheme {
    val ff = FontFamily(
        Font(Res.font.your_custom_font_regular)
    )

    Column {
        var text by remember { mutableStateOf("|i|l|あ|\n|w|M||") }
        var grid: List<List<String>> by remember { mutableStateOf(text.splitGrid()) }

        TextField(
            text,
            textStyle = textStyle,
            onValueChange = {
                grid = it.splitGrid()
                val nText = grid.toText()
                if (nText != text) text = it
            })

        val colWidth = grid.colWidth()
        val tGrid = grid.flatMap { it.trim() }
        Text(
            """
$colWidth

${tGrid.flatMapIndexed { _, x, it -> it.trim().padEnd((colWidth[x] + 8) / " ".textWidth()) }.toText()}
""", style = textStyle
        )

//        val gridW = grid.flatMap { it to it.textWidth() }
//        val gridC = grid.flatMapIndexed { _, x, c -> c.padEnd(colWidth[x]) }

//        LazyVerticalGrid(columns = GridCells.Fixed(cols)) {
//            items(grid.size * cols) { i ->
//                val row = i / cols
//                val col = i % cols
//                if (row < grid.size && col < grid[row].size) {
//                    Card(modifier = Modifier.padding(8.dp)) {
//                        Text(text = grid[row][col])
//                        Text(text = "${grid[row][col].textWidth()}")
//                    }
//                }
//            }
//        }
    }
}

fun Grid<String>.toText(colsWidth: List<Int>) = joinToString("\n") { row ->
    row.mapIndexed { i, cell -> cell.trim() }.joinToString("|")
    row.mapIndexed { i, cell ->
        println("$i: ${colsWidth[i]}")
        cell.trim()
    }.joinToString("|")
}

@Composable
fun String.textWidth(): Int {
    val measuredResult = rememberTextMeasurer()
    return measuredResult.measure(this, textStyle).size.width
}

fun String.padding(w: Int) {}

typealias Grid<T> = List<List<T>>

@Composable
fun <T, R> Grid<T>.flatMap(op: @Composable (T) -> R): Grid<R> = map { it.map { op(it) } }

@Composable
fun <T, R> Grid<T>.flatMapIndexed(op: @Composable (y: Int, x: Int, T) -> R): Grid<R> =
    mapIndexed { y, r -> r.mapIndexed { x, c -> op(y, x, c) } }

fun <T> Grid<T>.toText() = joinToString("\n") { it.joinToString("|") }

@Composable
fun Grid<String>.colWidth(): List<Int> = (0..<cols()).map { y -> maxOf { it.getOrNull(y)?.trim()?.textWidth() ?: 0 } }

fun Grid<String>.cols() = maxOf { it.size }
fun String.splitGrid(): Grid<String> = split("\n").map { it.trim().split('|').map { it } }
