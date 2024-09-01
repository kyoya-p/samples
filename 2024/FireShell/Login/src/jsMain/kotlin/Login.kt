import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.dom.clear
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.p
import org.w3c.dom.HTMLInputElement


suspend fun loginPage() = document.body!!.apply { clear() }.append {
    var userId = ""
    var password = ""
    fun login() = MainScope().launch {
        runCatching { auth.signInWithEmailAndPassword(userId, password) }.onFailure { window.alert("Failed.") }
    }
    p { +"USER ID:"; inputx { userId = it } }
    p { +"PASSWORD:"; inputx({ type = InputType.password }) { password = it } }
    p { button { +"LOGIN"; onClickFunction = { login() } } }
}

fun <T> TagConsumer<T>.logoutButton() = button {
    +"LOGOUT"
    onClickFunction = { MainScope().launch { auth.signOut() } }
}

fun <T> TagConsumer<T>.inputx(opt: INPUT.() -> Unit = {}, chg: suspend (v: String) -> Unit = {}) = input {
    opt()
    onChangeFunction = { MainScope().launch { chg((it.target as HTMLInputElement).value) } }
}
