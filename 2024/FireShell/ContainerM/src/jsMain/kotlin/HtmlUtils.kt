import dev.gitlive.firebase.*
import dev.gitlive.firebase.firestore.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.dom.addClass
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.*
import kotlinx.html.org.w3c.dom.events.Event
import org.w3c.dom.Element
import org.w3c.dom.HTMLInputElement
import kotlin.reflect.KProperty


class Cookie(val key: String, val default: String) {
    fun cookies() = document.cookie.split(";").filter { it.trim().isNotEmpty() }.map { it.trim().split("=", limit = 2) }
        .associate { it[0] to (it.getOrElse(1) { "" }) }

    operator fun getValue(nothing: Nothing?, property: KProperty<*>) = value
    operator fun setValue(nothing: Nothing?, property: KProperty<*>, v: String) = run { value = v }
    var value
        get():String = cookies()[key] ?: default
        set(v) = run { document.cookie = "$key=$v" }
}

fun <T> TagConsumer<T>.field(cookie: Cookie, opts: INPUT.() -> Unit = {}, onChange: (Element) -> Unit = {}) {
    var c by cookie
    input {
        id = cookie.key
        value = c
        onChangeFunction = { ev ->
            val e = document.querySelector("input[id='${cookie.key}']")!!
            c = e.asDynamic().value as String
            onChange(e)
        }
        opts()
    }
}
