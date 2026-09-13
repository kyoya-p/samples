package xtool

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class ShopBattleRecord(
    val tweetId: String? = "",
    val eventCategory: String? = "",
    val storeOrEventName: String? = "",
    val format: String? = "",
    val deckType: String? = "",
    val participants: String? = "",
    val winner: String? = "",
    val notes: String? = "",
)



class ExtractShopBattle : CliktCommand(name = "extract-sb") {
    override fun help(context: Context) = "Gemini API (flash-lite-2.5) を用いて.xキャッシュまたはX検索結果JSONからショップバトル結果を抽出しCSV出力する"

    val input by argument(help = "入力元のキャッシュディレクトリまたは検索結果JSONファイルパス（未指定時は.x/キャッシュフォルダを対象）").optional()
    val output by option("-o", "--output", help = "出力先CSVファイルパス（デフォルト: output/shop-battle-<日時>.csv）")
    val apiKey by option("--api-key", envvar = "GEMINI_API_KEY", help = "Gemini APIキー (環境変数 GEMINI_API_KEY も可)")
    val model by option("--model", help = "Geminiモデル名（デフォルト: gemini-2.5-flash-lite）").default("gemini-2.5-flash-lite")
    val batchSize by option("-b", "--batch-size", help = "1回のリクエストでGeminiに渡すツイート数").int().default(25)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun run() {
        val key = resolveApiKey(apiKey)
        if (key == null) {
            echo("Gemini APIキーが設定されていません。環境変数 GEMINI_API_KEY (または GOOGLE_API_KEY) / .env / --api-key オプションを設定してください。")
            return
        }

        val inputFile = resolveInputFile(input)
        if (inputFile == null || !inputFile.exists()) {
            echo("入力ファイル/キャッシュフォルダが見つかりません: ${inputFile?.path ?: ".x/"}")
            return
        }

        val outputFile = output?.let { File(it) } ?: defaultCsvOutputFile()
        outputFile.parentFile?.mkdirs()

        echo("入力ファイル: ${inputFile.path}")
        echo("モデル: $model (バッチサイズ: $batchSize)")

        val tweets: List<Tweet> = try {
            if (inputFile.isDirectory) {
                Tweet.loadAllFromCache(inputFile)
            } else {
                val text = inputFile.readText(Charsets.UTF_8).trim()
                if (text.startsWith("[")) {
                    json.decodeFromString<List<Tweet>>(text)
                } else {
                    listOf(json.decodeFromString<Tweet>(text))
                }
            }
        } catch (e: Exception) {
            echo("JSONファイルのパースに失敗しました: ${e.message}")
            return
        }

        if (tweets.isEmpty()) {
            echo("対象ツイートが0件です。")
            return
        }

        echo("全 ${tweets.size} 件のツイートをバッチ解析開始...")
        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build()

        val tweetMap = tweets.associateBy { it.id }
        val extractedRows = mutableListOf<List<String>>()

        val batches = tweets.chunked(batchSize)
        for ((index, batch) in batches.withIndex()) {
            echo("[${index + 1}/${batches.size}] ${batch.size} 件をGeminiで解析中...")
            val records = analyzeBatch(client, key, model, batch)
            for (rec in records) {
                val tid = rec.tweetId ?: ""
                val tweet = tweetMap[tid]
                val id = tid.ifEmpty { tweet?.id ?: "" }
                val url = tweet?.url ?: if (id.isNotEmpty()) "https://x.com/i/web/status/$id" else ""
                val postedAt = tweet?.postedAt ?: ""
                val author = tweet?.author ?: ""
                val text = tweet?.text ?: ""

                extractedRows.add(
                    listOf(
                        id,
                        url,
                        postedAt,
                        rec.eventCategory ?: "",
                        rec.storeOrEventName ?: "",
                        rec.format ?: "",
                        rec.deckType ?: "",
                        cleanParticipants(rec.participants),
                        rec.winner ?: "",
                        rec.notes ?: "",
                        author,
                        text
                    )
                )
            }


            // レート制限への配慮
            Thread.sleep(500)
        }

        // CSV書き出し
        val headers = listOf(
            "id",
            "url",
            "posted_at",
            "event_category",
            "store_or_event_name",
            "format",
            "deck_type",
            "participants",
            "winner",
            "notes",
            "author",
            "tweet_text"
        )


        val csvContent = buildString {
            appendLine(headers.joinToString(",") { escapeCsv(it) })
            for (row in extractedRows) {
                appendLine(row.joinToString(",") { escapeCsv(it) })
            }
        }

        outputFile.writeText(csvContent, Charsets.UTF_8)
        echo("抽出完了: ${extractedRows.size} 件の大会結果をCSV出力 -> ${outputFile.path}")
    }

    private fun resolveInputFile(path: String?): File? {
        if (!path.isNullOrBlank()) {
            val direct = File(path)
            if (direct.exists()) return direct
            val inCache = File(CACHE_DIR, path)
            if (inCache.exists()) return inCache
            val inOutput = File(OUTPUT_DIR, path)
            if (inOutput.exists()) return inOutput
            val inParentCache = File("../.x", path)
            if (inParentCache.exists()) return inParentCache
            val inParentOutput = File("../output", path)
            if (inParentOutput.exists()) return inParentOutput
            return direct
        }

        // 未指定時は .x キャッシュフォルダを最優先
        if (CACHE_DIR.exists() && CACHE_DIR.isDirectory) {
            val cacheFiles = CACHE_DIR.listFiles { file -> file.isFile && file.name.endsWith(".json") }
            if (!cacheFiles.isNullOrEmpty()) {
                return CACHE_DIR
            }
        }
        val candidateCacheDirs = listOf(File(".x"), File("X/.x"), File("../.x"), File("../X/.x"))
            .filter { it.isDirectory && it.exists() }
            .distinctBy { it.canonicalPath }
        for (dir in candidateCacheDirs) {
            val cacheFiles = dir.listFiles { file -> file.isFile && file.name.endsWith(".json") }
            if (!cacheFiles.isNullOrEmpty()) {
                return dir
            }
        }

        // キャッシュにツイートJSONがない場合のフォールバック（output/配下の最新JSON）
        val candidateDirs = listOf(OUTPUT_DIR, File("output"), File("X/output"), File("../output"))
            .filter { it.isDirectory && it.exists() }
            .distinctBy { it.canonicalPath }

        val allFiles = candidateDirs.flatMap { dir ->
            dir.listFiles { file ->
                file.isFile && file.name.startsWith("x-search-") && file.name.endsWith(".json")
            }?.toList() ?: emptyList()
        }
        return allFiles.maxByOrNull { it.lastModified() } ?: CACHE_DIR
    }


    private fun defaultCsvOutputFile(): File {
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
        return File(OUTPUT_DIR, "shop-battle-$stamp.csv")
    }

    private fun analyzeBatch(
        client: HttpClient,
        apiKey: String,
        modelName: String,
        tweets: List<Tweet>
    ): List<ShopBattleRecord> {
        val prompt = buildPrompt(tweets)
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject {
                            put("text", prompt)
                        })
                    }
                })
            }
            put("generationConfig", buildJsonObject {
                put("responseMimeType", "application/json")
            })
        }.toString()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .timeout(Duration.ofSeconds(60))
            .build()

        for (attempt in 1..3) {
            try {
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() == 200) {
                    return parseGeminiResponse(response.body())
                }
                echo("Gemini APIエラー (${response.statusCode()}): ${response.body()} (試行 $attempt/3)")
            } catch (e: Exception) {
                echo("Gemini APIリクエスト例外 (${e.message}) (試行 $attempt/3)")
            }
            if (attempt < 3) Thread.sleep(1000L * attempt)
        }
        return emptyList()
    }


    private fun buildPrompt(tweets: List<Tweet>): String {
        val items = tweets.map { t ->
            mapOf(
                "id" to t.id,
                "author" to t.author,
                "postedAt" to (t.postedAt ?: ""),
                "text" to t.text
            )
        }
        val itemsJson = json.encodeToString(items)

        return """
あなたはトレーディングカードゲーム「バトルスピリッツ（バトスピ）」の大会・ショップバトル情報抽出エキスパートです。
以下のツイート一覧から、**ショップバトル・公認大会・公式大会・非公認大会・バトラーズカップ等の大会結果（特に優勝報告や入賞報告）が含まれるツイートのみ**を抽出してください。
単なる大会告知、対戦感想、カード開封、関係のないツイートは除外（スキップ）してください。

抽出対象の各ツイートについて、以下のJSON配列フォーマットで返してください:
[
  {
    "tweetId": "ツイートのID (必須)",
    "eventCategory": "大会区分（「店舗バトル」「店舗予選」「公式大会」「非公認大会」のいずれか。通常のショップバトル・公認大会・バトラーズカップ等は「店舗バトル」、CS店舗予選・エリア店舗予選・ショップ予選等は「店舗予選」、CS本戦・エリア予選決勝・公式大型大会等は「公式大会」、CS以外の大型自主大会・非公認自主イベントは「非公認大会」）",
    "storeOrEventName": "店舗名または大会名（例: ドラゴンスター秋葉原店、バトスピチャンピオンシップ予選 東京会場、カードショップ名等）",
    "format": "レギュレーション/フォーマット（「スタンダード」「エターナル」「コラボ限定」「ブロックアイコン限定」「契約編」等。「エターナル」の記載がある場合や過去環境の場合は「エターナル」、特に指定がない通常大会は「スタンダード」）",
    "deckType": "優勝デッキタイプ・系統・デッキ名（例: 鋼契約、紫エヴァ、造兵、碧雷、アイカツ、獄契約、蒼契約等。不明時は空文字）",
    "participants": "参加人数（数字のみ。例: 16、8。不明時は空文字。「名」や「人」などの単位は付けない）",
    "winner": "優勝者・プレイヤー名（不明時は空文字）",
    "notes": "準優勝デッキやその他特記事項があれば記載（なければ空文字）"
  }

]

※該当する結果が1件もない場合は空配列 `[]` を返してください。

入力ツイート一覧:
$itemsJson
""".trimIndent()
    }

    private fun cleanParticipants(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val digits = Regex("""\d+""").find(raw)
        return digits?.value ?: ""
    }



    private fun parseGeminiResponse(responseBody: String): List<ShopBattleRecord> {
        return try {
            val root = json.decodeFromString<JsonObject>(responseBody)
            val candidates = root["candidates"]?.jsonArray ?: return emptyList()
            val first = candidates.firstOrNull()?.jsonObject ?: return emptyList()
            val content = first["content"]?.jsonObject ?: return emptyList()
            val parts = content["parts"]?.jsonArray ?: return emptyList()
            val text = parts.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content ?: return emptyList()

            json.decodeFromString<List<ShopBattleRecord>>(text)
        } catch (e: Exception) {
            echo("Geminiレスポンスのパース失敗: ${e.message}")
            emptyList()
        }
    }

    private fun escapeCsv(value: String): String {
        val needQuote = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needQuote) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun resolveApiKey(cliKey: String?): String? {
        if (!cliKey.isNullOrBlank()) return cliKey.trim()

        val envNames = listOf("GEMINI_API_KEY", "GOOGLE_API_KEY", "GOOGLE_GENAI_API_KEY")
        for (env in envNames) {
            val v = System.getenv(env)
            if (!v.isNullOrBlank()) return v.trim()
        }

        // カレントディレクトリおよび上位ディレクトリから .env / mise.gemini.toml を探索
        var cur: File? = File(".").canonicalFile
        while (cur != null && cur.exists()) {
            val envFile = File(cur, ".env")
            if (envFile.isFile && envFile.exists()) {
                val lines = envFile.readLines()
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("#") || !trimmed.contains("=")) continue
                    val (key, value) = trimmed.split("=", limit = 2).map { it.trim().trim('"', '\'') }
                    if (key in envNames && value.isNotBlank()) {
                        return value
                    }
                }
            }

            val miseGeminiFile = File(cur, "mise.gemini.toml")
            if (miseGeminiFile.isFile && miseGeminiFile.exists()) {
                val extracted = extractKeyFromMiseToml(miseGeminiFile, envNames)
                if (!extracted.isNullOrBlank()) return extracted
            }

            val dotConfigMiseGemini = File(cur, ".config/mise.gemini.toml")
            if (dotConfigMiseGemini.isFile && dotConfigMiseGemini.exists()) {
                val extracted = extractKeyFromMiseToml(dotConfigMiseGemini, envNames)
                if (!extracted.isNullOrBlank()) return extracted
            }

            cur = cur.parentFile
        }

        return null
    }

    private fun extractKeyFromMiseToml(file: File, envNames: List<String>): String? {
        val content = try {
            // まず sops で復号を試行
            val process = ProcessBuilder("sops", "-d", file.absolutePath)
                .redirectErrorStream(true)
                .start()
            val text = process.inputStream.bufferedReader().readText()
            if (process.waitFor() == 0 && text.isNotBlank()) {
                text
            } else {
                file.readText()
            }
        } catch (e: Exception) {
            file.readText()
        }

        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#") || !trimmed.contains("=")) continue
            val (k, v) = trimmed.split("=", limit = 2).map { it.trim().trim('"', '\'') }
            if (k in envNames && v.isNotBlank()) {
                return v
            }
        }
        return null
    }
}

