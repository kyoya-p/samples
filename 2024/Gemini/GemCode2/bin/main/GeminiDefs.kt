import kotlinx.serialization.Serializable

val GEMINI_1_0_PRO = "v1/models/gemini-pro"
val GEMINI_1_5_FLUSH = "v1beta/models/gemini-1.5-flash-latest"
val GEMINI_1_5_PRO = "v1beta/models/gemini-pro"

@Serializable
data class ReqGemini(val contents: List<ReqContent>)

@Serializable
data class ReqContent(val parts: List<ReqPart>)

@Serializable
data class ReqPart(val text: String)

@Serializable
data class ResGemini(
    val candidates: List<ResCandidate>,
    val usageMetadata: ResUsageMetadata
)

@Serializable
data class ResCandidate(
    val content: ResContent,
    val finishReason: String,
    val index: Int,
    val safetyRatings: List<ResSafetyRating>,
    val citationMetadata: ResCitationMetadata? = null
)

@Serializable
data class ResContent(val parts: List<ResPart>, val role: String)

@Serializable
data class ResPart(val text: String)

@Serializable
data class ResSafetyRating(val category: String, val probability: String)

@Serializable
data class ResCitationMetadata(val citationSources: List<ResCitationSource>)

@Serializable
data class ResCitationSource(
    val startIndex: Int,
    val endIndex: Int,
    val uri: String,
    val license: String? = null
)

@Serializable
data class ResUsageMetadata(val promptTokenCount: Int, val candidatesTokenCount: Int, val totalTokenCount: Int)

