package bstools.model.cards

import bstools.model.*
import bstools.model.effects.CardScriptRegistry

/**
 * [26RSD03-002] パッファー
 *
 * 効果テキスト:
 * [Lv1-2]『このスピリットの召喚時』
 * ボイドからコア1個を自分のトラッシュに置く。
 */
object Card26RSD03_002 : CardDefinition {
    override val data: CardData = CardData(
cardNo = "26RSD03-002", name = "パッファー", cost = 3,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.GREEN), systems = listOf("甲魚"),
        effect = "[Lv1-2]『召喚時』ボイドからコア1個を自分のトラッシュに置く。",
        lvBps = listOf(2000, 3000), shinkai = false
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
        if (card.level >= 1) {
            player.trash.normal += 1
            messages.add("🌀 【パッファー】召喚時効果: ボイドからコア1個をトラッシュに置いた！ (トラッシュ:${player.trash.normal}個)")
        }
    }
}
