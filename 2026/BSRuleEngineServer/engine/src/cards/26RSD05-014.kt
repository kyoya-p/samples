package bstools.model.cards

import bstools.model.*

/**
 * [26RSD05-014] トリプルサンダー
 */
object Card26RSD05_014 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD05-014", name = "トリプルサンダー", cost = 6,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.MAGIC, lvCosts = listOf(0),
        symbols = listOf(), systems = listOf("雷竜"),
        effect = "《ソウルマジック：黄》自分の黄シンボルがあればだけで使用できる。\n[フラッシュ]相手のスピリット1体を指定する。このターン中、それをBP-2000する。その後、それがBP0なら破壊する。これらを3回行う。",
        lvBps = listOf(), shinkai = false
    )
}
