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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val ARTICLE_SELECTOR = "article[data-testid=\"tweet\"]"
private const val NO_GROWTH_LIMIT = 5
private const val SCROLL_WAIT_MS = 1500.0

private data class SearchChunk(val label: String, val query: String)

class Search : CliktCommand(name = "search") {
    override fun help(context: Context) = "検索キーワードでXを検索し結果をすべて採取する"

    val query by argument(help = "検索キーワード（複数指定でスペース区切り結合）").multiple(required = true)
    val latest by option("-l", "--latest", help = "「最新」タブで検索する（デフォルト: トップタブ）").flag()
    val date by option("-d", "--date", help = "検索日時範囲を指定（例: \"8\": 直前8ヶ月、\"2508-\": 2025年8月1日〜現在、\"2508-2608\": 2025年8月1日〜2026年8月1日、\"240101-240630\"）")
    val since by option("--since", help = "検索開始日を指定（例: \"2024-01-01\", \"240101\", \"2401\"）")
    val until by option("--until", help = "検索終了日を指定（例: \"2024-06-30\", \"240630\", \"2406\"）")
    val splitDays by option("-s", "--split-days", help = "長期間の検索を分割する日数（デフォルト: 30日/約1か月。0で分割無効）").int().default(30)
    val max by option("-m", "--max", help = "採取するツイート数の上限").int().default(Int.MAX_VALUE)
    val output by option("-o", "--output", help = "出力先ファイルパス")
    val headed by option("--headed", help = "ブラウザをヘッド付きで起動する（デバッグ用）").flag()

    override fun run() {
        val baseQuery = query.joinToString(" ")

        if (!AUTH_STATE_FILE.exists()) {
            echo("ログインセッションが見つかりません: ${AUTH_STATE_FILE.path}", err = true)
            echo("先に `X login`（例: mise run login）を実行してください。", err = true)
            return
        }

        // 開始日・終了日の決定（--since / --until または -d から決定）
        val now = LocalDate.now()
        val resolvedRange: Pair<LocalDate, LocalDate>? = when {
            since != null || until != null -> {
                val sDate = since?.let { parseDateToken(it) } ?: now.minusMonths(6)
                val uDate = until?.let { parseDateToken(it) } ?: now
                Pair(sDate, uDate)
            }
            date != null -> parseDateRange(date!!)
            else -> null
        }

        // 検索クエリ・区間一覧の生成
        val chunkList = mutableListOf<SearchChunk>()

        if (resolvedRange != null && splitDays > 0) {
            val chunks = splitDateRange(resolvedRange.first, resolvedRange.second, splitDays)
            for (chunk in chunks) {
                val sStr = chunk.first.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val uStr = chunk.second.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val chunkFilter = if (chunk.second == now) "since:$sStr" else "since:$sStr until:$uStr"
                val label = "$sStr 〜 $uStr"
                chunkList.add(SearchChunk(label, "$baseQuery $chunkFilter"))
            }
        } else if (resolvedRange != null) {
            val sStr = resolvedRange.first.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val uStr = resolvedRange.second.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val filter = if (resolvedRange.second == now) "since:$sStr" else "since:$sStr until:$uStr"
            val label = "$sStr 〜 $uStr"
            chunkList.add(SearchChunk(label, "$baseQuery $filter"))
        } else {
            chunkList.add(SearchChunk("全期間", baseQuery))
        }

        val outputFile = output?.let { File(it) } ?: defaultOutputFile(baseQuery + (date?.let { "_$it" } ?: ""))
        outputFile.parentFile?.mkdirs()

        val collected = LinkedHashMap<String, Tweet>()

        Playwright.create().use { playwright ->
            val browser: Browser = playwright.chromium().launch(
                BrowserType.LaunchOptions().setHeadless(!headed).setArgs(STEALTH_LAUNCH_ARGS)
            )
            val context = browser.newContext(
                Browser.NewContextOptions().setStorageStatePath(AUTH_STATE_FILE.toPath())
            )
            context.applyStealth()
            val page = context.newPage()

            for (chunk in chunkList) {
                if (collected.size >= max) break

                val beforeCount = collected.size
                val url = buildSearchUrl(chunk.query, latest)
                page.navigate(url)

                try {
                    page.waitForSelector(ARTICLE_SELECTOR, Page.WaitForSelectorOptions().setTimeout(15000.0))
                } catch (e: Exception) {
                    echo("${chunk.label}: 0件", err = true)
                    continue
                }

                var noGrowthCount = 0

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
                    }

                    if (noGrowthCount >= NO_GROWTH_LIMIT) {
                        break
                    }

                    page.mouse().wheel(0.0, 4000.0)
                    page.waitForTimeout(SCROLL_WAIT_MS)
                }

                val chunkCount = collected.size - beforeCount
                echo("${chunk.label}: ${chunkCount}件", err = true)
            }

            val results = collected.values.take(max)
            val json = Json { prettyPrint = true }
            outputFile.writeText(json.encodeToString(results))
            echo("合計: ${results.size}件 -> ${outputFile.path}", err = true)


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
