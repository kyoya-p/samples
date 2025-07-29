import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.letsPlot.Figure
import org.jetbrains.letsPlot.Stat
import org.jetbrains.letsPlot.geom.geomBar
import org.jetbrains.letsPlot.geom.geomLine
import org.jetbrains.letsPlot.geom.geomPie
import org.jetbrains.letsPlot.geom.geomPolygon
import org.jetbrains.letsPlot.letsPlot
import org.jetbrains.letsPlot.skia.compose.PlotPanel

/*
* https://kotlinlang.org/docs/lets-plot.html#create-a-box-plot
*/

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Lets-Plot in Compose Desktop (median)") {

        val figures = createFigures()

        val preserveAspectRatio = remember { mutableStateOf(false) }
        val figureIndex = remember { mutableStateOf(0) }

        MaterialTheme {
            Column(
                modifier = Modifier.fillMaxSize().padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
            ) {
                PlotPanel(
                    figure = letsPlot(dataXYZ())
//                    +geomPolygon { x = "X"; y = "Y"; }
                            + geomLine { x = "X"; y = "X"; }
                            + geomLine { x = "X"; y = "Z" }
                    , modifier = Modifier.fillMaxSize()
                ) { computationMessages ->
                    computationMessages.forEach { println("[DEMO APP MESSAGE] $it") }
                }
            }
        }
    }
}

@Composable
fun DemoList(
    options: List<String>,
    selectedIndex: MutableState<Int>
) {
    Column(
        modifier = Modifier.width(IntrinsicSize.Max)
    ) {
        options.forEachIndexed { index, name ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .selectable(
                        selected = selectedIndex.value == index,
                        onClick = { selectedIndex.value = index }
                    )
            ) {
                RadioButton(
                    onClick = { selectedIndex.value = index },
                    selected = index == selectedIndex.value,
                )
                Text(name)
            }
        }
    }
}

fun createFigures(): List<Pair<String, Figure>> {
    return listOf(
        "Density" to plotDataXY(),
        "Bar" to barPlot(),
        "Pie" to piePlot(),
    )
}

fun dataXYZ(): Map<String, List<Double>> {
    val rand = java.util.Random()
    val n = 50
    val xs = List(n) { rand.nextGaussian() }
    val ys = List(n) { rand.nextGaussian() }
    val zs = List(n) { rand.nextGaussian() }
    return mapOf(
        "X" to xs,
        "Y" to ys,
        "Z" to zs,
    )
}

private fun plotDataXY(): Figure {
    val rand = java.util.Random()
    val n = 200
    val xs = List(n) { rand.nextGaussian() }
    val ys = List(n) { rand.nextGaussian() }
    val data = mapOf<String, Any>(
        "X" to xs,
        "Y" to ys
    )

//    return letsPlot(data) + geomDensity { x = "x" }
    return letsPlot(data) + geomPolygon {
        x = "X"
        y = "Y"
    }
}

fun barPlot(): Figure {
    val data = mapOf(
        "time" to listOf("Lunch", "Lunch", "Dinner", "Dinner", "Dinner")
    )

    return letsPlot(data) +
            geomBar(alpha = 0.5) {
                x = "time"
                color = "time"
                fill = "time"
            }
}

fun piePlot(): Figure {
    val data = mapOf(
        "name" to listOf('a', 'b', 'c', 'd', 'b'),
        "value" to listOf(40, 90, 10, 50, 20)
    )
    return letsPlot(data) +
            geomPie(stat = Stat.identity, size = 0.7, sizeUnit = "x") {
                slice = "value"
                fill = "name"
            }
}