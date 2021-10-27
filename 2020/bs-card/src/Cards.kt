package BSSim


enum class Category { SPIRITCARD, BRAVECARD, NEXUSCARD, MAGICCARD, DECK, HAND, TRASH, RSV, BURST, CARDTRUSH, CORETRASH, LIFE, RESERVE, PICKEDCARD, PICKEDCORE, PLACE, DECKBOTTOM }

abstract class Card(
        val category: Category,
        val name: String,
        val colors: Color,
        val cost: Int,
        val simbols: Sbl,
        val reduction: Sbl,
        val family: Set<Family>,
        val lvInfo: List<LevelInfo>
) : Maneuver {
    data class LevelInfo(val level: Int, val keepCore: Int, val bp: Int)

    override fun toString() = "$name"
}


data class CardAttr(
        val place: FO
) {
    data class Mutable(var place: FO) {
        fun toImmutable() = CardAttr(place = place)
    }

    fun toMutable() = Mutable(place = place)
    fun tr(op: Mutable.() -> Unit) = toMutable().apply { this@apply.op() }.toImmutable()

    fun tr(place: FO = this.place): CardAttr = CardAttr(place)
}

typealias Cards = List<Card>

fun Cards.pick(ca: Card): Sequence<Pair<Card, Cards>> = sequence {
    val n = indexOf(ca)
    if (n >= 0) {
        yield(ca to (take(n) + drop(n + 1)))
    }
}

fun Cards.top(n: Int, amap: Boolean = true): Sequence<Cards> = if (amap) {
    sequenceOf(take(min(this.size, n)))
} else {
    if (n > this.size) sequenceOf()
    else sequenceOf(take(n))
}

fun Cards.remove(c: Card): Cards = pick(c).onlyTakeOneCase().second
fun Cards.remove(cs: Cards): Cards = cs.fold(this) { res, c -> res.remove(c) }


// デッキボトムを示す仮想カード
// これがデッキからなくなったら遷移終了
//   終了パターンに保存し、遷移先は無し
// デッキの掘り具合を示す指標とする
class DeckBottom : Card(category = Category.DECKBOTTOM
        , name = "DeckBotton"
        , colors = Color.None
        , cost = 0
        , simbols = Sbl.Zero
        , reduction = Sbl.Zero, family = setOf(), lvInfo = listOf()) {
    override val efName = "DeckBottom:Dummy"
    override fun use(h: History): ParallelWorld = if (h.world.ownSide.deck.cards.contains(this)) {
        sequenceOf(h)
    } else {
        println("[Terminate simulation] Reached bottom of deck. $h")
        sequenceOf()
    }
}


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


