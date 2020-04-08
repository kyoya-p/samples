package BSSim

import BSSim.*

// BattleSpitirs Object
// フィールドのスピリット/ネクサス等、デッキ、手札、トラッシュ、リザーブなど
// カードのリスト、コアを置ける
//  識別は置かれた一番上のカードで行う。フィールドオブジェクトでない場合(デッキなど)は仮想カードIDで指定
class BSO(val id: String, val ordering: Boolean = true, val cards: Cards = listOf<Card>(), val core: Core = Core(0)) : Comparable<BSO> {
    constructor(org: BSO, ordering: Boolean = org.ordering, id: String = org.id, cards: Cards = org.cards, core: Core = org.core) : this(id, ordering, cards, core)

    val name = if (cards.size >= 1) cards[0].name else id

    override fun hashCode(): Int {
        return (id.hashCode() * 31 + core.hashCode()) * 31 + cards.fold(0) { s, ca ->
            ca.hashCode() + (if (ordering) s * 31 else s)
        }
    }

    override fun equals(o: Any?): Boolean {
        o as BSO
        return id == o.id && cards == o.cards && core == o.core
    }

    override fun compareTo(o: BSO): Int {
        id.compareTo(o.id).let { if (it != 0) return it }
        cards.size.compareTo(o.cards.size).let { if (it != 0) return it }
        cards.zip(o.cards).forEach { (a, b) -> a.compareTo(b).let { if (it != 0) return it } }
        core.compareTo(o.core).let { if (it != 0) return it }
        return 0
    }

    override fun toString() = if (id != "") {
        "$id" + (if (core.c != 0) ":${core}" else "") + (if (cards.size > 0) ":${cards[0]}" else "") + (if (cards.size >= 2) "~${cards.size}" else "")
    } else {
        "$name" + (if (core.c != 0) ":${core}" else "")
    }

    val simbols: Sbl = cards.let { if (it.size >= 1) it[0].simbols else Sbl.Zero }

    inline fun tr(id: String = this.id, cards: Cards = this.cards, core: Core = this.core): BSO = BSO(id = id, cards = cards, core = core)

    inline fun modifyCoreBy(op: (Core) -> Core): BSO = BSO(id = id, core = op(core), cards = cards)
    inline fun modifyCardBy(op: (Cards) -> Cards): BSO = BSO(id = id, core = core, cards = op(cards))

    companion object {
        class VCard(category: Category) : Card(
                category = category
                , name = ""
                , colors = Color.None
                , cost = 0
                , simbols = Sbl.Zero
                , reduction = Sbl.Zero
                , familiy = setOf()
                , lvInfo = listOf()
        ) {
            override fun effect(tr: Transition): Sequence<Transition> = sequenceOf()
        }


    }
}


typealias BSOs = List<BSO>


fun BSO.Companion.test() {
    val Ca1 = ちょうちん()
    val Ca1a = ちょうちん()
    val Ca2 = 子フ()
    val Ca3 = ピグレ()
    val Ca4 = オルリ()
    val cas1 = BSO(id = "Dk", core = Core(0), cards = listOf<Card>(Ca1, Ca2, Ca3, Ca4)).cards
    cas1.pickTop(2).toList().assert(listOf(listOf(Ca1, Ca2) to listOf(Ca3, Ca4)))
    cas1.pickBottom(2).toList().assert(listOf(listOf(Ca4, Ca3) to listOf(Ca1, Ca2)))
    cas1.pickTop(5).toList().assert(listOf<List<Card>>())

    listOf(子フ(), ピグレ(), 子フ()).drops(listOf(子フ(), 子フ())).assert(listOf(ピグレ()))
}