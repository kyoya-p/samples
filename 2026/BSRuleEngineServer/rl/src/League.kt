package bstools.valuerl

import kotlin.math.pow

/**
 * 歴代の最良モデルを保持する対戦リーグと Elo レーティング。
 *
 * 「直前の最良にだけ勝てた」が強さの循環 (じゃんけん) でも起こりうるため、
 * 現行モデルをリーグの全メンバーと先後交互で対戦させ、メンバーの Elo を固定したまま
 * 現行モデルの Elo を最尤推定する。基準はランダム方策 = Elo 0 (常駐、除外されない)。
 *
 * メンバーの Elo は加入時点の推定値で固定する (後から動かさない) ため、
 * 反復をまたいで値を比較できる。
 */
class LeagueMember(val name: String, val agent: Agent, val elo: Double)

class LeagueResult(val elo: Double, val rows: List<Pair<LeagueMember, ArenaStats>>)

class League(private val gamesPerOpponent: Int, private val maxSize: Int, private val game: GameConfig) {
    val members = mutableListOf(LeagueMember("random", RandomAgent(), 0.0))

    fun rate(agent: Agent, seed: Long): LeagueResult {
        val rows = members.mapIndexed { i, m -> m to arena(agent, m.agent, gamesPerOpponent, seed + i * 7919L, game) }
        return LeagueResult(estimateElo(rows.map { (m, st) -> Triple(m.elo, st.aWins + 0.5 * st.draws, st.games) }), rows)
    }

    /** 加入。上限を超えたらランダム方策以外で最も古いメンバーを外す */
    fun add(m: LeagueMember) {
        members.add(m)
        if (members.size > maxSize) members.removeAt(1)
    }

    companion object {
        fun expected(r: Double, opp: Double) = 1.0 / (1.0 + 10.0.pow((opp - r) / 400.0))

        /**
         * 相手の Elo を固定した最尤推定 (Σ(スコア - 期待スコア) = 0 を二分法で解く)。
         * 全勝・全敗で発散しないよう、相手ごとに「0.5勝の仮想1局」を加える。
         */
        fun estimateElo(results: List<Triple<Double, Double, Int>>): Double {
            fun f(r: Double) = results.sumOf { (opp, score, n) -> (score + 0.5) - (n + 1) * expected(r, opp) }
            var lo = -4000.0
            var hi = 6000.0
            repeat(100) {
                val mid = (lo + hi) / 2
                if (f(mid) > 0) lo = mid else hi = mid
            }
            return (lo + hi) / 2
        }
    }
}
