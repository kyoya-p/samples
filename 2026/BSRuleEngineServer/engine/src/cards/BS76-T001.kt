package bstools.model.cards

import bstools.model.*

/**
 * [BS76-T001] プラチナム・バグ
 */
object CardBS76_T001 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "BS76-T001", name = "プラチナム・バグ", cost = 1,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(),
        category = CardCategory.SPIRIT, lvCosts = listOf(1),
        symbols = listOf(CardColor.WHITE), systems = listOf("旗種", "光虫"),
        effect = "このカードはデッキに入れられず、魂状態/煌臨元を含む自分の「光虫の旗手ファラ」がなければ出せない。【契約煌臨元】/【スピリット】［Lv1］自分の効果か相手によってフィールドを離れるこのスピリットを、《F契約煌臨》を持つ自分の白のスピリットの下に煌臨元カードとして追加できる。【契約煌臨元】このスピリットは、BP+1000され、相手のスピリット/アルティメットの効果を受けない。さらに、〔重複不可〕このスピリットの煌臨元カードが6枚以上の間、系統：「旗種」を持つ自分の白1色のスピリットすべてをBP+10000し、白シンボル1つを追加する。",
        lvBps = listOf(1000), shinkai = false
    )
}
