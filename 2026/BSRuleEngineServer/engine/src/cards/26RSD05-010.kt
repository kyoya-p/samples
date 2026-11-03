package bstools.model.cards

import bstools.model.*

/**
 * [26RSD05-010] 最奥：大雷山嶺
 */
object Card26RSD05_010 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD05-010", name = "最奥：大雷山嶺", cost = 4,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.NEXUS, lvCosts = listOf(0, 3),
        symbols = listOf(CardColor.YELLOW), systems = listOf("雷竜"),
        effect = "[Lv1-2]『相手のアタックステップ開始時』色1色を指定できる。このステップ中、BP1000以下の相手のスピリットすべては、その色のシンボル1つを失う。\n[Lv2]《真界放》『自分のアタックステップ』〔重複不可・ターン1回〕自分のスピリットの効果で、BP0の相手のスピリットを破壊した時、1枚ドローする。",
        lvBps = listOf(), shinkai = true
    )
}
