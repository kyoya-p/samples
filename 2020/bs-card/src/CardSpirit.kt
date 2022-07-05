package BSSim

open class SummonnableCard(
        category: Category
        , name: String
        , colors: Color
        , cost: Int
        , simbols: Sbl
        , reduction: Sbl
        , family: Set<Family>
        , lvInfo: List<LevelInfo>
) : Card(
        category
        , name
        , colors
        , cost
        , simbols
        , reduction
        , family
        , lvInfo
) {
    override val efName: String = "召喚配置可能カード"
    override fun use(h: History): ParallelWorld = e召喚配置(this).use(h)
}

open class SpiritCard(
        category: Category
        , name: String
        , colors: Color
        , cost: Int
        , simbols: Sbl
        , reduction: Sbl
        , family: Set<Family>
        , lvInfo: List<LevelInfo>
) : SummonnableCard(
        category
        , name
        , colors
        , cost
        , simbols
        , reduction
        , family
        , lvInfo
) {
//    override fun use(p: History): Sequence<History> =
    //          sequenceOf(p).choices(op召喚配置(this))

    override val efName: String = "Spirit"
}

open class BraveCard(
        category: Category
        , name: String
        , colors: Color
        , cost: Int
        , simbols: Sbl
        , reduction: Sbl
        , family: Set<Family>
        , lvInfo: List<LevelInfo>
) : SummonnableCard(
        category
        , name
        , colors
        , cost
        , simbols
        , reduction
        , family
        , lvInfo
) {
    //override val effects0: List<Effect> get() = listOf(op召喚(this))
}

open class NexusCard(
        category: Category
        , name: String
        , colors: Color
        , cost: Int
        , simbols: Sbl
        , reduction: Sbl
        , family: Set<Family>
        , lvInfo: List<LevelInfo>
) : SummonnableCard(
        category
        , name
        , colors
        , cost
        , simbols
        , reduction
        , family
        , lvInfo
) {
    //override val effects0: List<Effect> get() = listOf(op召喚(this))
}


// スピリット/ブレイヴの召喚、ネクサスの配置

class e召喚配置(val card: Card) : Maneuver {
    override val efName: String = "召喚 配置"
    override fun use(p: History): Sequence<History> = sequenceOf(p)
            .effect(ePayCardCost(card)) // コストを支払い
            .flatMap_ownSide { opCreateFO(card) } // カードをフィールドに配置
            .flatMap_ownSide { opMoveCore(fo(card), payableCoreHolders, card.lvInfo[0].keepCore) } // TODO: とりあえず最少維持コア
            .effect(e消滅チェック())
}

class e召喚配置_NoCost(val card: Card) : Maneuver {
    override val efName: String = "召喚 配置"
    override fun use(p: History): Sequence<History> = sequenceOf(p)
            .flatMap_ownSide { opCreateFO(card) } // カードをフィールドに配置
            .flatMap_ownSide { opMoveCore(fo(card), payableCoreHolders, card.lvInfo[0].keepCore) } // TODO: とりあえず最少維持コア
            .effect(e消滅チェック())
}

