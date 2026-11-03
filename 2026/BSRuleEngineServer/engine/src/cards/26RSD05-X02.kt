package bstools.model.cards

import bstools.model.*

/**
 * [26RSD05-X02] 雷術ルクーナス
 */
object Card26RSD05_X02 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD05-X02", name = "雷術ルクーナス", cost = 7,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW, CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.YELLOW), systems = listOf("雷竜"),
        effect = "《継召》トラッシュのEXシンボルを除外し軽減できる。\n[Lv1-2]『召喚時』相手のスピリット1体を指定する。このターン中、それをBP-4000する。その後、それがBP0なら破壊する。\n[Lv2]《真界放》『アタック時』相手のスピリット1体を指定する。このターン中、それをBP-4000する。その後、それがBP0なら破壊する。",
        lvBps = listOf(7000, 9000), shinkai = true
    )
}
