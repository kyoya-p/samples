package bstools.model

import kotlin.native.concurrent.ThreadLocal
import kotlin.random.Random

/**
 * 選択肢の評価値(勝率ヒューリスティック)を決めるすべての「重み」をここに集約する。
 *
 * これまで Rules.kt に直接書かれていたマジックナンバー（コスト係数・自壊ペナルティ・
 * アタック/ブロックの評価値など）を1箇所にまとめることで、AutoTuner (Tuner.kt) が
 * 自己対戦の勝率を見ながら自動的に調整できるようにする。
 *
 * フィールドを増やす場合は [names] と [get] / [with] にも追加すること。
 */
data class EvalWeights(
    /** 召喚・配置・マジック使用の基本評価値 */
    val summonBase: Double = 0.55,
    /** カードコスト1につき加算する係数 (高コストカードを少し高く評価する) */
    val summonCostFactor: Double = 0.05,
    /** 最低Lvより高いLvで召喚したとき、Lv1あたりに加算するボーナス */
    val summonLevelBonus: Double = 0.02,
    /** 配置コアにソウルコアを使ったときのペナルティ */
    val summonSoulPenalty: Double = 0.03,
    /** リザーブではなく自分のフィールドのコアを使ったときのペナルティ */
    val summonFieldUsePenalty: Double = 0.04,
    /** 自壊(コアを抜いてLv1未満にする)1体につき差し引くペナルティ。召喚・コア移動の両方で使う */
    val vanishPenalty: Double = 0.30,
    /** コア移動でLvが上がるときの評価値 */
    val coreMoveRaise: Double = 0.55,
    /** コア移動でLvが変わらないとき・ソウル/通常コアを交換するときの評価値 */
    val coreMoveFlat: Double = 0.40,
    /** アタック宣言の評価値 */
    val attackEval: Double = 0.70,
    /** ブロックしてBPで勝つときの評価値 */
    val blockWinEval: Double = 0.80,
    /** ブロックして相打ちになるときの評価値 */
    val blockTieEval: Double = 0.45,
    /** ブロックしてBPで負けるときの評価値 */
    val blockLoseEval: Double = 0.30,
    /** ブロックせずライフでアタックを受けるときの評価値 */
    val passBlockEval: Double = 0.35,
    /** 「ステップ終了」自体の基準評価値。他の手がこれを上回らない限りステップを進める */
    val stepEndEval: Double = 0.45,

    // --- ここから、公開情報(相手フィールド・両者のライフ)を評価値に反映させる重み ---
    /** アタック宣言時、相手に回復状態のブロッカーが1体もいない(公開領域=相手フィールドで判定できる)ときの加点 */
    val attackNoBlockerBonus: Double = 0.10,
    /** アタック宣言時、自分のBPが相手の最強ブロッカーのBPを上回る量(1000あたり)に応じた加点。下回れば減点になる */
    val attackBpEdgeFactor: Double = 0.05,
    /** このアタックが通れば相手のライフを0にできる(致死)とき、大きく加点する */
    val attackLethalBonus: Double = 0.25,
    /** ブロックしない場合、失うライフが自分の残りライフに占める割合に応じて差し引くペナルティ係数 */
    val passBlockLifeFactor: Double = 0.40,
    /** ブロックしない場合、それが致死(自分のライフが0になる)なら追加で差し引く大きなペナルティ */
    val passBlockLethalPenalty: Double = 0.90,
    /** ブロックしなければ致死という状況で、ブロックする側に追加する加点(BPで負ける手でも致死回避を優先させる) */
    val blockLethalPreventionBonus: Double = 0.35
) {
    companion object {
        /** チューニング対象フィールドの名前一覧 (mutate/toMap で使う) */
        val names: List<String> = listOf(
            "summonBase", "summonCostFactor", "summonLevelBonus", "summonSoulPenalty",
            "summonFieldUsePenalty", "vanishPenalty", "coreMoveRaise", "coreMoveFlat",
            "attackEval", "blockWinEval", "blockTieEval", "blockLoseEval",
            "passBlockEval", "stepEndEval",
            "attackNoBlockerBonus", "attackBpEdgeFactor", "attackLethalBonus",
            "passBlockLifeFactor", "passBlockLethalPenalty", "blockLethalPreventionBonus"
        )

        fun fromMap(m: Map<String, Double>): EvalWeights {
            val d = EvalWeights()
            return d.copy(
                summonBase = m["summonBase"] ?: d.summonBase,
                summonCostFactor = m["summonCostFactor"] ?: d.summonCostFactor,
                summonLevelBonus = m["summonLevelBonus"] ?: d.summonLevelBonus,
                summonSoulPenalty = m["summonSoulPenalty"] ?: d.summonSoulPenalty,
                summonFieldUsePenalty = m["summonFieldUsePenalty"] ?: d.summonFieldUsePenalty,
                vanishPenalty = m["vanishPenalty"] ?: d.vanishPenalty,
                coreMoveRaise = m["coreMoveRaise"] ?: d.coreMoveRaise,
                coreMoveFlat = m["coreMoveFlat"] ?: d.coreMoveFlat,
                attackEval = m["attackEval"] ?: d.attackEval,
                blockWinEval = m["blockWinEval"] ?: d.blockWinEval,
                blockTieEval = m["blockTieEval"] ?: d.blockTieEval,
                blockLoseEval = m["blockLoseEval"] ?: d.blockLoseEval,
                passBlockEval = m["passBlockEval"] ?: d.passBlockEval,
                stepEndEval = m["stepEndEval"] ?: d.stepEndEval,
                attackNoBlockerBonus = m["attackNoBlockerBonus"] ?: d.attackNoBlockerBonus,
                attackBpEdgeFactor = m["attackBpEdgeFactor"] ?: d.attackBpEdgeFactor,
                attackLethalBonus = m["attackLethalBonus"] ?: d.attackLethalBonus,
                passBlockLifeFactor = m["passBlockLifeFactor"] ?: d.passBlockLifeFactor,
                passBlockLethalPenalty = m["passBlockLethalPenalty"] ?: d.passBlockLethalPenalty,
                blockLethalPreventionBonus = m["blockLethalPreventionBonus"] ?: d.blockLethalPreventionBonus
            )
        }
    }

    fun get(name: String): Double = when (name) {
        "summonBase" -> summonBase
        "summonCostFactor" -> summonCostFactor
        "summonLevelBonus" -> summonLevelBonus
        "summonSoulPenalty" -> summonSoulPenalty
        "summonFieldUsePenalty" -> summonFieldUsePenalty
        "vanishPenalty" -> vanishPenalty
        "coreMoveRaise" -> coreMoveRaise
        "coreMoveFlat" -> coreMoveFlat
        "attackEval" -> attackEval
        "blockWinEval" -> blockWinEval
        "blockTieEval" -> blockTieEval
        "blockLoseEval" -> blockLoseEval
        "passBlockEval" -> passBlockEval
        "stepEndEval" -> stepEndEval
        "attackNoBlockerBonus" -> attackNoBlockerBonus
        "attackBpEdgeFactor" -> attackBpEdgeFactor
        "attackLethalBonus" -> attackLethalBonus
        "passBlockLifeFactor" -> passBlockLifeFactor
        "passBlockLethalPenalty" -> passBlockLethalPenalty
        "blockLethalPreventionBonus" -> blockLethalPreventionBonus
        else -> error("unknown weight: $name")
    }

    fun with(name: String, value: Double): EvalWeights = when (name) {
        "summonBase" -> copy(summonBase = value)
        "summonCostFactor" -> copy(summonCostFactor = value)
        "summonLevelBonus" -> copy(summonLevelBonus = value)
        "summonSoulPenalty" -> copy(summonSoulPenalty = value)
        "summonFieldUsePenalty" -> copy(summonFieldUsePenalty = value)
        "vanishPenalty" -> copy(vanishPenalty = value)
        "coreMoveRaise" -> copy(coreMoveRaise = value)
        "coreMoveFlat" -> copy(coreMoveFlat = value)
        "attackEval" -> copy(attackEval = value)
        "blockWinEval" -> copy(blockWinEval = value)
        "blockTieEval" -> copy(blockTieEval = value)
        "blockLoseEval" -> copy(blockLoseEval = value)
        "passBlockEval" -> copy(passBlockEval = value)
        "stepEndEval" -> copy(stepEndEval = value)
        "attackNoBlockerBonus" -> copy(attackNoBlockerBonus = value)
        "attackBpEdgeFactor" -> copy(attackBpEdgeFactor = value)
        "attackLethalBonus" -> copy(attackLethalBonus = value)
        "passBlockLifeFactor" -> copy(passBlockLifeFactor = value)
        "passBlockLethalPenalty" -> copy(passBlockLethalPenalty = value)
        "blockLethalPreventionBonus" -> copy(blockLethalPreventionBonus = value)
        else -> error("unknown weight: $name")
    }

    /** 1つのフィールドだけをランダムに選び、±sigma の範囲で摂動した近傍解を返す (山登り法の近傍生成) */
    fun mutate(rng: Random, sigma: Double = 0.05): EvalWeights {
        val name = names[rng.nextInt(names.size)]
        val delta = (rng.nextDouble() * 2.0 - 1.0) * sigma
        val newValue = (get(name) + delta).coerceIn(-1.0, 1.5)
        return with(name, newValue)
    }

    /** コピー&ペーストしてそのまま `EvalWeights(...)` として使える形式で出力する */
    fun toKotlinLiteral(): String =
        "EvalWeights(\n" + names.joinToString(",\n") { "    $it = ${get(it)}" } + "\n)"
}

/**
 * 現在アクティブな重み。GameServer と Playmats はこれを参照して評価値を計算する。
 * `Tuner` が自己対戦の結果に基づいて書き換える対象でもある。
 *
 * `@ThreadLocal`: Tuner の自己対戦をスレッド並列化すると、対戦ごとに異なるプレイヤーの
 * 重みへ切り替える処理(`activeWeights = weights`)が同時に複数スレッドから走る。
 * プロセス共有のグローバル変数のままだと他スレッドの対戦を破壊してしまうため、
 * スレッドごとに独立したコピーを持たせる。単一スレッドの本番実行(GameServer/Playmats)には
 * 影響しない(スレッドが1つしかないため従来と同じ挙動)。
 */
@ThreadLocal
var activeWeights: EvalWeights = EvalWeights()
