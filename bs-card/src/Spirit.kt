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
            sequenceOf(p).action(op配置(this))
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


