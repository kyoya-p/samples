package bstools.model.cards

import bstools.model.*

/**
 * [BS76-041] プラチナム・マンティス
 */
object CardBS76_041 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "BS76-041", name = "プラチナム・マンティス", cost = 8,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE, CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3, 5),
        symbols = listOf(CardColor.WHITE), systems = listOf("光契約", "旗種", "光虫"),
        effect = "［フラッシュ］《F契約煌臨：光契約＆C6以上》『お互いのアタックステップ』自分のをトラッシュに置くことで、対象の自分のスピリット/ネクサスに手札から重ねる。このカードが系統：「旗種」を持つ効果でデッキからオープンされたとき、自分のトークンの「プラチナム・バグ」1体を出せる。［Lv1-2-3］『このスピリットの煌臨/アタック時』相手のスピリット/アルティメット/ネクサス1つをデッキの下に戻せる。自分の「プラチナム・バグ」1体を破壊することで、この効果でデッキに戻す数を+1する。",
        lvBps = listOf(7000, 13000, 15000), shinkai = false
    )
}
