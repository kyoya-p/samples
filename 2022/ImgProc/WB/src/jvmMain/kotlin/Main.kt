package jp.wjg.shokkaa.whiteboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.flow.MutableStateFlow
import java.awt.image.BufferedImage

typealias WB = BufferedImage

operator fun WB.get(x: Int, y: Int) = getRGB(x, y)
operator fun WB.set(x: Int, y: Int, v: Int) = setRGB(x, y, v)
fun WB.width() = width
fun WB.height() = height

fun main() = wbApp()
fun wbApp(onTry: (WB) -> Unit = {}) = application {
    val pixs = BufferedImage(28, 28, BufferedImage.TYPE_BYTE_GRAY)
    Window(onCloseRequest = ::exitApplication) {
        MaterialTheme {
            Scaffold(topBar = { TopAppBar({ Text("White Board") }) },
                content = {
                    var text by remember { mutableStateOf("Try") }
                    Column {
                        Button(onClick = { onTry(pixs) }) { Text(text) }
                        wb(pixs)
                    }
                }
            )
        }
    }
}

@Suppress("unused")
@Composable
fun countButton_Test(counter: MutableStateFlow<Int>) =
    Button(onClick = { counter.value++ }) { Text("${counter.collectAsState().value}") }

data class Pixel(val x: Int, val y: Int, val v: Int)


@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun wb(pixels: WB) {
    val pixelSize = 10
    val width by remember { mutableStateOf(28) }
    val height by remember { mutableStateOf(28) }
    val pixFlow = MutableStateFlow(Pixel(0, 0, 0))
    val pixChg = pixFlow.collectAsState()
    var drawColor by remember { mutableStateOf(0) }
    fun onPixel(ev: PointerEvent, onMouse: (Pixel) -> Unit) {
        val pos = ev.changes.first().position
        val x = pos.x.toInt() / pixelSize
        val y = pos.y.toInt() / pixelSize
        if ((0 until width).contains(x) && (0 until height).contains(y)) onMouse(Pixel(x, y, pixels.getRGB(x, y)))
    }

    var msPress by remember { mutableStateOf(false) }
    Canvas(modifier = Modifier.fillMaxSize()
        .onPointerEvent(PointerEventType.Move) { ev ->
            onPixel(ev) { p ->
                if (msPress && p.v != drawColor) {
                    pixels[p.x, p.y] = drawColor
                    pixFlow.value = Pixel(p.x, p.y, drawColor)
                }
            }
        }
        .onPointerEvent(PointerEventType.Press) { ev ->
            onPixel(ev) { p ->
                msPress = true
                drawColor = 255 - p.v
                pixels[p.x, p.y] = drawColor
                pixFlow.value = Pixel(p.x, p.v, drawColor)
            }
        }
        .onPointerEvent(PointerEventType.Release) { msPress = false }
    ) {
        pixChg.value // DrawScope内でStateを参照しないと画面が更新されない
        for (y in 0 until pixels.height()) {
            for (x in 0 until pixels.width()) {
                drawRect(
                    Color(pixels[x, y], pixels[x, y], pixels[x, y]),
                    Offset(x * pixelSize.toFloat(), y * pixelSize.toFloat()),
                    Size(pixelSize.toFloat(), pixelSize.toFloat())
                )
            }
        }
    }
}




