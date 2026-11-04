package bstools.model.cards

import bstools.model.*

/**
 * [26RSD05-006] セマローグ
 */
object Card26RSD05_006 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD05-006", name = "セマローグ", cost = 5,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.YELLOW), systems = listOf("黄玉", "雷竜"),
        effect = "[Lv1-2]『アタック時』相手のスピリット1体を指定する。このターン中、それをBP-2000する。その後、それがBP0なら破壊する。\n[Lv2]《真界放》『アタック中』〔重複不可〕自分が系統：「雷竜」を持つマジックカードを使用した時、1枚ドローする。",
        lvBps = listOf(5000, 7000), shinkai = true
    )
}
