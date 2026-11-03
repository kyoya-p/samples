package bstools.model.effects

import bstools.model.*
import kotlinx.serialization.Serializable

/**
 * 外部プロセスや別ライブラリモジュールと連携するための汎用イベントデータ。
 * JSON-RPC / stdio / IPC 経由でシリアライズ可能。
 */
@Serializable
data class EffectEvent(
    val eventType: String, // "ON_SUMMON", "ON_ATTACK", "ON_BLOCK", "ON_DESTROY"
    val cardNo: String,
    val instanceId: String,
    val playerId: Int,
    val level: Int,
    val turn: Int,
    val step: String
)

/**
 * カード効果がルールエンジンへ要求する盤面変更コマンド。
 * ルールエンジンの不変条件（Core Invariants）を壊さないよう、許可されたプリミティブ操作のみを表現。
 */
@Serializable
data class EffectCommand(
    val type: String, // "ADD_CORE", "MOVE_CORE"
    val source: String = "Void", // "Void", "Reserve", "Trash"
    val target: String = "Trash", // "Trash", "Reserve"
    val count: Int = 1,
    val coreType: String = "normal" // "normal" or "soul"
)

/**
 * カード効果プラグインの実行結果。
 */
@Serializable
data class EffectResult(
    val message: String? = null,
    val commands: List<EffectCommand> = emptyList()
)

/**
 * 外部プラグイン（別実行ファイル・別ライブラリ等）のアダプターインターフェース。
 */
interface CardEffectPlugin {
    val pluginName: String
    val supportedCardNos: Set<String>

    fun handleEvent(event: EffectEvent): EffectResult?
}

/**
 * [CardEffectPlugin] をルールエンジンの [CardScript] へ橋渡しするアダプタークラス。
 * 外部プロセスや別ライブラリのプラグインを登録・廃止する際に利用する。
 */
class PluginCardScript(
    val plugin: CardEffectPlugin,
    override val cardNo: String
) : CardScript {

    override fun onSummon(
        state: GameState,
        card: FieldCard,
        player: PlayerState,
        messages: MutableList<String>
    ) {
        val event = EffectEvent(
            eventType = "ON_SUMMON",
            cardNo = card.cardNo,
            instanceId = card.instanceId,
            playerId = player.playerId,
            level = card.level,
            turn = state.turn,
            step = state.step.name
        )
        val result = plugin.handleEvent(event) ?: return
        if (!result.message.isNullOrBlank()) {
            messages.add(result.message)
        }
        for (cmd in result.commands) {
            applyCommand(state, player, cmd, messages)
        }
    }

    private fun applyCommand(
        state: GameState,
        player: PlayerState,
        cmd: EffectCommand,
        messages: MutableList<String>
    ) {
        when (cmd.type) {
            "ADD_CORE" -> {
                if (cmd.source == "Void") {
                    when (cmd.target) {
                        "Trash" -> player.trash.add(cmd.count, 0)
                        "Reserve" -> player.reserve.add(cmd.count, 0)
                    }
                }
            }
            "MOVE_CORE" -> {
                // Reserve <-> Trash 等の基本移動
                if (cmd.source == "Reserve" && cmd.target == "Trash") {
                    val n = minOf(player.reserve.normal, cmd.count)
                    player.reserve.normal -= n
                    player.trash.normal += n
                }
            }
        }
    }
}
