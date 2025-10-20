// カードとスピリットのデータクラス
sealed class Card(
    val name: String,
    val cost: Int, // コスト
    val reductionSymbol: Int, // 軽減シンボル数
    val symbols: Int, // シンボル数
) {
    class SpiritCard(
        name: String,
        cost: Int, // 召喚コスト
        reductionSymbol: Int, // 軽減シンボル数 (赤シンボルとして扱う)
        symbols: Int, // シンボル数
        val levelCosts: Map<Int, Int> // レベル -> そのレベルに必要なコア数
    ) : Card(name, cost, reductionSymbol, symbols) {
        override fun getProvidedSymbols(): Int = 1
    }

    open fun getProvidedSymbols(): Int = 0 //TODO     // フィールドオブジェクトが提供する軽減シンボル数
}

sealed class FieldObject(val card: Card,val cores:Int) {

    class Spirit(
        _card: Card.SpiritCard,
        _cores: Int
    ) : FieldObject(_card,_cores) {
        fun calculateLevel(): Int {
            return (card as Card.SpiritCard).levelCosts.entries
                .filter { it.value <= cores }
                .maxOfOrNull { it.key } ?: 0 // 該当するレベルがなければ0
        }
    }

    fun getProvidedSymbols() = card.getProvidedSymbols()
}

// ゲームの状態を表すデータクラス
data class Board(
    val hand: List<Card>,
    val reserveCores: Int = 4,
    val fieldObjects: List<FieldObject.Spirit> = emptyList(),
    val trashCores: Int = 0
) {
    fun getTotalReductionSymbols() = fieldObjects.sumOf { it.getProvidedSymbols() } // フィールド上の合計軽減シンボル数を取得

    override fun toString(): String {
        val handStr = if (hand.isEmpty()) "[]" else hand.joinToString(", ") { it.name }
        val fieldStr = if (fieldObjects.isEmpty()) "[]" else fieldObjects.joinToString(", ") {
            "${it.card.name} (Lv${it.calculateLevel()}, Core:${it.cores})"
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
    data class Summon(val card: Card.SpiritCard) : Action() // スピリット召喚
    data class LevelUp(val spirit: FieldObject.Spirit, val targetLevel: Int) : Action() // スピリットのレベルアップ

    //    data class PlaceCoreOnSpirit(val spirit: Spirit) : Action() // リザーブからスピリットにコアを置く
    data class SwapObjectCores(val ix: Int, val iy: Int) : Action() // フィールドオブジェクトのコアを入れ替える
    data class SwapReserveCores(val ix: Int) : Action() // フィールドオブジェクトのコアを入れ替える
}

// 現在の状態から可能な行動/効果のリストを取得
fun Board.listChoices() = sequence {
    yield(Action.DoNothing) // 何もしない=ステップを進める

    // Main
    hand.map { it as Card.SpiritCard }.forEach { yield(Action.Summon(it)) } // 手札から発揮
    // TODO Trashから発揮
    // TODO FieldObjectから発揮
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
//TODO
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
                for (coresToMove in 1..maxOf(spirit1.cores, spirit2.cores)) {
                    // spirit1 -> spirit2
                    if (spirit1.cores >= coresToMove) {
                        // TODO
                    }
                    // spirit2 -> spirit1
                    if (spirit2.cores >= coresToMove) {
                        //TODO
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
//                    for (coresToMove in 1..reserveCores) {
//                        val newFieldObjects = fieldObjects.toMutableList()
//                        newFieldObjects[idx] = spirit.copy(coresOnSpirit = spirit.coresOnSpirit + coresToMove)
//                        newFieldObjects[idx].calculateLevel() = newFieldObjects[idx].calculateLevel()
//                        yield(
//                            this@applyAction.copy(
//                                reserveCores = reserveCores - coresToMove,
//                                fieldObjects = newFieldObjects
//                            )
//                        )
//                    }
                }

                // スピリットからリザーブへコアを移動
                if (spirit.cores > 0) {
//                    for (coresToMove in 1..spirit.coresOnSpirit) {
//                        val newFieldObjects = fieldObjects.toMutableList()
//                        newFieldObjects[idx] = spirit.copy(coresOnSpirit = spirit.coresOnSpirit - coresToMove)
//                        newFieldObjects[idx].currentLevel = newFieldObjects[idx].calculateLevel()
//                        yield(
//                            this@applyAction.copy(
//                                reserveCores = reserveCores + coresToMove,
//                                fieldObjects = newFieldObjects
//                            )
//                        )
//                    }
                }
            }
        }
    }
}

fun main() {
    // カードの定義
    val dragnovScout = Card.SpiritCard(
        name = "ドラグノ偵察兵",
        cost = 2,
        reductionSymbol = 1,
        levelCosts = mapOf(1 to 1, 2 to 2),
        symbols = 1,
    )

    val rokuceratops = Card.SpiritCard(
        name = "ロクケラトプス",
        cost = 1,
        reductionSymbol = 1,
        levelCosts = mapOf(1 to 1, 2 to 2, 3 to 3),
        symbols = 1,
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
	 