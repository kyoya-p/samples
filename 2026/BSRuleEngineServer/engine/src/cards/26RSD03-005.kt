package bstools.model.cards

import bstools.model.*

/**
 * [26RSD03-005] モラモーラ
 */
object Card26RSD03_005 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD03-005", name = "モラモーラ", cost = 4,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.GREEN), systems = listOf("海緑", "甲魚"),
        effect = "",
        lvBps = listOf(5000, 8000), shinkai = false
    )
}
