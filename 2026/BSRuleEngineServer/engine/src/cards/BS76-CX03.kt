package bstools.model.cards

import bstools.model.*

/**
 * [BS76-CX03] 光虫の旗手ファラ
 */
object CardBS76_CX03 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "BS76-CX03", name = "光虫の旗手ファラ", cost = 3,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.NEXUS, lvCosts = listOf(0),
        symbols = listOf(CardColor.WHITE), systems = listOf("フラッグ", "光契約", "旗種"),
        effect = "このカード/ネクサスの効果すべては相手の効果を受けず、魂状態を含むこのネクサスには自分のフィールド/リザーブのコアを好きなだけ置いて《F契約煌臨》できる。【契約煌臨元】/【ネクサス】［Lv1］『このスピリット/ネクサスの配置/アタック時』自分のトークンの「プラチナム・バグ」1体を出せる。このとき、ターンに1回、対象のLv1コストと同じになるようにボイドからコアを置いて出せる。【契約煌臨元】/【ネクサス】［Lv1］自分が《F契約煌臨》したか、自分の「プラチナム・バグ」が出たとき、自分のカウント+1できる（カウントは最大10まで）。その後、《F契約煌臨》していたとき、自分の「プラチナム・バグ」1体を破壊できる。そうしたとき、相手のバースト1つをデッキの下に戻す。",
        lvBps = listOf(), shinkai = false
    )
}
