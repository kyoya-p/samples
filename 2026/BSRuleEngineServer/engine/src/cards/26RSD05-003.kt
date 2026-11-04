package bstools.model.cards

import bstools.model.*

/**
 * [26RSD05-003] ラニラヤ
 */
object Card26RSD05_003 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD05-003", name = "ラニラヤ", cost = 3,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.YELLOW), systems = listOf("黄玉", "雷竜"),
        effect = "[Lv1-2]『破壊時』相手のスピリット1体を指定できる。このターン中、それをBP-2000する。その後、それがBP0なら破壊する。",
        lvBps = listOf(3000, 4000), shinkai = false
    )
}
