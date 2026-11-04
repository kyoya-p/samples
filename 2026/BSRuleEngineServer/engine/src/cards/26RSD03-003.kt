package bstools.model.cards

import bstools.model.*

/**
 * [26RSD03-003] ラスヘッド
 *
 * 効果テキスト:
 * [Lv1-2]『召喚時』コスト4以下の相手のスピリット1体を指定できる。それを疲労させる。
 * [Lv2]《真界放》『アタック中』このスピリットをBP+2000する。
 */
object Card26RSD03_003 : CardDefinition {
    override val data: CardData = CardData(
        cardNo = "26RSD03-003", name = "ラスヘッド", cost = 3,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.GREEN), systems = listOf("海緑", "甲魚"),
        effect = "[Lv1-2]『召喚時』コスト4以下の相手のスピリット1体を指定できる。それを疲労させる。\n[Lv2]《真界放》『アタック中』このスピリットをBP+2000する。",
        lvBps = listOf(3000, 5000), shinkai = true
    )

    override fun computeBpModifier(
        state: GameState,
        card: FieldCard,
        isAttackingHypothetical: Boolean
    ): Int {
        // [Lv2]《真界放》『アタック中』このスピリットをBP+2000する。
        // Lv2以上 かつ アタック中 (実アタックまたはアタック宣言選択肢の先読み)
        if (card.level >= 2 && (state.attackingCardId == card.instanceId || isAttackingHypothetical)) {
            return 2000
        }
        return 0
    }

    override fun onAttack(
        state: GameState,
        attacker: FieldCard,
        action: GameAction,
        messages: MutableList<String>
    ) {
        if (attacker.level >= 2) {
            messages.add("🌀 【ラスヘッド】Lv2《真界放》効果: アタック中BP+2000！ (BP${attacker.effectiveBp(state)})")
        }
    }
}
