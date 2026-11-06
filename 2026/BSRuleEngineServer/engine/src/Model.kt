package bstools.model

import bstools.model.effects.CardScriptRegistry
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
    val lvBps: List<Int> = emptyList(),
    /** 《真界放》: Lv2 に必要な数のコアを置くか、ソウルコアが置かれていれば Lv2 になる (総合ルール 3-2-5-5 / 3-3-3-4) */
    val shinkai: Boolean = false
)

/**
 * Lvコストを満たしている中で最大のLvを返す (総合ルール 2-8-3)。
 * Lvコスト0は、ネクサスのLv1 (3-3-3-3) を表す。
 * [shinkai] (《真界放》) のカードは、ソウルコアが置かれていれば Lv2 になる (3-2-5-5 / 3-3-3-4)。
 * コアを動かした後のLvを先読みするためにも使うので、コア数を引数で受け取る。
 */
fun levelFor(lvCosts: List<Int>, coreTotal: Int, coreSoul: Int, shinkai: Boolean = false): Int {
    var lv = 0
    for ((idx, req) in lvCosts.withIndex()) {
        val satisfied = when {
            req > 0 -> coreTotal >= req
            idx == 0 -> true
            else -> coreSoul > 0
        }
        if (satisfied) lv = idx + 1
    }
    if (shinkai && coreSoul > 0 && lv >= 1 && lvCosts.size >= 2) lv = maxOf(lv, 2)
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
    /** 重疲労状態 (逆向き180度。1回の回復で疲労状態になる) */
    var isHeavyExhausted: Boolean = false,
    var lvCosts: List<Int> = listOf(1),
    var baseSymbols: List<CardColor> = listOf(CardColor.GREEN),
    var systems: List<String> = emptyList(),
    /** Lvごとの基本BP (lvCosts と同じ並び) */
    var lvBps: List<Int> = emptyList(),
    /** フィールドに置かれた通し番号 (重複しない一意の番号) */
    var seq: Int = 0,
    var cost: Int = 3,
    /** 《真界放》 (ソウルコアが置かれていれば Lv2) */
    var shinkai: Boolean = false,
    /** 一時的なBP増減修正値 */
    var bpModifier: Int = 0
) {
    /** 重疲労させる (疲労かつ重疲労) */
    fun heavyExhaust() {
        isExhausted = true
        isHeavyExhausted = true
    }

    /** 回復処理 (重疲労なら疲労へ、疲労なら未疲労へ) */
    fun refresh() {
        if (isHeavyExhausted) {
            isHeavyExhausted = false
            isExhausted = true
        } else {
            isExhausted = false
        }
    }
    /** 現在のLvに対応する基本BP (修正なし) */
    val baseBp: Int
        get() {
            if (lvBps.isEmpty()) return 0
            val idx = (level - 1).coerceIn(0, lvBps.size - 1)
            return lvBps[idx]
        }

    /** 現在のLvに対応するBP */
    val currentBp: Int
        get() = maxOf(0, baseBp + bpModifier)

    /**
     * 盤面 [state] に応じた動的効果（例: 『アタック中』BP+2000、他カードからのBP+1000付与など）を適用した実効BP。
     */
    fun effectiveBp(state: GameState? = null, isAttackingHypothetical: Boolean = false): Int {
        if (state == null) return currentBp
        var dynamicMod = CardScriptRegistry.get(cardNo)?.computeBpModifier(state, this, isAttackingHypothetical) ?: 0
        val owner = if (state.player1.field.any { it.instanceId == this.instanceId }) state.player1
                    else if (state.player2.field.any { it.instanceId == this.instanceId }) state.player2
                    else state.activePlayer
        if (owner != null) {
            for (fc in owner.field) {
                if (fc.instanceId != this.instanceId) {
                    val script = CardScriptRegistry.get(fc.cardNo)
                    if (script != null) {
                        dynamicMod += script.computeAuraBpModifier(state, fc, this, isAttackingHypothetical)
                    }
                }
            }
        }
        return maxOf(0, currentBp + dynamicMod)
    }

    val level: Int get() = levelFor(lvCosts, cores.total, cores.soul, shinkai)

    val symbols: List<CardColor>
        get() = if (level > 0) baseSymbols else emptyList()

    /** フィールド番号(通し番号)付きの表示名 (例: "#1 パッファー") */
    val displayName: String
        get() = if (seq > 0) "#$seq $name" else name
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
    var mustBlockInstanceId: String? = null,
    var winner: Int? = null,
    var format: GameFormat = GameFormat.STANDARD,
    var seed: Long = 42L,
    /**
     * これまでに到達した局面のハッシュ。同じ局面に戻る手を検出するために保持する。
     * その手自体は合法なので選択は妨げないが、評価値を0にしてAIの自動選択が
     * 往復し続けないようにし、UIには「選ぶと局面が進まない」ことを明記する。
     */
    var visitedPositions: MutableSet<Long> = mutableSetOf(),
    /** フィールドに置かれるカードの通し番号カウンタ */
    var nextCardSeq: Int = 1,
    /** ゲーム開始から評価・推論計算を行った累計局面数 */
    var evaluatedCount: Long = 0L,
    /**
     * 直前のプレイ等 (コアの再配置以外の手) の後に、コアの再配置をすでに行った対象のキー
     * ([GameAction.rearrangeKey]: カードの instanceId / "SOUL" / 一括再配置は "ALL")。
     * 同じ対象の再配置を2回続けても1回で同じ結果にできるため、同じキーの再配置は列挙しない
     * (コアを動かし続ける循環を構造的になくす)。再配置以外の手・ステップ進行で空に戻る。
     * 局面ハッシュには含めない (同じ配置に戻る手は「同一局面」として扱う)
     */
    var coreWindow: MutableSet<String> = mutableSetOf(),
    /** ターンに1回しか使えない効果の使用済みフラグ (例: "fc_26RSD03-010_2:Lv2_SHINKAI_MAIN") */
    var usedOncePerTurnEffects: MutableSet<String> = mutableSetOf(),
    /** 公開・オープンされたカードのリスト (相手・観戦者にも確認できるように表示) */
    var openedCards: MutableList<CardData> = mutableListOf(),
    /** フラッシュタイミングで優先権を持つプレイヤーID (総合ルール 8-1-2-1: 防御側から優先権を得る) */
    var flashPriorityPlayerId: Int? = null,
    /** フラッシュタイミングでの連続パス回数 (2回連続パスでフラッシュ終了) */
    var flashPassCount: Int = 0,
    /** 未解決の効果（召喚時効果等の対象選択）がある場合の情報 */
    var pendingEffect: PendingEffect? = null
) {
    val activePlayer: PlayerState get() = if (activePlayerId == 1) player1 else player2
    val opponentPlayer: PlayerState get() = if (activePlayerId == 1) player2 else player1

    /**
     * いま選択を行うプレイヤー。通常はターンプレイヤーだが、
     * フラッシュタイミングは防御側から優先権を得て交互に行い (総合ルール 8-1-2-1)、
     * ブロック宣言はアタックされている側が行う (総合ルール 8-1-3-1)。
     * 未解決の効果がある場合はそのカードの持ち主が行う。
     */
    val choosingPlayerId: Int
        get() {
            if (pendingEffect != null) {
                val instId = pendingEffect!!.cardInstanceId
                if (player1.field.any { it.instanceId == instId }) return player1.playerId
                if (player2.field.any { it.instanceId == instId }) return player2.playerId
            }
            return when (step) {
                Step.FLASH_TIMING -> flashPriorityPlayerId ?: opponentPlayer.playerId
                Step.BLOCK_DECLARATION -> opponentPlayer.playerId
                else -> activePlayerId
            }
        }

    val choosingPlayer: PlayerState get() = if (choosingPlayerId == 1) player1 else player2
    val notChoosingPlayer: PlayerState get() = if (choosingPlayerId == 1) player2 else player1
}

/**
 * 未解決の効果（召喚時効果等の対象選択）
 */
@Serializable
data class PendingEffect(
    val cardInstanceId: String,
    val effectType: String,
    val description: String = ""
)

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
