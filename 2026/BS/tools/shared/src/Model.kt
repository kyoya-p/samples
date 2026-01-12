import kotlinx.serialization.Serializable

@Serializable
data class SearchCard(
    val cardNo: String,
    val name: String,
    val rarity: String,
    val cost: String,
    val category: String, // S:スピリット、U:アルティメット, B:ブラヴ、N:ネクサス、M:マジック
    val attribute: String, // 全色なら"赤紫緑白黄青"
    val systems: List<String>,
    val imgUrl: String,
)

@Serializable
data class Card(
    val cardNo: String,
    val sideA: CardFace,
    val sideB: CardFace?,
)

@Serializable
data class CardFace(
    val cardNo: String,
    val side: String, // "" or "A" or "B"
    val name: String,
    val rarity: String,
    val cost: Int,
    val symbols: String, // 紫シンボル1個と緑シンボル1個なら "紫1緑1", 赤神シンボル1個なら "赤神1"
    val reductionSymbols: String, // 赤軽減１個、青軽減2個なら "赤1青2"、全色軽減2個なら"全2"
    val category: String, // S:スピリット、U:アルティメット, B:ブラヴ、N:ネクサス、M:マジック
    val attributes: String, // 全色なら"赤紫緑白黄青"
    val systems: List<String>,
    val lvInfo: List<String>, // Lv1維持コア2個でBP1000の場合、 "1,2,1000"。ネクサスの場合でLv2維持コア1個の場合、"2,1"
    val effect: String,
    val imageUrl: String
)

