package BSSim

import javax.swing.UIManager.put

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
    override fun effect(p: Transition): Sequence<Transition> = sequenceOf()
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
    override fun effect(p: Transition): Sequence<Transition> =
            sequenceOf(p).action(op召喚配置(this))
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
    override fun effect(p: Transition): Sequence<Transition> = sequenceOf(p)
            .sqOwnSide_flatMap { this.opPayCost(card.cost - fieldSimbols.reduction(card.reduction)) } // コストをフィールドから選択しトラッシュに置く
            .sqOwnSide_flatMap {
                val newFo = Place(card.name) //FOを新しく生成
                setPlaceCardBy(newFo) { listOf(card) } //カードを置いてフィールドに配置
                        .opMoveCore(newFo, payableCoreHolders, 1)// [TODO]とりあえす1コアおいてみる
            }
}
