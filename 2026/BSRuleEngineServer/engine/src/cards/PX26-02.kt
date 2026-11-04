package bstools.model.cards

import bstools.model.*

/**
 * [PX26-02] ピットリポーター ポリッシュ
 */
object CardPX26_02 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "PX26-02", name = "ピットリポーター ポリッシュ", cost = 4,
        colors = listOf(CardColor.RED, CardColor.PURPLE, CardColor.GREEN, CardColor.WHITE, CardColor.YELLOW, CardColor.BLUE),
        reductionSymbols = listOf(),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2, 4),
        symbols = listOf(CardColor.RED), systems = listOf("旗種", "眷属", "道化"),
        effect = "自分の効果でデッキからオープンされたこのカードは、セットできる。〔ターンに1回：同名〕セットしているこのカードは、系統：「旗種」/「十冠」/「神皇」を持つ自分のスピリットがアタックしたとき、ただちに発動できる。［Ｓバースト：自分か相手の《封印》後］1枚ドローし、ボイドからコア1個を自分のリザーブに置く。この効果発揮後、このカードをコストを支払わずに召喚する。\n［Lv2-3］このスピリットのシンボルは赤/紫/緑/白/青としても扱う。",
        lvBps = listOf(3000, 5000, 6000), shinkai = false
    )
}
