package jp.wjg.shokkaa.util

import androidx.compose.foundation.*
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.koalaplot.core.Symbol
import io.github.koalaplot.core.line.LinePlot2
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.Point
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.autoScaleXRange
import io.github.koalaplot.core.xygraph.autoScaleYRange
import io.github.koalaplot.core.xygraph.rememberIntLinearAxisModel
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.collections.plus

fun main() = application {
    val alwaysOnTop = System.getProperty("app.alwaysOnTop", "false").toBoolean()
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

@Serializable
data class D(
    @ProtoNumber(1) val x: Int,
    @ProtoNumber(2) val y1: Int,
    @ProtoNumber(3) val y2: Int
)

@OptIn(ExperimentalFoundationApi::class)
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
                    data1.addAll(dList.map { Point(it.x, it.y1) })
                    data2.addAll(dList.map { Point(it.x, it.y2) })
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
        rememberIntLinearAxisModel(data1.autoScaleXRange()),
        rememberIntLinearAxisModel((data1 + data2).autoScaleYRange()),
    ) {
        val dot = @Composable { c: Color -> Symbol(size = 1.dp, fillBrush = SolidColor(c), outlineBrush = null) }
        LinePlot2(data1, symbol = { dot(Color.Blue) })
        LinePlot2(data2, symbol = { dot(Color.Red) })
    }
}
