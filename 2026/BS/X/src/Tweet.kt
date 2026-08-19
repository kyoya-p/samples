package xtool

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Tweet(
    val id: String,
    val url: String,
    val author: String,
    val handle: String,
    val postedAt: String?,
    val text: String,
    val hasImage: Boolean,
    val hasVideo: Boolean,
    val replyCount: Long,
    val retweetCount: Long,
    val likeCount: Long,
) {
    fun saveToCache(cacheDir: File = CACHE_DIR) {
        try {
            cacheDir.mkdirs()
            val datePrefix = extractYyyyMmDd(postedAt)?.let { "${it}_" } ?: ""
            val file = File(cacheDir, "$datePrefix$id.json")
            val json = Json { prettyPrint = true }
            file.writeText(json.encodeToString(this), Charsets.UTF_8)
        } catch (e: Exception) {
            System.err.println("キャッシュ保存失敗 ($id): ${e.message}")
        }
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        private fun extractYyyyMmDd(postedAt: String?): String? {
            if (postedAt.isNullOrBlank()) return null
            val match = Regex("""^(\d{4})-?(\d{2})-?(\d{2})""").find(postedAt.trim())
            return match?.let { "${it.groupValues[1]}${it.groupValues[2]}${it.groupValues[3]}" }
        }

        fun loadFromCache(id: String, cacheDir: File = CACHE_DIR): Tweet? {
            if (!cacheDir.exists() || !cacheDir.isDirectory) return null
            val file = cacheDir.listFiles { f ->
                f.isFile && (f.name == "$id.json" || f.name.endsWith("_$id.json") || f.name.endsWith("-$id.json"))
            }?.firstOrNull() ?: return null
            return try {
                json.decodeFromString<Tweet>(file.readText(Charsets.UTF_8))
            } catch (e: Exception) {
                null
            }
        }

        fun loadAllFromCache(cacheDir: File = CACHE_DIR): List<Tweet> {
            if (!cacheDir.exists() || !cacheDir.isDirectory) return emptyList()
            val files = cacheDir.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: return emptyList()
            return files.mapNotNull { file ->
                try {
                    json.decodeFromString<Tweet>(file.readText(Charsets.UTF_8))
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun parseTweets(raw: Any?): List<Tweet> {
    val list = raw as? List<Map<String, Any?>> ?: return emptyList()
    return list.mapNotNull { m ->
        val id = m["id"] as? String ?: return@mapNotNull null
        Tweet(
            id = id,
            url = m["url"] as? String ?: "",
            author = m["author"] as? String ?: "",
            handle = m["handle"] as? String ?: "",
            postedAt = m["postedAt"] as? String,
            text = m["text"] as? String ?: "",
            hasImage = m["hasImage"] as? Boolean ?: false,
            hasVideo = m["hasVideo"] as? Boolean ?: false,
            replyCount = (m["replyCount"] as? Number)?.toLong() ?: 0L,
            retweetCount = (m["retweetCount"] as? Number)?.toLong() ?: 0L,
            likeCount = (m["likeCount"] as? Number)?.toLong() ?: 0L,
        )
    }
}
