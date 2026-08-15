package xtool

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val ARTICLE_SELECTOR = "article[data-testid=\"tweet\"]"
private const val NO_GROWTH_LIMIT = 5
private const val SCROLL_WAIT_MS = 1500.0

class Search : CliktCommand(name = "search") {
    override fun help(context: Context) = "検索キーワードでXを検索し結果をすべて採取する"

    val query by argument(help = "検索キーワード（複数指定でスペース区切り結合）").multiple(required = true)
    val latest by option("-l", "--latest", help = "「最新」タブで検索する（デフォルト: トップタブ）").flag()
    val max by option("-m", "--max", help = "採取するツイート数の上限").int().default(Int.MAX_VALUE)
    val output by option("-o", "--output", help = "出力先ファイルパス")
    val headed by option("--headed", help = "ブラウザをヘッド付きで起動する（デバッグ用）").flag()

    override fun run() {
        val queryText = query.joinToString(" ")

        if (!AUTH_STATE_FILE.exists()) {
            echo("ログインセッションが見つかりません: ${AUTH_STATE_FILE.path}")
            echo("先に `X login`（例: mise run login）を実行してください。")
            return
        }

        val outputFile = output?.let { File(it) } ?: defaultOutputFile(queryText)
        outputFile.parentFile?.mkdirs()

        val url = buildSearchUrl(queryText, latest)
        echo("検索URLへアクセス: $url")

        Playwright.create().use { playwright ->
            val browser: Browser = playwright.chromium().launch(
                BrowserType.LaunchOptions().setHeadless(!headed).setArgs(STEALTH_LAUNCH_ARGS)
            )
            val context = browser.newContext(
                Browser.NewContextOptions().setStorageStatePath(AUTH_STATE_FILE.toPath())
            )
            context.applyStealth()
            val page = context.newPage()
            page.navigate(url)

            val collected = LinkedHashMap<String, Tweet>()
            var noGrowthCount = 0

            try {
                page.waitForSelector(ARTICLE_SELECTOR, Page.WaitForSelectorOptions().setTimeout(15000.0))
            } catch (e: Exception) {
                echo("検索結果が見つかりませんでした。")
            }

            while (collected.size < max) {
                val raw = page.evalOnSelectorAll(ARTICLE_SELECTOR, TWEET_EXTRACT_SCRIPT)
                val before = collected.size
                for (tweet in parseTweets(raw)) {
                    collected.putIfAbsent(tweet.id, tweet)
                }

                if (collected.size == before) {
                    noGrowthCount++
                } else {
                    noGrowthCount = 0
                    echo("採取件数: ${collected.size}")
                }

                if (noGrowthCount >= NO_GROWTH_LIMIT) {
                    echo("新規ツイートが取得できなくなったため終了します（検索結果の末尾に到達）。")
                    break
                }

                page.mouse().wheel(0.0, 4000.0)
                page.waitForTimeout(SCROLL_WAIT_MS)
            }

            val results = collected.values.take(max)
            val json = Json { prettyPrint = true }
            outputFile.writeText(json.encodeToString(results))
            echo("採取完了: ${results.size}件 -> ${outputFile.path}")

            browser.close()
        }
    }
}

private fun buildSearchUrl(query: String, latest: Boolean): String {
    val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8)
    val tab = if (latest) "&f=live" else ""
    return "$BASE_URL/search?q=$encoded&src=typed_query$tab"
}

private fun defaultOutputFile(query: String): File {
    val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
    val safeQuery = query.replace(Regex("[^\\w-]+"), "_").take(50).ifEmpty { "query" }
    return File(OUTPUT_DIR, "x-search-$safeQuery-$stamp.json")
}
