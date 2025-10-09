import kotlinx.serialization.Serializable

@Serializable
data class Game(
    val myBoard: Board,
    val isTerminated: Boolean = false,
    val turnCount: Int = 0
)

@Serializable
data class Board(
    val deck: Deck,
    val cardTrash: List<Card>,
    val hands: List<Card>,
    val field: Field,
    val reserve: Int,
    val coreTrash: Int
)

@Serializable
data class Deck(val fixed: Int/*戦闘からn枚が固定カード*/, val cards: List<Card>)
@Serializable
data class Field(val objects: List<Object>)
@Serializable
data class Card(
    val cardType: CardType,
    val cardName: String,
    val cardSymbols: List<Symbol>,
    val cardCost: Int,
    val cardReduction: List<ReductionSymbol>,
    val lvCosts: List<Int>
) {
    fun cost() = cardCost
    fun reduction() = cardReduction
    fun symbols() = cardSymbols
}

@Serializable
data class Symbol(val color: List<SymbolColor>, val value: Int)
@Serializable
data class ReductionSymbol(val color: SymbolColor, val value: Int)
@Serializable
data class Object(val cards: List<Card>, val cores: Int)

enum class CardType(val type: String) {
    SPIRIT("S"),
    NEXUS("N"),
}

enum class SymbolColor {
    RED,
    PURPLE,
    GREEN,
    WHITE,
    YELLOW,
    BLUE,
    GOD,
    ULTIMATE,
}
