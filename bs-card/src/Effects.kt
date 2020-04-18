package  BSSim


// 効果
interface Effect {
    val efName: String
    fun use(h: History): ParallelWorld
}

// 効果を発揮するもの
// カードやトラッシュ等
abstract class Effectable(val effectName: String) {
    abstract fun effect(tr: History): Sequence<History>

    companion object {
        val opNone = object : Effectable("noOp") {
            override fun effect(tr: History): Sequence<History> = sequenceOf()
        }
    }
}

fun <T : Effectable> Sequence<History>.choices(effectable: T): Sequence<History> = flatMap { effectable.effect(it) }

open class opMoveCore(val dst: FO, val srcs: List<FO>, val pick: Int) : Effectable("コア移動") {
    override fun effect(p: History): Sequence<History> = sequenceOf(p).flatMap_ownSide {
        opMoveCore(dst, srcs, pick)
    }
}

open class opPayCost(val pick: Int) : Effectable("コスト支払") {
    override fun effect(p: History): Sequence<History> = sequenceOf(p).flatMap_ownSide {
        opMoveCore(CORETRASH, payableCoreHolders, pick)
    }
}

// 指定のFOを消滅させる
class op消滅(val fo: FO) : Effectable("消滅") {
    override fun effect(p: History): Sequence<History> = sequenceOf(p).flatMap_ownSide {
        opDestruct(fo)
    }
}

class e消滅(val fo: FO) : Effect {
    override val efName = "消滅"
    override fun use(p: History): Sequence<History> = sequenceOf(p).flatMap_ownSide {
        opDestruct(fo)
    }
}

// FOをチェックし維持コア不足のFOを消滅させる
class e消滅チェック : Effect {
    override val efName: String = "消滅チェック"

    override fun use(tr: History): Sequence<History> = sequenceOf(tr).flatMap_ownSide {
        val exts = fieldObjects.filter { fo -> attr(fo).core.c < attr(fo).cards[0].lvInfo[0].keepCore } //維持コア未満のカードのリストを用意し
        destroy(exts, tr).map { it.stn.ownSide } //すべて消滅
    }

    //リストされたFOをすべて消滅させる TODO:順番が大切
    fun destroy(bsos: List<FO>, tr: History): Sequence<History> = sequence {
        if (bsos.size != 0) {
            sequenceOf(tr).choices(op消滅(bsos[0])).forEach {
                destroy(bsos.drop(1), it).forEach {
                    yield(it)
                }
            }
        } else {
            yield(tr)
        }
    }
}


// FOをチェックし維持コア不足のFOを消滅させる
class op消滅チェック : Effectable("消滅チェック") {
    override fun effect(tr: History): Sequence<History> = sequenceOf(tr).flatMap_ownSide {
        val exts = fieldObjects.filter { fo -> attr(fo).core.c < attr(fo).cards[0].lvInfo[0].keepCore } //維持コア未満のカードのリストを用意し
        destroy(exts, tr).map { it.stn.ownSide } //すべて消滅
    }

    //リストされたFOをすべて消滅させる TODO:順番が大切
    fun destroy(bsos: List<FO>, tr: History): Sequence<History> = sequence {
        if (bsos.size != 0) {
            sequenceOf(tr).choices(op消滅(bsos[0])).forEach {
                destroy(bsos.drop(1), it).forEach {
                    yield(it)
                }
            }
        } else {
            yield(tr)
        }
    }
}

// 現在のSituationで取りえるTransition(操作)を列挙
// (例: メインステップなら手札の召喚可能カードを召喚)
fun listChoice(tr: History): Sequence<Effectable> = sequence {
    // 全カード/FieldObject/Fieldが有する Effectを実行し、取り得る選択肢を列挙
    val prevStn = tr.stn.ownSide.hand.cards.forEach { ca -> //手札の効果(召喚など)をチェック
        yield(ca)
    }
}

// 効果の発揮に伴う状態遷移/派生効果も含め解決
fun tr(tr: History): Sequence<History> = sequence {
    yield(tr) //何もしない選択肢
    val choise = listChoice(tr)
    choise.forEach {
        //it.pln("ACTION:")
        sequenceOf(tr).choices(it).forEach {
            tr(it)
                    .distinctBy { it.hashCode() }
                    .forEach {
                        //it.pln()
                        yield(it)
                    }
        }
    }
}

