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
}

/**
 * カードスクリプトのレジストリ。
 * カード番号から固有スクリプトを取得する。
 */
object CardScriptRegistry {
    private val scripts = mutableMapOf<String, CardScript>()

    fun register(script: CardScript) {
        scripts[script.cardNo] = script
    }

    fun get(cardNo: String): CardScript? = scripts[cardNo]

    init {
        // カード固有スクリプトの登録
        bstools.model.cards.rsd03.Card26RSD03_007.register()
    }
}
