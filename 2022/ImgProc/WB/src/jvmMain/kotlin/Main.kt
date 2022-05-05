// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:Suppress("OPT_IN_IS_NOT_ENABLED")

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

@Composable
@Preview
fun App() {

    MaterialTheme {
        Scaffold(topBar = { TopAppBar({ Text("White Board") }) },
            content = {
                var text by remember { mutableStateOf("Try") }
                Column(content = {
                    wb()
                    Button(onClick = { text = "Hello, Desktop!" }) { Text(text) }
                })
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun wb() {
    val pixelSize = 10
    val width by remember { mutableStateOf(28) }
    val height by remember { mutableStateOf(28) }
    val pixels by remember { mutableStateListOf<Double>() }
    var msPress by remember { mutableStateOf(false) }
    Canvas(modifier = Modifier.fillMaxSize()
        .onPointerEvent(PointerEventType.Move) { ev ->
            val pos = ev.changes.first().position
            val x = pos.x.toInt() / pixelSize
            val y = pos.y.toInt() / pixelSize
            println("($x,$y) $msPress")
            if ((0 until width).contains(x) && (0 until height).contains(y)) {
                pixels[pos.x.toInt() / pixelSize][pos.y.toInt() / pixelSize] = 1.0
            }
        }
        .onPointerEvent(PointerEventType.Press) { msPress = true }
        .onPointerEvent(PointerEventType.Release) { msPress = false }
    ) {
        for (y in pixels.indices) {
            for (x in pixels[y].indices) {
                val color = if (pixels[y][x] == 0.0) Color.Black else Color.White
                drawRect(color,
                    Offset(x * pixelSize.toFloat(), y * pixelSize.toFloat()),
                    Size(pixelSize.toFloat(), pixelSize.toFloat())
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun cell(index: Int, onMouse: () -> Double) {
    var pixel by remember { mutableStateOf(0.0) }
    Card(
        backgroundColor = if (pixel == 0.0) Color.Black else Color.White,
        modifier = Modifier.padding(0.dp).width(10.dp).height(10.dp)
            //    .onPointerEvent(PointerEventType.Move) { ev -> pixel = onMouse() }
            .onPointerEvent(PointerEventType.Enter) { pixel = 1.0 }
            .onPointerEvent(PointerEventType.Exit) { pixel = 0.0 },
        onClick = { pixel = 1.0 - pixel },
    ) {}
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun grid1() {
    val list = (1..10).map { it.toString() }

    LazyVerticalGrid(
        cells = GridCells.Adaptive(128.dp),
        contentPadding = PaddingValues(start = 12.dp, top = 16.dp, end = 12.dp, bottom = 16.dp),
        content = {
            items(list.size) { index ->
                Card(
                    backgroundColor = Color.Red,
                    modifier = Modifier
                        .padding(1.dp)
                        .fillMaxWidth(),
                    elevation = 8.dp,
                ) {
                    Text(
                        text = list[index],
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        color = Color(0xFFFFFFFF),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }


    )
}


fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
