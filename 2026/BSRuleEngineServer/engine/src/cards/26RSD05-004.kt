package bstools.model.cards

import bstools.model.*

/**
 * [26RSD05-004] ディヴァエス
 */
object Card26RSD05_004 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD05-004", name = "ディヴァエス", cost = 4,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.YELLOW), systems = listOf("雷竜"),
        effect = "[Lv2]《真界放》『アタック時』相手のスピリット1体を指定する。このターン中、それをBP-2000する。その後、それがBP0なら破壊する。",
        lvBps = listOf(4000, 6000), shinkai = true
    )
}
