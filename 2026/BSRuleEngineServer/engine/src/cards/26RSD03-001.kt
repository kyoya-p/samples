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
        val minCores = requiredCoresOnSummon(data) // 1 (Lv1コスト)

        // 「召喚コストの支払いと上に置くコアをリザーブから使用して召喚できる」
        // 支払い・配置ともにリザーブのコアのみ使用可能 (総合ルール・カードテキスト指定)
        val rNormal = player.reserve.normal
        val rSoul = player.reserve.soul
        val totalReserve = rNormal + rSoul

        if (totalReserve < cost + minCores) return emptyList()

        val actions = mutableListOf<GameAction>()

        // 1. コスト支払いに通常コアのみを使用する場合
        if (rNormal >= cost) {
            val remainNorm = rNormal - cost
            val remainSoul = rSoul

            // Lv1配置 (通常コア1個)
            if (remainNorm >= 1) {
                actions.add(
                    GameAction(
                        index = nextIndex(),
                        type = "FLASH_SUMMON",
                        category = "⚡【フラッシュ召喚】 フラッティ",
                        detail = "フラッシュ召喚 Lv1 (コスト${cost}支払い, 配置1個)",
                        handIndex = handIndex,
                        costToPay = cost,
                        payFrom = listOf(CoreAlloc("Reserve", "R", normal = cost, soul = 0)),
                        placeFrom = listOf(CoreAlloc("Reserve", "R", normal = 1, soul = 0))
                    )
                )
            }
            // Lv1配置 (ソウルコア1個)
            if (remainSoul == 1) {
                actions.add(
                    GameAction(
                        index = nextIndex(),
                        type = "FLASH_SUMMON",
                        category = "⚡【フラッシュ召喚】 フラッティ",
                        detail = "フラッシュ召喚 Lv1[S] (コスト${cost}支払い, 配置ソウルコア)",
                        handIndex = handIndex,
                        costToPay = cost,
                        payFrom = listOf(CoreAlloc("Reserve", "R", normal = cost, soul = 0)),
                        placeFrom = listOf(CoreAlloc("Reserve", "R", normal = 0, soul = 1))
                    )
                )
            }
            // Lv2配置 (通常コア3個)
            if (remainNorm >= 3) {
                actions.add(
                    GameAction(
                        index = nextIndex(),
                        type = "FLASH_SUMMON",
                        category = "⚡【フラッシュ召喚】 フラッティ",
                        detail = "フラッシュ召喚 Lv2 (コスト${cost}支払い, 配置3個)",
                        handIndex = handIndex,
                        costToPay = cost,
                        payFrom = listOf(CoreAlloc("Reserve", "R", normal = cost, soul = 0)),
                        placeFrom = listOf(CoreAlloc("Reserve", "R", normal = 3, soul = 0))
                    )
                )
            }
        }

        // 2. コスト支払いにソウルコアを含める場合 (rSoul == 1 かつ rNormal >= cost - 1)
        if (rSoul == 1 && cost >= 1 && rNormal >= cost - 1) {
            val remainNorm = rNormal - (cost - 1)
            // 配置に通常コア1個
            if (remainNorm >= 1) {
                actions.add(
                    GameAction(
                        index = nextIndex(),
                        type = "FLASH_SUMMON",
                        category = "⚡【フラッシュ召喚】 フラッティ",
                        detail = "フラッシュ召喚 Lv1 (ソウルコアでコスト支払い, 配置1個)",
                        handIndex = handIndex,
                        costToPay = cost,
                        payFrom = listOf(CoreAlloc("Reserve", "R", normal = cost - 1, soul = 1)),
                        placeFrom = listOf(CoreAlloc("Reserve", "R", normal = 1, soul = 0))
                    )
                )
            }
        }

        return actions
    }
}
