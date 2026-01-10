#!/usr/bin/env kotlin

@file:DependsOn("com.microsoft.playwright:playwright:1.57.0") // https://mvnrepository.com/artifact/com.microsoft.playwright/playwright
@file:DependsOn("io.ktor:ktor-server-cio-jvm:3.3.3") // https://mvnrepository.com/artifact/io.ktor/ktor-server-cio
@file:DependsOn("ch.qos.logback:logback-classic:1.5.24") // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.AriaRole
import io.ktor.http.ContentType
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

fun main() = runBlocking {
    println("Starting test server...")
    val server = testServer()
    delay(1.seconds)

    println("Starting Playwright...")
    Playwright.create().use { playwright ->
        playwright.chromium().launch(BrowserType.LaunchOptions().setHeadless(false)).use { browser ->
            val page = browser.newPage()
            page.navigate("http://localhost:8080/")

            val text = page.getByRole(AriaRole.HEADING).textContent()
            println("Page Content: $text")
            page.close()
            browser.close()
        }
        println("Playwright closed.")
    }
    server.stop(1000, 1000)
    println("Done.")
}

val testPage = """
<!DOCTYPE html><html>
<head><title>Hello World</title></head><body>
<h1>Hello World!</h1>
</body></html>
"""

fun testServer() = embeddedServer(CIO, port = 8080) {
    routing {
        get("/") { call.respondText(testPage, ContentType.Text.Html) }
    }
}.start(wait = false)

main()
