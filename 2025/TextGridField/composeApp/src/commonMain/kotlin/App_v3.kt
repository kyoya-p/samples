package v3

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import defaultTextStyle
import kotlin.math.abs

@Composable
fun App() = MaterialTheme {
    var textField by remember { mutableStateOf(TextFieldValue("||l|\n|あ|ああ|")) }
    var newTextField by remember { mutableStateOf(textField) }
    Column {
        TextField(
            textField,
            modifier = Modifier.padding(4.dp),
            textStyle = defaultTextStyle(),
            onValueChange = { newTextField = it }
        )
        textField = newTextField.align(defaultTextStyle(), paddingLetter = ' ')

        HorizontalDivider()
        Text("TextGridField-0.3.250918")
    }
}

typealias Grid<T> = List<List<T>>

const val zeroWidthSpace = '\u200B'

@Composable
fun String.textWidth(style: TextStyle): Int {
    val w = rememberTextMeasurer().measure(this, style).size.width
    return w
}

// カーソル位置にマーカーを埋め込む
@Composable
fun TextFieldValue.align(style: TextStyle = defaultTextStyle(), paddingLetter: Char = ' ') : TextFieldValue {
    fun String.insMarker(p: Int) = take(p) + zeroWidthSpace + drop(p)
    fun String.delMarker(): Pair<Int, String> = indexOf(zeroWidthSpace).let { it to take(it) + drop(it + 1) }
    val (s, e) = with(selection) { if (start > end) end to start else start to end }
    return text.insMarker(e).insMarker(s).alignPipes(style, paddingLetter)
        .delMarker().let { (p1, s1) ->
            s1.delMarker().let { (p2, s2) ->
                TextFieldValue(
                    text = s2,
                    selection = with(selection) { if (start > end) TextRange(p2, p1) else TextRange(p1, p2) }
                )
            }
        }
}

// 各行の各"|"の前に空白を挿入しUI上の位置を揃える
@Composable
fun String.alignPipes(
    style: TextStyle,
    paddingLetter: Char,
): String = runCatching {
    val grid = split("\n").map { it.split("|") }

    @Composable
    fun Grid<Int>.max(): List<Int> = (0..<maxOf { it.size }).map { x -> maxOf { it.getOrNull(x) ?: 0 } }

    // 各行n番目の"|"までの文字幅の最大値を返す
    val maxPipePos = grid.map { r ->
        r.mapIndexed { x, _ -> r.take(x + 1).joinToString("|").textWidth(style) }
    }.max()

    // 指定文字幅に最も近くなるよう文字列に" "を加える
    @Composable
    fun String.padding(w: Int): String {
        var s1 = this
        while (true) {
            val s2 = "$s1 "
            if (abs(w - s1.textWidth(style)) <= abs(w - s2.textWidth(style))) return s1
            s1 = s2
        }
    }

    // 各列の左端からの文字幅を最大の文字幅に合わせる
    val rows = MutableList(grid.size) { grid[it][0].dropLastWhile { it == paddingLetter } }
    for (x in 1..<maxPipePos.size) {
        val mw = rows.maxOf { it.textWidth(style) }
        for (y in grid.indices) {
            rows[y] = rows[y].padding(mw)
            val c = grid[y].getOrNull(x)?.dropLastWhile { it == paddingLetter } ?: continue
            rows[y] += "|" + c
        }
    }
    return rows.joinToString("\n")
}.getOrElse { it.stackTraceToString() }
