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
    override fun effect(p: History): Sequence<History> = sequenceOf()
    override val efName: String = "召喚配置可能カード"
    override fun use(h: History): ParallelWorld = Ef召喚配置(this).use(h)
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
    override fun effect(p: History): Sequence<History> =
            sequenceOf(p).choices(op召喚配置(this))

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
//
// 維持コストをフィールドから選択しFOに置く
class op召喚配置(val card: Card) : Effectable("召喚 配置") {
    override fun effect(p: History): Sequence<History> = sequenceOf(p)
            .flatMap_ownSide { this.opPayCost(card.cost - fieldSimbols.reduction(card.reduction)) } // コストをフィールドから選択しトラッシュに置く
            .flatMap_ownSide {
                val newFo = FO(card.efName) //FOを新しく生成
                setPlaceCardBy(newFo) { listOf(card) } //カードを置いてフィールドに配置
                        .opMoveCore(newFo, payableCoreHolders, 1)// [TODO]とりあえす1コアおいてみる
            }
}

class Ef召喚配置(val card: Card) : Effect {
    override val efName: String = "召喚 配置"
    override fun use(p: History): Sequence<History> = sequenceOf(p)
            .flatMap_ownSide {
                val reqCore = card.cost - fieldSimbols.reduction(card.reduction)
                opPayCost(if (reqCore < 0) 0 else reqCore)
            } // コストを支払い
            .flatMap_ownSide {
                val newFo = FO(card.name) //FOを新しく生成し
                opCreateNewFO(newFo) // それをフィールドに配置
                        .flatMap { it.opMoveCard(newFo, card) } //カードを置き
                        .flatMap { opMoveCore(newFo, payableCoreHolders, 1) } // [TODO]とりあえす1コアおいてみる
            }
}
