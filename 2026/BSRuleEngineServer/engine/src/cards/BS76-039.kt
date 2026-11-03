package bstools.model.cards

import bstools.model.*

/**
 * [BS76-039] プラチナム・アーマードフィン
 */
object CardBS76_039 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "BS76-039", name = "プラチナム・アーマードフィン", cost = 6,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2, 5),
        symbols = listOf(CardColor.WHITE), systems = listOf("旗種", "空魚"),
        effect = "〔ターンに1回：同名〕このカードを召喚するとき、自分の「プラチナム・バグ」1体を破壊することで、このカードをコスト3にする。［Lv1-2-3］『このスピリットの召喚/アタック時』自分のデッキを上から3枚オープンできる。その中の系統：「旗種」を持つ白のカード1枚を手札に加える。残ったカードはデッキの下に戻す。\n［Lv2-3］『自分のアタックステップ』系統：「旗種」を持つ自分の白のスピリットがアタックしている間、相手は効果でアタックステップを終了できない。",
        lvBps = listOf(5000, 9000, 12000), shinkai = false
    )
}
