package bstools.model.cards

import bstools.model.*

/**
 * [26RSD05-002] アマール
 */
object Card26RSD05_002 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD05-002", name = "アマール", cost = 3,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.YELLOW), systems = listOf("雷竜"),
        effect = "[Lv1-2]『召喚時』自分のデッキの上から3枚をオープンできる。その中の系統：「雷竜」を持つマジックカード1枚を手札に加える。残りは好きな順でデッキの下に戻す。",
        lvBps = listOf(2000, 3000), shinkai = false
    )
}
