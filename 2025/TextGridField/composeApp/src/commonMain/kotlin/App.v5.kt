package v5

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import defaultTextStyle
import kotlin.math.abs

@Composable
fun App() = MaterialTheme {
    val textMeasurer = rememberTextMeasurer()
    val defaultTextStyle = defaultTextStyle()
    var textField by remember { mutableStateOf(TextFieldValue("|aaa|wwww|\n|,,,,|MMMMM|")) }
    var newTextField by remember { mutableStateOf(textField) }
    Column {
        var t by remember { mutableStateOf("test") }
        var tf by remember { mutableStateOf(TextFieldValue("|aaa|wwww|\n|,,,,|MMMMM|")) }

        TextField(tf, textStyle = defaultTextStyle, onValueChange = {
            tf = it.alignPipes(textMeasurer, defaultTextStyle, ' ')
        })
        Text(tf.selection.toString())
        val xy1 = tf.text.posToXY(tf.selection.end)
        val xy2 = tf.text.posToXY(tf.selection.start)
        Text("$xy1 - $xy2")

        val p1 = tf.text.xyToPos(xy1.first, xy1.second)
        val p2 = tf.text.xyToPos(xy2.first, xy2.second)
        Text("$p1 - $p2")

        HorizontalDivider()
        Text("TextGridField-0.4.250918")
    }
}

typealias Grid<T> = List<List<T>>

const val zeroWidthSpace = '\u200B'

@Composable
fun String.textWidth(style: TextStyle): Int {
    val w = rememberTextMeasurer().measure(this, style).size.width
    return w
}

fun String.textWidth(textMeasurer: TextMeasurer, style: TextStyle): Int {
    val w = textMeasurer.measure(this, style).size.width
    return w
}


fun TextFieldValue.X(textMeasurer: TextMeasurer, style: TextStyle, paddingLetter: Char): TextFieldValue {
//    fun String.insMarker(p: Int) = take(p) + zeroWidthSpace + drop(p)
//    fun String.delMarker(): Pair<Int, String> = indexOf(zeroWidthSpace).let { it to take(it) + drop(it + 1) }
    var (s, e) = with(selection) { if (start > end) end to start else start to end }
    return TextFieldValue(
        text = text.alignPipes(textMeasurer, style, paddingLetter),
        selection = selection,
        composition = composition,
    )
}
// '\n'で区切られた各行のそれぞれの'|'の位置をあわせるよう'|'の前に' '(スぺース)を挿入
// その際、alinePipes()は使用せず、TextFieldValue.insert(), TextFieldValue.remove()を使用
fun TextFieldValue.aline(textMeasurer: TextMeasurer, style: TextStyle, paddingLetter: Char): TextFieldValue {

}

fun TextFieldValue.insert(pos: Int, str: String): TextFieldValue {
    val newText = text.take(pos) + str + text.drop(pos)
    val (p1, p2) = selection.start to selection.end
    val newSelection = when {
        p1 > pos -> TextRange(p1 + str.length, p2 + str.length)
        p2 > pos -> TextRange(p1, p2 + str.length)
        else -> selection
    }
    return TextFieldValue(newText, newSelection, composition)
}

fun TextFieldValue.remove(pos: Int, len: Int): TextFieldValue {
    val newText = text.take(pos) + text.drop(pos + len)
    val (p1, p2) = selection.start to selection.end
    val newSelection = when {
        p1 > pos -> TextRange(p1 - len, p2 - len)
        p2 > pos -> TextRange(p1, p2 - len)
        else -> selection
    }
    return TextFieldValue(newText, newSelection, composition)
}

fun String.posToXY(p: Int): Pair<Int, Int> =
    p - take(p).dropLastWhile { it != '\n' }.count() to take(p).count { it == '\n' }

fun String.xyToPos(x: Int, y: Int): Int = split('\n').take(y).sumOf { it.length + 1 } + x


// 各行の各"|"の前に空白を挿入しUI上の位置を揃える
fun TextFieldValue.alignPipes(
    textMeasurer: TextMeasurer,
    style: TextStyle,
    paddingLetter: Char,
): TextFieldValue {
    var (x1, y1) = text.posToXY(selection.start)
    var (x2, y2) = text.posToXY(selection.end)

    val grid = text.split("\n").map { it.split("|") }
    fun Grid<Int>.max(): List<Int> = (0..<maxOf { it.size }).map { x -> maxOf { it.getOrNull(x) ?: 0 } }

    // 各行n番目の"|"までの文字幅の最大値を返す
    val maxPipePos = grid.map { r ->
        r.mapIndexed { x, _ -> r.take(x + 1).joinToString("|").textWidth(textMeasurer, style) }
    }.max()

    // 指定文字幅に最も近くなるよう文字列に" "を加える
    fun String.padding(w: Int, onIns: (Int) -> Unit): String {
        var s1 = this
        while (true) {
            val s2 = "$s1$paddingLetter"
            if (abs(w - s1.textWidth(textMeasurer, style)) <= abs(w - s2.textWidth(textMeasurer, style))) return s1
            onIns(s1.count())
            s1 = s2
        }
    }

    // 各列の左端からの文字幅を最大の文字幅に合わせる
    val rows = MutableList(grid.size) { "" }
    for (x in 0..<maxPipePos.size) {
        grid.indices.forEach { y ->
            if (x != 0) rows[y] += "|"
            val s1 = rows[y] + grid[y].getOrElse(x) { "" }
            val s2 = s1.dropLastWhile { it == paddingLetter }
            if (y == y1 && s2 < s1 && x1 > s1.length) x1 -= s1.length - s2.length
            if (y == y2 && s2 < s1 && x2 > s1.length) x2 -= s1.length - s2.length
            rows[y] = s2
        }
        val colWidth = rows.maxOf { it.textWidth(textMeasurer, style) }

        for (y in grid.indices) {
            val r1 = grid[y].getOrNull(x)?.dropLastWhile { it == paddingLetter } ?: continue
            rows[y] = rows[y].padding(colWidth) { ipos ->
                if (y == y1 && ipos < x1) ++x1
                if (y == y2 && ipos < x2) ++x2
            }
//            val c = grid[y].getOrNull(x)?.dropLastWhile { it == paddingLetter } ?: continue
//            rows[y] += "|" + r1
        }
    }
    return TextFieldValue(
        text = rows.joinToString("\n"),
        selection = TextRange(text.xyToPos(x1, y1), text.xyToPos(x2, y2)),
        composition = composition,
    )
}


// 各行の各"|"の前に空白を挿入しUI上の位置を揃える
fun String.alignPipes(
    textMeasurer: TextMeasurer,
    style: TextStyle,
    paddingLetter: Char,
): String = runCatching {
    val grid = split("\n").map { it.split("|") }
    fun Grid<Int>.max(): List<Int> = (0..<maxOf { it.size }).map { x -> maxOf { it.getOrNull(x) ?: 0 } }

    // 各行n番目の"|"までの文字幅の最大値を返す
    val maxPipePos = grid.map { r ->
        r.mapIndexed { x, _ -> r.take(x + 1).joinToString("|").textWidth(textMeasurer, style) }
    }.max()

    // 指定文字幅に最も近くなるよう文字列に" "を加える
    fun String.padding(w: Int): String {
        var s1 = this
        while (true) {
            val s2 = "$s1$paddingLetter"
            if (abs(w - s1.textWidth(textMeasurer, style)) <= abs(w - s2.textWidth(textMeasurer, style))) return s1
            s1 = s2
        }
    }

    // 各列の左端からの文字幅を最大の文字幅に合わせる
    val rows = MutableList(grid.size) { grid[it][0].dropLastWhile { it == paddingLetter } }
    for (x in 1..<maxPipePos.size) {
        val mw = rows.maxOf { it.textWidth(textMeasurer, style) }
        for (y in grid.indices) {
            rows[y] = rows[y].padding(mw)
            val c = grid[y].getOrNull(x)?.dropLastWhile { it == paddingLetter } ?: continue
            rows[y] += "|" + c
        }
    }
    return rows.joinToString("\n")
}.getOrElse { it.stackTraceToString() }
