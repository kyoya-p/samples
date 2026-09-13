package bstools.model

import kotlinx.serialization.Serializable

/**
 * カード属性（色）
 */
@Serializable
enum class CardColor(val displayName: String) {
    RED("赤"),
    PURPLE("紫"),
    GREEN("緑"),
    WHITE("白"),
    YELLOW("黄"),
    BLUE("青"),
    NONE("無");

    companion object {
        fun fromString(str: String): List<CardColor> {
            val list = mutableListOf<CardColor>()
            for (c in entries) {
                if (c != NONE && str.contains(c.displayName)) {
                    val idx = str.indexOf(c.displayName)
                    val rest = str.substring(idx + c.displayName.length)
                    val count = rest.firstOrNull()?.digitToIntOrNull() ?: 1
                    repeat(count) { list.add(c) }
                }
            }
            return list
        }
    }
}

/**
 * カード種別
 */
@Serializable
enum class CardCategory(val displayName: String) {
    SPIRIT("スピリット"),
    NEXUS("ネクサス"),
    MAGIC("マジック"),
    BRAVE("ブレイヴ"),
    ULTIMATE("アルティメット")
}

/**
 * 対戦フォーマット (総合ルール 1-1-1)。
 * ターンシークエンスが異なり、スタンダードは8ステップ制、エターナルは第2メインステップがない7ステップ制。
 * 使用できるカードも異なる (スタンダード: ブロックアイコンが英字のみ / エターナル: 禁止カードを除く全カード)。
 */
@Serializable
enum class GameFormat(val displayName: String) {
    STANDARD("スタンダード"),
    ETERNAL("エターナル");

    /** エターナルにはアタックステップの後の第2メインステップが存在しない */
    val hasSecondMain: Boolean get() = this == STANDARD

    companion object {
        fun fromString(s: String): GameFormat =
            if (s.contains("eternal", ignoreCase = true) || s.contains("エターナル")) ETERNAL else STANDARD
    }
}

/**
 * ターンステップ・フェイズ (スタンダードフォーマット対応)
 */
@Serializable
enum class Step(val displayName: String) {
    START("スタートステップ"),
    CORE("コアステップ"),
    DRAW("ドローステップ"),
    REFRESH("リフレッシュステップ"),
    MAIN("メインステップ"),
    ATTACK_START("アタックステップ開始"),
    ATTACK_DECLARATION("アタック宣言"),
    FLASH_TIMING("フラッシュタイミング"),
    BLOCK_DECLARATION("ブロック宣言"),
    BATTLE_RESOLUTION("バトル解決"),
    // 総合ルール 7-1-1: スタンダードは8ステップ制で、アタックステップの後に第2メインステップを持つ
    // (エターナルには第2メインステップが存在しない)
    MAIN_2("第2メインステップ"),
    END("エンドステップ")
}

/**
 * コア情報 (通常コア + ソウルコア)
 */
@Serializable
data class Cores(
    var normal: Int = 0,
    var soul: Int = 0
) {
    val total: Int get() = normal + soul

    fun format(): String = if (soul == 0) "$total" else "${total}s"

    fun formatIcons(): String {
        if (total == 0) return "-"
        return "🔷".repeat(normal) + "🔶".repeat(soul)
    }

    fun add(norm: Int, s: Int = 0) {
        normal += norm
        soul += s
    }

    fun sub(norm: Int, s: Int = 0) {
        normal = maxOf(0, normal - norm)
        soul = maxOf(0, soul - s)
    }
}

/**
 * カードマスターデータ
 */
@Serializable
data class CardData(
    val cardNo: String,
    val name: String,
    val cost: Int,
    val colors: List<CardColor> = listOf(CardColor.GREEN),
    val reductionSymbols: List<CardColor> = emptyList(),
    val category: CardCategory = CardCategory.SPIRIT,
    val lvCosts: List<Int> = listOf(1),
    val symbols: List<CardColor> = listOf(CardColor.GREEN),
    val systems: List<String> = emptyList(),
    val effect: String = "",
    /** Lvごとの基本BP (lvCosts と同じ並び) */
    val lvBps: List<Int> = emptyList()
)

/**
 * Lvコストを満たしている中で最大のLvを返す (総合ルール 2-8-3)。
 * Lvコスト0は、ネクサスのLv1 (3-3-3-3) か、スピリットの《真界放》= ソウルコアが乗っていれば
 * そのLvに到達する (12-2-1-1-2) ことを表す。
 * コアを動かした後のLvを先読みするためにも使うので、コア数を引数で受け取る。
 */
fun levelFor(lvCosts: List<Int>, coreTotal: Int, coreSoul: Int): Int {
    var lv = 0
    for ((idx, req) in lvCosts.withIndex()) {
        val satisfied = when {
            req > 0 -> coreTotal >= req
            idx == 0 -> true
            else -> coreSoul > 0
        }
        if (satisfied) lv = idx + 1
    }
    return lv
}

/**
 * フィールド上のカード状態
 */
@Serializable
data class FieldCard(
    val instanceId: String,
    val cardNo: String,
    var name: String,
    var category: CardCategory,
    var colors: List<CardColor>,
    var cores: Cores,
    var isExhausted: Boolean = false,
    var lvCosts: List<Int> = listOf(1),
    var baseSymbols: List<CardColor> = listOf(CardColor.GREEN),
    var systems: List<String> = emptyList(),
    /** Lvごとの基本BP (lvCosts と同じ並び) */
    var lvBps: List<Int> = emptyList()
) {
    /** 現在のLvに対応するBP。BPはLvによって変わる (総合ルール 2-8-4) */
    val currentBp: Int
        get() {
            if (lvBps.isEmpty()) return 0
            val idx = (level - 1).coerceIn(0, lvBps.size - 1)
            return lvBps[idx]
        }

    val level: Int get() = levelFor(lvCosts, cores.total, cores.soul)

    val symbols: List<CardColor>
        get() = if (level > 0) baseSymbols else emptyList()
}

/**
 * プレイヤー盤面状態
 */
@Serializable
data class PlayerState(
    val playerId: Int,
    var name: String,
    // 総合ルール 6-2-1-3: ライフに5個、リザーブに通常コア3個 + ソウルコア1個
    var life: Int = 5,
    var reserve: Cores = Cores(3, 1),
    var trash: Cores = Cores(0, 0),
    var deckCount: Int = 36,
    var hand: MutableList<CardData> = mutableListOf(),
    var deck: MutableList<CardData> = mutableListOf(),
    var field: MutableList<FieldCard> = mutableListOf(),
    var trashCards: MutableList<CardData> = mutableListOf()
)

/**
 * ゲーム全体の局面状態
 */
@Serializable
data class GameState(
    var turn: Int = 1,
    var activePlayerId: Int = 1,
    var step: Step = Step.MAIN,
    val player1: PlayerState = PlayerState(1, "Player 1"),
    val player2: PlayerState = PlayerState(2, "Player 2"),
    var attackingCardId: String? = null,
    var blockingCardId: String? = null,
    var winner: Int? = null,
    var format: GameFormat = GameFormat.STANDARD,
    var seed: Long = 42L,
    /**
     * これまでに到達した局面のハッシュ。同じ局面に戻る手を検出するために保持する。
     * その手自体は合法なので選択は妨げないが、評価値を0にしてAIの自動選択が
     * 往復し続けないようにし、UIには「選ぶと局面が進まない」ことを明記する。
     */
    var visitedPositions: MutableSet<Long> = mutableSetOf()
) {
    val activePlayer: PlayerState get() = if (activePlayerId == 1) player1 else player2
    val opponentPlayer: PlayerState get() = if (activePlayerId == 1) player2 else player1

    /**
     * いま選択を行うプレイヤー。通常はターンプレイヤーだが、
     * ブロック宣言はアタックされている側が行う (総合ルール 8-1-3-1)。
     */
    val choosingPlayerId: Int
        get() = if (step == Step.BLOCK_DECLARATION) (if (activePlayerId == 1) 2 else 1) else activePlayerId

    val choosingPlayer: PlayerState get() = if (choosingPlayerId == 1) player1 else player2
    val notChoosingPlayer: PlayerState get() = if (choosingPlayerId == 1) player2 else player1
}

/**
 * プレイヤーが選択可能なアクション
 */
@Serializable
data class AvailableAction(
    val actionId: Int,
    val type: String, // SUMMON, ATTACK, BLOCK, USE_MAGIC, MOVE_CORE, END_STEP, SURRENDER
    val description: String,
    val targetCardId: String? = null,
    val costRequired: Int = 0,
    val scoreEstimate: Double = 0.0
)

/**
 * X探索・大会調査結果データモデル
 */
@Serializable
data class DeckTrendEntry(
    val archetype: String,
    val winCount: Int,
    val sharePercent: Double,
    val sampleDeckList: List<String> = emptyList(),
    val sourceTweets: List<String> = emptyList()
)

@Serializable
data class MetaReport(
    val generatedAt: String,
    val totalTournaments: Int,
    val totalWinners: Int,
    val trends: List<DeckTrendEntry>
)
