package BSSim.ロードラ

import BSSim.*
import java.util.*

val LvNone = listOf<Card.LevelInfo>()
val Lv11 = listOf(Card.LevelInfo(1, 1, 0))
val Lv10_24 = listOf(Card.LevelInfo(1, 0, 0), Card.LevelInfo(2, 4, 0))


//デッキをオープンし条件に合致したカード1枚を手札に加え、残ったカードはデッキの下に
class eサーチ1枚手札By_残デッキ下(val nOpen: Int, val cond: Cards.() -> Cards) : Effect {
    override val efName = "サーチ"
    override fun use(h: History): ParallelWorld = sequenceOf(h)
            .flatMap_ownSide { opMoveCards(PICKEDCARD, deck.cards.top(nOpen).onlyTakeOneCase("${nOpen}枚オープンだが　デッキのカード数は${deck.cards.size}")) }//nOpen枚オープンしpickedに置く
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
}

//デッキをオープンしカードを～、残ったカードはデッキの下に
class eサーチBy_デッキ下(val nOpen: Int, val cond: History.() -> ParallelWorld) : Effect {
    override val efName = "サーチ"

    override fun use(h: History): ParallelWorld = sequenceOf(h)
            .flatMap_ownSide { opMoveCards(PICKEDCARD, deck.cards.top(nOpen).onlyTakeOneCase()) }//4枚オープンしpickedに置く
            .flatMap { it.cond() }
            .flatMap_ownSide {
                opMoveCardsBy(DECK, pickedCards.cards) { dst, cs ->
                    sequenceOf(dst + cs) // TODO: 本来は好きな順序で戻す。まじめにやると組合せ爆発しそう...
                }
            }
}

//デッキをオープンし条件に合致したカードをすべて手札に加え、残ったカードはデッキの下に
class eサーチAll(val nOpen: Int, val cond: (Card) -> Boolean) : Effect {
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
    override fun use(tr: History): Sequence<History> = sequenceOf(tr)
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
    override fun use(tr: History): Sequence<History> = sequenceOf(tr)
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

class 神世界トリスタ : SpiritCard(Category.SPIRITCARD, "神トリ", Color.Y, 5, Sbl.Y, Sbl.Y * 3, setOf(Family.道化, Family.界渡), Lv11) {
    override fun use(tr: History): Sequence<History> = sequenceOf(tr)
            .effect(e召喚配置(this))  // このカードを召喚する
            //以下召喚時効果
            .optional { //～できる
                effect(eサーチBy_デッキ下(5) {
                    stn.ownSide.pickedCards.cards.asSequence().filter { it.family.contains(Family.創界神) || it.family.contains(Family.界渡) }.flatMap {
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

class 転校生 : MagicCard(Category.MAGICCARD, "転校生", Color.G, 4, Sbl.G + Sbl.R) {
    override fun use(h: History): ParallelWorld = sequenceOf(h)
            .effect(eマジック使用(this))
            .effect(eサーチBy_デッキ下(4) {
                val gw = stn.ownSide.pickedCards.cards.filter { it.family == Family.ウル && it.family == Family.創界神 }
                if (gw.count() == 0) {
                    sequenceOf(this) //GWなければなにもしない
                } else {
                    gw.asSequence().flatMap {
                        sequenceOf(this).effect(e召喚配置_NoCost(it)) //あればどれか一枚をコストを支払わず配置
                    }
                }
            })
}

class チヒロ : NexusCard(Category.NEXUSCARD, "チヒロ", Color.W, 4, Sbl.Gd, Sbl.W + Sbl.Gd, setOf(Family.創界神, Family.ウル), Lv10_24) {
    override fun use(tr: History): Sequence<History> = sequenceOf(tr)
            .effect(e召喚配置(this))
}

class ダミーカード : SpiritCard(Category.SPIRITCARD, "Dmy", Color.Y, 12, Sbl.R, Sbl.R * 0, setOf(Family.界渡), Lv11) {
    override fun use(tr: History): Sequence<History> = sequenceOf()
//            .filter_ownSide { hand.cards.contains(this@ダミーカード) } //このカードが手札にあったら
//            .flatMap_ownSide { opMoveCard(CARDTRASH, this@ダミーカード) } //トラッシュに置かれる(計算量削減のため)
}


fun main() {
    val deckBottom = DeckBottom()

    val deck1 = setOf(
            電人トレイン(), 電人トレイン(), 電人トレイン()
            , 幼グランロロ(), 幼グランロロ(), 幼グランロロ()
            , サルベージ(), サルベージ(), サルベージ()
            , 転校生(), 転校生(), 転校生()
            , 神世界トリスタ(), 神世界トリスタ(), 神世界トリスタ()

            , チヒロ(), チヒロ(), チヒロ()
            , ダミーカード(), ダミーカード(), ダミーカード()
            , ダミーカード(), ダミーカード(), ダミーカード()
            , ダミーカード(), ダミーカード(), ダミーカード()
            , ダミーカード(), ダミーカード(), ダミーカード()
            , ダミーカード(), ダミーカード(), ダミーカード()
            , ダミーカード(), ダミーカード(), ダミーカード()
    ).shuffled(Random(23445)).toSet() + deckBottom


    // ロードラ用評価関数
    fun Side.evaluation(): Int = deckDepth(deckBottom)        // デッキ進度
    fun ParallelWorld.cutBranches(): ParallelWorld = distinct().sortedBy { it.stn.ownSide.evaluation() }.take(5)


    eden(deck1, enemyDeck = setOf())
            .map_ownSide { mutation { reserve.core = Core(4, 0) } }.pln { "${stn.ownSide} : ${stn.ownSide.deckDepth(deckBottom)} " }
            .effect(eDrawStep(4))

            .effect(eCoreStep(1)).effect(eDrawStep(1)).effect(eRefreshStep()).effect(eMainStep()).cutBranches()
            .effect(eCoreStep(1)).effect(eDrawStep(1)).effect(eRefreshStep()).effect(eMainStep()).cutBranches()
            .effect(eCoreStep(1)).effect(eDrawStep(1)).effect(eRefreshStep()).effect(eMainStep()).cutBranches()
            .effect(eCoreStep(1)).effect(eDrawStep(1)).effect(eRefreshStep()).effect(eMainStep()).cutBranches()

            .cutBranches()
            .pln { "${stn.ownSide} : ${stn.ownSide.deckDepth(deckBottom)} " }
            .count().pln()

}


