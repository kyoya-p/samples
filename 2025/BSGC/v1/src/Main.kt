import kotlin.collections.plus
import kotlin.sequences.flatMap
import kotlin.sequences.map

// カードとスピリットのデータクラス
sealed class Card(
    val name: String,
    val cost: Int, // コスト
    val reductionSymbol: Int, // 軽減シンボル数
    val symbols: Symbols, // シンボル数
) {
    class SpiritCard(
        name: String,
        cost: Int, // 召喚コスト
        reductionSymbol: Int, // 軽減シンボル数 (赤シンボルとして扱う)
        symbols: Symbols, // シンボル数
        val levelCosts: Map<Int, Int> // レベル -> そのレベルに必要なコア数
    ) : Card(name, cost, reductionSymbol, symbols)
}

sealed class FieldObject(val card: Card, val cores: Int) {

    class Spirit(
        _card: Card.SpiritCard,
        _cores: Int
    ) : FieldObject(_card, _cores)

    fun calculateLevel(): Int {
        return (card as Card.SpiritCard).levelCosts.entries
            .filter { it.value <= cores }
            .maxOfOrNull { it.key } ?: 0 // 該当するレベルがなければ0
    }

    fun symbols() = card.symbols
}

typealias Color = Int

val C: Color = 0x0000_0000 // Clear
val R: Color = 0x0000_0001
val P: Color = 0x0000_0010
val G: Color = 0x0000_0100
val W: Color = 0x0000_1000
val Y: Color = 0x0001_0000
val B: Color = 0x0010_0000
val primaryColors = setOf(R, P, G, W, Y, B)

/*
def johntrot_rv(n):
  if n == 1: return [[1]]
  perm = []
  for k, pm in enumerate(johntrot_rv(n-1)):
    for i in range(n):
      i1 = n-1-i if k%2==0 else i
      perm += [pm[:i1]+[n]+pm[i1:]]
  return perm

* */

// t個の要素からn個を選択する場合の順列
fun perm(t:Int, pick:Int) = sequence {
    if (pick == 0) {
        yield(emptyList())
        return@sequence
    }
    if (t == 0) {
        return@sequence
    }

    val elements = (0..<t).toList()
    val indices = (0..<t).toMutableList()
    val c = IntArray(t) { 0 }
    val a = elements.toMutableList()

    // 初期順列
    yield(a.take(pick))

    var i = 1
    while (i < t) {
        if (c[i] < i) {
            val k = if (i % 2 == 0) 0 else c[i]
            val temp = a[i]
            a[i] = a[k]
            a[k] = temp
            c[i]++
            i = 1
            yield(a.take(pick))
        } else {
            c[i] = 0
            i++
        }
    }
}


//fun perm0(n: Int): Sequence<List<Int>> = when (n) {
//    0 -> sequenceOf(emptyList())
//    1 -> sequenceOf(listOf(0))
//    else -> perm(n - 1).flatMap { p -> (0..n - 1).map { i -> p.take(i) + (n - 1) + p.drop(i) } }
//}

// Heap's Algorithm
//fun perm(n: Int): Sequence<List<Int>> = sequence {
//    val a = (0..<n).toMutableList()
//    val c = IntArray(n) { 0 }
//    yield(a.toList())
//
//    var i = 1
//    while (i < n) {
//        if (c[i] < i) {
//            val k = if (i % 2 == 0) 0 else c[i]
//            val temp = a[i]
//            a[i] = a[k]
//            a[k] = temp
//            c[i]++
//            i = 1
//            yield(a.toList())
//        } else {
//            c[i] = 0
//            i++
//        }
//    }
//}


fun comb(n: Int) = sequence {
    val a = (0..<n).toMutableList()
    val c = IntArray(n) { 0 }
    yield(a.toList())

    var i = 1
    while (i < n) {
        if (c[i] < i) {
            val k = if (i % 2 == 0) 0 else c[i]
            val temp = a[i]
            a[i] = a[k]
            a[k] = temp
            c[i]++
            i = 1
            yield(a.toList())
        } else {
            c[i] = 0
            i++
        }
    }
}

fun <T> Collection<T>.perm(n: Int): Sequence<List<T>> = when (n) {
    0 -> sequenceOf(emptyList())
    else -> asSequence().flatMap { elem -> (this - elem).perm(n - 1).map { it + elem } }
}

fun <T> Collection<T>.comb(n: Int): Sequence<List<T>> = perm(n).distinctBy { it.toSet() }


fun Color.isColor(c: Color) = if (c == C) this == 0 else (this and c) != 0
infix operator fun Color.contains(c: Color) = (this and c) != 0
fun union(vararg o: Color): Color = o.fold(0) { a, e -> a or e }
fun intersect(vararg o: Color): Color = o.fold(0) { a, e -> a and e }

data class Symbol(val color: Color, val symbols: Int)

typealias Symbols = Map<Color, Int>

operator fun Symbols.plus(o: Symbols) = entries.map { it.key }


val R1: Symbols = mapOf(R to 1)
val P1: Symbols = mapOf(P to 1)
val G1: Symbols = mapOf(G to 1)

val NoSymbol: Symbols = emptyMap()

// ゲームの状態を表すデータクラス
data class Board(
    val hand: List<Card> = emptyList(),
    val reserveCores: Int = 4,
    val fieldObjects: List<FieldObject> = emptyList(),
    val trashCores: Int = 0
) {
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

data class Game(val board: Board, @Suppress("unused") val board2: Board = Board())

//fun Game.fieldSymbols() = board.fieldObjects.fold(NoSymbol) { e, a -> a.symbols() + e }
fun Game.fieldSymbols(): Int = TODO()
//board.fieldObjects.fold(1) { a,e-> 1 }


// 可能なアクション
sealed class Action() {
    object DoNothing : Action() // 何もしない（ターンをパスするなど）
    data class Summon(val card: Card.SpiritCard) : Action() // スピリット召喚
    data class SwapObjectCores(val ix: Int, val iy: Int) : Action() // フィールドオブジェクトのコアを入れ替える
    data class SwapReserveCores(val ix: Int) : Action() // フィールドオブジェクトのコアを入れ替える
}

// 現在の状態から可能な行動/効果のリストを取得
fun Game.listChoices() = sequence {
    yield(Action.DoNothing) // 何もしない=ステップを進める

    // Main
    board.hand.map { it as Card.SpiritCard }.forEach { yield(Action.Summon(it)) } // 手札から発揮
    val nObj = board.fieldObjects.size
    (0..<nObj).forEach { a -> (a + 1..<nObj).forEach { b -> yield(Action.SwapObjectCores(a, b)) } } // オブジェクト間のコア置換
    (0..<nObj).forEach { yield(Action.SwapReserveCores(it)) } // リザーブとのコア置換
    // TODO Trashから発揮
    // TODO FieldObjectから発揮
}

// 現在の状態からActionの結果、可能な行動/効果のリストを取得
fun Game.listChoices(a: Action) = sequence<Game> {
    when (a) {
        is Action.DoNothing -> {}  // ステップ終了
        is Action.Summon -> summon(a)
        is Action.SwapReserveCores -> swapReserveCores(a)
        is Action.SwapObjectCores -> swapObjectCores(a)
    }
}

//TODO
//fun Game.fieldSymbols(): Symbols = board.fieldObjects.flatMap { it.card.symbols }.toSet()

fun Game.summon(a: Action.Summon) = sequence<Game> {
//    a.card.cost - fieldSymbols() //TODO
}

fun Game.swapReserveCores(a: Action.SwapReserveCores) {

}

fun Game.swapObjectCores(a: Action.SwapObjectCores) {

}

// アクションを適用し、新しいゲームの状態を返す
fun Board.applyAction(action: Action) = sequence<Board> {
    when (action) {
        is Action.DoNothing -> yield(this@applyAction)
        is Action.Summon -> {
            val cardToSummon = action.card
            if (hand.contains(cardToSummon)) {
                // コスト計算
                val actualCost: Int = TODO()

                if (reserveCores >= actualCost) {
                    // 手札からカードを除去
                    val newHand = hand.toMutableList().apply { remove(cardToSummon) }

                    // フィールドにスピリットを追加（最低1コアを置く）
                    val newFieldObjects = fieldObjects.toMutableList().apply {
                        add(FieldObject.Spirit(cardToSummon, 1))
                    }

                    // リザーブコアを更新
                    val newReserveCores = reserveCores - actualCost

                    yield(
                        this@applyAction.copy(
                            hand = newHand,
                            reserveCores = newReserveCores,
                            fieldObjects = newFieldObjects
                        )
                    )
                }
            }
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
                        val newFieldObjects = fieldObjects.toMutableList()
                        newFieldObjects[idx1] =
                            FieldObject.Spirit(spirit1.card as Card.SpiritCard, spirit1.cores - coresToMove)
                        newFieldObjects[idx2] =
                            FieldObject.Spirit(spirit2.card as Card.SpiritCard, spirit2.cores + coresToMove)
                        yield(this@applyAction.copy(fieldObjects = newFieldObjects))
                    }
                    // spirit2 -> spirit1
                    if (spirit2.cores >= coresToMove) {
                        val newFieldObjects = fieldObjects.toMutableList()
                        newFieldObjects[idx1] =
                            FieldObject.Spirit(spirit1.card as Card.SpiritCard, spirit1.cores + coresToMove)
                        newFieldObjects[idx2] =
                            FieldObject.Spirit(spirit2.card as Card.SpiritCard, spirit2.cores - coresToMove)
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
                        newFieldObjects[idx] =
                            FieldObject.Spirit(spirit.card as Card.SpiritCard, spirit.cores + coresToMove)
                        yield(
                            this@applyAction.copy(
                                reserveCores = reserveCores - coresToMove,
                                fieldObjects = newFieldObjects
                            )
                        )
                    }
                }

                // スピリットからリザーブへコアを移動
                if (spirit.cores > 0) {
                    for (coresToMove in 1..spirit.cores) {
                        val newFieldObjects = fieldObjects.toMutableList()
                        newFieldObjects[idx] =
                            FieldObject.Spirit(spirit.card as Card.SpiritCard, spirit.cores - coresToMove)
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
    val dragnovScout = Card.SpiritCard(
        name = "ドラグノ偵察兵",
        cost = 2,
        reductionSymbol = 1,
        levelCosts = mapOf(1 to 1, 2 to 2),
        symbols = R1,
    )

    val rokuceratops = Card.SpiritCard(
        name = "ロクケラトプス",
        cost = 1,
        reductionSymbol = 1,
        levelCosts = mapOf(1 to 1, 2 to 2, 3 to 3),
        symbols = R1,
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
