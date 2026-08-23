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
private const val NO_GROWTH_LIMIT = 4
private const val BASE_SCROLL_WAIT_MS = 1200.0

private data class SearchChunk(val label: String, val query: String)


class Search : CliktCommand(name = "search") {
    override fun help(context: Context) = "検索キーワードでXを検索し結果をすべて採取する"

    val query by argument(help = "検索キーワード（複数指定でスペース区切り結合。省略時は環境変数 X_QUERY を使用）").multiple()
    val latest by option("-l", "--latest", help = "「最新」タブで検索する（デフォルト: トップタブ）").flag()
    val date by option("-d", "--date", help = "検索日時範囲を指定（例: \"8\": 直前8ヶ月、\"2508-\": 2025年8月1日〜現在、\"2508-2608\": 2025年8月1日〜2026年8月1日、\"240101-240630\"）")
    val since by option("--since", help = "検索開始日を指定（例: \"2024-01-01\", \"240101\", \"2401\"）")
    val until by option("--until", help = "検索終了日を指定（例: \"2024-06-30\", \"240630\", \"2406\"）")
    val splitDays by option("-s", "--split-days", help = "長期間の検索を分割する日数（デフォルト: 30日/約1か月。0で分割無効）").int().default(30)
    val max by option("-m", "--max", help = "採取するツイート数の上限").int().default(Int.MAX_VALUE)
    val output by option("-o", "--output", help = "出力先ファイルパス")
    val headed by option("--headed", help = "ブラウザをヘッド付きで起動する（デバッグ用）").flag()
    val clearCache by option("-c", "--clear-cache", help = "検索開始前に既存の.xキャッシュフォルダ内のJSONをクリアする").flag()

    override fun run() {
        if (clearCache) {
            val deleted = Tweet.clearCache()
            echo("[${nowStr()}] キャッシュをクリアしました (${deleted}件削除)")
        }

        val baseQuery = if (query.isNotEmpty()) {
            query.joinToString(" ")
        } else {
            DEFAULT_SEARCH_QUERY
        }

        if (baseQuery.isNullOrBlank()) {
            echo("検索キーワードが指定されていません。引数または環境変数 X_QUERY で指定してください。", err = true)
            return
        }

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
        for (cached in Tweet.loadAllFromCache()) {
            collected[cached.id] = cached
        }
        if (collected.isNotEmpty()) {
            echo("[${nowStr()}] 既存キャッシュから ${collected.size} 件のツイートをロードしました")
        }

        Playwright.create().use { playwright ->
            val browser: Browser = playwright.chromium().launch(
                BrowserType.LaunchOptions().setHeadless(!headed).setArgs(STEALTH_LAUNCH_ARGS)
            )
            val context = browser.newContext(
                Browser.NewContextOptions().setStorageStatePath(AUTH_STATE_FILE.toPath())
            )
            context.applyStealth()
            val page = context.newPage()

            var rateLimitHit = false
            page.onResponse { response ->
                if (response.status() == 429) {
                    rateLimitHit = true
                }
            }

            val pendingChunks = ArrayDeque(chunkList)
            val retryChunks = mutableListOf<SearchChunk>()
            var pass = 1

            fun flushOutput() {
                val results = collected.values.take(max)
                val json = Json { prettyPrint = true }
                outputFile.writeText(json.encodeToString(results))
            }

            try {
                while (pendingChunks.isNotEmpty() && collected.size < max) {
                    val totalInPass = pendingChunks.size
                    var currentPassIndex = 0

                    while (pendingChunks.isNotEmpty() && collected.size < max) {
                        val chunk = pendingChunks.removeFirst()
                        currentPassIndex++

                        val progress = if (pass > 1) "[$currentPassIndex/$totalInPass (再試行$pass)]" else "[$currentPassIndex/$totalInPass]"
                        echo("[${nowStr()}] [${chunk.label}] $progress 検索開始 (現在累積: ${collected.size}件)")

                        // 区間間のインターバル（3〜6秒）
                        val baseCooldown = kotlin.random.Random.nextDouble(3000.0, 6000.0)
                        page.waitForTimeout(baseCooldown)

                        val beforeCount = collected.size
                        val url = buildSearchUrl(chunk.query, latest)
                        rateLimitHit = false
                        page.navigate(url)

                        var loaded = false
                        var attempts = 0
                        val maxAttempts = 3

                        while (attempts < maxAttempts && !loaded) {
                            attempts++
                            try {
                                page.waitForSelector(ARTICLE_SELECTOR, Page.WaitForSelectorOptions().setTimeout(12000.0))
                                loaded = true
                            } catch (e: Exception) {
                                if (rateLimitHit || isErrorState(page)) {
                                    echo("[${nowStr()}] [${chunk.label}] $progress 【クールダウン判定】レート制限を検知 (HTTP 429/エラー画面)。${BATCH_COOLDOWN_SEC}秒間クールダウン待機中...")
                                    page.waitForTimeout(BATCH_COOLDOWN_MS)
                                    rateLimitHit = false
                                    echo("[${nowStr()}] [${chunk.label}] $progress クールダウン待機完了。再試行します...")
                                    try {
                                        page.reload()
                                        page.waitForSelector(ARTICLE_SELECTOR, Page.WaitForSelectorOptions().setTimeout(15000.0))
                                        loaded = true
                                    } catch (e2: Exception) {
                                        loaded = false
                                    }
                                } else {
                                    loaded = false
                                    break
                                }
                            }
                        }

                        if (!loaded) {
                            if (rateLimitHit || isErrorState(page)) {
                                echo("[${nowStr()}] [${chunk.label}] $progress 未完了区間としてリトライキューに登録")
                                retryChunks.add(chunk)
                            } else {
                                echo("[${nowStr()}] [${chunk.label}] $progress 検索完了: 0件 (累積: ${collected.size}件)")
                            }
                            continue
                        }

                        var noGrowthCount = 0
                        var lastReportedCount = beforeCount
                        var scrollErrorRetries = 0
                        var wasTruncatedByRateLimit = false

                        while (collected.size < max) {
                            val raw = page.evalOnSelectorAll(ARTICLE_SELECTOR, TWEET_EXTRACT_SCRIPT)
                            val beforeEval = collected.size
                            for (tweet in parseTweets(raw)) {
                                if (collected.putIfAbsent(tweet.id, tweet) == null) {
                                    tweet.saveToCache()
                                }
                            }

                            val currentChunkCount = collected.size - beforeCount
                            if (collected.size > lastReportedCount) {
                                echo("[${nowStr()}] [${chunk.label}] $progress 採取中: 区間内 ${currentChunkCount}件 (累積: ${collected.size}件)")
                                lastReportedCount = collected.size
                            }

                            if (collected.size == beforeEval) {
                                noGrowthCount++
                            } else {
                                noGrowthCount = 0
                            }

                            // スクロール中にレート制限が発生した場合はクールダウン後に画面復旧
                            if (rateLimitHit || isErrorState(page)) {
                                scrollErrorRetries++
                                if (scrollErrorRetries > 2) {
                                    echo("[${nowStr()}] [${chunk.label}] $progress レート制限のためこの区間を一時終了し、リトライキューに登録して次へ進みます")
                                    wasTruncatedByRateLimit = true
                                    retryChunks.add(chunk)
                                    break
                                }
                                echo("[${nowStr()}] [${chunk.label}] $progress 【クールダウン判定】スクロール中にレート制限/エラー画面を検知 (試行 $scrollErrorRetries/2)。${BATCH_COOLDOWN_SEC}秒間クールダウン待機中...")
                                page.waitForTimeout(BATCH_COOLDOWN_MS)
                                rateLimitHit = false
                                noGrowthCount = 0
                                echo("[${nowStr()}] [${chunk.label}] $progress クールダウン完了。画面状態の復旧を試行します...")
                                try {
                                    val retryBtn = page.locator("button:has-text(\"やりなおす\"), button:has-text(\"Retry\"), div[role=\"button\"]:has-text(\"やりなおす\")").first()
                                    if (retryBtn.isVisible()) {
                                        retryBtn.click()
                                        page.waitForTimeout(3000.0)
                                    } else {
                                        page.reload()
                                        page.waitForTimeout(3000.0)
                                    }
                                    page.waitForSelector(ARTICLE_SELECTOR, Page.WaitForSelectorOptions().setTimeout(15000.0))
                                } catch (e: Exception) {
                                    // 復旧失敗
                                }
                                continue
                            } else {
                                scrollErrorRetries = 0
                            }

                            // 正常末尾到達: 新規追加がなくレート制限もない場合は即終了
                            if (noGrowthCount >= NO_GROWTH_LIMIT) {
                                break
                            }

                            page.mouse().wheel(0.0, 4000.0)
                            val jitter = kotlin.random.Random.nextDouble(200.0, 600.0)
                            page.waitForTimeout(BASE_SCROLL_WAIT_MS + jitter)
                        }

                        val chunkCount = collected.size - beforeCount
                        val statusSuffix = if (wasTruncatedByRateLimit) " (一部未完了・後続リトライ予定)" else ""
                        echo("[${nowStr()}] [${chunk.label}] $progress 検索完了: 区間内 ${chunkCount}件 / 累積合計: ${collected.size}件$statusSuffix")
                        flushOutput()
                    }

                    // 2パス目以降の処理
                    if (retryChunks.isNotEmpty() && pass < 3) {
                        pass++
                        echo("[${nowStr()}] [再試行パス$pass] レート制限で中断/未完了となった ${retryChunks.size} 件の区間を補完します。${BATCH_COOLDOWN_SEC}秒待機中...")
                        page.waitForTimeout(BATCH_COOLDOWN_MS)
                        pendingChunks.addAll(retryChunks)
                        retryChunks.clear()
                    }
                }
            } finally {
                flushOutput()
                val results = collected.values.take(max)
                echo("[${nowStr()}] 合計: ${results.size}件 -> ${outputFile.path}", err = true)
                browser.close()
            }
        }
    }
}

private fun isErrorState(page: Page): Boolean {
    return try {
        page.locator("[data-testid=\"error-detail\"], button:has-text(\"やりなおす\"), button:has-text(\"Retry\"), div:has-text(\"問題が発生しました\")").count() > 0
    } catch (e: Exception) {
        false
    }
}

private fun nowStr(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

private fun buildSearchUrl(query: String, latest: Boolean): String {
    val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8)
    val tab = if (latest) "&f=live" else ""
    return "$BASE_URL/search?q=$encoded&src=typed_query$tab"
}

private fun defaultOutputFile(query: String): File {
    val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
    val safeQuery = query
        .replace(Regex("""[\\/:*?"<>|\r\n\t]"""), "_")
        .replace(Regex("""_+"""), "_")
        .trim('_')
        .take(50)
        .ifEmpty { "query" }
    return File(OUTPUT_DIR, "x-search-$safeQuery-$stamp.json")
}
