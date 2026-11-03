package bstools.model.cards

import bstools.model.*

/**
 * [26RSD05-009] 雷坑道
 */
object Card26RSD05_009 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD05-009", name = "雷坑道", cost = 3,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.NEXUS, lvCosts = listOf(0, 3),
        symbols = listOf(CardColor.YELLOW), systems = listOf("雷竜"),
        effect = "[Lv1-2]『自分のアタックステップ』系統：「雷竜」を持つ自分のスピリットすべては、BP1000以下の相手のスピリットからブロックされない。\n[Lv2]『自分のメインステップ開始時』〔重複不可〕疲労状態の系統：「雷竜」を持つ自分のスピリットがいるなら、相手のスピリット1体を指定できる。このターン中、それをBP-2000する。その後、それがBP0なら破壊する。",
        lvBps = listOf(), shinkai = false
    )
}
