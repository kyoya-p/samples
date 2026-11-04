package bstools.model.cards

import bstools.model.*

/**
 * [BS76-085] フォトンサークル
 */
object CardBS76_085 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "BS76-085", name = "フォトンサークル", cost = 2,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE),
        category = CardCategory.MAGIC, lvCosts = listOf(0),
        symbols = listOf(), systems = listOf("旗種"),
        effect = "〔ターンに1回：同名〕トラッシュにあるこのカードは、系統：「光契約」を持つ自分のスピリットが《F契約煌臨》したとき、手札に戻せる。\n［フラッシュ］自分のトークンの「プラチナム・バグ」1体を出せる。その後、自分のカウント6以上のとき、このターンの間、自分の「プラチナム・バグ」すべては相手の効果を受けない。",
        lvBps = listOf(), shinkai = false
    )
}
