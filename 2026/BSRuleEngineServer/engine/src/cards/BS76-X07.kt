package bstools.model.cards

import bstools.model.*

/**
 * [BS76-X07] 光翅の導き手ファラ・ルクス
 */
object CardBS76_X07 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "BS76-X07", name = "光翅の導き手ファラ・ルクス", cost = 7,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE, CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2, 5),
        symbols = listOf(CardColor.WHITE), systems = listOf("光契約", "旗種", "光虫"),
        effect = "［フラッシュ］《F契約煌臨：光契約＆C4以上》『お互いのアタックステップ』自分のをトラッシュに置くことで、対象の自分のスピリット/ネクサスに手札から重ねる。【煌臨中】［Lv1-2-3：フラッシュ］『お互いのアタックステップ』自分の「プラチナム・バグ」1体を破壊できる。そうしたとき、相手のスピリット/アルティメット1体をデッキの下に戻す。この効果は相手の効果では防げない。その後、ターンに1回、このスピリットは回復できる。\n【煌臨中】［Lv2-3：フラッシュ］『自分のアタックステップ』煌臨元の自分の「プラチナム・バグ」4枚を破棄することで、相手のライフのコア2個までをトラッシュに置く。",
        lvBps = listOf(7000, 13000, 16000), shinkai = false
    )
}
