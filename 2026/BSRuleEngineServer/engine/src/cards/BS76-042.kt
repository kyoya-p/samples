package bstools.model.cards

import bstools.model.*

/**
 * [BS76-042] プラチナム・ジンベー
 */
object CardBS76_042 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "BS76-042", name = "プラチナム・ジンベー", cost = 9,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE, CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.WHITE), systems = listOf("旗種", "空魚"),
        effect = "セットしているこのカードは、自分の「プラチナム・バグ」が出たとき、自分のカウント3以上なら、ただちに発動できる。［バースト：相手の『このスピリット/ブレイヴの召喚時』発揮後］煌臨元を含む自分の「光虫の旗手ファラ」1つにつき、自分のトークンの「プラチナム・バグ」1体を好きなだけ出す。その後、ボイドからコア1個ずつを自分の「プラチナム・バグ」3体に置ける。この効果発揮後、このカードをコストを支払わずに召喚する。\n［Lv2］『このスピリットのアタック/ブロック時』相手のスピリット/ネクサス1つをデッキの下に戻せる。",
        lvBps = listOf(8000, 14000), shinkai = false
    )
}
