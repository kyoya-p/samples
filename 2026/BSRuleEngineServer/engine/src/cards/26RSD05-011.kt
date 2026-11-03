package bstools.model.cards

import bstools.model.*

/**
 * [26RSD05-011] ファイアピラー
 */
object Card26RSD05_011 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD05-011", name = "ファイアピラー", cost = 3,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.MAGIC, lvCosts = listOf(0),
        symbols = listOf(), systems = listOf("雷竜"),
        effect = "[メイン]1枚ドローする。\n[フラッシュ]相手のスピリット1体を指定する。このターン中、それをBP-2000する。その後、それがBP0なら破壊する。",
        lvBps = listOf(), shinkai = false
    )
}
