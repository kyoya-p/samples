@file:OptIn(ExperimentalSerializationApi::class)

package jp.wjg.shokkaa.snmp

import androidx.compose.animation.core.snap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
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
import io.github.koalaplot.core.ChartLayout
import io.github.koalaplot.core.animation.StartAnimationUseCase
import io.github.koalaplot.core.bar.*
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.*
import kotlinx.coroutines.delay
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.random.Random

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

// Sample
@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun IntFuncBarPlot(range: IntRange, func: (Int) -> Int) {
    val data = remember { mutableStateListOf<Int>() }

    fun barChartEntries(): List<VerticalBarPlotEntry<Int, Int>> {
        return buildList {
//            data.forEachIndexed { index, y ->
//                add(DefaultVerticalBarPlotEntry(index, DefaultBarPosition(0, y)))
//            }
            range.forEach { x ->
                data.add(func(x))
                add(DefaultVerticalBarPlotEntry(x, DefaultBarPosition(0, func(x))))
            }
        }
    }

    val barChartEntries = remember { barChartEntries() }

    ChartLayout(
        title = { "aaaa" }
    ) {
        val XAxisRange = range.start - 1..range.endInclusive + 1
        val YAxisRange = 0..(data.toList().maxOrNull() ?: 0) + 1

        XYGraph(
            xAxisModel = IntLinearAxisModel(
                XAxisRange,
//                minimumMajorTickIncrement = 1,
//                minimumMajorTickSpacing = 10.dp,
//                minViewExtent = 3,
//                minorTickCount = 0
            ),
            yAxisModel = IntLinearAxisModel(
                YAxisRange,
//                minimumMajorTickIncrement = 1,
//                minorTickCount = 0
            ),
//            xAxisStyle = rememberAxisStyle(
//                tickPosition = tickPositionState.horizontalAxis,
//                color = Color.LightGray
//            ),
//            xAxisLabels = {
//                if (!thumbnail) {
//                    AxisLabel(it.toString(0), Modifier.padding(top = 2.dp))
//                }
//            },
//            xAxisTitle = { "Position in Sequence" },
//            yAxisStyle = rememberAxisStyle(tickPosition = tickPositionState.verticalAxis),
//            yAxisLabels = {
//                if (!thumbnail) AxisLabel(it.toString(1), Modifier.absolutePadding(right = 2.dp))
//            },
//            yAxisTitle = { "Value" },
            verticalMajorGridLineStyle = null
        ) {
            VerticalBarPlot(
                data = barChartEntries,
                bar = { index, _, _ ->
                    DefaultBar(
                        brush = SolidColor(Color.Blue),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(barChartEntries[index].y.end.toString())
                    }
                },
                barWidth = 0.8f
            )
        }
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun IntListBarPlot(data: List<Int>, range: IntRange = 0..data.lastIndex) = KoalaPlotTheme(animationSpec = snap()) {

    val barChartEntries2 = range.map { x -> DefaultVerticalBarPlotEntry(x, DefaultBarPosition(0, data[x])) }

    ChartLayout(
        title = { "aaaa" }
    ) {
        val XAxisRange = range.start - 1..range.endInclusive + 1
        val YAxisRange = 0..(data.toList().maxOrNull() ?: 0) + 1

        XYGraph(
            xAxisModel = IntLinearAxisModel(
                XAxisRange,
//                minimumMajorTickIncrement = 1,
//                minimumMajorTickSpacing = 10.dp,
//                minViewExtent = 3,
//                minorTickCount = 0
            ),
            yAxisModel = IntLinearAxisModel(
                YAxisRange,
//                minimumMajorTickIncrement = 1,
//                minorTickCount = 0
            ),
//            xAxisStyle = rememberAxisStyle(
//                tickPosition = tickPositionState.horizontalAxis,
//                color = Color.LightGray
//            ),
//            xAxisLabels = {
//                if (!thumbnail) {
//                    AxisLabel(it.toString(0), Modifier.padding(top = 2.dp))
//                }
//            },
//            xAxisTitle = { "Position in Sequence" },
//            yAxisStyle = rememberAxisStyle(tickPosition = tickPositionState.verticalAxis),
//            yAxisLabels = {
//                if (!thumbnail) AxisLabel(it.toString(1), Modifier.absolutePadding(right = 2.dp))
//            },
//            yAxisTitle = { "Value" },
            verticalMajorGridLineStyle = null
        ) {
            VerticalBarPlot(
                data = barChartEntries2,
                bar = { index, _, _ ->
                    DefaultBar(
                        brush = SolidColor(Color.Blue),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(barChartEntries2[index].y.end.toString())
                    }
                },

                barWidth = 1.001f,
            )
        }
    }
}

// データポイントの型定義（X軸：時間[Long], Y軸：値[Float]）
data class TimeData(val timestamp: Long, val value: Float)

// AI:Animationしない
@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun DynamicTimeSeriesChart() {
    var plotData by remember { mutableStateOf(listOf<TimeData>()) }
    var currentTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            val nextValue = Random.nextFloat() * 100f
            val newData = TimeData(currentTime, nextValue)
            plotData = (plotData + newData).takeLast(20)
            currentTime++
            delay(1000)
        }
    }

    ChartLayout {
        XYGraph(
            xAxisModel = rememberLongLinearAxisModel(
                range = (currentTime - 20)..currentTime + 20
            ),
            yAxisModel = rememberFloatLinearAxisModel(range = 0f..100f),
//                xAxisTitle = { Text("Time (s)") },
//                yAxisTitle = { Text("Value") }
        ) {
            VerticalBarPlot(
                data = plotData.map { DefaultVerticalBarPlotEntry(it.timestamp, DefaultBarPosition(0f, it.value)) },
                bar = { index, _, _ ->
                    DefaultBar(
                        brush = SolidColor(Color.Blue),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(plotData[index].timestamp.toString()) }
                },
                startAnimationUseCase = StartAnimationUseCase(
                    executionType = StartAnimationUseCase.ExecutionType.None
                ),
                barWidth = 0.8f,
            )
        }
    }
}