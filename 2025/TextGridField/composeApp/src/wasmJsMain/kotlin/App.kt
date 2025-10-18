package v1

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import girder.library.resources.MPLUS1Code_Medium
import girder.library.resources.Res
import org.jetbrains.compose.resources.Font
import kotlin.math.abs

@Composable
fun App() = MaterialTheme {
    val customFont = FontFamily(Font(Res.font.MPLUS1Code_Medium, FontWeight.Normal))
    val textStyle = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        fontFamily = customFont,
    )
    Column {
        var textField by remember { mutableStateOf(TextFieldValue("||l|\n|あ||")) }

        TextField(textField, textStyle = textStyle, onValueChange = { textField = it })
        Text(
            """
            len=${textField.text.length}
            selection=${textField.selection}
        """.trimIndent()
        )

        textField = TextFieldValue(
            text = textField.text.alignPipes(textStyle),
            selection = textField.selection, //キャレット座標保持
            composition = textField.composition
        )
    }
}

typealias Grid<T> = List<List<T>>

/*
各行の左端から各 "|"(パイプ)の位置(位置はUI上の位置でありString.textWidth()関数で算出される)を同じ位置にするよう"|"の前に空白を挿入する
- 文字列を"\n"で分離しrows:List<String>とする
- 各行の条件:
  - 左端からn文字までの文字幅は String.textWidth()関数で算出される。
  - 左端からm番目の"|"(パイプ)は、他の行のm番目の"|"(パイプ) と同じ位置(左端からの文字幅の位置)に最も近く配置される
- 返値は各行を"\n"をセパレータとして結合したもの
   */
@Composable
fun String.alignPipes(style: TextStyle): String = runCatching {

    val grid = split("\n").map { it.split("|") }

    @Composable
    fun String.textWidth(): Int = rememberTextMeasurer().measure(this, style).size.width

    @Composable
    fun Grid<String>.pipePos(): Grid<Int> = map { r ->
        r.mapIndexed { x, c -> r.take(x + 1).joinToString("|").textWidth() }

    } // 各行でn番目の"|"までの文字幅
    fun <T> Grid<T>.cols() = maxOf { it.size }

    @Composable
    fun Grid<Int>.max(): List<Int> = (0..<cols()).map { x -> maxOf { it.getOrNull(x) ?: 0 } }
    val maxPipePos = grid.pipePos().max()

    // 指定文字幅に最も近くなるよう文字列に" "を加える
    @Composable
    fun String.padding(w: Int): String {
        var s1 = this
        while (true) {
            val s2 = "$s1 "
            if (abs(w - s1.textWidth()) <= abs(w - s2.textWidth())) return s1
            s1 = s2
        }
    }

    // 各列の左端からの文字幅を最大の文字幅に合わせる
    val rows = MutableList(grid.size) { grid[it][0].trimEnd() }
    for (x in 1..<maxPipePos.size) {
        val mw = rows.maxOf { it.textWidth() }
        for (y in grid.indices) {
            rows[y] = rows[y].padding(mw)
            val c = grid[y].getOrNull(x)?.trimEnd() ?: continue
            rows[y] += "|" + c
        }
    }
    return rows.joinToString("\n")
}.getOrElse { it.stackTraceToString() }
