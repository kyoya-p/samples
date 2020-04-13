package BSSim

// カード、コアを置ける場所
open class Place(val name: String)

data class PlaceAttr(
        val core: Core = Core(0)
        , val cardOrdering: Boolean = false //順序に意味があるか
        , val cards: Cards = listOf()  //スタックされる場合トップのカードが[0]
) {
    fun tr(cardOrdering: Boolean = this.cardOrdering, cards: Cards = this.cards, core: Core = this.core): PlaceAttr =
            PlaceAttr(
                    cardOrdering = cardOrdering
                    , cards = if (cardOrdering) cards else cards.toSet().toList()
                    , core = core
            )

    override fun hashCode() = cardOrdering.hashCode() * 31 + core.hashCode() * 91 + if (cardOrdering) cards.fold(0) { s, t -> s * 31 + t.hashCode() } else cards.fold(0) { s, t -> s + t.hashCode() }
    override fun equals(o: Any?): Boolean {
        o as PlaceAttr
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

    fun top(n: Int): Cards = if (cards.size >= n) cardList.take(n) else throw Exception("Not enough Cards.") //カード枚数が不足

    fun stack(cs: Cards): PlaceAttr = tr(cards = cs + cardList)
    fun stackBottom(cs: Cards): PlaceAttr = tr(cards = cardList + cs)
}

open class FieldPlace(name: String) : Place(name)