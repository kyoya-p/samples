package xtool

import com.microsoft.playwright.BrowserContext
import java.io.File

const val BASE_URL = "https://x.com"

val AUTH_STATE_FILE: File
    get() {
        val candidates = listOf(
            File(".auth", "x-state.json"),
            File("X/.auth", "x-state.json"),
            File("../.auth", "x-state.json"),
            File("../X/.auth", "x-state.json")
        )
        return candidates.firstOrNull { it.exists() } ?: File(".auth", "x-state.json")
    }

val OUTPUT_DIR: File
    get() {
        val candidates = listOf(
            File("output"),
            File("X/output")
        )
        return candidates.firstOrNull { it.isDirectory && it.exists() } ?: File("output")
    }


// XがCDP経由の自動操作ブラウザを検知してログイン/検索を妨害するため、検知シグナルを消す。
val STEALTH_LAUNCH_ARGS: List<String> = listOf("--disable-blink-features=AutomationControlled")

fun BrowserContext.applyStealth() {
    addInitScript("Object.defineProperty(navigator, 'webdriver', { get: () => undefined });")
}
