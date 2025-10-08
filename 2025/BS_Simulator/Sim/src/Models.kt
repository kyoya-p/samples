data class Game(
    val myBoard: Board,
    //val enemyBoard: Board
)

data class Board(val deck: Deck, val trash: List<Card>, val hands: List<Card>, val field: Field)
data class Deck(val fixed: Int/*戦闘からn枚が固定カード*/, val cards: List<Card>)
data class Field(val objects: List<Object>)
data class Card(
    val cardType: CardType,
    val cardName: String,
    val cardSymbols: List<Symbol>,
    val cardCost: Int,
    val cardReduction: List<Symbol>
) {
    fun cost() = cardCost
    fun reduction() = cardReduction
    fun symbols() = cardSymbols
}

data class Symbol(val color: List<SymbolColor>, val value: Int)
data class Object(val cards: List<Card>, val cores: Int)

enum class CardType(val type: String) {
    SPIRIT("S"),
    NEXUS("N"),
}

enum class SymbolColor {
    PURPLE,
    GREEN,
    GOD
}
