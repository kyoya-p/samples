package bstools.model.cards

import bstools.model.*

/**
 * [BS76-072] 白の大音楽堂
 */
object CardBS76_072 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "BS76-072", name = "白の大音楽堂", cost = 3,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.NEXUS, lvCosts = listOf(0, 1),
        symbols = listOf(CardColor.WHITE), systems = listOf("旗種"),
        effect = "［Lv1-2］『このネクサスの配置時』自分のデッキを上から3枚オープンできる。その中の系統：「旗種」を持つ白のカードを、自分の手札と好きなだけ入れ替える。オープンしたカードはデッキの下に戻す。この効果で入れ替えたとき、自分のカウント+1する。\n［Lv2］自分の手札にある系統：「旗種」を持つ白のカードすべては、相手の効果を受けない。さらに、〔ターンに1回：同名〕自分が「プラチナム・バグ」を出すとき、対象すべては、Lv1コストと同じになるようにボイドからコアを置いて出せる。",
        lvBps = listOf(), shinkai = false
    )
}
