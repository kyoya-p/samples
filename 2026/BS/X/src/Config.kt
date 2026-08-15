package xtool

import com.microsoft.playwright.BrowserContext
import java.io.File

const val BASE_URL = "https://x.com"

val AUTH_STATE_FILE: File = File(".auth", "x-state.json")
val OUTPUT_DIR: File = File("output")

// XがCDP経由の自動操作ブラウザを検知してログイン/検索を妨害するため、検知シグナルを消す。
val STEALTH_LAUNCH_ARGS: List<String> = listOf("--disable-blink-features=AutomationControlled")

fun BrowserContext.applyStealth() {
    addInitScript("Object.defineProperty(navigator, 'webdriver', { get: () => undefined });")
}
