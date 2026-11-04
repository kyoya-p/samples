package bstools.model.cards

import bstools.model.*

/**
 * [26RSD05-008] 雷天拳ウィーギル
 */
object Card26RSD05_008 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD05-008", name = "雷天拳ウィーギル", cost = 6,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.YELLOW), systems = listOf("雷竜"),
        effect = "[Lv1-2]『召喚時』相手のスピリット1体を指定する。このターン中、それをBP-2000する。その後、それがBP0なら破壊する。\n[Lv2]《真界放》『アタック時』相手のスピリット1体を指定する。このターン中、それをBP-2000する。その後、それがBP0なら破壊する。",
        lvBps = listOf(6000, 7000), shinkai = true
    )
}
