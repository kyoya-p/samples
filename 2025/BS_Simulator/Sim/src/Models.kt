import kotlinx.serialization.Serializable

//@Serializable
data class Game(
    val myBoard: Board,
//    val isTerminated: Boolean = false,
    val turnCount: Int,
    val turnPlayer: Int = 0,
    val step: STEP = STEP.START(),
)

sealed class STEP {
    class START : STEP()
    class CORE : STEP()
    class DROW : STEP()
    class REFRESH : STEP()
    sealed class MAIN : STEP() {
        class PRE : MAIN()
        class IN : MAIN()
        class OUT : MAIN()
        class POST : MAIN()
    }
}

sealed class CorePlace {
    object Boid : CorePlace()
    class Field(val obj: Object) : CorePlace()
    object Reserve : CorePlace()
    object CoreTrash : CorePlace()
}

sealed class Event {
    class MoveCore(val src: CorePlace, val dst: CorePlace) : Event()
}

data class Board(
    val deck: Deck,
    val cardTrash: List<Card>,
    val hands: List<Card>,
    val field: Field,
    val reserve: Int,
    val coreTrash: Int
)

data class Deck(val fixed: Int/*戦闘からn枚が固定カード*/, val cards: List<Card>)

data class Field(val objects: List<Object>)

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
