package bs.game

import kotlinx.serialization.Serializable

/**
 * 属性・シンボルの定義
 */
enum class Color {
    RED, PURPLE, GREEN, WHITE, YELLOW, BLUE, NONE
}

/**
 * コアの状態（通常コアとソウルコア）
 */
data class Cores(
    val normal: Int = 0,
    val soulCore: Boolean = false
)

/**
 * ターンの進行ステップ（スタンダード）
 */
enum class StandardStep {
    START,
    CORE,
    DRAW,
    REFRESH,
    MAIN,
    ATTACK,
    SECOND_MAIN, // スタンダード固有
    END
}

/**
 * フィールド上のカードインスタンス（状態を持つ）
 */
sealed class CardInstance {
    abstract val cardNo: String
    abstract var cores: Cores

    data class Spirit(
        override val cardNo: String,
        override var cores: Cores,
        var currentLv: Int = 1,
        var isExhausted: Boolean = false
    ) : CardInstance()

    data class Nexus(
        override val cardNo: String,
        override var cores: Cores,
        var currentLv: Int = 1
    ) : CardInstance()
}

/**
 * プレイヤーごとのゲーム領域（ゾーン）
 */
data class PlayerState(
    val name: String,
    val deck: MutableList<String> = mutableListOf(),
    val hand: MutableList<String> = mutableListOf(),
    val trash: MutableList<String> = mutableListOf(),
    val life: Int = 5,
    var reserve: Cores = Cores(normal = 3, soulCore = true), // 初期状態
    val field: MutableList<CardInstance> = mutableListOf(),
    val removed: MutableList<String> = mutableListOf() // 継召等で使用
)

/**
 * ゲーム全体のグローバル状態
 */
data class BSRuleState(
    val players: List<PlayerState>,
    var turnCount: Int = 1,
    var turnPlayerIndex: Int = 0,
    var currentStep: StandardStep = StandardStep.START,
    val log: MutableList<String> = mutableListOf()
) {
    val turnPlayer: PlayerState get() = players[turnPlayerIndex]
    val opponent: PlayerState get() = players[1 - turnPlayerIndex]

    /**
     * スタンダードレギュレーションの先攻1ターン目制限チェック
     */
    fun isStepAvailable(step: StandardStep): Boolean {
        if (turnCount == 1) {
            return when (step) {
                StandardStep.CORE, StandardStep.ATTACK, StandardStep.SECOND_MAIN -> false
                else -> true
            }
        }
        return true
    }
}
