import kotlinx.browser.localStorage
import kotlinx.browser.window

//actual val DEV_SERVER_HOST: String = "127.0.0.1"
actual val DEV_SERVER_HOST: String = window.location.hostname
actual fun setStorage(k: String, v: String?) = if (v == null) localStorage.removeItem(k) else localStorage.setItem(k, v)
actual fun getStorage(k: String) = localStorage.getItem(k)
