package bstools.model.cards

import bstools.model.*

/**
 * [26RSD05-005] ルクアンス
 */
object Card26RSD05_005 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD05-005", name = "ルクアンス", cost = 4,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.YELLOW), systems = listOf("黄玉", "雷竜"),
        effect = "",
        lvBps = listOf(5000, 8000), shinkai = false
    )
}
