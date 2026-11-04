package bstools.model.cards

import bstools.model.*

/**
 * [26RSD03-X01] 甲手スクィード
 */
object Card26RSD03_X01 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD03-X01", name = "甲手スクィード", cost = 5,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.GREEN), systems = listOf("海緑", "甲魚"),
        effect = "[Lv1-2]『召喚時』疲労状態の相手のスピリット1体を指定できる。それを重疲労させる。\n[Lv2]『アタック中』〔ターン1回〕バトル終了時、このスピリットを回復できる。",
        lvBps = listOf(5000, 7000), shinkai = false
    )
}
