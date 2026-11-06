package bstools.model.cards

import bstools.model.*
import bstools.model.effects.CardScriptRegistry

/**
 * [26RSD03-X02] 深巣ダンテクレオ
 */
object Card26RSD03_X02 : CardDefinition {
    override val data: CardData = CardData(
        cardNo = "26RSD03-X02", name = "深巣ダンテクレオ", cost = 6,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 4),
        symbols = listOf(CardColor.GREEN), systems = listOf("甲魚"),
        effect = "《継召》トラッシュのEXシンボルを除外し軽減できる。\n[Lv1-2]『召喚時』系統：「甲魚」を持つ自分のスピリット2体までを指定する。このターン中、それらをBP+3000する。\n[Lv2]《真界放》『自分のアタックステップ』【起動：フラッシュ】〔ターン1回〕このスピリット以外の系統：「甲魚」を持つ自分のスピリット1体を指定する。それを回復させる。",
        lvBps = listOf(6000, 9000), shinkai = true
    )

    override fun enumerateFlashActions(
        state: GameState,
        player: PlayerState,
        opponent: PlayerState,
        handIndex: Int,
        nextIndex: () -> Int
    ): List<GameAction> {
        // フィールドに存在するダンテクレオ自身を取得
        val myCard = player.field.find { it.cardNo == data.cardNo } ?: return emptyList()

        // [Lv2]《真界放》: Lv2以上であること (ソウルコアがあればshinkaiでLv2)
        if (myCard.level < 2) return emptyList()

        // 『自分のアタックステップ』: 手番側が自分であること
        if (state.activePlayerId != player.playerId) return emptyList()

        // 〔ターン1回〕: ターン中に既に使われていないこと
        val effectKey = "${myCard.instanceId}:SHINKAI_FLASH_RECOVER"
        if (effectKey in state.usedOncePerTurnEffects) return emptyList()

        // 「このスピリット以外の系統：「甲魚」を持つ自分のスピリット1体を指定する。それを回復させる。」
        // 回復効果のため、疲労状態のスピリットを対象とする
        val validTargets = player.field.filter {
            it.instanceId != myCard.instanceId && it.systems.contains("甲魚") && it.isExhausted
        }
        if (validTargets.isEmpty()) return emptyList()

        return validTargets.map { target ->
            GameAction(
                index = nextIndex(),
                type = "FLASH_EFFECT",
                category = "🌀【真界放フラッシュ】 深巣ダンテクレオ",
                detail = "《真界放》「${target.displayName}」を回復させる (ターン1回)",
                targetInstanceId = myCard.instanceId,
                effectTargetId = target.instanceId
            )
        }
    }

    override fun onFlash(
        state: GameState,
        player: PlayerState,
        action: GameAction,
        messages: MutableList<String>
    ) {
        val effectKey = "${action.targetInstanceId}:SHINKAI_FLASH_RECOVER"
        state.usedOncePerTurnEffects.add(effectKey)

        val target = player.field.find { it.instanceId == action.effectTargetId }
        if (target != null) {
            val wasHeavy = target.isHeavyExhausted
            target.refresh()
            if (wasHeavy) {
                messages.add("🌀 【深巣ダンテクレオ】Lv2《真界放》効果: 重疲労の「${target.displayName}」を疲労状態へ回復！")
            } else {
                messages.add("🌀 【深巣ダンテクレオ】Lv2《真界放》効果: 「${target.displayName}」を回復！")
            }
        }
    }
}
