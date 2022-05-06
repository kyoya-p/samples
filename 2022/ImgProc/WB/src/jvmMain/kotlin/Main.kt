// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import androidx.compose.desktop.ui.tooling.preview.Preview
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

@Composable
@Preview
fun App() {
    MaterialTheme {
        Scaffold(topBar = { TopAppBar({ Text("White Board") }) },
            content = {
                var text by remember { mutableStateOf("Try") }
                Column(content = {
                    // countButton_Test(MutableStateFlow(0))
                    val pixs = MutableList(28) { MutableList(28) { 0 } }
                    wb(pixs)
                    Button(onClick = { text = "Hello, Desktop!" }) { Text(text) }
                })
            }
        )
    }
}

@Suppress("unused")
@Composable
fun countButton_Test(counter: MutableStateFlow<Int>) =
    Button(onClick = { counter.value++ }) { Text("${counter.collectAsState().value}") }

data class Pixel(val x: Int, val y: Int, val v: Int)

typealias WB = MutableList<MutableList<Int>>

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
        if ((0 until width).contains(x) && (0 until height).contains(y)) onMouse(Pixel(x, y, pixels[y][x]))
    }

    var msPress by remember { mutableStateOf(false) }
    Canvas(modifier = Modifier.fillMaxSize()
        .onPointerEvent(PointerEventType.Move) { ev ->
            onPixel(ev) { p ->
                if (msPress && p.v != drawColor) {
                    pixels[p.y][p.x] = drawColor
                    pixFlow.value = Pixel(p.x, p.y, drawColor)
                }
            }
        }
        .onPointerEvent(PointerEventType.Press) { ev ->
            onPixel(ev) { p ->
                msPress = true
                drawColor = 1 - p.v
                pixels[p.y][p.x] = drawColor
                pixFlow.value = Pixel(p.x, p.v, drawColor)
            }
        }
        .onPointerEvent(PointerEventType.Release) { msPress = false }
    ) {
        println(pixChg.value) // DrawScope内でStateを参照しないと画面が更新されない
        for (y in pixels.indices) {
            for (x in pixels[y].indices) {
                val color = if (pixels[y][x] == 0) Color.Black else Color.White
                drawRect(
                    color,
                    Offset(x * pixelSize.toFloat(), y * pixelSize.toFloat()),
                    Size(pixelSize.toFloat(), pixelSize.toFloat())
                )
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
