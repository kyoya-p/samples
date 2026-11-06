package bstools.model.cards

import bstools.model.*

/**
 * [26RSD03-001] フラッティ
 *
 * 効果テキスト:
 * [フラッシュ]『相手のアタックステップ』
 * 手札のこのカードは、召喚コストの支払いと上に置くコアをリザーブから使用して召喚できる。
 */
object Card26RSD03_001 : CardDefinition {
    override val data: CardData = CardData(
        cardNo = "26RSD03-001", name = "フラッティ", cost = 2,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.GREEN), systems = listOf("甲魚"),
        effect = "[フラッシュ]『相手のアタックステップ』手札のこのカードは、召喚コストの支払いと上に置くコアをリザーブから使用して召喚できる。",
        lvBps = listOf(1000, 3000), shinkai = false
    )

    override fun enumerateFlashActions(
        state: GameState,
        player: PlayerState,
        opponent: PlayerState,
        handIndex: Int,
        nextIndex: () -> Int
    ): List<GameAction> {
        // 『相手のアタックステップ』: 手番側(activePlayer)が対戦相手のときのみ発動可能
        if (state.activePlayerId == player.playerId) return emptyList()
        if (handIndex < 0 || handIndex >= player.hand.size) return emptyList()
        val card = player.hand[handIndex]
        if (card.cardNo != data.cardNo) return emptyList()

        // 召喚コスト (自陣の緑シンボルによる軽減)
        val cost = summonCost(data, player)

        // 「召喚コストの支払いと上に置くコアをリザーブから使用して召喚できる」
        // 支払い・配置ともにリザーブのコアのみ使用可能 (総合ルール・カードテキスト指定)
        val sources = listOf(
            CoreSource("Reserve", "R", player.reserve.normal, player.reserve.soul, player.reserve.total, keep = 0)
        )
        val places = placementOptions(data, cost, sources)
        if (places.isEmpty()) return emptyList()

        val category = "⚡【フラッシュ召喚】 ${data.name}"
        val acts = mutableListOf<GameAction>()
        for (place in places) {
            val rawActs = playOptions(data, handIndex, category, "FLASH_SUMMON", cost, place, sources, limit = Int.MAX_VALUE)
            for (act in rawActs) {
                acts.add(act.copy(index = nextIndex()))
            }
        }
        return acts
    }
}
