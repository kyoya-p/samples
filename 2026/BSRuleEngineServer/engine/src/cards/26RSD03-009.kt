package bstools.model.cards

import bstools.model.*

/**
 * [26RSD03-009] 透笠オリンディアス
 */
object Card26RSD03_009 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD03-009", name = "透笠オリンディアス", cost = 6,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.GREEN), systems = listOf("甲魚"),
        effect = "[Lv1-2]『召喚時』自分のデッキの上から3枚をオープンする。その中のコスト3以下で系統：「甲魚」を持つカード1枚を召喚/配置できる。残りは好きな順でデッキの下に戻す。\n[Lv2]『ブロック時』相手のスピリット1体を指定できる。それを疲労させる。",
        lvBps = listOf(6000, 7000), shinkai = false
    )
}
