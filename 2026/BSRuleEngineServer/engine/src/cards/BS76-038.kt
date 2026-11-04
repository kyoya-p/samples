package bstools.model.cards

import bstools.model.*

/**
 * [BS76-038] プラチナム・アゲハ
 */
object CardBS76_038 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "BS76-038", name = "プラチナム・アゲハ", cost = 5,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2, 5),
        symbols = listOf(CardColor.WHITE), systems = listOf("光契約", "旗種", "光虫"),
        effect = "［フラッシュ］《F契約煌臨：光契約＆C1以上》『お互いのアタックステップ』自分のをトラッシュに置くことで、対象の自分のスピリット/ネクサスに手札から重ねる。［Lv1-2-3］『このスピリットの煌臨/アタック時』相手のスピリット/ネクサス1つをデッキの下に戻すことで、ボイドからコア1個をこのスピリットに置く。\n［Lv2-3］『このスピリットのアタック/ブロック時』自分のトークンの「プラチナム・バグ」1体を出せる。",
        lvBps = listOf(4000, 8000, 11000), shinkai = false
    )
}
