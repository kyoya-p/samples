package bstools.model.cards

import bstools.model.*
import bstools.model.effects.CardScriptRegistry

/**
 * [26RSD03-010] 最奥：緑淵深
 *
 * 効果テキスト:
 * [Lv1-2]『自分のアタックステップ』アタックしている系統：「甲魚」を持つ自分のスピリットすべてをBP+1000する。
 * [Lv2]《真界放》【起動：メイン】〔ターン1回〕自分の手札のコスト4以下で系統：「甲魚」を持つカード1枚を相手に見せる。それをデッキの下に戻す▶1枚ドローする。
 */
object Card26RSD03_010 : CardDefinition {
    override val data: CardData = CardData(
        cardNo = "26RSD03-010", name = "最奥：緑淵深", cost = 3,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.NEXUS, lvCosts = listOf(0, 3),
        symbols = listOf(CardColor.GREEN), systems = listOf("甲魚"),
        effect = "[Lv1-2]『自分のアタックステップ』アタックしている系統：「甲魚」を持つ自分のスピリットすべてをBP+1000する。\n[Lv2]《真界放》【起動：メイン】〔ターン1回〕自分の手札のコスト4以下で系統：「甲魚」を持つカード1枚を相手に見せる。それをデッキの下に戻す▶1枚ドローする。",
        lvBps = listOf(), shinkai = true
    )

    fun register() {
        CardScriptRegistry.register(this)
    }

    override fun computeAuraBpModifier(
        state: GameState,
        sourceCard: FieldCard,
        targetCard: FieldCard,
        isAttackingHypothetical: Boolean
    ): Int {
        // [Lv1-2]『自分のアタックステップ』アタックしている系統：「甲魚」を持つ自分のスピリットすべてをBP+1000する。
        if (sourceCard.level < 1) return 0
        val owner = if (state.player1.field.any { it.instanceId == sourceCard.instanceId }) state.player1 else state.player2
        if (state.activePlayerId != owner.playerId) return 0
        val isAttackStep = state.step == Step.ATTACK_DECLARATION || state.step == Step.FLASH_TIMING || state.step == Step.BLOCK_DECLARATION || state.step == Step.BATTLE_RESOLUTION
        if (!isAttackStep) return 0
        if (targetCard.category != CardCategory.SPIRIT || !targetCard.systems.contains("甲魚")) return 0
        val isAttacking = state.attackingCardId == targetCard.instanceId || isAttackingHypothetical
        return if (isAttacking) 1000 else 0
    }

    override fun enumerateFieldActions(
        state: GameState,
        card: FieldCard,
        player: PlayerState,
        nextIndex: () -> Int
    ): List<GameAction> {
        // [Lv1-2]《真界放》【起動：メイン】〔ターン1回〕(第1メインで使用したら第2メインでは使用不可)
        if (card.level < 1) return emptyList()
        if (state.step != Step.MAIN && state.step != Step.MAIN_2) return emptyList()
        if (state.activePlayerId != player.playerId) return emptyList()
        val effectKey = "${card.instanceId}:MAIN_ACTIVATE"
        val nameKey = "${player.playerId}:${card.cardNo}:MAIN_ACTIVATE"
        if (state.usedOncePerTurnEffects.contains(effectKey) ||
            state.usedOncePerTurnEffects.contains(nameKey) ||
            state.usedOncePerTurnEffects.contains("${card.instanceId}:Lv2_SHINKAI_MAIN")) return emptyList()

        // 自分の手札のコスト4以下で系統：「甲魚」を持つカード
        val eligibleIndices = player.hand.indices.filter { idx ->
            val c = player.hand[idx]
            c.cost <= 4 && c.systems.contains("甲魚")
        }.distinctBy { player.hand[it].cardNo }

        return eligibleIndices.map { idx ->
            val targetCard = player.hand[idx]
            GameAction(
                index = nextIndex(),
                type = "ACTIVATE_FIELD",
                category = "🔮【起動：メイン】 ${card.displayName}",
                detail = "手札の「${targetCard.name}」(コスト${targetCard.cost}) をオープンしてデッキ下に戻し、1枚ドロー",
                targetInstanceId = card.instanceId,
                handIndex = idx
            )
        }
    }

    override fun onFieldAction(
        state: GameState,
        card: FieldCard,
        player: PlayerState,
        action: GameAction,
        messages: MutableList<String>
    ) {
        val effectKey = "${card.instanceId}:MAIN_ACTIVATE"
        val nameKey = "${player.playerId}:${card.cardNo}:MAIN_ACTIVATE"
        if (state.usedOncePerTurnEffects.contains(effectKey) ||
            state.usedOncePerTurnEffects.contains(nameKey) ||
            state.usedOncePerTurnEffects.contains("${card.instanceId}:Lv2_SHINKAI_MAIN")) {
            messages.add("⚠️ 「${card.displayName}」の【起動：メイン】はターンに1回しか使えません")
            return
        }
        if (action.handIndex < 0 || action.handIndex >= player.hand.size) return
        val cardToReturn = player.hand.removeAt(action.handIndex)
        // デッキの下に戻す
        player.deck.add(cardToReturn)
        player.deckCount = player.deck.size
        state.usedOncePerTurnEffects.add(effectKey)
        state.usedOncePerTurnEffects.add(nameKey)
        state.usedOncePerTurnEffects.add("${card.instanceId}:Lv2_SHINKAI_MAIN")

        // オープン（相手に確認できるように公開領域に記録）
        state.openedCards.clear()
        state.openedCards.add(cardToReturn)
        messages.add("👁️ 「${card.displayName}」効果: 手札の「${cardToReturn.name}」(コスト${cardToReturn.cost}) をオープンしてデッキ下に戻した")

        // ▶1枚ドローする
        if (player.deck.isNotEmpty()) {
            val drawn = player.deck.removeAt(0)
            player.deckCount = player.deck.size
            player.hand.add(drawn)
            messages.add("🃏 1枚ドローした: 「${drawn.name}」 (手札: ${player.hand.size}枚)")
        } else {
            messages.add("⚠️ デッキが0枚のためドローできず")
            state.winner = if (player.playerId == 1) 2 else 1
        }
    }
}
