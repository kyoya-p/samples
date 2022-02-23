package com.sample

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.renderComposable
import com.sample.components.*
import com.sample.content.*
import com.sample.style.AppStylesheet
import kotlinx.browser.window
import org.jetbrains.compose.web.css.GridAutoFlow
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Text

fun main() {
    renderComposable(rootElementId = "root") {
        MyButton()
    }
}

@Composable
fun MyButton() {
    Button({
        onClick { window.alert("Clicked!") }
    }) {
        Text("Show Dialog")
    }
}


fun main_bak() {
    renderComposable(rootElementId = "root") {
        Style(AppStylesheet)

        Layout {
            Header()
            MainContentLayout {
                Intro()
                ComposeWebLibraries()
                GetStarted()
                CodeSamples()
                JoinUs()
            }
            PageFooter()
        }
    }
}
