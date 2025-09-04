import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
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
import kotlin.math.abs
import kotlin.text.split

@Composable
fun App() = MaterialTheme {
    val customFont = FontFamily(Font(Res.font.MPLUS1Code_Medium, FontWeight.Normal))
    val textStyle = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        fontFamily = customFont,
    )

    @Composable
    fun String.textWidth(): Int {
        val measuredResult = rememberTextMeasurer()
        return measuredResult.measure(this, textStyle).size.width
    }


    @Composable
    fun Grid<String>.colWidth(): List<Int> =
        (0..<cols()).map { y -> maxOf { it.getOrNull(y)?.trim()?.textWidth() ?: 0 } }

    @Composable
    fun <T, R> Grid<T>.flatMap(op: @Composable (T) -> R): Grid<R> = map { it.map { op(it) } }

    @Composable
    fun <T, R> Grid<T>.flatMapIndexed(op: @Composable (y: Int, x: Int, T) -> R): Grid<R> =
        mapIndexed { y, r -> r.mapIndexed { x, c -> op(y, x, c) } }

    fun <T> Grid<T>.toText() = joinToString("\n") { it.joinToString("|") }


    fun String.splitGrid(): Grid<String> = split("\n").map { it.trim().split('|').map { it } }

    Column {
        var text by remember { mutableStateOf("||l|\n|あ||") }
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
//        Text(text.alignPipes(textStyle), style = textStyle)
        text=text.alignPipes(textStyle)
    }
}

/*
- 文字列を"\n"で分離しrows:List<String>とする
- 文字幅はUI状の文字幅であり textMeasurer.measure() で算出される。単位はdp
- y行目について:
  - 左端からm番目の位置の"|"(パイプ)の直前までの文字列の文字幅を pipePos[y][m] とする
  - 各yについて最大のpipePos[y][m]を maxPipePos[m]とする
- 各行の各mについて左端から順に:
  - 左端から”｜”直前までの文字列の文字幅と、それに" "(スペース)を追加した文字列の文字幅のどちらがmaxPipePos[m]に近いか比較し、
  - " "を追加したほうが近ければ左端からの文字列に" "を追加したものについて、上の比較を繰り返す
  - 追加しないほうが近ければ、処理を終える
- 返値は各行を"\n"をセパレータとして結合したもの
*/
@Composable
fun String.align(style: TextStyle): String {
    val rows = split("\n")
    val textMeasurer = rememberTextMeasurer()

    fun String.textWidth(): Int = textMeasurer.measure(this, style).size.width

    val pipePositions = mutableListOf<MutableList<Int>>()
    for (row in rows) {
        val currentRowPipePositions = mutableListOf<Int>()
        var currentWidth = 0
        for ((index, char) in row.withIndex()) {
            currentWidth += char.toString().textWidth()
            if (char == '|') {
                currentRowPipePositions.add(currentWidth)
            }
        }
        pipePositions.add(currentRowPipePositions)
    }

    val maxPipes = pipePositions.maxOfOrNull { it.size } ?: 0
    val alignedPipePositions = MutableList(maxPipes) { mutableListOf<Int>() }

    for (rowPipePositions in pipePositions) {
        for ((pipeIndex, pos) in rowPipePositions.withIndex()) {
            if (pipeIndex < maxPipes) {
                alignedPipePositions[pipeIndex].add(pos)
            }
        }
    }

    val averagePipePositions = alignedPipePositions.map { positions ->
        if (positions.isEmpty()) 0 else positions.sum() / positions.size
    }

    val alignedRows = rows.map { row ->
        val newRow = StringBuilder()
        var currentTextWidth = 0
        var pipeIndex = 0

        for (char in row) {
            if (char == '|') {
                val targetPos =
                    if (pipeIndex < averagePipePositions.size) averagePipePositions[pipeIndex] else currentTextWidth
                val spaceWidth = " ".textWidth()
                val padding = if (spaceWidth > 0) (targetPos - currentTextWidth) / spaceWidth else 0
                repeat(padding) { newRow.append(' ') }
                newRow.append(char)
                currentTextWidth = newRow.toString().textWidth()
                pipeIndex++
            } else {
                newRow.append(char)
                currentTextWidth += char.toString().textWidth()
            }
        }
        newRow.toString()
    }
    return alignedRows.joinToString("\n")
}

typealias Grid<T> = List<List<T>>

fun <T> Grid<T>.cols() = maxOf { it.size }
fun String.toGrid(): Grid<String> = split("\n").map { it.split("|") }


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

    @Composable
    fun Grid<Int>.max(): List<Int> = (0..<cols()).map { x -> maxOf { it.getOrNull(x) ?: 0 } }
    val maxPipePos = grid.pipePos().max()

    // 指定文字幅に最も近くなるよう文字列に" "を加える
    @Composable
    fun String.padding(w: Int): String {
        var s1 = this
        while (true) {
            val s2 = "$s1 "
            val w1 = s1.textWidth()
            val w2 = s2.textWidth()
            if (abs(w - w1) <= abs(w - w2)) return s1
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
