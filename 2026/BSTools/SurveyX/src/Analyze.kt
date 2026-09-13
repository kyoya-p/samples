package xtool

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * X探索・大会調査結果データモデル
 */
@Serializable
data class DeckTrendEntry(
    val archetype: String,
    val winCount: Int,
    val sharePercent: Double,
    val sampleDeckList: List<String> = emptyList(),
    val sourceTweets: List<String> = emptyList()
)

@Serializable
data class MetaReport(
    val generatedAt: String,
    val totalTournaments: Int,
    val totalWinners: Int,
    val trends: List<DeckTrendEntry>
)

class SurveyXAnalyzer {
    private val archetypeKeywords: Map<String, List<String>> = mapOf(
        "アイツのデッキ/ペガサス" to listOf("ペガサス", "アイツ"),
        "獄契約" to listOf("獄契約", "ジャブラッド", "邪神"),
        "蒼契約" to listOf("蒼契約", "シャック", "蒼波"),
        "秘契約" to listOf("秘契約", "ガタル", "碧雷"),
        "緋契約" to listOf("緋契約", "グロウ", "緋炎"),
        "極契約" to listOf("極契約", "ゼッター", "極争"),
        "血晶" to listOf("血晶", "バット", "パラディ・バット"),
        "呪契約" to listOf("呪契約", "カミュ"),
        "鋼契約" to listOf("鋼契約", "ヴリック"),
        "突契約" to listOf("突契約", "ガット"),
        "零契約" to listOf("零契約", "ウィズ"),
        "造契約" to listOf("造契約", "レーヴ"),
        "幻契約" to listOf("幻契約", "パルム"),
        "冥契約" to listOf("冥契約", "ハデス"),
        "原初" to listOf("原初", "ガイア"),
        "熱契約" to listOf("熱契約", "ショコラ"),
        "GS/グッドスタッフ" to listOf("グッドスタッフ", "GS")
    )

    fun classifyArchetype(text: String): String {
        for ((archetype, keywords) in archetypeKeywords) {
            if (keywords.any { text.contains(it, ignoreCase = true) }) {
                return archetype
            }
        }
        return "その他"
    }

    fun analyzeTweets(tweets: List<Tweet>): MetaReport {
        val counts = mutableMapOf<String, Int>()
        val tweetMap = mutableMapOf<String, MutableList<String>>()

        for (tweet in tweets) {
            val arch = classifyArchetype(tweet.text)
            counts[arch] = (counts[arch] ?: 0) + 1
            val link = tweet.url.ifBlank { "https://x.com/${tweet.handle}/status/${tweet.id}" }
            tweetMap.getOrPut(arch) { mutableListOf() }.add(link)
        }

        val total = tweets.size
        val trends = counts.entries
            .sortedByDescending { it.value }
            .map { (arch, count) ->
                val share = if (total > 0) (count.toDouble() / total) * 100.0 else 0.0
                DeckTrendEntry(
                    archetype = arch,
                    winCount = count,
                    sharePercent = share,
                    sampleDeckList = emptyList(),
                    sourceTweets = tweetMap[arch] ?: emptyList()
                )
            }

        return MetaReport(
            generatedAt = java.time.Instant.now().toString(),
            totalTournaments = total,
            totalWinners = total,
            trends = trends
        )
    }

    companion object {
        fun createSampleTweets(): List<Tweet> {
            return listOf(
                Tweet(id = "101", url = "https://x.com/shopA/status/101", author = "ShopA", handle = "shopA", postedAt = "2026-02-20", text = "【バトスピ ショップバトル】優勝は「蒼契約」デッキを使用したタロウ様でした！おめでとうございます！", hasImage = true, hasVideo = false, replyCount = 0, retweetCount = 2, likeCount = 10),
                Tweet(id = "102", url = "https://x.com/shopB/status/102", author = "CardShopB", handle = "shopB", postedAt = "2026-02-21", text = "本日のバトルスピリッツ公認大会 優勝「獄契約」デッキ プレイヤー：ジロウ様", hasImage = true, hasVideo = false, replyCount = 0, retweetCount = 1, likeCount = 8),
                Tweet(id = "103", url = "https://x.com/shopC/status/103", author = "ShopC", handle = "shopC", postedAt = "2026-02-22", text = "バトスピSB 優勝者：サブロウ様 デッキ名：蒼契約シャック", hasImage = true, hasVideo = false, replyCount = 1, retweetCount = 5, likeCount = 15),
                Tweet(id = "104", url = "https://x.com/shopD/status/104", author = "ShopD", handle = "shopD", postedAt = "2026-02-23", text = "大会結果 優勝：シロウ様 使用デッキ：秘契約 ガタル", hasImage = true, hasVideo = false, replyCount = 0, retweetCount = 0, likeCount = 4),
                Tweet(id = "105", url = "https://x.com/playerX/status/105", author = "PlayerX", handle = "playerX", postedAt = "2026-02-24", text = "今日のショップバトル優勝しました！使ったのはアイツのデッキ(ペガサス)です！", hasImage = true, hasVideo = false, replyCount = 2, retweetCount = 3, likeCount = 25),
                Tweet(id = "106", url = "https://x.com/shopE/status/106", author = "ShopE", handle = "shopE", postedAt = "2026-02-25", text = "バトスピショップバトル 優勝：ゴロウ様 デッキ名：獄契約", hasImage = true, hasVideo = false, replyCount = 0, retweetCount = 1, likeCount = 7)
            )
        }
    }
}

class Analyze : CliktCommand(name = "analyze") {
    override fun help(context: Context) = "キャッシュまたはサンプルデータからアーキタイプを集計し、メタゲームレポートを生成します"

    val sample by option("--sample", help = "サンプルデータを使用して分析を実行").flag(default = false)
    val output by option("-o", "--output", help = "出力JSONファイルのパス")

    override fun run() {
        val tweets = if (sample) {
            echo("サンプルツイートを使用して分析を実行します...")
            SurveyXAnalyzer.createSampleTweets()
        } else {
            val cached = Tweet.loadAllFromCache()
            if (cached.isEmpty()) {
                echo("キャッシュにツイートが見つかりません。--sample を指定するか、先に search / extract-sb を実行してください。")
                return
            }
            echo("キャッシュから ${cached.size} 件のツイートを読み込みました。")
            cached
        }

        val analyzer = SurveyXAnalyzer()
        val report = analyzer.analyzeTweets(tweets)

        echo("\n=== Battle Spirits Meta Report ===")
        echo("Generated: ${report.generatedAt}")
        echo("Total Tournaments/Winners: ${report.totalWinners}")
        echo("%-24s %-8s %s".format("Archetype", "Wins", "Share"))
        echo("-".repeat(44))
        for (trend in report.trends) {
            echo("%-24s %-8d %5.1f%%".format(trend.archetype, trend.winCount, trend.sharePercent))
        }

        val json = Json { prettyPrint = true }
        val jsonStr = json.encodeToString(report)

        if (output != null) {
            val file = File(output!!)
            file.parentFile?.mkdirs()
            file.writeText(jsonStr, Charsets.UTF_8)
            echo("\nレポートを保存しました: ${file.absolutePath}")
        }
    }
}
