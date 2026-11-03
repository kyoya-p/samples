package bstools.model.effects

import bstools.model.*

/**
 * カード固有効果をルールエンジンから切り離して実装するためのインターフェース。
 *
 * ルールエンジン (Rules.kt) は特定カードのIDやテキストを直接知らず、
 * 各フェーズやタイミングで CardScript のフックを呼び出す。
 */
interface CardScript {
    val cardNo: String

    /**
     * 召喚・配置完了時の効果解決 (例: パッファーのボイドからトラッシュへコア+1)。
     */
    fun onSummon(
        state: GameState,
        card: FieldCard,
        player: PlayerState,
        messages: MutableList<String>
    ) {}

    /**
     * アタック宣言時の追加・固有選択肢の列挙 (例: ガリザロトの疲労ブロック強制指定)。
     * 空リストを返した場合は、ルールエンジンの通常アタックが生成される。
     */
    fun enumerateAttackActions(
        state: GameState,
        attacker: FieldCard,
        opponent: PlayerState,
        nextIndex: () -> Int
    ): List<GameAction> = emptyList()

    /**
     * アタック宣言・解決時の効果適用 (例: 指定対象を state.mustBlockInstanceId に格納)。
     */
    fun onAttack(
        state: GameState,
        attacker: FieldCard,
        action: GameAction,
        messages: MutableList<String>
    ) {}

    /**
     * 現在の盤面における追加BP (BP修正) を計算するフック。
     * 例: ラスヘッドの「[Lv2]《真界放》『アタック中』このスピリットをBP+2000する」など。
     */
    fun computeBpModifier(
        state: GameState,
        card: FieldCard,
        isAttackingHypothetical: Boolean = false
    ): Int = 0

    /**
     * 自フィールドの他カード等に与えるオーラBP修正値の計算。
     * 例: 最奥：緑淵深の「[Lv1-2]『自分のアタックステップ』アタックしている系統：「甲魚」を持つ自分のスピリットすべてをBP+1000する」
     */
    fun computeAuraBpModifier(
        state: GameState,
        sourceCard: FieldCard,
        targetCard: FieldCard,
        isAttackingHypothetical: Boolean = false
    ): Int = 0

    /**
     * フィールドカードの起動効果 (例: ネクサス・スピリットの【起動：メイン】など) の選択肢列挙。
     */
    fun enumerateFieldActions(
        state: GameState,
        card: FieldCard,
        player: PlayerState,
        nextIndex: () -> Int
    ): List<GameAction> = emptyList()

    /**
     * フィールド起動アクション選択・解決時の効果適用。
     */
    fun onFieldAction(
        state: GameState,
        card: FieldCard,
        player: PlayerState,
        action: GameAction,
        messages: MutableList<String>
    ) {}

    /**
     * 各ステップ開始時に発揮する自動・誘発効果の選択肢を列挙する。
     * 複数存在する場合はプレイヤーが解決順を選択する (総合ルール 8-1)。
     * 例: 水泡群域の「[Lv2]『自分のメインステップ開始時』...」
     */
    fun enumerateStepStartActions(
        state: GameState,
        card: FieldCard,
        player: PlayerState,
        step: Step,
        nextIndex: () -> Int
    ): List<GameAction> = emptyList()

    /**
     * ステップ開始時効果の適用。
     */
    fun onStepStartAction(
        state: GameState,
        card: FieldCard,
        player: PlayerState,
        action: GameAction,
        messages: MutableList<String>
    ) {}

    /**
     * ブロック宣言時の選択肢フィルタリングや制限 (例: 強制ブロック時の選択肢固定)。
     */
    fun filterBlockActions(
        state: GameState,
        actions: List<GameAction>
    ): List<GameAction> = actions

    /**
     * ブロック宣言・解決時の効果適用。
     */
    fun onBlock(
        state: GameState,
        blocker: FieldCard,
        action: GameAction,
        messages: MutableList<String>
    ) {}

    /**
     * フラッシュタイミングにおける追加・固有選択肢の列挙 (例: フラッティの手札からのフラッシュ召喚)。
     *
     * @param state 現在のゲーム盤面
     * @param player フラッシュアクションを行おうとしているプレイヤー (防御側または攻撃側)
     * @param opponent 対戦相手プレイヤー
     * @param handIndex 手札カードから発動する場合のインデックス (手札でない場合は -1)
     * @param nextIndex アクション採番関数
     */
    fun enumerateFlashActions(
        state: GameState,
        player: PlayerState,
        opponent: PlayerState,
        handIndex: Int = -1,
        nextIndex: () -> Int
    ): List<GameAction> = emptyList()

    /**
     * フラッシュアクション選択・解決時の効果適用。
     */
    fun onFlash(
        state: GameState,
        player: PlayerState,
        action: GameAction,
        messages: MutableList<String>
    ) {}

    /**
     * 破壊・消滅時の効果解決。
     */
    fun onDestroy(
        state: GameState,
        card: FieldCard,
        player: PlayerState,
        isVanished: Boolean,
        messages: MutableList<String>
    ) {}
}

/**
 * カードスクリプトのレジストリ。
 * カード番号から固有スクリプトを取得する。動的な追加・廃止に対応。
 */
object CardScriptRegistry {
    private val scripts = mutableMapOf<String, CardScript>()

    fun register(script: CardScript) {
        scripts[script.cardNo] = script
    }

    fun unregister(cardNo: String): Boolean {
        return scripts.remove(cardNo) != null
    }

    fun clear() {
        scripts.clear()
    }

    fun registeredCardNos(): Set<String> = scripts.keys.toSet()

    fun get(cardNo: String): CardScript? = scripts[cardNo]

    init {
        // 全カード定義（固有スクリプト実装含む）の自動登録
        ALL_CARD_DEFINITIONS.forEach { register(it) }
    }
}
