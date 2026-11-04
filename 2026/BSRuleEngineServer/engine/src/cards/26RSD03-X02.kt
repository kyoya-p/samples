package bstools.model.cards

import bstools.model.*

/**
 * [26RSD03-X02] 深巣ダンテクレオ
 */
object Card26RSD03_X02 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD03-X02", name = "深巣ダンテクレオ", cost = 6,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 4),
        symbols = listOf(CardColor.GREEN), systems = listOf("甲魚"),
        effect = "《継召》トラッシュのEXシンボルを除外し軽減できる。\n[Lv1-2]『召喚時』系統：「甲魚」を持つ自分のスピリット2体までを指定する。このターン中、それらをBP+3000する。\n[Lv2]《真界放》『自分のアタックステップ』【起動：フラッシュ】〔ターン1回〕このスピリット以外の系統：「甲魚」を持つ自分のスピリット1体を指定する。それを回復させる。",
        lvBps = listOf(6000, 9000), shinkai = true
    )
}
