import kotlinx.coroutines.await
import kotlin.js.Promise

external fun require(s: String): dynamic

fun hello(req: dynamic, res: dynamic) {
    res.send("Hello, world!")
}

fun mainX(args: Array<String>) {
    val express = require("express")
    val app = express()
    app.get("/", ::runBrowser)
    app.listen(8080) { println("Start Server.") }
}

fun runBrowser(req: dynamic, res: dynamic) {
    val playwright = require("playwright")
    val browser = playwright.chromium.launch()
    val page = browser.newPage()
    page.goto("https://digital.onl.jp/")
    val screenshot = page.screenshot()
    res.setHeader("Content-Type", "image/png")
    res.send(screenshot)
    browser.close()
}

external interface Buffer

@JsModule("playwright")
external interface Playwright {
    interface Driver {
        fun launch(): Promise<Browser>
    }

    interface Browser {
        fun newPage(): Promise<Page>
        fun close(): Promise<Unit>
    }

    interface Page {
        fun goto(url: String): Promise<Unit>
        fun screenshot(): Promise<Buffer>
    }

    val chromium: Driver
}

fun main() {
    val playwright = require("@playwright/test")
    playwright.test("test1") { page ->
        page.goto("https://playwright.dev/")
    }
    playwright.test("test2") { page ->
        page.goto("https://digital.onl.jp/")
    }

}

fun mainXXX(args: Array<String>) {
    val playwright = require("playwright")
    val browser = playwright.chromium.launch()
    val page = browser.newPage()
    page.goto("https://www.google.com")
    page.screenshot(path = "screenshot.png")
    browser.close()
}

//fun mainX(args: Array<String>) {
//    val browser = playwright.chromium.launch()
//    val page = browser.newPage()
//    page.goto("https://www.google.com")
//    page.screenshot(path = "screenshot.png")
//    browser.close()
//    fun main() {
//        val name = "Kotlin"
//        println("Hello, " + name + "!")
//
//        for (i in 1..5) {
//            println("i = $i")
//        }
//
//    }
//
//}
