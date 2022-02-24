package com.sample

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.renderComposable
import com.sample.components.*
import com.sample.content.*
import com.sample.style.AppStylesheet
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Text

//import * as mdc from 'material-components-web';
//@JsModule("material-components-web")
//external fun mdc()

fun main() {

    //js("checkbox = new mdc.checkbox.MDCCheckbox(document.querySelector('.mdc-checkbox'));")

    renderComposable(rootElementId = "root") {
        ButtonToggle()
        MyButton()
    }
}

@Composable
fun ButtonToggle() {
    Button({
        attr("data-mdc-auto-init", "MDCIconButtonToggle")
        onClick { e -> window.alert("Clicked! [${e.clientX},${e.clientY}]") }
    }) {
        Text("Show Dialog")
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
