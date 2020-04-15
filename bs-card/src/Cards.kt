package BSSim


enum class Category { SPIRITCARD, BRAVECARD, NEXUSCARD, MAGICCARD, DECK, HAND, TRASH, RSV, BURST, CARDTRUSH, CORETRASH, LIFE, RESERVE, PICKEDCARD, PICKEDCORE, PLACE }

abstract class Card(
        val category: Category,
        val name: String,
        val colors: Color,
        val cost: Int,
        val simbols: Sbl,
        val reduction: Sbl,
        val familiy: Set<Family>,
        val lvInfo: List<LevelInfo>
) : Effectable("Card"), Effect {
    data class LevelInfo(val level: Int, val keepCore: Int, val bp: Int)

    override fun toString() = "$name"
}


data class CardAttr(
        val place: FO
) {
    fun tr(place: FO = this.place): CardAttr = CardAttr(place)
}


typealias Cards = List<Card>

fun Cards.pick(ca: Card): Sequence<Pair<Card, Cards>> = sequence {
    val n = indexOf(ca)
    if (n >= 0) {
        yield(ca to (take(n) + drop(n + 1)))
    }
}

fun Cards.top(n: Int): Sequence<Cards> = if (n > this.size) sequenceOf() else sequenceOf(take(n))
fun Cards.bottom(n: Int): Sequence<Cards> = if (n > this.size) sequenceOf() else sequenceOf(takeLast(n))

fun Cards.remove(c: Card): Cards = pick(c).onlyTakeOneCase().second
fun Cards.remove(cs: Cards): Cards = cs.fold(this) { res, c -> res.remove(c) }

fun main() {
    class T1 : SpiritCard(Category.SPIRITCARD, "T1", Color.Y, 0, Sbl.Y, Sbl.Zero, setOf(), listOf())

    val t1_1 = T1()
    val t1_2 = T1()
    t1_1 assert t1_1
    t1_1 assertNot t1_2

    setOf(t1_1) assert setOf(t1_1)
    setOf(t1_1) assertNot setOf(t1_2)
    setOf(t1_1, t1_2) assert setOf(t1_1, t1_2)
    setOf(t1_1, t1_2) assert setOf(t1_2, t1_1)
    listOf(t1_1, t1_2) assert listOf(t1_1, t1_2)
    listOf(t1_1, t1_2) assertNot listOf(t1_2, t1_1)
}
