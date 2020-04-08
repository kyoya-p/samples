package ロードラ

import BSSim.*
import BSSim.Side_XXX.Companion.DECK
import BSSim.Side_XXX.Companion.HAND

val LvNone = listOf<Card.LevelInfo>()
val Lv11 = listOf(Card.LevelInfo(1, 1, 0))
val Lv10_24 = listOf(Card.LevelInfo(1, 0, 0), Card.LevelInfo(2, 4, 0))

class 電人トレイン : SpiritCard(Category.SPIRITCARD, "電トレ", Color.W, 3, Sbl.W, Sbl.W * 1, setOf(Family.武装, Family.界渡), Lv11) {
    override fun effect(tr: Transition): Sequence<Transition> = sequenceOf(tr)
            .action(op配置(this))  // このカードを召喚し
            .sqOwnSide_flatMap { opPickDeck(4) } //4枚オープンしpickedに置く
            .sqOwnSide_flatMap { opPayCost(1) }//1コスト支払うことで
            .sqOwnSide_flatMap {
                sequence<Side_XXX> {
                    picked_Cards.forEach {// pickedカードの一枚が
                        if (it.familiy.contains(Family.創界神) && it.colors == Color.W) { //白の創界神の場合
                            opMoveCard(it, Side_XXX.HAND).forEach { yield(it) }  //手札に加える
                        }
                    }
                }
            }
}

class 幼グランロロ : SpiritCard(Category.SPIRITCARD, "幼グラ", Color.RPGWYB, 3, Sbl.W, Sbl.W * 1, setOf(Family.武装, Family.界渡), Lv11)
class 神世界トリスタ : SpiritCard(Category.SPIRITCARD, "神トリ", Color.Y, 5, Sbl.Y, Sbl.Y * 3, setOf(Family.道化, Family.界渡), Lv11)
class サルベージ : SummonnableCard(Category.MAGICCARD, "サルベ", Color.B, 4, Sbl.Zero, Sbl.B * 2, setOf(), LvNone)
class 転校生 : SummonnableCard(Category.MAGICCARD, "転校生", Color.G, 4, Sbl.Zero, Sbl.G + Sbl.R, setOf(), LvNone)
class チヒロ : NexusCard(Category.NEXUSCARD, "チヒロ", Color.W, 4, Sbl.Gd, Sbl.W + Sbl.Gd, setOf(Family.創界神, Family.ウル), Lv10_24)

// サーチ効果
// nOpen枚デッキを開き、条件selectorのカードを手札に、残りはデッキボトムに
class opサーチorボトムBy(val nOpen: Int, val selector: (Cards) -> Sequence<Cards>) : Effectable("サーチ") {
//    override val effects0: List<Effect> get() = TODO("Not yet implemented")

    override fun effect(tr: Transition): Sequence<Transition> = sequence {
        tr.stn.ownSide.opPickDeck(nOpen).forEach { s ->  // n枚オープンしpickedに置く
            selector(s.picked_Cards).forEach { selected -> //pickedに置いたカードから選択したカードを
                s.opMoveCards(selected, Side_XXX.HAND) //手札に移動し
                        .opMoveCardsBy(Side_XXX.PICKEDCARD, Side_XXX.DECK) { src, dst -> sequenceOf(listOf<Card>() to dst + src) }  //残りをデッキの下に移動し
                        .forEach { yield(Transition(tr.stn, tr.stn.tr(ownSide = it))) } //その結果のパターンを返す
            }
        }
    }
}

/*デッキ定義*/

val deck_ロードラ1 = (
        電人トレイン() * 1
                + チヒロ() * 1
                + チヒロ() * 1
                + 幼グランロロ() * 1
                + 神世界トリスタ() * 1
                + サルベージ() * 1
                + 転校生() * 1
        )

fun test() {
    println("---------------")
    val mySide = Side_XXX(deck_ロードラ1).opMoveCardsBy(DECK, HAND) { deck, hand ->
        sequenceOf(deck.drop(1) to deck.take(1))
    }.take1Case()
    val board = Situation(ownSide = mySide, enemySide = Side_XXX())

    print("Hand=" + board.ownSide.handCards)
    println(" Deck=" + board.ownSide.deckCards)

    sequenceOf(Transition(board, board)).action(opメイン()).forEach {
        it.pln("main:")
        //print("Hand=" + it.stn.ownSide.hand)
        //println(" Deck=" + it.stn.ownSide.deck)
    }
}


//fun Sequence<Transition>.action(act: Effectable): Sequence<Transition> = act.effect(this)