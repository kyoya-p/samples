package bstools.model.cards

import bstools.model.*

/**
 * [26RSD03-008] 重顎ドロゴリアス
 */
object Card26RSD03_008 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD03-008", name = "重顎ドロゴリアス", cost = 5,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.GREEN), systems = listOf("甲魚"),
        effect = "[Lv1-2]『アタック中』系統：「甲魚」を持つ自分のスピリット1体につき、このスピリットをBP+1000する。\n[Lv2]『アタック時』コスト3以下の相手のスピリット1体を指定できる。それを疲労させる。",
        lvBps = listOf(5000, 6000), shinkai = false
    )
}
