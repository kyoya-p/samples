package bstools.model.cards

import bstools.model.*

/**
 * [BS76-034] プラチナム・パラボリザード
 */
object CardBS76_034 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "BS76-034", name = "プラチナム・パラボリザード", cost = 3,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.WHITE), systems = listOf("旗種", "甲竜"),
        effect = "このカードが系統：「旗種」を持つ効果でデッキからオープンされたとき、自分のトークンの「プラチナム・バグ」1体を出せる。［Lv1-2］『このスピリットの召喚時』自分のカウント+1する。その後、ボイドからコア1個ずつをこのスピリットと自分の「プラチナム・バグ」1体に置ける。\n［Lv2］お互い、コストを支払わずにミラージュカードをセットするとき、そのカードの本来のコストを支払わなければセットできない。",
        lvBps = listOf(3000, 4000), shinkai = false
    )
}
