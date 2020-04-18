package BSSim

// カード、コアを置ける場所
open class FO(val name: String) {
    override fun toString(): String = name
    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = name == (other as FO).name
}


data class FoAttr(
        val id: FO
        , val core: Core = Core(0)
        , val cardOrdering: Boolean = false //順序に意味があるか
        , val cards: Cards = listOf()  //スタックされる場合トップのカードが[0]
) {
    data class Mutable(
            var id: FO
            , var core: Core = Core(0)
            , var cardOrdering: Boolean = false //順序に意味があるか
            , var cards: MutableList<Card> = mutableListOf()  //スタックされる場合トップのカードが[0]
    ) {
        fun toImmutable() = FoAttr(id = id, core = core, cardOrdering = cardOrdering, cards = cards)
    }

    fun toMutable() = Mutable(id = id, core = core, cardOrdering = cardOrdering, cards = cards.toMutableList())
    fun tr(op: Mutable.() -> Unit) = toMutable().apply { op() }.toImmutable()

    override fun toString() = "${id.name}" + (if (core.c > 0) ":${core}" else "") + if (cards.size != 0) "${cards}" else ""

    fun tr(id: FO = this.id, cardOrdering: Boolean = this.cardOrdering, cards: Cards = this.cards, core: Core = this.core): FoAttr =
            FoAttr(
                    id = id
                    , cardOrdering = cardOrdering
                    , cards = if (cardOrdering) cards else cards.toSet().toList()
                    , core = core
            )

    override fun hashCode() = cardOrdering.hashCode() * 31 + core.hashCode() * 91 + if (cardOrdering) cards.fold(0) { s, t -> s * 31 + t.hashCode() } else cards.fold(0) { s, t -> s + t.hashCode() }
    override fun equals(o: Any?): Boolean {
        o as FoAttr
        if (cardOrdering != o.cardOrdering) return false
        if (core != o.core) return false
        if (cardOrdering) {
            if (cards != o.cards) return false
        } else {
            if (cardSet != o.cardSet) return false
        }
        return true
    }

    val cardSet get() = cards.toSet()

    val cardList
        get() = if (cardOrdering) {
            cards
        } else {
            throw Exception("The cards are out of order.")
        }

    fun top(n: Int): Cards = if (cards.size >= n) cardList.take(n) else throw Exception("Not enough Cards. size=${cards.size}<${n} ${this}.${hashCode()}") //カード枚数が不足

    fun stack(cs: Cards): FoAttr = tr(cards = cs + cardList)
    fun stackBottom(cs: Cards): FoAttr = tr(cards = cardList + cs)
}


open class FieldFO(name: String) : FO(name)

