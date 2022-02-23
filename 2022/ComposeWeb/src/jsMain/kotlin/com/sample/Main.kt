package com.sample

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.renderComposable
import com.sample.components.*
import com.sample.content.*
import com.sample.style.AppStylesheet
import org.jetbrains.compose.web.css.GridAutoFlow
import org.jetbrains.compose.web.css.selectors.CSSSelector
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Text

fun main() {
    renderComposable(rootElementId = "root") {
        Style(AppStylesheet)

        Layout {
//            Header()
            MainContentLayout {
//                Intro()
//                ComposeWebLibraries()
//                GetStarted()
//                CodeSamples()
                //JoinUs()
                MyButton()
            }
//            PageFooter()
        }
    }
}

@Composable
fun MyButton() {
    //Button()
    Text("Text Sample3")
}