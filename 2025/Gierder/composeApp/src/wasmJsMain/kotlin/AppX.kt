package old

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import girder.library.resources.MPLUS1Code_Medium
import girder.library.resources.Res
import org.jetbrains.compose.resources.Font


@Composable
fun AppX() = MaterialTheme {
    val customFont = FontFamily(Font(Res.font.MPLUS1Code_Medium, FontWeight.Normal),)

    val textStyle = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        fontFamily = customFont,
    )
    Column {
        var text by remember { mutableStateOf("|i|l|\n|w|M|") }
        println(text)
        var grid: List<List<String>> by remember { mutableStateOf(text.splitGrid()) }

        TextField(
            text,
            textStyle = textStyle,
            onValueChange = {
                text = it
                grid = it.splitGrid()
            })
        val cols = grid.cols()
        val colWidth = colWidth(grid)
        val gridW = grid.flatMap { it to it.textWidth() }
        Text(gridW.toText())
        Text(colWidth.toString())
        val gridC = grid.flatMapIndexed { _, x, c -> c.padEnd(colWidth[x]) }
        Text(gridC.toText())

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
//    row.mapIndexed { i, cell -> cell.padEnd(colsWidth[i] / 10) }.joinToString("|")
    row.mapIndexed { i, cell -> cell.trim() }.joinToString("|")
    row.mapIndexed { i, cell ->
        println("$i: ${colsWidth[i]}")
        cell.trim()
    }.joinToString("|")
}

@Composable
fun String.textWidth(): Int {
    val style = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
    val measuredResult = rememberTextMeasurer()
//    return measuredResult.measure(this, style).size.width / measuredResult.measure(" ", style).size.width
    return measuredResult.measure(this, style).size.width
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
fun colWidth(g: Grid<String>): List<Int> = (0..<g.cols()).map { y -> g.maxOf { it.getOrNull(y)?.textWidth() ?: 0 } }

fun Grid<String>.cols() = maxOf { it.size }
fun String.splitGrid(): List<List<String>> = split("\n").map { it.trim().split('|').map { it.trim() } }
