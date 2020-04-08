package BSSim

import java.util.Collection

enum class Category { SPIRITCARD, BRAVECARD, NEXUSCARD, MAGICCARD, DECK, HAND, TRASH, RSV, BURST, CARDTRUSH, CORETRASH, LIFE, RESERVE, PICKEDCARD, PICKEDCORE }

abstract class Card(
        val category: Category,
        val name: String,
        val colors: Color,
        val cost: Int,
        val simbols: Sbl,
        val reduction: Sbl,
        val familiy: Set<Family>,
        val lvInfo: List<LevelInfo>
) : Effectable("Card"), Comparable<Card> {
    data class LevelInfo(val level: Int, val keepCore: Int, val bp: Int)

    //override val effects0 get() = listOf<Effect>()

    override fun hashCode(): Int {
        return category.hashCode() * 3131 + name.hashCode() * 31 + cost
    }

    override fun equals(o: Any?): Boolean {
        o as Card
        return category == o.category
                && name == o.name
                && colors == o.colors
                && cost == o.cost
                && simbols == o.simbols
                && reduction == o.reduction
                && familiy == o.familiy
                && lvInfo == o.lvInfo
    }

    override fun compareTo(o: Card): Int {
        return category.compareTo(o.category)
                .if0 { name.compareTo(o.name) }
                .if0 { colors.compareTo(o.colors) }
                .if0 { cost.compareTo(o.cost) }
                .if0 { simbols.compareTo(o.simbols) }
    }

    inline operator fun times(n: Int): List<Card> = mutableListOf<Card>().also {
        for (i in 0 until n) {
            it.add(this)
        }
    }

    override fun toString() = "$name"
}

//typealias Cards = List<Card>
class Cards(val cs: List<Card>) : Comparable<Cards> {
    constructor() : this(listOf())

    override fun compareTo(other: Cards): Int {
        val i1 = cs.iterator()
        val i2 = other.cs.iterator()
        while (i1.hasNext() || i2.hasNext()) {
            i1.hasNext().compareTo(i2.hasNext()).let { if (it != 0) return it }
            i1.next().compareTo(i2.next()).let { if (it != 0) return it }
        }
        return 0
    }

    fun take(n: Int) = Cards(cs.take(n))
    fun drop(n: Int) = Cards(cs.drop(n))
    operator fun plus(b: Cards): Cards = Cards(cs + b.cs)

}

inline fun Cards.pick(ca: Card): Sequence<Pair<Card, Cards>> = sequence {
    val n = cs.indexOf(ca)
    if (n >= 0) {
        yield(ca to (take(n) + drop(n + 1)))
    }
}

inline fun Cards.drop(ca: Card): Cards = pick(ca).take1Case().second
inline fun Cards.drops(cas: Cards): Cards = cas.cs.fold(this) { cs, ca -> cs.drop(ca) }
