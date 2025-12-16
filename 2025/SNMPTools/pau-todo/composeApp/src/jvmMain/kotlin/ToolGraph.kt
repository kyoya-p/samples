package jp.wjg.shokkaa.snmp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.Symbol
import io.github.koalaplot.core.line.LinePlot2
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.math.max

//fun main() = application {
//    val alwaysOnTop = System.getProperty("app.alwaysOnTop", "false").toBoolean()
//    Window(onCloseRequest = ::exitApplication) {
//        App2()
//    }
//}

@Serializable
data class Log @OptIn(ExperimentalSerializationApi::class) constructor(
    @ProtoNumber(1) val n: Int, // 要求連番
    @ProtoNumber(2) val t0: Int, // 要求生成時刻
    @ProtoNumber(3) val t1: Int, // 送信時刻
    @ProtoNumber(4) val t2: Int, // コールバック時刻
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalSerializationApi::class)
@Composable
fun App2() = MaterialTheme {
    var sFile by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val data = remember { mutableStateListOf<Log>() }

    LaunchedEffect(sFile) {
        loading = true
        launch(Dispatchers.Default) {
            if (sFile.isNotEmpty()) {
                runCatching {
                    data.clear()
                    val path = Path(sFile)
                    if (SystemFileSystem.exists(path)) {
                        val srcData = SystemFileSystem.source(path).buffered().readByteArray()
                        val dList = ProtoBuf.decodeFromByteArray<List<Log>>(srcData)
                        data.addAll(dList)
                    }
                }.onFailure {
                    it.printStackTrace()
                }
                loading = false
            } else loading = false
        }
    }

    Scaffold {
        if (loading) CircularProgressIndicator()
        Column(modifier = Modifier.padding(it)) {
            DropFileBox(onDrop = { sFile = it }) {
//                sendRecvGraph(data1, data2)
                SendLogGraph(data)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun DropFileBox(onDrop: (String) -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    var showBorder by remember { mutableStateOf(false) }
    val dragAndDropTarget = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                showBorder = true
            }

            override fun onEnded(event: DragAndDropEvent) {
                showBorder = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val data = event.dragData()
                if (data is DragData.FilesList) onDrop(data.readFiles().first().removePrefix("file:/"))
                return true
            }
        }
    }

    Box(
        Modifier.then(
            if (showBorder) Modifier.border(BorderStroke(3.dp, Color.Black))
            else Modifier
        ).dragAndDropTarget(
            shouldStartDragAndDrop = { true },
            target = dragAndDropTarget
        )
    ) { Column { content() } }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun sendRecvGraph(data1: List<Point<Int, Int>>, data2: List<Point<Int, Int>>) {
    XYGraph(
        rememberIntLinearAxisModel((data1 + data2).autoScaleXRange()),
        rememberIntLinearAxisModel(data1.autoScaleYRange()),
    ) {
        val dot = @Composable { c: Color -> Symbol(size = 1.5.dp, fillBrush = SolidColor(c), outlineBrush = null) }
        LinePlot2(data1, symbol = { dot(Color.Blue) })
        LinePlot2(data2, symbol = { dot(Color.Red) })
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun SendLogGraph(data: List<Log>) {
    XYGraph(
        rememberIntLinearAxisModel(0..(data.maxOfOrNull { max(it.t1, it.t2) } ?: 1)),
        rememberIntLinearAxisModel(0..(data.maxOfOrNull { it.n } ?: 1)),
    ) {
        val dot = @Composable { c: Color -> Symbol(size = 1.8.dp, fillBrush = SolidColor(c), outlineBrush = null) }
        data.forEach {
            LinePlot2(
                data = listOf(Point(it.t1, it.n), Point(it.t2, it.n)),
                lineStyle = LineStyle(brush = SolidColor(Color.LightGray), strokeWidth = 1.dp),
                symbol = { p -> dot(if (p.x == it.t1) Color.Blue else Color.Red) }
            )
        }
        LinePlot2(
            data.map { Point(it.t0, it.n) },
            symbol = { dot(Color.Black) }
        )
    }
}
