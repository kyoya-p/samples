package xtool

import kotlinx.serialization.Serializable

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
)

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
