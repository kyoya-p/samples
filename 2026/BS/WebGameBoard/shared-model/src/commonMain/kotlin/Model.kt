import kotlinx.serialization.Serializable

@Serializable
data class SearchCard(
    val cardId: String,
    val name: String,
    val rarity: String,
    val cost: String,
    val type: String,
    val color: String,
    val symbols: List<String>,
    val imgUrl: String
)
