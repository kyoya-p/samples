package bstools.model.cards

import bstools.model.*
import bstools.model.effects.CardScriptRegistry

/**
 * [26RSD03-X01] 甲手スクィード
 *
 * 効果テキスト:
 * [Lv1-2]『このスピリットの召喚時』
 * 疲労状態の相手のスピリット1体を指定できる。それを重疲労させる。
 * (重疲労状態のカードは逆向きにし、1回の回復で疲労状態になる)
 * [Lv2]『アタック中』〔ターン1回〕バトル終了時、このスピリットを回復できる。
 */
object Card26RSD03_X01 : CardDefinition {
    override val data: CardData = CardData(
        cardNo = "26RSD03-X01", name = "甲手スクィード", cost = 5,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.GREEN), systems = listOf("海緑", "甲魚"),
        effect = "[Lv1-2]『召喚時』疲労状態の相手のスピリット1体を指定できる。それを重疲労させる。\n[Lv2]『アタック中』〔ターン1回〕バトル終了時、このスピリットを回復できる。",
        lvBps = listOf(5000, 7000), shinkai = false
    )

    fun register() {
        CardScriptRegistry.register(this)
    }

    override fun onSummon(
        state: GameState,
        card: FieldCard,
        player: PlayerState,
        messages: MutableList<String>
    ) {
        if (card.level < 1) return
        val opponent = if (player.playerId == state.player1.playerId) state.player2 else state.player1
        val exhaustedSpirits = opponent.field.filter { it.isExhausted && it.category == CardCategory.SPIRIT }
        if (exhaustedSpirits.isEmpty()) {
            messages.add("🌀 【甲手スクィード】召喚時効果: 疲労状態の相手スピリットが存在しないため不発")
            return
        }
        // 疲労状態の相手スピリットが存在する場合、プレイヤーに選択させる
        state.pendingEffect = PendingEffect(
            cardInstanceId = card.instanceId,
            effectType = "ON_SUMMON",
            description = "重疲労させる相手スピリットを選択"
        )
        messages.add("🌀 【甲手スクィード】召喚時効果: 重疲労させる相手スピリットを選択してください (${exhaustedSpirits.size}体の候補)")
    }

    override fun enumeratePendingEffectActions(
        state: GameState,
        card: FieldCard,
        player: PlayerState,
        pending: PendingEffect,
        nextIndex: () -> Int
    ): List<GameAction> {
        if (pending.effectType != "ON_SUMMON") return emptyList()
        val opponent = if (player.playerId == state.player1.playerId) state.player2 else state.player1
        val exhaustedSpirits = opponent.field.filter { it.isExhausted && it.category == CardCategory.SPIRIT }
        val actions = mutableListOf<GameAction>()

        for (target in exhaustedSpirits) {
            val heavyNote = if (target.isHeavyExhausted) " (既に重疲労)" else ""
            actions.add(
                GameAction(
                    index = nextIndex(),
                    type = "RESOLVE_PENDING_EFFECT",
                    category = "🌀【召喚時効果】 ${card.displayName}",
                    detail = "相手の「${target.displayName}」を指定して重疲労させる$heavyNote",
                    targetInstanceId = card.instanceId,
                    effectTargetId = target.instanceId
                )
            )
        }

        // 「指定できる」のため、指定しない選択肢も用意
        actions.add(
            GameAction(
                index = nextIndex(),
                type = "RESOLVE_PENDING_EFFECT",
                category = "🌀【召喚時効果】 ${card.displayName}",
                detail = "指定しない (効果を発揮しない)",
                targetInstanceId = card.instanceId,
                effectTargetId = null
            )
        )

        return actions
    }

    override fun onResolvePendingEffect(
        state: GameState,
        card: FieldCard,
        player: PlayerState,
        action: GameAction,
        messages: MutableList<String>
    ) {
        val targetId = action.effectTargetId
        if (targetId == null) {
            messages.add("🌀 【甲手スクィード】召喚時効果: 対象を指定しませんでした")
            return
        }
        val opponent = if (player.playerId == state.player1.playerId) state.player2 else state.player1
        val target = opponent.field.find { it.instanceId == targetId }
        if (target != null) {
            target.heavyExhaust()
            messages.add("🌀 【甲手スクィード】召喚時効果: 相手の「${target.displayName}」を指定！重疲労させた！")
        }
    }

    override fun onBattleEnd(
        state: GameState,
        attacker: FieldCard,
        owner: PlayerState,
        messages: MutableList<String>
    ) {
        // [Lv2]『アタック中』〔ターン1回〕バトル終了時、このスピリットを回復できる。
        if (attacker.level >= 2) {
            val key = "${attacker.instanceId}:BATTLE_END_REFRESH"
            if (!state.usedOncePerTurnEffects.contains(key)) {
                state.usedOncePerTurnEffects.add(key)
                val wasHeavy = attacker.isHeavyExhausted
                attacker.refresh()
                if (wasHeavy) {
                    messages.add("🌀 【甲手スクィード】Lv2効果: バトル終了時、重疲労から疲労状態へ回復！")
                } else {
                    messages.add("🌀 【甲手スクィード】Lv2効果: バトル終了時、このスピリットを回復した！")
                }
            }
        }
    }
}
