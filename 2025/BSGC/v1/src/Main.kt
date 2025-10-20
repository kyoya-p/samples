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
data class Board(
    val hand: List<SpiritCard>,
    val reserveCores: Int,
    val fieldObjects: List<Spirit>,
    val trashCores: Int = 0
) {
    fun getTotalReductionSymbols() = fieldObjects.sumOf { it.getProvidedSymbols() } // フィールド上の合計軽減シンボル数を取得
    fun deepCopy(): Board {
        return Board(
            hand = hand.map { it }.toMutableList(), // SpiritCardは不変なのでシャローコピーでOK
            reserveCores = reserveCores,
            fieldObjects = fieldObjects.map { it.deepCopy() }.toMutableList(),
            trashCores = trashCores
        )
    }

    override fun toString(): String {
        val handStr = if (hand.isEmpty()) "[]" else hand.joinToString(", ") { it.name }
        val fieldStr = if (fieldObjects.isEmpty()) "[]" else fieldObjects.joinToString(", ") {
            "${it.card.name} (Lv${it.currentLevel}, Core:${it.coresOnSpirit})"
        }
        return """
                 Hand: $handStr
                 Reserve: Core: $reserveCores
                 Field: $fieldStr
                 Trash: Core: $trashCores
             """.trimIndent()
    }

    // Set<GameState>で正しく動作するためのequalsとhashCode
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Board

        if (hand.toSet() != other.hand.toSet()) return false // 手札の順序は考慮しない
        if (reserveCores != other.reserveCores) return false
        if (fieldObjects.size != other.fieldObjects.size) return false
        if (trashCores != other.trashCores) return false

        // フィールドのスピリットを名前でソートして比較することで、順序の違いを吸収
        val sortedThisSpirits = fieldObjects.sortedBy { it.card.name }
        val sortedOtherSpirits = other.fieldObjects.sortedBy { it.card.name }
        if (sortedThisSpirits != sortedOtherSpirits) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hand.toSet().hashCode()
        result = 31 * result + reserveCores
        result = 31 * result + fieldObjects.sortedBy { it.card.name }.hashCode()
        result = 31 * result + trashCores
        return result
    }
}

// 可能なアクション
sealed class Action() {
    object DoNothing : Action() // 何もしない（ターンをパスするなど）
    data class Summon(val card: SpiritCard) : Action() // スピリット召喚
    data class LevelUp(val spirit: Spirit, val targetLevel: Int) : Action() // スピリットのレベルアップ

    //    data class PlaceCoreOnSpirit(val spirit: Spirit) : Action() // リザーブからスピリットにコアを置く
    data class SwapObjectCores(val ix: Int, val iy: Int) : Action() // フィールドオブジェクトのコアを入れ替える
    data class SwapReserveCores(val ix: Int) : Action() // フィールドオブジェクトのコアを入れ替える
}

// 現在の状態から可能なアクションのリストを取得
fun Board.getPossibleActions() = sequence {
    yield(Action.DoNothing) // 何もしない=ステップを進める

    // Main
    hand.forEach { yield(Action.Summon(it)) } // 手札から償還
    val nObj = fieldObjects.indices
    nObj.forEach { a -> nObj.forEach { b -> if (a != b) yield(Action.SwapObjectCores(a, b)) } } // オブジェクト間のコア置換
    nObj.forEach { yield(Action.SwapReserveCores(it)) } // リザーブとのコア置換
}

// アクションを適用し、新しいゲームの状態を返す
fun Board.applyAction(action: Action) = sequence<Board> {
    when (action) {
        is Action.DoNothing -> yield(this@applyAction)
        is Action.Summon -> {
            val cardToSummon = action.card
            if (hand.contains(cardToSummon)) {
                // 召喚コストの計算
                val actualCost = max(0, cardToSummon.cost - getTotalReductionSymbols() - cardToSummon.reductionSymbol)

                if (reserveCores >= actualCost) {
                    val newHand = hand.toMutableList().apply { remove(cardToSummon) }
                    val newReserveCores = reserveCores - actualCost
                    val newSpirit = Spirit(cardToSummon, 0, 0) // 召喚時はコア0、レベル0
                    val newFieldObjects = fieldObjects.toMutableList().apply { add(newSpirit) }

                    // 召喚後、リザーブのコアをスピリットに置くすべての場合を考慮
                    // 1コアから召喚コスト分のコアを置く
                    for (coresToPlace in 1..actualCost) {
                        if (newReserveCores >= coresToPlace) {
                            val stateAfterPlacingCores = Board(
                                hand = newHand,
                                reserveCores = newReserveCores - coresToPlace,
                                fieldObjects = newFieldObjects.toMutableList().apply {
                                    // 新しいスピリットにコアを置く
                                    this[this.lastIndex] = newSpirit.copy(coresOnSpirit = coresToPlace)
                                },
                                trashCores = trashCores + actualCost - coresToPlace
                            )
                            // スピリットのレベルを更新
                            stateAfterPlacingCores.fieldObjects.last().currentLevel =
                                stateAfterPlacingCores.fieldObjects.last().calculateLevel()
                            yield(stateAfterPlacingCores)
                        }
                    }
                    // コアを1つも置かない場合も考慮 (コスト0召喚など)
                    if (actualCost == 0) {
                        yield(
                            Board(
                                hand = newHand,
                                reserveCores = newReserveCores,
                                fieldObjects = newFieldObjects,
                                trashCores = trashCores + actualCost
                            )
                        )
                    }
                }
            }
        }

        is Action.LevelUp -> {
            // このアクションは現在使用されていないが、将来のために残す
            // スピリットのレベルアップは、コアの配置によって自動的に行われるため、明示的なアクションとしては不要かもしれない
            // ただし、リザーブからコアを移動させるアクションと組み合わせることで実現可能
            yield(this@applyAction)
        }

        is Action.SwapObjectCores -> {
            val (idx1, idx2) = Pair(action.ix, action.iy)
            if (idx1 in fieldObjects.indices && idx2 in fieldObjects.indices && idx1 != idx2) {
                val spirit1 = fieldObjects[idx1]
                val spirit2 = fieldObjects[idx2]

                // コアの移動パターンをすべて試す
                for (coresToMove in 1..maxOf(spirit1.coresOnSpirit, spirit2.coresOnSpirit)) {
                    // spirit1 -> spirit2
                    if (spirit1.coresOnSpirit >= coresToMove) {
                        val newFieldObjects = fieldObjects.toMutableList()
                        newFieldObjects[idx1] = spirit1.copy(coresOnSpirit = spirit1.coresOnSpirit - coresToMove)
                        newFieldObjects[idx2] = spirit2.copy(coresOnSpirit = spirit2.coresOnSpirit + coresToMove)
                        newFieldObjects[idx1].currentLevel = newFieldObjects[idx1].calculateLevel()
                        newFieldObjects[idx2].currentLevel = newFieldObjects[idx2].calculateLevel()
                        yield(this@applyAction.copy(fieldObjects = newFieldObjects))
                    }
                    // spirit2 -> spirit1
                    if (spirit2.coresOnSpirit >= coresToMove) {
                        val newFieldObjects = fieldObjects.toMutableList()
                        newFieldObjects[idx1] = spirit1.copy(coresOnSpirit = spirit1.coresOnSpirit + coresToMove)
                        newFieldObjects[idx2] = spirit2.copy(coresOnSpirit = spirit2.coresOnSpirit - coresToMove)
                        newFieldObjects[idx1].currentLevel =
                            newFieldObjects[idx1].calculateLevel()
                            newFieldObjects[idx2].calculateLevel()
                        yield(this@applyAction.copy(fieldObjects = newFieldObjects))
                    }
                }
            }
        }

        is Action.SwapReserveCores -> {
            val idx = action.ix
            if (idx in fieldObjects.indices) {
                val spirit = fieldObjects[idx]

                // リザーブからスピリットへコアを移動
                if (reserveCores > 0) {
                    for (coresToMove in 1..reserveCores) {
                        val newFieldObjects = fieldObjects.toMutableList()
                        newFieldObjects[idx] = spirit.copy(coresOnSpirit = spirit.coresOnSpirit + coresToMove)
                        newFieldObjects[idx].currentLevel = newFieldObjects[idx].calculateLevel()
                        yield(
                            this@applyAction.copy(
                                reserveCores = reserveCores - coresToMove,
                                fieldObjects = newFieldObjects
                            )
                        )
                    }
                }

                // スピリットからリザーブへコアを移動
                if (spirit.coresOnSpirit > 0) {
                    for (coresToMove in 1..spirit.coresOnSpirit) {
                        val newFieldObjects = fieldObjects.toMutableList()
                        newFieldObjects[idx] = spirit.copy(coresOnSpirit = spirit.coresOnSpirit - coresToMove)
                        newFieldObjects[idx].currentLevel = newFieldObjects[idx].calculateLevel()
                        yield(
                            this@applyAction.copy(
                                reserveCores = reserveCores + coresToMove,
                                fieldObjects = newFieldObjects
                            )
                        )
                    }
                }
            }
        }
    }
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
    val initialState = Board(
        hand = mutableListOf(dragnovScout),
        reserveCores = 4,
        fieldObjects = mutableListOf()
    )

//    val finalStates = simulateMainStep(initialState)
//
//    println("GameStatus(${finalStates.size}):")
//    finalStates.sortedBy { it.toString() }.forEachIndexed { index, state -> // 出力を一貫させるためにソート
//        println("--- Pattern ${index + 1} ---")
//        println(state)
//    }
}
	 