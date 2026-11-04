package bstools.model.cards

import bstools.model.*

/**
 * [26RSD03-014] テンタクルアタック
 */
object Card26RSD03_014 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD03-014", name = "テンタクルアタック", cost = 6,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.MAGIC, lvCosts = listOf(0),
        symbols = listOf(), systems = listOf("甲魚"),
        effect = "《ソウルマジック：緑》自分の緑シンボルがあればだけで使用できる。\n[フラッシュ]相手のスピリット1体を指定する。それを重疲労させる。",
        lvBps = listOf(), shinkai = false
    )
}
