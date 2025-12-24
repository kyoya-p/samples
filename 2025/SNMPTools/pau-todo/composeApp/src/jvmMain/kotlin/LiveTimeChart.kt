package io.github.koalaplot.sample

import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.ChartLayout
import io.github.koalaplot.core.Symbol
import io.github.koalaplot.core.legend.LegendLocation
import io.github.koalaplot.core.line.LinePlot2
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.CategoryAxisModel
import io.github.koalaplot.core.xygraph.DefaultPoint
import io.github.koalaplot.core.xygraph.IntLinearAxisModel
import io.github.koalaplot.core.xygraph.LongLinearAxisModel
import io.github.koalaplot.core.xygraph.Point
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberAxisStyle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Clock.System.now
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


internal val padding = 8.dp
internal val paddingMod = Modifier.padding(padding)

@Composable
fun ChartTitle(title: String) {
    Column {
        Text(
            title,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun AxisLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        label,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1
    )
}

data class GraphData(
    val xAxis: ImmutableList<Long>,
    val yAxis: ImmutableList<Int>,
    val points: ImmutableList<Point<Long, Int>>,
    val xRange: LongRange,
)

private const val HistorySize = 60
private val UpdateDelay = 500.milliseconds

@OptIn(ExperimentalKoalaPlotApi::class, ExperimentalTime::class)
@Composable
fun LiveTimeChart(takeSnapshot: () -> Point<Long, Int>) {
    var info by remember {
//        val x = kotlin.time.Clock.System.now().toEpochMilliseconds()
//        val y = if (Random.nextBoolean()) 1 else -1
        mutableStateOf(
//            GraphData(
//                persistentListOf(Instant.fromEpochMilliseconds(x).toString()),
//                persistentListOf(y),
//                persistentListOf(DefaultPoint(Instant.fromEpochMilliseconds(x).toString(), y)),
//                -1..1
//            )
            GraphData(
                persistentListOf(),
                persistentListOf(),
                persistentListOf(),
                xRange = (now() - 60.seconds).toEpochMilliseconds()..now().toEpochMilliseconds(),
            )
        )
    }
    var yRange by remember { mutableStateOf(1) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Main) {
            var count = 0
            while (isActive) {
                count++
                delay(UpdateDelay)
//                val yLast = info.value.yAxis.last()
//                val yNext = if (Random.nextBoolean()) yLast + 1 else yLast - 1
                val next = takeSnapshot()
                val x = now().toEpochMilliseconds()
                info = info.copy(
                    xAxis = info.xAxis.toPersistentList().mutate { it.add(x) }.takeLast(HistorySize)
                        .toImmutableList(),
                    yAxis = info.yAxis.toPersistentList().mutate { it.add(next.y) }.takeLast(HistorySize)
                        .toImmutableList(),
                    points = info.points.toPersistentList()
                        .mutate { it.add(next) }
                        .takeLast(HistorySize).toImmutableList(),
                )
            }
        }
    }

    ChartLayout(
        modifier = paddingMod.padding(horizontal = 8.dp),
        title = { ChartTitle("Live Time Chart") },
        legendLocation = LegendLocation.NONE,
    ) {
        XYGraph(
//            xAxisModel = CategoryAxisModel(info.xAxis),
            xAxisModel = LongLinearAxisModel((now() - 60.seconds).toEpochMilliseconds()..now().toEpochMilliseconds()),
            yAxisModel = IntLinearAxisModel(
                range = 0..(yRange + 10),
                minimumMajorTickSpacing = 50.dp
            ),
            xAxisLabels = { AxisLabel(Instant.fromEpochMilliseconds(it).toString(), Modifier.padding(top = 2.dp)) },
            yAxisLabels = { AxisLabel(it.toString()) },
            yAxisTitle = { },
            xAxisTitle = { },
            xAxisStyle = rememberAxisStyle(labelRotation = 45),
        ) {
            LinePlot2(
                data = info.points,
                symbol = { Symbol(fillBrush = SolidColor(Color.Black)) },
                animationSpec = TweenSpec(0)
            )
        }
    }
}