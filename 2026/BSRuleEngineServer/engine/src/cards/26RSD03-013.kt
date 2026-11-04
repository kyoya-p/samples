package bstools.model.cards

import bstools.model.*

/**
 * [26RSD03-013] ボルテクスシェイブ
 */
object Card26RSD03_013 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD03-013", name = "ボルテクスシェイブ", cost = 4,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.MAGIC, lvCosts = listOf(0),
        symbols = listOf(), systems = listOf("甲魚"),
        effect = "[フラッシュ]相手のスピリット1体を指定する。それを疲労させる。",
        lvBps = listOf(), shinkai = false
    )
}
