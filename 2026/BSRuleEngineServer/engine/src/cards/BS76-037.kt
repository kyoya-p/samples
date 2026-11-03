package bstools.model.cards

import bstools.model.*

/**
 * [BS76-037] プラチナム・トリバ
 */
object CardBS76_037 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "BS76-037", name = "プラチナム・トリバ", cost = 4,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE, CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.WHITE), systems = listOf("旗種", "光虫"),
        effect = "デッキからオープンされたこのカードは、自分のミラージュをセットしていないとき、コストを支払わずにセットできる。［ミラージュ：コスト2（このカードは手札からセットできる）］【セット中】自分が《F契約煌臨》したとき、自分のトークンの「プラチナム・バグ」1体を出せる。そうしたとき、このターンの間、そのトークンは相手の効果を受けない。",
        lvBps = listOf(5000, 7000), shinkai = false
    )
}
