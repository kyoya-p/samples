package bstools.model.cards

import bstools.model.*
import bstools.model.effects.CardScriptRegistry

/**
 * [26RSD03-011] 水泡群域
 *
 * 効果テキスト:
 * [Lv1-2]『配置時』相手のスピリット1体を指定できる。それを疲労させる。
 * [Lv2]『自分のメインステップ開始時』疲労状態の系統：「甲魚」を持つ自分のスピリットが2体以上いるなら、ボイドからコア1個を自分のリザーブに置く。
 */
object Card26RSD03_011 : CardDefinition {
    override val data: CardData = CardData(
        cardNo = "26RSD03-011", name = "水泡群域", cost = 4,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.NEXUS, lvCosts = listOf(0, 2),
        symbols = listOf(CardColor.GREEN), systems = listOf("甲魚"),
        effect = "[Lv1-2]『配置時』相手のスピリット1体を指定できる。それを疲労させる。\n[Lv2]『自分のメインステップ開始時』疲労状態の系統：「甲魚」を持つ自分のスピリットが2体以上いるなら、ボイドからコア1個を自分のリザーブに置く。",
        lvBps = listOf(), shinkai = false
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
        // [Lv1-2]『配置時』相手のスピリット1体を指定できる。それを疲労させる。
        val opponent = if (player.playerId == state.player1.playerId) state.player2 else state.player1
        val target = opponent.field.firstOrNull { !it.isExhausted && it.category == CardCategory.SPIRIT }
        if (target != null) {
            target.isExhausted = true
            messages.add("🌀 「${card.displayName}」の配置時効果: 相手の「${target.displayName}」を疲労させた！")
        }
    }

    override fun enumerateStepStartActions(
        state: GameState,
        card: FieldCard,
        player: PlayerState,
        step: Step,
        nextIndex: () -> Int
    ): List<GameAction> {
        // [Lv2]『自分のメインステップ開始時』疲労状態の系統：「甲魚」を持つ自分のスピリットが2体以上いるなら、ボイドからコア1個を自分のリザーブに置く。
        if (step != Step.MAIN) return emptyList()
        if (state.activePlayerId != player.playerId) return emptyList()
        if (card.level < 2) return emptyList()

        val effectKey = "${card.instanceId}:MAIN_START_CORE_BOOST"
        if (state.usedOncePerTurnEffects.contains(effectKey)) return emptyList()

        val exhaustedKogyoCount = player.field.count {
            it.category == CardCategory.SPIRIT && it.isExhausted && it.systems.contains("甲魚")
        }
        if (exhaustedKogyoCount < 2) return emptyList()

        return listOf(
            GameAction(
                index = nextIndex(),
                type = "STEP_START_EFFECT",
                category = "🌀【効果発揮】 ${card.displayName}",
                detail = "ボイドからコア1個をリザーブに置く (疲労甲魚${exhaustedKogyoCount}体)",
                targetInstanceId = card.instanceId
            )
        )
    }

    override fun onStepStartAction(
        state: GameState,
        card: FieldCard,
        player: PlayerState,
        action: GameAction,
        messages: MutableList<String>
    ) {
        val effectKey = "${card.instanceId}:MAIN_START_CORE_BOOST"
        state.usedOncePerTurnEffects.add(effectKey)
        player.reserve.add(1)
        messages.add("🌀 「${card.displayName}」のLv2メインステップ開始時効果: ボイドからコア1個をリザーブに置いた！")
    }
}
