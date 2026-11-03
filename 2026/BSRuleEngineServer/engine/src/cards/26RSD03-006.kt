package bstools.model.cards

import bstools.model.*

/**
 * [26RSD03-006] 海幻コエラ＝カンサス
 */
object Card26RSD03_006 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD03-006", name = "海幻コエラ＝カンサス", cost = 5,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.GREEN), systems = listOf("甲魚"),
        effect = "[Lv1-2]『召喚時』このスピリット以外の系統：「甲魚」を持つ自分のスピリット1体を指定できる。ボイドからコア1個をそれに置く。その後、疲労状態の系統：「甲魚」を持つ自分のスピリットがいるなら、自分のスピリット1体を指定できる。それを回復させる。",
        lvBps = listOf(4000, 6000), shinkai = false
    )
}
