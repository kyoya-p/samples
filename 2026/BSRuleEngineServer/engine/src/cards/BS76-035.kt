package bstools.model.cards

import bstools.model.*

/**
 * [BS76-035] プラチナム・バシリスク
 */
object CardBS76_035 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "BS76-035", name = "プラチナム・バシリスク", cost = 4,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2, 5),
        symbols = listOf(CardColor.WHITE), systems = listOf("光契約", "旗種", "甲竜"),
        effect = "［フラッシュ］《F契約煌臨：光契約》『自分のターン』自分のをトラッシュに置くことで、対象の自分のスピリット/ネクサスに手札から重ねる。［Lv1-2-3］『このスピリットの煌臨/アタック時』ターンに1回、自分のデッキを上から3枚オープンできる。その中の系統：「旗種」を持つ白のカード1枚を手札に加える。自分の「プラチナム・バグ」1体を破壊することで、この効果で手札に加えるカードの枚数を+1枚する。残ったカードはデッキの下に戻す。\n［Lv2-3］『このスピリットのアタック時』煌臨元を含む自分の「プラチナム・バグ」1体につき、相手の創界神ネクサスのコア3個をボイドに置ける。",
        lvBps = listOf(4000, 6000, 8000), shinkai = false
    )
}
