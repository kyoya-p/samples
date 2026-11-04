package bstools.model.cards

import bstools.model.*

/**
 * [26RSD05-X01] 雷噴ペールクアール
 */
object Card26RSD05_X01 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD05-X01", name = "雷噴ペールクアール", cost = 7,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.YELLOW), systems = listOf("雷竜"),
        effect = "《継召》トラッシュのEXシンボルを除外し軽減できる。\n[Lv1-2]『アタック中』このスピリットはBP4000以下の相手のスピリットからブロックされない。\n[Lv2]『自分のアタックステップ開始時』このターン中、相手のスピリットすべてをBP-2000する。その後、それらがBP0なら破壊する。",
        lvBps = listOf(8000, 10000), shinkai = false
    )
}
