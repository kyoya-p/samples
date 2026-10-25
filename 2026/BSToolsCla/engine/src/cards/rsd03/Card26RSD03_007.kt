package bstools.model.cards.rsd03

import bstools.model.*
import bstools.model.effects.CardScript
import bstools.model.effects.CardScriptRegistry

/**
 * [26RSD03-007] 甲骨樹ガリザロト
 *
 * 効果テキスト:
 * Lv1・Lv2『このスピリットのアタック時』
 * 疲労状態の相手のスピリット1体を指定できる。
 * このバトル中、そのスピリットは可能なら疲労状態でもブロックしなければならない。
 */
object Card26RSD03_007 : CardScript {
    override val cardNo: String = "26RSD03-007"

    fun register() {
        CardScriptRegistry.register(this)
    }

    override fun enumerateAttackActions(
        state: GameState,
        attacker: FieldCard,
        opponent: PlayerState,
        nextIndex: () -> Int
    ): List<GameAction> {
        if (attacker.level < 1) return emptyList()

        val exhaustedSpirits = opponent.field.filter {
            it.isExhausted && it.level > 0 && it.category == CardCategory.SPIRIT
        }
        if (exhaustedSpirits.isEmpty()) return emptyList()

        val actions = mutableListOf<GameAction>()
        for (target in exhaustedSpirits) {
            actions.add(
                GameAction(
                    index = nextIndex(),
                    type = "ATTACK",
                    category = "🗡️【アタック】 ${attacker.displayName}",
                    detail = "${attacker.displayName} でアタック宣言 ➔ 指定: ${target.displayName} (疲労ブロック強制, Lv${attacker.level}, BP${attacker.currentBp})",
                    targetInstanceId = attacker.instanceId,
                    effectTargetId = target.instanceId
                )
            )
        }

        // 効果は「指定できる」のため、指定しない選択肢も用意
        actions.add(
            GameAction(
                index = nextIndex(),
                type = "ATTACK",
                category = "🗡️【アタック】 ${attacker.displayName}",
                detail = "${attacker.displayName} でアタック宣言 (指定なし, Lv${attacker.level}, BP${attacker.currentBp})",
                targetInstanceId = attacker.instanceId,
                effectTargetId = null
            )
        )

        return actions
    }

    override fun onAttack(
        state: GameState,
        attacker: FieldCard,
        action: GameAction,
        messages: MutableList<String>
    ) {
        if (action.effectTargetId != null) {
            val opponent = state.choosingPlayer // BLOCK_DECLARATIONの手番は防御側
            val target = opponent.field.find { it.instanceId == action.effectTargetId }
            if (target != null) {
                state.mustBlockInstanceId = target.instanceId
                messages.add("🌀 【ガリザロト】アタック時効果: 相手の「${target.displayName}」を指定！(疲労状態でも強制ブロック)")
            }
        }
    }
}
