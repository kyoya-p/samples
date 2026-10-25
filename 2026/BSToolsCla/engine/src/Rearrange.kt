package bstools.model

/**
 * コアの一括再配置: メインステップのコア移動・交換を「1個ずつ動かす手の連続」ではなく、
 * **最終的なコア配置を1手で選ぶ**選択肢として列挙する。
 *
 * コアを何手動かしても結果は最終配置だけで決まり、途中の順番はその後に影響しない。
 * そのため、プレイとプレイの間のコア移動の連続はすべて、配置の選択1手に置き換えられる (選べる結果は変わらない)。
 *
 * 配置の数: 場所 N か所 (リザーブ + 自分のフィールドの全カード) に、通常コア X−1 個と
 * ソウルコア1個を0個以上置く組み合わせ = N × C(X+N−2, N−1) 通り (ソウルコアがなければ C(X+N−2, N−1))。
 * 現在の配置を除いたものを列挙する。
 *
 * - コアが Lv1 のLvコストを下回るスピリットは消滅する (個別の移動と同じ。detail に明記する)。
 *   ネクサスは0個でも消滅しない
 * - 再配置の直後 ([GameState.coreWindow] に "ALL") は列挙しない。2回続けても1回で同じ配置にできるため、
 *   選べる結果は減らない。これでコアを動かし続ける循環が構造上なくなる
 * - 適用は従来の MOVE_CORE と同じ形式 (現在の配置との差分を移動の組に分解する)。
 *   applyAction は全部の移動元から取り出してから移動先に置き、その後に消滅を判定する
 */
fun enumerateRearrangements(state: GameState): List<GameAction> {
    if (state.winner != null || (state.step != Step.MAIN && state.step != Step.MAIN_2) || "ALL" in state.coreWindow) {
        return emptyList()
    }
    val p = state.choosingPlayer
    val places = listOf(Place("Reserve", "R", p.reserve.normal, p.reserve.soul, null)) +
        p.field.map { Place(it.instanceId, it.displayName, it.cores.normal, it.cores.soul, it) }
    val n = places.size
    if (n < 2) return emptyList()
    val totalNormal = places.sumOf { it.normal }
    val soulAt = places.indexOfFirst { it.soul > 0 }
    val soulChoices = if (soulAt >= 0) (0 until n).toList() else listOf(-1)

    val out = ArrayList<GameAction>()
    val dist = IntArray(n)
    fun emit() {
        for (s in soulChoices) {
            if (s == soulAt && places.indices.all { dist[it] == places[it].normal }) continue // 現在の配置
            out.add(rearrangeAction(places, dist, soulAt, s))
        }
    }
    fun rec(i: Int, remaining: Int) {
        if (i == n - 1) { dist[i] = remaining; emit(); return }
        for (k in remaining downTo 0) { dist[i] = k; rec(i + 1, remaining - k) }
    }
    rec(0, totalNormal)
    return markRepetitions(state, out)
}

/** 再配置の対象になる場所 (リザーブ / フィールドのカード) と現在のコア */
private class Place(val id: String, val label: String, val normal: Int, val soul: Int, val card: FieldCard?)

/** 現在の配置から目標の配置 (通常コア [dist]、ソウルコアの場所 [soulTo]) への再配置の手を作る */
private fun rearrangeAction(places: List<Place>, dist: IntArray, soulFrom: Int, soulTo: Int): GameAction {
    val moves = ArrayList<CoreMove>()
    if (soulFrom >= 0 && soulTo != soulFrom) {
        moves.add(CoreMove(places[soulFrom].id, places[soulTo].id, places[soulFrom].label, places[soulTo].label, normal = 0, soul = 1))
    }
    // 通常コアは差分だけ動かす (減る場所 → 増える場所 を順に対応させる)
    val givers = ArrayDeque<Pair<Int, Int>>()
    val takers = ArrayDeque<Pair<Int, Int>>()
    for (i in places.indices) {
        val d = dist[i] - places[i].normal
        if (d < 0) givers.addLast(i to -d) else if (d > 0) takers.addLast(i to d)
    }
    while (givers.isNotEmpty() && takers.isNotEmpty()) {
        val (g, gn) = givers.removeFirst()
        val (t, tn) = takers.removeFirst()
        val k = minOf(gn, tn)
        moves.add(CoreMove(places[g].id, places[t].id, places[g].label, places[t].label, normal = k, soul = 0))
        if (gn > k) givers.addFirst(g to gn - k)
        if (tn > k) takers.addFirst(t to tn - k)
    }

    val vanishing = ArrayList<String>()
    val target = places.mapIndexed { i, pl ->
        val soul = if (i == soulTo) 1 else 0
        val card = pl.card
        val text = if (card == null) "${pl.label}:${coreIcons(dist[i], soul).ifEmpty { "0" }}" else {
            val total = dist[i] + soul
            val keep = if (card.category == CardCategory.NEXUS) 0 else maxOf(1, card.lvCosts.firstOrNull() ?: 1)
            if (total < keep) vanishing.add(pl.label)
            val lv = if (total < keep) "消滅" else "Lv${levelFor(card.lvCosts, total, soul, card.shinkai)}"
            "${pl.label}:${coreIcons(dist[i], soul).ifEmpty { "0" }}($lv)"
        }
        text to CoreAlloc(pl.id, pl.label, dist[i], soul)
    }
    val vanishNote = if (vanishing.isEmpty()) "" else " ⚠️${vanishing.joinToString("・")}消滅"
    return GameAction(
        index = 0,
        type = "MOVE_CORE",
        category = "🔷【コア再配置】",
        detail = target.joinToString(" / ") { it.first } + vanishNote,
        moves = moves,
        targetConfig = target.map { it.second },
        rearrangeKey = "ALL"
    )
}
