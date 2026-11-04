package bstools.model.cards

import bstools.model.*

/**
 * [BS76-086] ラージスクアッシュ
 */
object CardBS76_086 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "BS76-086", name = "ラージスクアッシュ", cost = 4,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.MAGIC, lvCosts = listOf(0),
        symbols = listOf(), systems = listOf("旗種"),
        effect = "手札にあるこのカードは、相手の効果を受けない。使用したこのカードは、煌臨元を含む自分のフラッグネクサスがあるとき、このマジックの効果発揮後まで、相手の効果を受けない。\n［フラッシュ］相手のスピリット/アルティメット1体までを手札に戻す。その後、自分の「プラチナム・バグ」1体を破壊することで、このターンの間、自分のライフは1しか減らない。",
        lvBps = listOf(), shinkai = false
    )
}
