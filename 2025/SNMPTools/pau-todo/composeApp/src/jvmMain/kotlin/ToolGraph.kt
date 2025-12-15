package jp.wjg.shokkaa.util

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.koalaplot.core.ChartLayout
import io.github.koalaplot.core.Symbol
import io.github.koalaplot.core.bar.HorizontalBarPlot
import io.github.koalaplot.core.bar.HorizontalBarPlotEntry
import io.github.koalaplot.core.bar.VerticalBarPlot
import io.github.koalaplot.core.line.LinePlot2
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.*
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

fun main() = application {
    val alwaysOnTop = System.getProperty("app.alwaysOnTop", "false").toBoolean()
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

@Serializable
data class D @OptIn(ExperimentalSerializationApi::class) constructor(
    @ProtoNumber(1) val n: Int,
    @ProtoNumber(2) val t1: Int,
    @ProtoNumber(3) val t2: Int
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalSerializationApi::class)
@Composable
fun App() = MaterialTheme {
    var sFile by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val data1 = remember { mutableStateListOf<Point<Int, Int>>() }
    val data2 = remember { mutableStateListOf<Point<Int, Int>>() }

    LaunchedEffect(sFile) {
        loading = true
        if (sFile.isNotEmpty()) {
            runCatching {
                data1.clear()
                data2.clear()
                val path = Path(sFile)
                if (SystemFileSystem.exists(path)) {
                    val srcData = SystemFileSystem.source(path).buffered().readByteArray()
                    val dList = ProtoBuf.decodeFromByteArray<List<D>>(srcData)
                    println(dList)
                    data1.addAll(dList.map { Point(it.n, it.t1) })
                    data2.addAll(dList.map { Point(it.n, it.t2) })
                }
            }.onFailure {
                it.printStackTrace()
            }
            loading = false
        }
    }

    Scaffold {
        Column(modifier = Modifier.padding(it)) {
            DropFileBox(onDrop = { sFile = it }) {
                sendRecvGraph(data1, data2)
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
fun sendLifeGraph(data1: List<D>) {
    XYGraph(
        rememberIntLinearAxisModel(0..data1.maxOf { max(it.t1, it.t2) }),
        rememberIntLinearAxisModel(0..data1.maxOf { it.n }),
    ) {
        val dot = @Composable { c: Color -> Symbol(size = 1.5.dp, fillBrush = SolidColor(c), outlineBrush = null) }
        HorizontalBarPlot(
            data = data1.map { HorizontalBarPlotEntry(it.t1, it.t2) }, symbol = { dot(Color.Blue) },
            yData = TODO(),
            bar = TODO(),
            barWidth = TODO(),
            content = TODO()
        )
    }
}
