package BSSim.ロードラ

import BSSim.*

val LvNone = listOf<Card.LevelInfo>()
val Lv11 = listOf(Card.LevelInfo(1, 1, 0))
val Lv10_24 = listOf(Card.LevelInfo(1, 0, 0), Card.LevelInfo(2, 4, 0))

class 電人トレイン : SpiritCard(Category.SPIRITCARD, "電トレ", Color.W, 3, Sbl.W, Sbl.W * 1, setOf(Family.武装, Family.界渡), Lv11) {
    override fun effect(tr: History): Sequence<History> = sequenceOf(tr)
            .choices(op召喚配置(this))  // このカードを召喚する
            .flatMap_ownSide { deck.cards.top(4).flatMap { t4 -> opMoveCards(PICKEDCARD, t4) } }//4枚オープンしpickedに置く
            .flatMap_ownSide { opPayCost(1) }//1コスト支払うことで
            .flatMap_ownSide {
                pickedCards.cards.asSequence().filter { it.familiy.contains(Family.創界神) && it.colors == Color.W }.flatMap {   //pickedが白の創界神の場合
                    opMoveCard(HAND, it) //手札に加える
                }
            }
}


/*デッキ定義*/

class 幼グランロロ : SpiritCard(Category.SPIRITCARD, "幼グラ", Color.RPGWYB, 3, Sbl.W, Sbl.W * 1, setOf(Family.武装, Family.界渡), Lv11)
class 神世界トリスタ : SpiritCard(Category.SPIRITCARD, "神トリ", Color.Y, 5, Sbl.Y, Sbl.Y * 3, setOf(Family.道化, Family.界渡), Lv11)
class サルベージ : SummonnableCard(Category.MAGICCARD, "サルベ", Color.B, 4, Sbl.Zero, Sbl.B * 2, setOf(), LvNone)
class 転校生 : SummonnableCard(Category.MAGICCARD, "転校生", Color.G, 4, Sbl.Zero, Sbl.G + Sbl.R, setOf(), LvNone)
class チヒロ : NexusCard(Category.NEXUSCARD, "チヒロ", Color.W, 4, Sbl.Gd, Sbl.W + Sbl.Gd, setOf(Family.創界神, Family.ウル), Lv10_24)

val deck_ロードラ1 = listOf(
        電人トレイン()
        , チヒロ()
        , チヒロ()
        , 幼グランロロ()
        , 神世界トリスタ()
        , サルベージ()
        , 転校生()
)

fun main() {
}


//fun Sequence<Transition>.action(act: Effectable): Sequence<Transition> = act.effect(this)