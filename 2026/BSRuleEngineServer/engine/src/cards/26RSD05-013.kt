package bstools.model.cards

import bstools.model.*

/**
 * [26RSD05-013] ネストリング
 */
object Card26RSD05_013 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD05-013", name = "ネストリング", cost = 4,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.MAGIC, lvCosts = listOf(0),
        symbols = listOf(), systems = listOf("雷竜"),
        effect = "《継召》トラッシュのEXシンボルを除外し軽減できる。\n[メイン]自分の手札の系統：「雷竜」を持つカード2枚までを相手に見せる。それらを好きな順でデッキの下に戻す。2枚戻したなら、3枚ドローする。\n[フラッシュ]自分のスピリット1体を指定する。このターン中、それをBP+3000する。",
        lvBps = listOf(), shinkai = false
    )
}
