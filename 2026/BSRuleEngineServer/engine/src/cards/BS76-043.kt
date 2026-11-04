package bstools.model.cards

import bstools.model.*

/**
 * [BS76-043] プラチナム・イグアナ
 */
object CardBS76_043 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "BS76-043", name = "プラチナム・イグアナ", cost = 10,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE, CardColor.WHITE, CardColor.WHITE, CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.WHITE), systems = listOf("旗種", "甲竜"),
        effect = "［フラッシュ］手札にあるこのカードは召喚できる。この効果で召喚するとき、自分の「プラチナム・バグ」3体を破壊することで、このカードをコスト1にする。この効果で召喚したとき、相手のスピリット1体をデッキの下に戻せる。［Lv1-2］系統：「旗種」を持つ自分の白のスピリットすべてに“【重装甲：赤/紫/黄/青】このスピリットは、相手の赤/紫/黄/青のスピリット/ブレイヴ/ネクサス/マジックの効果を受けない”を与える。",
        lvBps = listOf(12000, 16000), shinkai = false
    )
}
