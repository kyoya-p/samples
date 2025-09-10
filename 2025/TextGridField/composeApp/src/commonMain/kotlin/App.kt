package v2

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
expect fun defaultTextStyle(): TextStyle

@Composable
fun App() = MaterialTheme {
    var textField by remember { mutableStateOf(TextFieldValue("||l|\n|あ|ああ|")) }
    var newTextField by remember { mutableStateOf(textField) }
    var text by remember { mutableStateOf("||1|11|111|あ|ああ|") }
    Column {
        TextField(
            textField,
            modifier = Modifier.padding(4.dp),
            textStyle = defaultTextStyle(),
            onValueChange = { newTextField = it }
        )
        Text(textField.text.alignPipes(), style = defaultTextStyle())
        textField = TextFieldValue(
            text = newTextField.text.alignPipes(),
            selection = newTextField.selection,
            composition = newTextField.composition,
        )

        HorizontalDivider()
        Text("FlexPillar-0.2.250909")
    }
}

typealias Grid<T> = List<List<T>>

val textWidth = mutableMapOf<String, Int>()

//@Composable
//fun TextStyle.textWidth(s: String): Int {
//    if (textWidth.containsKey(s)) return textWidth[s]!!
//    val w = rememberTextMeasurer().measure(s, this).size.width
//    textWidth[s] = w
//    return w
//}

@Composable
fun String.textWidth(style: TextStyle): Int {
//    if (textWidth.containsKey(s)) return textWidth[s]!!
    val w = rememberTextMeasurer().measure(this, style).size.width
//    textWidth[s] = w
    return w
}

//@Composable
//fun TextGridField(
//    value: TextFieldValue,
//    textStyle: TextStyle = defaultTextStyle(),
//    paddingLetter: Char = ' ',
//    onValueChange: (TextFieldValue) -> Unit
//) = Column(Modifier.padding(4.dp)) {
//    var textField by remember { mutableStateOf(value) }
//    var newTextField by remember { mutableStateOf(value) }
//    TextField(value, textStyle = textStyle, onValueChange = { textField = it })
//    Text(
//        """
//len=${value.text.length}
//selection=${value.selection}
//===========
//${value.text.alignPipes()}}
//===========
//ver: 0.2
//        """.trimIndent(), style = textStyle
//    )
//    newTextField = TextFieldValue(
//        text = value.text.alignPipes(),
//        selection = value.selection, //キャレット座標保持
//        composition = value.composition
//    )
//
//    LaunchedEffect(newTextField) {
//        onValueChange(newTextField)
//    }
//}

/*
各行の左端から各 "|"(パイプ)の位置(位置はUI上の位置でありString.textWidth()関数で算出される)を同じ位置にするよう"|"の前に空白を挿入する
- 文字列を"\n"で分離しrows:List<String>とする
- 各行の条件:
  - 左端からn文字までの文字幅は String.textWidth()関数で算出される。
  - 左端からm番目の"|"(パイプ)は、他の行のm番目の"|"(パイプ) と同じ位置(左端からの文字幅の位置)に最も近く配置される
- 返値は各行を"\n"をセパレータとして結合したもの
   */
@Composable
fun String.alignPipes(
    style: TextStyle = defaultTextStyle(),
    paddingLetter: Char = ' ',
): String = runCatching {
    val grid = split("\n").map { it.split("|") }

    @Composable
    fun Grid<Int>.max(): List<Int> = (0..<maxOf { it.size }).map { x -> maxOf { it.getOrNull(x) ?: 0 } }

    // 各行n番目の"|"までの文字幅の最大値を返す
    val maxPipePos = grid.map { r ->
        r.mapIndexed { x, c -> r.take(x + 1).joinToString("|").textWidth(style) }
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
