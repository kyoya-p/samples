import kotlinx.browser.localStorage

actual val DEV_SERVER_HOST: String = "127.0.0.1"
actual fun setStorage(k: String, v: String?) = if (v == null) localStorage.removeItem(k) else localStorage.setItem(k, v)
actual fun getStorage(k: String) = localStorage.getItem(k)
