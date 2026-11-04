package bstools.model.cards

import bstools.model.*

/**
 * [26RSD05-012] リバースサンダー
 */
object Card26RSD05_012 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD05-012", name = "リバースサンダー", cost = 3,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.MAGIC, lvCosts = listOf(0),
        symbols = listOf(), systems = listOf("雷竜"),
        effect = "トラッシュのこのカードは、系統：「雷竜」を持つ自分のスピリットが破壊された時、手札に戻せる。自身は同じカード名のこの効果をターンに1回しか使えない。\n[フラッシュ]相手のスピリット1体を指定する。このターン中、それをBP-2000する。その後、それがBP0なら破壊する。",
        lvBps = listOf(), shinkai = false
    )
}
