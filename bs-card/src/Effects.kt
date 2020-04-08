package  BSSim

typealias Effect = Transition.() -> Sequence<Transition>

abstract class Effectable(val effectName: String) {
    abstract fun effect(tr: Transition): Sequence<Transition>

    companion object {
        val opNone = object : Effectable("noOp") {
            override fun effect(tr: Transition): Sequence<Transition> = sequenceOf()
        }
    }
}

fun <T : Effectable> Sequence<Transition>.action(effectable: T): Sequence<Transition> = flatMap { effectable.effect(it) }

// 全BSOを操作
open class opTouchBSOs(val op: (BSO) -> BSO) : Effectable("BSO操作") {
    override fun effect(tr: Transition): Sequence<Transition> =
            sequenceOf(tr).sqSituation_map {
                trOwnSide {
                    replaceBSOsBy { bso -> op(bso) }
                }.trEnemySide {
                    replaceBSOsBy { bso -> op(bso) }
                }
            }
}

// ownSideにBSOを新規作成
open class opCreateBSO(val op: () -> BSO) : Effectable("BSO生成") {
    override fun effect(tr: Transition): Sequence<Transition> =
            sequenceOf(tr).sqSituation_map {
                trOwnSide { tr(bsos = bsObjects + op()) }
            }
}

// -----------------------------------------

open class opPickCore(val pick: Int) : Effectable("コア抽出") {
    override fun effect(p: Transition): Sequence<Transition> {
        val stn = p.stn
        return stn.ownSide.payableCoreHolder.pickCore(pick).map { (pk, coreHolders) ->
            val postSide = stn.ownSide
                    .replacePayableCores(coreHolders)
                    .replaceFOCore(Side_XXX.PICKEDCORE) { c -> c + pk }
            stn.tr(ownSide = postSide)
        }.map { Transition(p.prevStn, it) }
    }
}

// カードの使用のためのコスト支払い
// コストをフィールドから減らし、トラッシュに置く。カードをPickedに置く
// 消滅したFOはトラッシュに置く
class opコスト(val card: Card) : Effectable("コスト") {

    override fun effect(tr: Transition): Sequence<Transition> {
        val reqCost = min(0, card.cost - tr.stn.ownSide.fieldSimbols.reduction(card.reduction))  // 使用コスト算出
        return sequenceOf(tr)
                .sqOwnSide_flatMap { opPayCost(reqCost) }
                .action(op消滅チェック())
    }

    private fun min(a: Int, b: Int) = if (a > b) b else a

}

// 指定のFOを消滅させる
class op消滅(val fo: BSO) : Effectable("消滅") {
    override fun effect(tr: Transition): Sequence<Transition> = sequenceOf(tr)
            .sqOwnSide_flatMap {
                replaceFoBy { it.filter { it !== fo } }.opMoveCards(fo.cards, Side_XXX.CARDTRASH) // FOを消滅させ、カードをトラッシュに置く
            }.sqOwnSide_flatMap {
                opMoveCore(fo, bsObjects[Side_XXX.RESERVE]) // FOのコアをリザーブに置く
            }
}

// FOをチェックし維持コア不足のFOを消滅させる
class op消滅チェック : Effectable("消滅チェック") {
    override fun effect(tr: Transition): Sequence<Transition> {
        val exts = tr.stn.ownSide.field.filter { bso -> bso.core.c < bso.cards[0].lvInfo[0].keepCore } //維持コア未満のカードのリストを用意し
        return extinction(exts, tr)
    }

    //リストされたFOをすべて消滅させる
    fun extinction(bsos: List<BSO>, tr: Transition): Sequence<Transition> = sequence {
        if (bsos.size != 0) {
            sequenceOf(tr).action(op消滅(bsos[0])).forEach {
                extinction(bsos.drop(1), it).forEach {
                    yield(it)
                }
            }
        } else {
            yield(tr)
        }
    }
}

// カードの使用のためのコスト支払い
// 支払いコストはcastCalcにて算出
// コストをフィールドから減らし、トラッシュに置く。カードをPickedに置く
// 消滅したFOはトラッシュに置く
class opCostedBy_XXX(val op: (Transition) -> Int) : Effectable("Costed") {
    override fun effect(tr: Transition): Sequence<Transition> = sequenceOf(tr)
            .action(opPickCore(op(tr)))
            .action(op消滅チェック())
}

// スピリット/ブレイヴの召喚、ネクサスの配置
//
// コストをフィールドから選択しトラッシュに置く
// 維持コストをフィールドから選択しFOに置く
class op配置(val card: Card) : Effectable("召喚配置") {
    override fun effect(p: Transition): Sequence<Transition> = sequenceOf(p)
            .action(opCostedBy_XXX { card.cost - it.stn.ownSide.fieldSimbols.reduction(card.reduction) }) // コストを集めてpickedに置く
            .sqOwnSide_flatMap { opMoveCore(Side_XXX.PICKEDCORE, Side_XXX.CORETRASH) } // pickedコアをトラッシュに置く
            //.action(opCostedBy_XXX { 1 }) //維持コストを集めてpickedに置く
            .sqOwnSide_map { pln("pick=") }
            .sqOwnSide_map { moveCardsBy(pickedCards, listOf(card)) { it + card } } // (手札の)カードをpickedに置く
            .sqSituation_map {
                this.pln("preSummon=")
                        //.createFO(card) // 指定カードからFOを作る [TODO]
                        //.pickCoreTo(card, 1)
            }
    //.sqOwnSide_flatMap { opMoveCore(1) } //維持コストを集めてFOに置く
}

class opメイン : Effectable("opメイン") {
    override fun effect(tr: Transition): Sequence<Transition> = tr.stn.ownSide.handCards.asSequence().flatMap {
        sequenceOf(tr).action(it)
    }

//    override val effects0: List<Effect> get() = TODO("Not yet implemented")
}


// 現在のSituationで取りえるTransition(操作)を列挙
// (例: メインステップなら手札の召喚可能カードを召喚)
fun listChoice(tr: Transition): Sequence<Effectable> = sequence {
    //何もしない選択肢
    //yield(Effectable.opNone)
    // 全カード/FieldObject/Fieldが有する Effectを実行し、取り得る選択肢を列挙
    val prevStn = tr.stn.ownSide.handCards.forEach { ca -> //手札の効果(召喚など)をチェック
        yield(ca)
    }
}

// 効果の発揮に伴う状態遷移/派生効果も含め解決
fun tr(tr: Transition): Sequence<Transition> = sequence {
    yield(tr) //何もしない選択肢
    val choise = listChoice(tr)
    choise.forEach {
        //it.pln("ACTION:")
        sequenceOf(tr).action(it).forEach {
            tr(it)
                    .distinctBy { it.hashCode() }
                    .forEach {
                        //it.pln()
                        yield(it)
                    }
        }
    }
}

