package BSSim


abstract class MagicCard(
        category: Category
        , name: String
        , colors: Color
        , cost: Int
        , reduction: Sbl
) : Card(
        category = Category.MAGICCARD
        , name = name
        , colors = colors
        , cost = cost
        , simbols = Sbl.Zero
        , reduction = reduction
        , family = setOf()
        , lvInfo = listOf()
) {
    override val efName: String = "Magic"
}


class eマジック使用(val card: Card, val op: History.() -> ParallelWorld = { sequenceOf(this) }) : Maneuver {
    override val efName: String = "使用"
    override fun use(p: History): Sequence<History> = sequenceOf(p)
            .effect(ePayCardCost(card)) // コストを支払い
            .flatMap { it.op() }
            .flatMap_ownSide { opMoveCard(CARDTRASH, card) } // カードをトラッシュへ
            .effect(e消滅チェック())
}


