package BSSim.ロードラ

import BSSim.*
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.io.FileOutputStream
import java.io.PrintStream
import kotlin.random.Random
import kotlin.io.*

val logger = PrintStream(FileOutputStream("log.csv"))

val LvNone = listOf<Card.LevelInfo>()
val Lv11 = listOf(Card.LevelInfo(1, 1, 0))
val Lv10_24 = listOf(Card.LevelInfo(1, 0, 0), Card.LevelInfo(2, 4, 0))


//デッキをオープンし条件に合致したカード1枚を手札に加え、残ったカードはデッキの下に
class eサーチ1枚手札By_残デッキ下(val nOpen: Int, val cond: Cards.() -> Cards) : Maneuver {
    override val efName = "サーチ"
    override fun use(h: History): ParallelWorld = kotlin.runCatching {
        sequenceOf(h)
                .flatMap_ownSide { opMoveCards(PICKEDCARD, deck.cards.top(nOpen).onlyTakeOneCase("${nOpen}枚オープンだが　デッキのカードは${deck.cards}\n${h.world}")) }//nOpen枚オープンしpickedに置く
                .flatMap_ownSide {
                    val pick = pickedCards.cards.cond()
                    if (pick.size == 0) sequenceOf(this) //手札に加えるカードはない
                    else pick.asSequence().flatMap { opMoveCard(HAND, it) } //手札に1枚加える
                }
                .flatMap_ownSide {
                    opMoveCardsBy(DECK, pickedCards.cards) { dst, cs -> // 残りをデッキに
                        sequenceOf(dst + cs) // TODO: 本来は好きな順序で戻す。まじめにやると組合せ爆発しそう...
                    }
                }
    }.onFailure {
        println(h.world)
        throw it
    }.getOrThrow()
}

//デッキをオープンしカードを～、残ったカードはデッキの下に TODO: テスト用に除外
class eサーチBy_デッキ下(val nOpen: Int, val cond: History.() -> ParallelWorld) : Maneuver {
    override val efName = "サーチ"

    override fun use(h: History): ParallelWorld = sequenceOf(h)
            .flatMap_ownSide { opMoveCards(PICKEDCARD, deck.cards.top(nOpen).onlyTakeOneCase()) }//4枚オープンしpickedに置く

            .flatMap { it.cond() }
            .flatMap_ownSide {
                opMoveCardsBy(DECK, pickedCards.cards) { dst, cs ->
                    sequenceOf(dst + cs) // TODO: 本来は好きな順序で戻す。まじめにやると組合せ爆発しそう...
//                    sequenceOf(dst) // TODO: テスト時間短縮のため破棄
                }
            }
}

//デッキをオープンし条件に合致したカードをすべて手札に加え、残ったカードはデッキの下に
class eサーチAll(val nOpen: Int, val cond: (Card) -> Boolean) : Maneuver {
    override val efName = "サーチ"

    override fun use(h: History): ParallelWorld = sequenceOf(h)
            .flatMap_ownSide { opMoveCards(PICKEDCARD, deck.cards.top(nOpen).onlyTakeOneCase()) }//4枚オープンしpickedに置く
            .flatMap_ownSide {
                pickedCards.cards.asSequence().filter { cond(it) }.flatMap {
                    opMoveCard(HAND, it) //手札に加える
                }
            }
            .flatMap_ownSide {
                opMoveCardsBy(DECK, pickedCards.cards) { dst, cs ->
                    sequenceOf(dst + cs) // TODO: 本来は好きな順序で戻す。まじめにやると組合せ爆発しそう...
                }
            }
}

/*デッキ定義*/
class 電人トレイン : SpiritCard(Category.SPIRITCARD, "電トレ", Color.W, 3, Sbl.W, Sbl.W * 1, setOf(Family.武装, Family.界渡), Lv11) {
    override fun use(h: History): Sequence<History> = sequenceOf(h)
            .effect(e召喚配置(this))  // このカードを召喚する
            //以下召喚時効果
            .optional { //～できる
                サーチ1().effect(ePayCost(1))
            }


    fun ParallelWorld.サーチ1(): ParallelWorld = effect(eサーチ1枚手札By_残デッキ下(4) {
        filter { it.family.contains(Family.創界神) && it.colors == Color.W } //白の創界神1枚
    })
}

class 幼グランロロ : SpiritCard(Category.SPIRITCARD, "幼グラ", Color.RPGWYB, 2, Sbl.Gd, Sbl.Zero * 1, setOf(Family.武装, Family.界渡), Lv11) {
    override fun use(h: History): Sequence<History> = sequenceOf(h)
            .effect(e召喚配置(this))  // このカードを召喚する
            //以下召喚時効果
            .optional { //～できる
                サーチ1()
            }
            .optional {
                effect(ePayCost(1)) //1コスト支払いもう一度
                        .サーチ1()
                        .effect(e消滅チェック())
            }

    fun ParallelWorld.サーチ1(): ParallelWorld = effect(eサーチ1枚手札By_残デッキ下(3) {
        filter { it.family.contains(Family.創界神) } //創界神1枚
    })
}

// https://batspi.com/index.php?神世界の案内人トリックスター
class 神世界トリスタ : SpiritCard(Category.SPIRITCARD, "神トリ", Color.Y, 5, Sbl.Y, Sbl.Y * 2 + Sbl.Gd, setOf(Family.道化, Family.界渡), Lv11) {
    override fun use(h: History): Sequence<History> = sequenceOf(h)
            .effect(e召喚配置(this))  // このカードを召喚する
            //以下召喚時効果
            .optional { //～できる
                effect(eサーチBy_デッキ下(5) {
                    world.ownSide.pickedCards.cards.asSequence().filter { it.family.contains(Family.創界神) || it.family.contains(Family.界渡) }.flatMap {
                        sequenceOf(this).flatMap_ownSide { opMoveCard(HAND, it) }
                    }
                })
            }
}

// https://batspi.com/index.php?cmd=read&page=サルベージ&word=サルベージ
class サルベージ : MagicCard(Category.MAGICCARD, "サルベ", Color.B, 4, Sbl.Zero) {
    override fun use(h: History): ParallelWorld = sequenceOf(h)
            .effect(eマジック使用(this))
            //効果
            .effect(eサーチ1枚手札By_残デッキ下(5) {
                filter { it.category == Category.NEXUSCARD }
            })
}

// https://batspi.com/index.php?cmd=read&page=転校生
class 転校生 : MagicCard(Category.MAGICCARD, "転校生", Color.G, 4, Sbl.G + Sbl.R) {
    override fun use(h: History): ParallelWorld = sequenceOf(h)
            .effect(eマジック使用(this))
            .effect(eサーチBy_デッキ下(4) {
                val gw = world.ownSide.pickedCards.cards.filter { it.family.contains(Family.ウル) && it.family.contains(Family.創界神) }
                if (gw.count() == 0) {
                    sequenceOf(this) //GWなければなにもしない
                } else {
                    gw.asSequence().flatMap {
                        sequenceOf(this).effect(e召喚配置_NoCost(it)) //あればどれか一枚をコストを支払わず配置
                    }
                }
            })
}

// https://batspi.com/index.php?cmd=read&page=日下%20チヒロ
class チヒロ : NexusCard(Category.NEXUSCARD, "チヒロ", Color.W, 2, Sbl.Gd, Sbl.W + Sbl.Gd, setOf(Family.創界神, Family.ウル), Lv10_24) {
    override fun use(h: History): Sequence<History> = sequenceOf(h)
            .effect(e召喚配置(this))
}

// https://batspi.com/index.php?cmd=read&page=巽%20キマリ
val Lv10_26 = listOf(Card.LevelInfo(1, 0, 0), Card.LevelInfo(2, 6, 0))

class キマリ : NexusCard(Category.NEXUSCARD, "キマリ", Color.P, 2, Sbl.Gd, Sbl.P + Sbl.Gd, setOf(Family.創界神, Family.ウル), Lv10_26) {
    override fun use(h: History): Sequence<History> = sequenceOf(h)
            .effect(e召喚配置(this))
}

class ダミー : SpiritCard(Category.SPIRITCARD, "ダミー", Color.Y, 12, Sbl.R, Sbl.R * 0, setOf(Family.覇王), Lv11) {
    override fun use(h: History): Sequence<History> = sequenceOf()//何もしない
}

fun ParallelWorld.turn(eval: (World) -> Double): ParallelWorld = effect(eStartStep())
        .effect(eCoreStep(1))
        .effect(eDrawStep(1))
        .effect(eRefreshStep())
        .effect(eMainStep() { eval(it) })

fun main(args: Array<String>) = runBlocking {
    val seedTop = args[0]?.toInt()
    val loopCount = args[1]?.toInt()

    fun <T> x(n: Int, op: () -> T): List<T> = sequence { repeat(n) { yield(op()) } }.toList()
    val deckBase = x(3) { チヒロ() } + x(3) { キマリ() } + x(40) { ダミー() }
    val deckOpt = listOf(
            x(3) { ダミー() } + x(3) { ダミー() } + x(3) { ダミー() } + x(3) { ダミー() } + x(3) { ダミー() }
            , x(3) { 電人トレイン() } + x(3) { 幼グランロロ() } + x(3) { サルベージ() } + x(3) { 転校生() } + x(3) { 神世界トリスタ() }
            , x(3) { 電人トレイン() } + x(3) { 幼グランロロ() } + x(3) { ダミー() } + x(3) { 転校生() } + x(3) { 神世界トリスタ() }
            , x(3) { 電人トレイン() } + x(3) { 幼グランロロ() } + x(3) { サルベージ() } + x(3) { ダミー() } + x(3) { 神世界トリスタ() }
            , x(3) { 電人トレイン() } + x(3) { 幼グランロロ() } + x(3) { サルベージ() } + x(3) { 転校生() } + x(3) { ダミー() }
    )

    logger.println("cond, seed, step, termRatio, nCase, deck")

    for (deckOpt1 in deckOpt) {
        async {
            for (seed in seedTop until seedTop + loopCount) {
                test11(deckOpt1.toSet(), deckBase, seed)
            }
        }
    }
}

fun test11(deckOpt: Set<Card>, deckBase: List<Card>, seed: Int) {
    val deck = (deckOpt + deckBase).take(40)
    val deckBottom = DeckBottom()
    val deck1 = deck.shuffled(Random(seed)).toSet() + deckBottom
    fun Side.evaluation(): Int = deckDepth(deckBottom)            // ロードラ用評価関数=デッキ進度
    fun <T> Sequence<T>.cutBranchesBy(nTake: Int, nBest: Int, op: (T) -> Int): Sequence<T> = sortedBy { op(it) }.let { it.take(nBest) + it.drop(nBest).toList().shuffled() }.take(nTake)
    fun ParallelWorld.cutBranches(): ParallelWorld = cutBranchesBy(20, 5) { it.world.ownSide.evaluation() }

    //    val termStn = mutableSetOf<World>()
    fun ParallelWorld.terminationCheck(): ParallelWorld {
        val nCond = count()
        val passStn = flatMap_world { // 終了条件チェック
            if (ownSide.fieldObjectsMap.any { (_, a) -> a.cards[0] is チヒロ }
                    || ownSide.hand.cards.any { it is チヒロ }
            ) {
                //println("TermWorld: ${this}")
                //termStn.add(this)
                sequenceOf() //世界は継続しない
            } else {
                sequenceOf(this)
            }
        }
        val nTerm = nCond - passStn.count()

        if (nCond != 0) {
            val aStep = map { it.world.step }.average()
            logger.println(
                    "${deckOpt.joinToString("_")}, ${seed}, ${aStep}, ${nTerm * 1.0 / nCond}, ${nCond}, ${deck1.joinToString("_")}"
            )
        }
        return passStn
    }

    fun eval(w: World) = w.ownSide.fieldSimbols.toInt().toDouble()
    fun ParallelWorld.turn(): ParallelWorld = effect(eStartStep())
            .effect(eCoreStep(1))
            .effect(eDrawStep(1))
            .effect(eRefreshStep())
            .effect(eMainStep() { eval(it) })
            .terminationCheck()

    History.eden(deck1, enemyDeck = setOf())
            .map_ownSide { mutation { reserve.core = Core(4, 0) } }  //.pln { "${stn.ownSide} : ${stn.ownSide.deckDepth(deckBottom)} " }
            .effect(eDrawStep(4)).terminationCheck().cutBranches()
            .turn().cutBranches()
            .pln { this.world }
            .forEach { }
}



