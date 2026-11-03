package bstools.model.cards

import bstools.model.*

/**
 * [26RSD05-007] 雷探ペルボーグ
 */
object Card26RSD05_007 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD05-007", name = "雷探ペルボーグ", cost = 5,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.YELLOW), systems = listOf("雷竜"),
        effect = "[Lv1-2]『召喚時』自分のデッキの上から3枚をオープンできる。その中の「雷探ペルボーグ」以外の系統：「雷竜」を持つカード1枚を手札に加える。残りは好きな順でデッキの下に戻す。",
        lvBps = listOf(3000, 4000), shinkai = false
    )
}
