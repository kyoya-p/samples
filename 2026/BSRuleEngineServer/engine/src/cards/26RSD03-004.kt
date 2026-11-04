package bstools.model.cards

import bstools.model.*

/**
 * [26RSD03-004] ストラレイ
 */
object Card26RSD03_004 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD03-004", name = "ストラレイ", cost = 3,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.GREEN), systems = listOf("海緑", "甲魚"),
        effect = "",
        lvBps = listOf(4000, 7000), shinkai = false
    )
}
