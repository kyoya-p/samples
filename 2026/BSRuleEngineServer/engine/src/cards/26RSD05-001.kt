package bstools.model.cards

import bstools.model.*

/**
 * [26RSD05-001] クイール
 */
object Card26RSD05_001 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD05-001", name = "クイール", cost = 2,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.YELLOW), systems = listOf("黄玉", "雷竜"),
        effect = "[Lv2]『破壊時』相手のスピリット1体を指定できる。このターン中、それをBP-2000する。その後、それがBP0なら破壊する。",
        lvBps = listOf(1000, 2000), shinkai = false
    )
}
