import kotlinx.browser.document
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.html.*
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlin.reflect.KProperty
import kotlinx.html.org.w3c.dom.events.Event

class Cookie(val key: String, val default: String) {
    fun cookies() = document.cookie.split(";").filter { it.trim().isNotEmpty() }.map { it.trim().split("=", limit = 2) }
        .associate { it[0] to (it.getOrElse(1) { "" }) }

    operator fun getValue(nothing: Nothing?, property: KProperty<*>) = value
    operator fun setValue(nothing: Nothing?, property: KProperty<*>, v: String) = run { value = v }
    var value
        get():String = cookies()[key] ?: default
        set(v) = run { document.cookie = "$key=$v" }
}

fun <T> TagConsumer<T>.field(cookie: Cookie, opts: INPUT.() -> Unit = {}, onChange: (v:String) -> Unit = {}) {
    var c by cookie
    input {
        id = cookie.key
        value = c
        onChangeFunction = { ev ->
            val e = document.querySelector("input[id='${cookie.key}']")!!
            c = e.asDynamic().value as String
            onChange(c)
        }
        opts()
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun <T> TagConsumer<T>.btn(label: String, op: suspend (Event) -> Unit) =
    button { +label; onClickFunction = { GlobalScope.launch { op(it) } } }
