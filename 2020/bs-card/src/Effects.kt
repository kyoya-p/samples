package BSSim


// 効果
interface Maneuver {
    val efName: String
    fun use(h: History): ParallelWorld
}

class NoOp : Maneuver {
    override val efName = "noOp"

    override fun use(h: History): ParallelWorld = sequenceOf(h)
}

// 効果を発揮するもの
// カードやトラッシュ等
abstract class Effectable(val effectName: String) {
    abstract fun effect(tr: History): Sequence<History>
}

fun <T : Effectable> Sequence<History>.choices(effectable: T): Sequence<History> = flatMap { effectable.effect(it) }

open class opMoveCore(val dst: FO, val srcs: List<FO>, val pick: Int) : Effectable("コア移動") {
    override fun effect(p: History): Sequence<History> = sequenceOf(p).flatMap_ownSide {
        opMoveCore(dst, srcs, pick)
    }
}


open class ePayCost(val pick: Int) : Maneuver {
    override val efName = "コスト支払い"
    override fun use(p: History): Sequence<History> = sequenceOf(p).flatMap_ownSide {
        opPayCost(pick)
    }
}

open class ePayCardCost(val c: Card) : Maneuver {
    override val efName = "コスト支払い"
    override fun use(p: History): Sequence<History> = sequenceOf(p).flatMap_ownSide {
        opPayCost(c)
    }
}


// 指定のFOを消滅させる
class op消滅(val fo: FO) : Effectable("消滅") {
    override fun effect(p: History): Sequence<History> = sequenceOf(p).flatMap_ownSide {
        opDestruct(fo)
    }
}

class e消滅(val fo: FO) : Maneuver {
    override val efName = "消滅"
    override fun use(p: History): Sequence<History> = sequenceOf(p).flatMap_ownSide {
        opDestruct(fo)
    }
}

// FOをチェックし維持コア不足のFOを消滅させる
class e消滅チェック : Maneuver {
    override val efName: String = "消滅チェック"

    override fun use(tr: History): Sequence<History> = sequenceOf(tr).flatMap_ownSide {
        val exts = fieldObjects.filter { fo -> attr(fo).core.c < attr(fo).cards[0].lvInfo[0].keepCore } //維持コア未満のカードのリストを用意し
        destroy(exts, tr).map { it.world.ownSide } //すべて消滅
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


