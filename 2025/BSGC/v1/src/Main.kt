import java.util.*
import kotlin.math.max

// カードとスピリットのデータクラス
data class SpiritCard(
    val name: String,
    val cost: Int, // 召喚コスト
    val reductionSymbol: Int, // 軽減シンボル数 (赤シンボルとして扱う)
    val levelCosts: Map<Int, Int> // レベル -> そのレベルに必要なコア数
)

data class Spirit(
    val card: SpiritCard,
    var currentLevel: Int,
    var coresOnSpirit: Int
) {
    // フィールドにいるスピリットが提供する軽減シンボル数
    fun getProvidedSymbols(): Int {
        return 1 // スピリットは1つのシンボルを提供すると仮定
    }

    fun deepCopy(): Spirit {
        return Spirit(card, currentLevel, coresOnSpirit)
    }

    // スピリット上のコア数に基づいて現在のレベルを計算
    fun calculateLevel(): Int {
        return card.levelCosts.entries
            .filter { it.value <= coresOnSpirit }
            .maxOfOrNull { it.key } ?: 0 // 該当するレベルがなければ0
    }
}

// ゲームの状態を表すデータクラス
data class GameState(
    val hand: MutableList<SpiritCard>,
    var reserveCores: Int,
    val fieldSpirits: MutableList<Spirit>
) {
    // フィールド上の合計軽減シンボル数を取得
    fun getTotalReductionSymbols(): Int {
        return fieldSpirits.sumOf { it.getProvidedSymbols() }
    }

    fun deepCopy(): GameState {
        return GameState(
            hand = hand.map { it }.toMutableList(), // SpiritCardは不変なのでシャローコピーでOK
            reserveCores = reserveCores,
            fieldSpirits = fieldSpirits.map { it.deepCopy() }.toMutableList()
        )
    }

    override fun toString(): String {
        val handStr = if (hand.isEmpty()) "[]" else hand.joinToString(", ") { it.name }
        val fieldStr = if (fieldSpirits.isEmpty()) "[]" else fieldSpirits.joinToString(", ") {
            "${it.card.name} (Lv${it.currentLevel}, Core:${it.coresOnSpirit})"
        }
        return """
                 Hand: $handStr
                 Reserve: Core: $reserveCores
                 Field: $fieldStr
             """.trimIndent()
    }

    // Set<GameState>で正しく動作するためのequalsとhashCode
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameState

        if (hand.toSet() != other.hand.toSet()) return false // 手札の順序は考慮しない
        if (reserveCores != other.reserveCores) return false
        if (fieldSpirits.size != other.fieldSpirits.size) return false

        // フィールドのスピリットを名前でソートして比較することで、順序の違いを吸収
        val sortedThisSpirits = fieldSpirits.sortedBy { it.card.name }
        val sortedOtherSpirits = other.fieldSpirits.sortedBy { it.card.name }
        if (sortedThisSpirits != sortedOtherSpirits) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hand.toSet().hashCode()
        result = 31 * result + reserveCores
        result = 31 * result + fieldSpirits.sortedBy { it.card.name }.hashCode()
        return result
    }
}

// 可能なアクション
sealed class Action {
    object DoNothing : Action() // 何もしない（ターンをパスするなど）
    data class Summon(val card: SpiritCard) : Action() // スピリット召喚
    data class LevelUp(val spirit: Spirit, val targetLevel: Int) : Action() // スピリットのレベルアップ
    data class PlaceCoreOnSpirit(val spirit: Spirit) : Action() // リザーブからスピリットにコアを置く
}

// 現在の状態から可能なアクションのリストを取得
fun getPossibleActions(state: GameState): List<Action> {
    val actions = mutableListOf<Action>()

    // 召喚アクション
    for (cardInHand in state.hand) {
        val actualCost = max(0, cardInHand.cost - state.getTotalReductionSymbols()) // コストは0まで軽減可能
        // 召喚コストとLv1維持コア1個を支払えるか確認
        if (state.reserveCores >= actualCost + 1) {
            actions.add(Action.Summon(cardInHand))
        }
    }

    // レベルアップアクション
    for (spiritOnField in state.fieldSpirits) {
        val currentLevel = spiritOnField.currentLevel
        val coresOnSpirit = spiritOnField.coresOnSpirit

        // 現在のレベルより高いレベルで、かつそのレベルに必要なコア数が現在のコア数より多い場合
        for (targetLevel in (currentLevel + 1)..(spiritOnField.card.levelCosts.keys.maxOrNull() ?: currentLevel)) {
            val requiredCoresForTargetLevel = spiritOnField.card.levelCosts[targetLevel]!!
            if (requiredCoresForTargetLevel > coresOnSpirit) {
                val coresToMove = requiredCoresForTargetLevel - coresOnSpirit
                if (state.reserveCores >= coresToMove) {
                    actions.add(Action.LevelUp(spiritOnField, targetLevel))
                }
            }
        }
    }


    // コアをスピリットに置くアクション
    if (state.reserveCores > 0) {
        for (spiritOnField in state.fieldSpirits) {
            actions.add(Action.PlaceCoreOnSpirit(spiritOnField))
        }
    }

    return actions
}

// アクションを適用し、新しいゲームの状態を返す
fun applyAction(state: GameState, action: Action): GameState {
    val newState = state.deepCopy() // 状態を変更しないようにコピーで作業

    when (action) {
        is Action.Summon -> {
            val actualCost = max(0, action.card.cost - newState.getTotalReductionSymbols())
            newState.reserveCores -= actualCost
            newState.hand.remove(action.card)

            val newSpirit = Spirit(action.card, 1, 1) // Lv1で召喚、コア1個を置く
            newState.reserveCores -= 1 // Lv1維持コアをリザーブから支払う
            newState.fieldSpirits.add(newSpirit)
        }

        is Action.LevelUp -> {
            // newStateのfieldSpiritsから対象のスピリットを見つけて変更
            val spiritToLevel =
                newState.fieldSpirits.find { it.card == action.spirit.card && it.coresOnSpirit == action.spirit.coresOnSpirit }!!
            val requiredCoresForTargetLevel = spiritToLevel.card.levelCosts[action.targetLevel]!!
            val coresToMove = requiredCoresForTargetLevel - spiritToLevel.coresOnSpirit

            newState.reserveCores -= coresToMove
            spiritToLevel.coresOnSpirit += coresToMove
            spiritToLevel.currentLevel = action.targetLevel
        }

        Action.DoNothing -> {
            // 何もしないアクションは状態を変更しない
        }

        is Action.PlaceCoreOnSpirit -> {
            // newStateのfieldSpiritsから対象のスピリットを見つけて変更
            val spiritToPlaceCore =
                newState.fieldSpirits.find { it.card == action.spirit.card && it.coresOnSpirit == action.spirit.coresOnSpirit }!!
            newState.reserveCores -= 1
            spiritToPlaceCore.coresOnSpirit += 1
            spiritToPlaceCore.currentLevel = spiritToPlaceCore.calculateLevel() // 新しいコア数に基づいてレベルを再計算
        }
    }
    return newState
}

// メインステップのシミュレーションを行い、最終的な状態のセットを返す
fun simulateMainStep(initialState: GameState): Set<GameState> {
    val finalStates = mutableSetOf<GameState>()
    val queue: Queue<GameState> = LinkedList()
    queue.add(initialState)

    val visitedStates = mutableSetOf<GameState>() // 訪問済み状態を記録

    while (queue.isNotEmpty()) {
        val currentState = queue.poll()

        if (visitedStates.contains(currentState)) {
            continue
        }
        visitedStates.add(currentState)

        val possibleActions = getPossibleActions(currentState)

        if (possibleActions.isEmpty()) {
            finalStates.add(currentState)
            continue
        }

        for (action in possibleActions) {
            val nextState = applyAction(currentState, action)
            queue.add(nextState)
        }
    }
    return finalStates
}



fun main() {
    // カードの定義
    val dragnovScout = SpiritCard(
        name = "ドラグノ偵察兵",
        cost = 2,
        reductionSymbol = 1,
        levelCosts = mapOf(1 to 1, 2 to 2)
    )

    val rokuceratops = SpiritCard(
        name = "ロクケラトプス",
        cost = 1,
        reductionSymbol = 1,
        levelCosts = mapOf(1 to 1, 2 to 2, 3 to 3)
    )

    // 初期ゲーム状態
    val initialState = GameState(
        hand = mutableListOf(dragnovScout, rokuceratops),
        reserveCores = 4,
        fieldSpirits = mutableListOf()
    )

    val finalStates = simulateMainStep(initialState)

    println("GameStatus(${finalStates.size}):")
    finalStates.sortedBy { it.toString() }.forEachIndexed { index, state -> // 出力を一貫させるためにソート
        println("--- Pattern ${index + 1} ---")
        println(state)
    }
}
	 