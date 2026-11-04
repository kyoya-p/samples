package bstools.model.cards

import bstools.model.*

/**
 * [BS76-033] プラチナム・グッピー
 */
object CardBS76_033 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "BS76-033", name = "プラチナム・グッピー", cost = 2,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.WHITE), systems = listOf("旗種", "空魚"),
        effect = "このカードが系統：「旗種」を持つ効果でデッキからオープンされたとき、自分のトークンの「プラチナム・バグ」1体を出せる。［Lv1-2］『このスピリットの召喚時』魂状態/煌臨元の自分の白のフラッグネクサスカード1枚を配置できる。",
        lvBps = listOf(2000, 4000), shinkai = false
    )
}
