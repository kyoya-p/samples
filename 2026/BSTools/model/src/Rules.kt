package bstools.model

import kotlinx.serialization.Serializable

/**
 * バトルスピリッツ スタンダード 総合ルール Ver.1.0 に基づく共通ルール判定。
 *
 * GameServer (選択肢列挙) と Playmats (WebUI) の双方がここを参照することで、
 * 同じ局面に対して両者が別々のルールを適用してしまうのを防ぐ。
 */

/** 初期手札の枚数 (総合ルール 6-2-1-5: 各プレイヤーはデッキからカードを4枚ドローする) */
const val INITIAL_HAND_SIZE = 4

/** 自分のフィールドにあるシンボルを色別に集計する。Lv0のスピリットはシンボルを持たない (総合ルール 2-10-3) */
fun fieldSymbolCounts(p: PlayerState): MutableMap<CardColor, Int> {
    val counts = mutableMapOf<CardColor, Int>()
    for (fc in p.field) {
        for (sym in fc.symbols) counts[sym] = (counts[sym] ?: 0) + 1
    }
    return counts
}

/**
 * コスト軽減量を求める (総合ルール 2-3-3 / 11-1-1-4-1)。
 * 「軽減シンボルの数まで、自分のフィールドに対応するシンボルがあればコストを軽減する」
 * = 印刷された軽減シンボル1つを、同じ色のシンボル1つが打ち消す多重集合マッチ。
 * 同じシンボルを2つの軽減シンボルに二重計上してはならない。
 */
fun reductionFor(card: CardData, p: PlayerState): Int {
    val available = fieldSymbolCounts(p)
    var reduced = 0
    for (req in card.reductionSymbols) {
        val n = available[req] ?: 0
        if (n > 0) {
            available[req] = n - 1
            reduced++
        }
    }
    return reduced
}

/** 軽減後の召喚コスト。軽減可能な場合は必ず軽減する (総合ルール 2-3-3: 過剰な支払いは不可) */
fun summonCost(card: CardData, p: PlayerState): Int = maxOf(0, card.cost - reductionFor(card, p))

/** 召喚時にカード上へ置く必要があるコア数 = 最低LvのLvコスト (総合ルール 11-1-1-6) */
fun requiredCoresOnSummon(card: CardData): Int {
    if (card.category == CardCategory.NEXUS) return 0
    return maxOf(1, card.lvCosts.firstOrNull() ?: 1)
}

/**
 * 召喚・配置時にカード上へ置けるコア数の候補 (総合ルール 11-1-1-6)。
 * 「最低LvのLvコスト**以上**」なので、最初からLv2・Lv3で召喚することもできる。
 * 最低値しか列挙しないと、この合法手が選べなくなる。
 *
 * 以前はLvのしきい値(次のLvに届く枚数)だけを候補にしていたが、Lvの境目をまたがない枚数
 * (単にコアをリザーブから退避させる目的など)も総合ルール上「以上」で許される合法な手であり、
 * 選べないのはおかしいという指摘を受け、支払い分を除いて実際に賄える枚数まで**すべての枚数**
 * を候補にする (例: Lvコスト[1]のカードでもコアが余っていれば2個・3個…と置く手を選べる)。
 */
fun placementOptions(card: CardData, cost: Int, sources: List<CoreSource>): List<Int> {
    if (card.category == CardCategory.MAGIC) return listOf(0)
    val minimum = requiredCoresOnSummon(card)
    val totalAvailable = sources.sumOf { it.availNormal + it.availSoul }
    val maxPlace = totalAvailable - cost
    if (maxPlace < minimum) return emptyList()
    return (minimum..maxPlace).toList()
}

/** アタック宣言できるスピリットがいるか (回復状態・Lv1以上) */
fun hasAttackableSpirit(p: PlayerState): Boolean =
    p.field.any { !it.isExhausted && it.level > 0 && it.category == CardCategory.SPIRIT }

/**
 * 先攻プレイヤーの最初のターンか。
 * このターンにはコアステップ・アタックステップ・第2メインステップが存在しない
 * (総合ルール 7-3-3 / 7-7-2 / 7-8-2)。ドローステップには例外がなく、通常どおりドローする。
 */
fun isFirstTurn(state: GameState): Boolean = state.turn == 1

/** どの供給元から何個コアを取るか。sourceId は "Reserve" または FieldCard.instanceId */
@Serializable
data class CoreAlloc(
    val sourceId: String,
    val label: String,
    val normal: Int = 0,
    val soul: Int = 0
)

/** コア移動1件。交換は「逆向きの移動2件」として表現する */
@Serializable
data class CoreMove(
    val fromId: String,
    val toId: String,
    val fromLabel: String,
    val toLabel: String,
    val normal: Int = 0,
    val soul: Int = 0
)

/**
 * 供給元1件の在庫。
 * cap はそこから取り出せるコア数の上限、keep は消滅せずに残す必要があるコア数 (Lv1のLvコスト)。
 */
data class CoreSource(
    val id: String,
    val label: String,
    val availNormal: Int,
    val availSoul: Int,
    val cap: Int,
    val keep: Int
)

/**
 * 召喚コストの支払い・カード上への配置に使えるコアの供給元 (総合ルール 11-1-1-5 / 11-1-1-6)。
 * 「フィールドまたはリザーブ」のコアが使えるため、リザーブに加えて自分のフィールドのカードも供給元になる。
 * ライフのコアは使えない。
 *
 * フィールドのカードからはコアを全部抜くこともできる。Lv1のLvコストを下回ったスピリットは
 * 消滅する (3-2-5-4) が、それ自体は合法な手であり「コアをすべて取り除いて消滅させながら召喚」は
 * 公式Q&Aでも前提とされている。自壊する手は列挙したうえで評価値を下げる。
 * ネクサスはLv1のLvコストが0なので、コアを全部抜いても消滅しない (3-3-3-3)。
 */
fun coreSources(p: PlayerState): List<CoreSource> {
    val sources = mutableListOf(
        CoreSource("Reserve", "R", p.reserve.normal, p.reserve.soul, p.reserve.total, keep = 0)
    )
    for (fc in p.field) {
        if (fc.cores.total <= 0) continue
        sources.add(
            CoreSource(
                id = fc.instanceId,
                label = fc.name,
                availNormal = fc.cores.normal,
                availSoul = fc.cores.soul,
                cap = fc.cores.total,
                keep = if (fc.category == CardCategory.NEXUS) 0 else maxOf(1, fc.lvCosts.firstOrNull() ?: 1)
            )
        )
    }
    return sources
}

/**
 * 列挙されたアクション1件。
 * WebUI は index だけをサーバーへ返すため、同じ局面を再列挙して index を引き直せば
 * 選択された手をそのまま復元できる。GameServer / Playmats はこの型をそのまま受け渡す。
 */
@Serializable
data class GameAction(
    val index: Int,
    val type: String,
    val category: String,
    val detail: String,
    val eval: Double,
    val handIndex: Int = -1,
    val targetInstanceId: String? = null,
    val costToPay: Int = 0,
    /** トラッシュへ移すコアの供給元 */
    val payFrom: List<CoreAlloc> = emptyList(),
    /** 召喚したカードの上に置くコアの供給元 */
    val placeFrom: List<CoreAlloc> = emptyList(),
    /** MOVE_CORE の移動内容。交換の場合は逆向きの2件が入る */
    val moves: List<CoreMove> = emptyList(),
    /** 選べない選択肢。UIには理由付きで表示する (現状このフラグを立てる箇所はない。将来の真に不可な手のために予約) */
    val forbidden: Boolean = false,
    val forbiddenReason: String? = null,
    /**
     * この手を選ぶと既訪問の局面に戻ることを示す。
     * ルール上は合法な手であり選択は妨げない (`forbidden` にはしない) が、盤面は進まないため
     * 評価値は0にし、UIでは必ず「同一局面」であることを明記する。
     */
    val repeatsPosition: Boolean = false
)

/**
 * 局面のハッシュ。ステップ・ターン・手番に加え、両プレイヤーの見えている情報をすべて含める。
 *
 * これで「その手を指すと以前と同じ局面に戻るか」を判定できる。
 * コアを A➔B と B➔A に動かし続けるような、盤面が進まない循環を選択肢から除くために使う。
 * 手札とフィールドは並び順に意味がないので、正規化してから畳み込む。
 */
fun positionHash(state: GameState): Long {
    val sb = StringBuilder()
    sb.append(state.turn).append('|')
        .append(state.activePlayerId).append('|')
        .append(state.step.name).append('|')
        .append(state.format.name).append('|')
        .append(state.winner ?: -1).append('|')
        // ブロック宣言中はどのスピリットがアタックしているかで局面が違う
        .append(state.attackingCardId ?: "-").append('|')
    for (p in listOf(state.player1, state.player2)) {
        sb.append(p.playerId).append(':')
            .append(p.life).append(',')
            .append(p.reserve.normal).append('/').append(p.reserve.soul).append(',')
            .append(p.trash.normal).append('/').append(p.trash.soul).append(',')
            // deck そのものは列挙に不要なのでGameServerへ送る局面からは外される。
            // ハッシュが送信側と受信側で一致するよう、実体ではなく deckCount を使う
            .append(p.deckCount).append(',')
        sb.append(p.hand.map { it.cardNo }.sorted().joinToString("-")).append(',')
        sb.append(
            p.field.map { "${it.cardNo}:${it.cores.normal}/${it.cores.soul}:${if (it.isExhausted) 1 else 0}" }
                .sorted().joinToString("-")
        ).append('|')
    }
    // FNV-1a 64bit
    var hash = -3750763034362895579L
    for (ch in sb) {
        hash = hash xor ch.code.toLong()
        hash *= 1099511628211L
    }
    return hash
}

/** 手を試すための局面コピー。デッキは applyAction で変化しないので共有してよい */
fun GameState.snapshot(): GameState = GameState(
    turn = turn,
    activePlayerId = activePlayerId,
    step = step,
    player1 = player1.snapshot(),
    player2 = player2.snapshot(),
    attackingCardId = attackingCardId,
    blockingCardId = blockingCardId,
    winner = winner,
    format = format,
    seed = seed,
    visitedPositions = visitedPositions
)

private fun PlayerState.snapshot(): PlayerState = PlayerState(
    playerId = playerId,
    name = name,
    life = life,
    reserve = Cores(reserve.normal, reserve.soul),
    trash = Cores(trash.normal, trash.soul),
    deckCount = deckCount,
    hand = hand.toMutableList(),
    deck = deck,
    field = field.map {
        it.copy(cores = Cores(it.cores.normal, it.cores.soul))
    }.toMutableList(),
    trashCards = trashCards.toMutableList()
)

/** 各供給元から通常コアを何個取るかの組み合わせを列挙する (リザーブを多く使う順) */
private fun distributeNormalCores(
    sources: List<CoreSource>,
    need: Int,
    soulSourceId: String?,
    limit: Int
): List<List<Int>> {
    val results = mutableListOf<List<Int>>()

    fun walk(i: Int, remaining: Int, acc: MutableList<Int>) {
        if (results.size >= limit) return
        if (i == sources.size) {
            if (remaining == 0) results.add(acc.toList())
            return
        }
        val s = sources[i]
        // 同じ供給元からソウルコアも取る場合、その分だけ取り出せる上限が減る
        val soulTaken = if (soulSourceId == s.id) 1 else 0
        val maxHere = minOf(s.availNormal, s.cap - soulTaken, remaining)
        for (take in maxHere downTo 0) {
            acc.add(take)
            walk(i + 1, remaining - take, acc)
            acc.removeAt(acc.size - 1)
            if (results.size >= limit) return
        }
    }

    walk(0, need, mutableListOf())
    return results
}

/**
 * 1枚のカードについて、コアの供給元の組み合わせごとにプレイの選択肢を作る。
 * スピリット/ネクサスの召喚・配置 (place >= 0) とマジックの使用 (place = 0) を共通で扱う。
 */
private fun playOptions(
    card: CardData,
    handIdx: Int,
    category: String,
    actionType: String,
    cost: Int,
    place: Int,
    sources: List<CoreSource>,
    limit: Int = 50
): List<GameAction> {
    val options = mutableListOf<GameAction>()

    // ソウルコアは各プレイヤー1個だけ。未使用 / 配置に使う / 支払いに使う の3通りを考える。
    // ソウルコアを「どの供給元から取るか」も選択肢になる (リザーブ上でもスピリット上でもよい)。
    data class SoulUse(val sourceId: String?, val placed: Boolean)

    val soulUses = mutableListOf(SoulUse(null, false))
    for (s in sources) {
        if (s.availSoul <= 0) continue
        if (place > 0) soulUses.add(SoulUse(s.id, placed = true))
        if (cost > 0) soulUses.add(SoulUse(s.id, placed = false))
    }

    for (soul in soulUses) {
        val soulUsed = if (soul.sourceId != null) 1 else 0
        val normalNeeded = cost + place - soulUsed
        if (normalNeeded < 0) continue

        val placedSoul = if (soul.placed) 1 else 0
        val placedNormal = place - placedSoul

        for (dist in distributeNormalCores(sources, normalNeeded, soul.sourceId, limit - options.size)) {
            // 通常コアはどれも同じなので、どの供給元の分を「配置」に回しても結果の盤面は変わらない。
            // 表示のためリザーブに近い順から配置分を割り当てる。
            var remainingPlace = placedNormal
            val payFrom = mutableListOf<CoreAlloc>()
            val placeFrom = mutableListOf<CoreAlloc>()
            val vanishing = mutableListOf<String>()

            for ((i, s) in sources.withIndex()) {
                val takenNormal = dist[i]
                val takenSoul = if (soul.sourceId == s.id) 1 else 0

                val toPlace = minOf(takenNormal, remainingPlace)
                remainingPlace -= toPlace
                val toPay = takenNormal - toPlace

                val placeSoulHere = if (takenSoul > 0 && soul.placed) 1 else 0
                val paySoulHere = takenSoul - placeSoulHere

                if (toPay > 0 || paySoulHere > 0) {
                    payFrom.add(CoreAlloc(s.id, s.label, toPay, paySoulHere))
                }
                if (toPlace > 0 || placeSoulHere > 0) {
                    placeFrom.add(CoreAlloc(s.id, s.label, toPlace, placeSoulHere))
                }

                // コアを抜いた結果 Lv1 を維持できなくなるカードは消滅する (総合ルール 3-2-5-4)
                if (s.keep > 0 && s.cap - (takenNormal + takenSoul) < s.keep) {
                    vanishing.add(s.label)
                }
            }

            // コストは常に通常コアだけで払われるとは限らない (ソウルコアを支払いに回す組み合わせもある)。
            // 実際にこの選択肢が支払いに使う内訳 (payFrom の合計) を表示しないと、
            // 「配置コア」欄にソウルコアが出てこないパターンでソウルコアの使用がどこにも表示されなくなる。
            val paySoulTotal = payFrom.sumOf { it.soul }
            val payNormalTotal = payFrom.sumOf { it.normal }
            val costLabel = if (cost == 0) "コスト不要" else "コスト${coreIcons(payNormalTotal, paySoulTotal)}"
            val placeLabel = when {
                actionType == "USE_MAGIC" -> "コアは置かない"
                place == 0 -> "配置コアなし"
                else -> "配置コア${coreIcons(place - placedSoul, placedSoul)}"
            }
            val vanishLabel = if (vanishing.isEmpty()) "" else " ⚠️${vanishing.joinToString("・")}消滅"
            // WebUI はこの括弧内を解析して、クリックで選んだコアと選択肢を突き合わせる。
            // 消滅の注記は必ず括弧の外に置くこと (末尾の括弧が解析対象のため)
            val payStr = payFrom.joinToString(", ") { "${it.label}:${formatAlloc(it)}" }
            val placeStr = placeFrom.joinToString(", ") { "${it.label}:${formatAlloc(it)}" }
            val sourceStr = "(支払:${payStr.ifEmpty { "なし" }}, 配置:${placeStr.ifEmpty { "なし" }})"

            // リザーブだけで賄える手を優先し、自分のカードを消滅させる手は大きく評価を下げる
            val usesField = (payFrom + placeFrom).any { it.sourceId != "Reserve" }
            // 高いLvで召喚できるならその分わずかに高く評価する
            val summonedLevel = levelFor(card.lvCosts, place, placedSoul)
            val w = activeWeights
            val evalScore = w.summonBase + (card.cost * w.summonCostFactor) +
                    (w.summonLevelBonus * maxOf(0, summonedLevel - 1)) -
                    (if (placedSoul > 0) w.summonSoulPenalty else 0.0) -
                    (if (usesField) w.summonFieldUsePenalty else 0.0) -
                    (w.vanishPenalty * vanishing.size)

            options.add(
                GameAction(
                    index = 0,
                    type = actionType,
                    category = category,
                    detail = "$costLabel, ${placeLabel}${vanishLabel}, $sourceStr",
                    eval = evalScore,
                    handIndex = handIdx,
                    costToPay = cost,
                    payFrom = payFrom,
                    placeFrom = placeFrom
                )
            )
            if (options.size >= limit) return options
        }
    }
    return options
}

/**
 * コア数をアイコンで表記する。盤面と同じ 🔷(通常コア) / 🔶(ソウルコア) を使い、
 * 数字を読まなくても必要量が一目で分かるようにする。
 * WebUI の `parseDetailSources` もこのアイコン表記をそのまま数える。
 */
fun coreIcons(normal: Int, soul: Int): String =
    "🔷".repeat(maxOf(0, normal)) + "🔶".repeat(maxOf(0, soul))

/** 移動するコアの内訳 */
private fun describeCores(normal: Int, soul: Int): String = coreIcons(normal, soul)

/**
 * メインステップのコア移動を列挙する。
 * 自分のリザーブとフィールドのカードの間でコアを移動できる。1度に2個以上まとめて移動でき、
 * ソウルコアと通常コアを入れ替える（交換する）こともできる。
 * コアが減ってLv1のLvコストを下回るスピリットは消滅するため、その手は評価値を下げる。
 */
private fun coreMoveOptions(p: PlayerState, sources: List<CoreSource>, limit: Int = 120): List<GameAction> {
    val options = mutableListOf<GameAction>()
    val destinations = listOf("Reserve") + p.field.map { it.instanceId }
    fun labelOf(id: String) = if (id == "Reserve") "R" else p.field.first { it.instanceId == id }.name

    // 移動先のLvが上がるかどうか。上がらない移動を「ステップ終了」より高く評価すると、
    // 評価値だけを見て指す側が A➔B ➔ B➔A を延々と繰り返して局面が進まなくなる。
    fun raisesLevel(dstId: String, addNormal: Int, addSoul: Int): Boolean {
        val fc = p.field.find { it.instanceId == dstId } ?: return false
        return levelFor(fc.lvCosts, fc.cores.total + addNormal + addSoul, fc.cores.soul + addSoul) > fc.level
    }

    for (src in sources) {
        for (dstId in destinations) {
            if (dstId == src.id) continue
            val dstLabel = labelOf(dstId)

            // 1個以上まとめて移動する
            for (count in 1..src.cap) {
                for (soul in 0..minOf(1, src.availSoul)) {
                    val normal = count - soul
                    if (normal < 0 || normal > src.availNormal) continue
                    val vanishes = src.keep > 0 && src.cap - count < src.keep
                    val gain = if (raisesLevel(dstId, normal, soul)) activeWeights.coreMoveRaise else activeWeights.coreMoveFlat
                    options.add(
                        GameAction(
                            index = 0,
                            type = "MOVE_CORE",
                            category = "🔷【コア移動】 ${src.label} ➔ $dstLabel",
                            detail = describeCores(normal, soul) + "を移動" +
                                    (if (vanishes) " ⚠️${src.label}消滅" else ""),
                            eval = gain - (if (vanishes) activeWeights.vanishPenalty else 0.0),
                            targetInstanceId = dstId,
                            moves = listOf(CoreMove(src.id, dstId, src.label, dstLabel, normal, soul))
                        )
                    )
                    if (options.size >= limit) return options
                }
            }

            // ソウルコアと通常コアの交換。総数が変わらないのでLvは基本維持されるが、
            // 《真界放》のカードにソウルコアを渡す場合だけはLvが上がる
            val dst = sources.find { it.id == dstId }
            if (src.availSoul > 0 && dst != null && dst.availNormal > 0) {
                options.add(
                    GameAction(
                        index = 0,
                        type = "MOVE_CORE",
                        category = "🔄【コア交換】 ${src.label} ⇄ $dstLabel",
                        detail = "🔶 ⇄ 🔷 を交換",
                        eval = if (raisesLevel(dstId, -1, 1)) activeWeights.coreMoveRaise else activeWeights.coreMoveFlat,
                        targetInstanceId = dstId,
                        moves = listOf(
                            CoreMove(src.id, dstId, src.label, dstLabel, normal = 0, soul = 1),
                            CoreMove(dstId, src.id, dstLabel, src.label, normal = 1, soul = 0)
                        )
                    )
                )
                if (options.size >= limit) return options
            }
        }
    }
    return options
}

/** 供給元から取るコアをアイコンで表す。WebUI 側の parseDetailSources がこの表記を解釈する */
private fun formatAlloc(alloc: CoreAlloc): String = coreIcons(alloc.normal, alloc.soul)

/**
 * その手を指した結果が既出の局面になるものに「同一局面」の印を付ける。
 *
 * コアを A➔B / B➔A と往復させるような手は、どちらもLvが上がるため評価値では抑えられない。
 * ただしこれ自体は総合ルール上禁止された手ではないため、**選択は妨げない**。
 * AIの自動選択（評価値が最大の手を選ぶ）が無限に往復し続けないよう eval を下げ、
 * 人間のプレイヤーには「選んでも局面は進まない」ことが分かるよう `repeatsPosition` で明示する。
 *
 * 下げる先を `0.0`ではなく`-1.0`にしている。線形モデル(EvalWeights)ではステップ終了の基準値が
 * 固定の正の定数(既定0.45)なので0.0でも確実に負けたが、RNN評価器(evaluateActionWithRnn)は
 * 未学習だと出力が任意の負の値を取りうるため、`0.0`がステップ終了の評価値を上回ってしまい
 * 「同一局面へ戻る手を選び続けて300手ずっと停止する」無限ループが実際に発生した。
 * `-1.0`(tanh/評価値レンジの理論上の下限)にすることで、どちらの評価器を使っても
 * 常に他のどの選択肢よりも低い値になることを保証する。
 */
private fun markRepetitions(state: GameState, actions: List<GameAction>): List<GameAction> {
    if (state.visitedPositions.isEmpty()) return actions
    return actions.map { action ->
        // ステップ進行は必ず局面を進めるので確認不要
        if (action.type == "END_STEP") return@map action
        val trial = state.snapshot()
        applyAction(trial, action, mutableListOf())
        if (positionHash(trial) in state.visitedPositions) {
            action.copy(eval = -1.0, repeatsPosition = true)
        } else {
            action
        }
    }
}

/**
 * 局面で選択可能なアクションをすべて列挙する (バトスピ局面選択肢列挙エンジンの本体)。
 * 召喚コストの支払いと配置コアは、リザーブと自分のフィールドのカード上のコアから選べる (総合ルール 11-1-1-5 / 11-1-1-6)。
 * 同じ局面に戻るだけの手にも印(`repeatsPosition`)を付けるが、選択は妨げない (`markRepetitions` 参照)。
 *
 * `evalMode == EvalMode.RNN` のときは、各選択肢の `eval` を RnnEvaluator の
 * 局面価値関数 V(state) による1手先読み評価で上書きする (RnnEvaluator.kt 参照)。
 * 既定は `EvalMode.LINEAR` なので、本番の Playmats/GameServer の挙動はこれまでと変わらない。
 */
fun enumerateActions(state: GameState): List<GameAction> {
    syncEvalForTurn(state)
    val actions = markRepetitions(state, enumerateRawActions(state))
    val params = rnnParams
    if (evalMode == EvalMode.RNN && params != null) {
        return actions.map { a ->
            // 同一局面(repeatsPosition)は eval=0 のまま維持する。それ以外を RNN 評価で置き換える
            if (a.repeatsPosition) a else a.copy(eval = evaluateActionWithRnn(state, a, params))
        }
    }
    val kann = kannNet
    if (evalMode == EvalMode.KANN && kann != null) {
        return actions.map { a ->
            if (a.repeatsPosition) a else a.copy(eval = evaluateActionWithKann(state, a, kann))
        }
    }
    return actions
}

private fun enumerateRawActions(state: GameState): List<GameAction> {
    val acts = mutableListOf<GameAction>()
    if (state.winner != null) return acts

    val p = state.choosingPlayer
    var idx = 1

    if (state.step == Step.MAIN || state.step == Step.MAIN_2) {
        val prefix = if (state.step == Step.MAIN_2) "[第2メイン] " else ""

        val sources = coreSources(p)

        // 同名カードが手札に複数あってもどれをプレイしても同じ結果になるため、カード種別ごとに1度だけ列挙する
        val distinctHand = p.hand.indices.distinctBy { p.hand[it].cardNo }
        for (handIdx in distinctHand) {
            val card = p.hand[handIdx]
            val cost = summonCost(card, p)

            // スピリットは「召喚」、ネクサスは「配置」、マジックは「使用」。
            // マジックはフィールドに置かれずコアも乗らない (総合ルール 11-3-1-6)
            val actionType = if (card.category == CardCategory.MAGIC) "USE_MAGIC" else "SUMMON"
            val category = when (card.category) {
                CardCategory.MAGIC -> "✨【${prefix}マジック使用】 ${card.name}"
                CardCategory.NEXUS -> "🌐【${prefix}配置】 ${card.name}"
                else -> "🎴【${prefix}召喚】 ${card.name}"
            }
            for (place in placementOptions(card, cost, sources)) {
                for (action in playOptions(card, handIdx, category, actionType, cost, place, sources)) {
                    acts.add(action.copy(index = idx++))
                }
            }
        }

        for (action in coreMoveOptions(p, sources)) {
            acts.add(action.copy(index = idx++))
        }

    }

    if (state.step == Step.ATTACK_DECLARATION) {
        // アタックの評価には相手フィールド・相手ライフという「公開情報」を用いる。
        // 相手の手札・デッキの中身のような非公開情報は参照しない。
        val opponent = state.notChoosingPlayer
        val availableBlockers = opponent.field.filter {
            !it.isExhausted && it.level > 0 && it.category == CardCategory.SPIRIT
        }
        val bestBlockerBp = availableBlockers.maxOfOrNull { it.currentBp }

        for (fc in p.field) {
            if (fc.isExhausted || fc.level <= 0 || fc.category != CardCategory.SPIRIT) continue
            val symbolCount = maxOf(1, fc.symbols.size)
            val w = activeWeights

            var eval = w.attackEval
            eval += if (bestBlockerBp == null) {
                // 相手に回復状態のブロッカーが1体もいない = 確実にダメージが通る
                w.attackNoBlockerBonus
            } else {
                // 自分のBPが相手の最強ブロッカーをどれだけ上回るか(下回れば減点)
                w.attackBpEdgeFactor * ((fc.currentBp - bestBlockerBp) / 1000.0)
            }
            if (symbolCount >= opponent.life) {
                // このアタックが通れば相手のライフが0になる(致死)
                eval += w.attackLethalBonus
            }

            acts.add(
                GameAction(
                    index = idx++,
                    type = "ATTACK",
                    category = "🗡️【アタック】 ${fc.name}",
                    detail = "${fc.name} でアタック宣言 (Lv${fc.level}, BP${fc.currentBp}, 通ればライフ-${coreIcons(symbolCount, 0)})",
                    eval = eval,
                    targetInstanceId = fc.instanceId
                )
            )
        }
    }

    // ブロック宣言はアタックされている側が行う (総合ルール 8-1-3-1)。
    // 回復状態のスピリット1体だけがブロックでき、ブロックしない選択もできる。
    if (state.step == Step.BLOCK_DECLARATION) {
        val attacker = state.notChoosingPlayer.field.find { it.instanceId == state.attackingCardId }
        val attackerBp = attacker?.currentBp ?: 0
        val lifeLoss = maxOf(1, attacker?.symbols?.size ?: 1)
        val w = activeWeights
        // 「ブロックしなければ致死か」は自分のライフ(常に自分から見える情報)だけで判定できる
        val wouldBeLethal = lifeLoss >= p.life

        for (fc in p.field) {
            if (fc.isExhausted || fc.level <= 0 || fc.category != CardCategory.SPIRIT) continue
            // BPが低い方が破壊される。同じなら相打ち (総合ルール 8-1-6-1)
            val outcome = when {
                fc.currentBp > attackerBp -> "${attacker?.name ?: "相手"}を破壊"
                fc.currentBp < attackerBp -> "${fc.name}が破壊される"
                else -> "相打ち"
            }
            var eval = if (fc.currentBp > attackerBp) w.blockWinEval
            else if (fc.currentBp == attackerBp) w.blockTieEval
            else w.blockLoseEval
            // 致死を防げるなら、BPで負ける手であっても大きく加点する
            if (wouldBeLethal) eval += w.blockLethalPreventionBonus

            acts.add(
                GameAction(
                    index = idx++,
                    type = "BLOCK",
                    category = "🛡️【ブロック】 ${fc.name}",
                    detail = "${fc.name} でブロック (BP${fc.currentBp} vs BP${attackerBp} → $outcome)",
                    eval = eval,
                    targetInstanceId = fc.instanceId
                )
            )
        }

        // ブロックしない場合の損失は、自分の残りライフに占める割合が大きいほど、
        // また致死になるならなおさら悪い手だと評価する
        var passEval = w.passBlockEval - (w.passBlockLifeFactor * (lifeLoss.toDouble() / maxOf(1, p.life)))
        if (wouldBeLethal) passEval -= w.passBlockLethalPenalty

        acts.add(
            GameAction(
                index = idx++,
                type = "PASS_BLOCK",
                category = "💔【ブロックしない】",
                detail = "ライフでアタックを受ける (ライフ-${coreIcons(lifeLoss, 0)})",
                eval = passEval
            )
        )
    }

    return acts
}
/** ターン開始時の非対話ステップ (スタート→コア→ドロー→リフレッシュ) をまとめて処理する */
fun beginTurn(state: GameState, messages: MutableList<String>) {
    val p = state.activePlayer

    // スタートステップ: デッキ0枚なら敗北 (総合ルール 7-2-2 / 1-3-2-2)
    state.step = Step.START
    if (p.deck.isEmpty()) {
        state.winner = state.opponentPlayer.playerId
        messages.add("🏁 スタートステップ: プレイヤー${p.playerId} のデッキが0枚のため敗北")
        return
    }

    // コアステップ: ボイドからリザーブへコア1個 (先攻1ターン目は存在しない / 総合ルール 7-3-2, 7-3-3)
    if (!isFirstTurn(state)) {
        state.step = Step.CORE
        p.reserve.add(1)
        messages.add("🔷 コアステップ: ボイドからリザーブへコア1個")
    }

    // ドローステップ: 1枚ドロー。先攻1ターン目もドローする (総合ルール 7-4-2 に例外規定なし)
    state.step = Step.DRAW
    val drawn = p.deck.removeAt(0)
    p.hand.add(drawn)
    p.deckCount = p.deck.size
    messages.add("🃏 ドローステップ: 「${drawn.name}」をドロー (残り${p.deck.size}枚)")

    // リフレッシュステップ: トラッシュのコアをすべてリザーブへ戻し、自分のカードをすべて回復 (総合ルール 7-5-2)
    state.step = Step.REFRESH
    if (p.trash.total > 0) {
        messages.add("♻️ リフレッシュステップ: トラッシュのコア${p.trash.total}個をリザーブへ")
        p.reserve.add(p.trash.normal, p.trash.soul)
        p.trash = Cores(0, 0)
    }
    val recovered = p.field.count { it.isExhausted }
    if (recovered > 0) {
        p.field.forEach { it.isExhausted = false }
        messages.add("♻️ リフレッシュステップ: ${recovered}体を回復")
    }

    state.step = Step.MAIN
}

/** ステップ進行 (総合ルール 7-1-1: スタンダードは8ステップ制) */
fun advanceStep(state: GameState, messages: MutableList<String>) {
    when (state.step) {
        // 先攻1ターン目はアタックステップが存在しないため直接エンドへ
        // (スタンダードでは第2メインステップも同様に存在しない)
        Step.MAIN -> state.step = if (isFirstTurn(state)) Step.END else Step.ATTACK_DECLARATION
        // エターナルにはアタックステップの後の第2メインステップがない
        Step.ATTACK_DECLARATION -> state.step = if (state.format.hasSecondMain) Step.MAIN_2 else Step.END
        Step.MAIN_2 -> state.step = Step.END
        Step.END -> {
            state.turn++
            state.activePlayerId = if (state.activePlayerId == 1) 2 else 1
            messages.add("【ターン ${state.turn}】 手番: プレイヤー${state.activePlayerId}")
            beginTurn(state, messages)
        }
        else -> state.step = Step.MAIN
    }
}

/** 移動したコアの内訳を表示用に整形する */
fun describeMovedCores(normal: Int, soul: Int): String {
    val parts = mutableListOf<String>()
    if (soul > 0) parts.add("ソウルコア${soul}個")
    if (normal > 0) parts.add("通常コア${normal}個")
    return parts.joinToString("+")
}

/** 指定された供給元 (リザーブ / 自分のフィールドのカード) からコアを取り除く。足りなければ false */
fun takeCores(p: PlayerState, allocs: List<CoreAlloc>): Boolean {
    for (alloc in allocs) {
        if (alloc.sourceId == "Reserve") {
            if (p.reserve.normal < alloc.normal || p.reserve.soul < alloc.soul) return false
            p.reserve.sub(alloc.normal, alloc.soul)
        } else {
            val fc = p.field.find { it.instanceId == alloc.sourceId } ?: return false
            if (fc.cores.normal < alloc.normal || fc.cores.soul < alloc.soul) return false
            fc.cores.sub(alloc.normal, alloc.soul)
        }
    }
    return true
}

/**
 * Lvコストを満たさなくなったカードを消滅させる (総合ルール 3-2-5-4)。
 * カードはトラッシュへ、上に残っていたコアはリザーブへ移る
 * (「スピリットが破壊されたとき、そのスピリット上のコアはリザーブではなくトラッシュに置かれる」という
 *  例外効果が存在することから、既定の行き先はリザーブ)。
 */
fun resolveVanish(p: PlayerState, messages: MutableList<String>) {
    val vanished = p.field.filter { it.level <= 0 }
    for (fc in vanished) {
        if (fc.cores.total > 0) {
            p.reserve.add(fc.cores.normal, fc.cores.soul)
            fc.cores = Cores(0, 0)
        }
        p.field.remove(fc)
        p.trashCards.add(CardData(fc.cardNo, fc.name, 0, fc.colors, category = fc.category))
        messages.add("💥 「${fc.name}」はコアがLv1未満になり消滅")
    }
}

/**
 * 選択肢を適用し、到達した局面を記録する。
 * 記録があるおかげで、次の列挙で「同じ局面に戻るだけの手」を検出し `repeatsPosition` を付けられる
 * (選択自体は妨げない。`markRepetitions` 参照)。
 */
fun applyActionAndRecord(state: GameState, action: GameAction, messages: MutableList<String>) {
    if (action.forbidden) {
        messages.add("⚠️ ${action.forbiddenReason ?: "選択できません"}")
        return
    }
    if (state.visitedPositions.isEmpty()) state.visitedPositions.add(positionHash(state))
    applyAction(state, action, messages)
    state.visitedPositions.add(positionHash(state))
}

/**
 * スピリットを破壊してトラッシュへ送る。
 * 上に置かれていたコアは持ち主のリザーブへ移る
 * (「リザーブではなくトラッシュに置かれる」という例外効果があることから、既定の行き先はリザーブ)。
 */
fun destroySpirit(owner: PlayerState, fc: FieldCard, messages: MutableList<String>) {
    if (fc.cores.total > 0) {
        owner.reserve.add(fc.cores.normal, fc.cores.soul)
        fc.cores = Cores(0, 0)
    }
    owner.field.remove(fc)
    owner.trashCards.add(CardData(fc.cardNo, fc.name, 0, fc.colors, category = fc.category))
    messages.add("💥 「${fc.name}」が破壊された (コアはリザーブへ)")
}

/** 選択されたアクションを局面に適用する */
fun applyAction(state: GameState, action: GameAction, messages: MutableList<String>) {
    val p = state.choosingPlayer

    when (action.type) {
        "SUMMON" -> {
            if (action.handIndex < 0 || action.handIndex >= p.hand.size) return
            val card = p.hand[action.handIndex]

            if (!takeCores(p, action.payFrom) || !takeCores(p, action.placeFrom)) {
                messages.add("⚠️ コアが不足しているため「${card.name}」を召喚できません")
                return
            }

            // 召喚コストのコアはトラッシュへ (総合ルール 11-1-1-5)
            p.trash.add(action.payFrom.sumOf { it.normal }, action.payFrom.sumOf { it.soul })
            p.hand.removeAt(action.handIndex)

            // 残りは召喚したカードの上へ (総合ルール 11-1-1-6)
            p.field.add(
                FieldCard(
                    instanceId = "fc_${card.cardNo}_${state.turn}_${p.field.size + 1}",
                    cardNo = card.cardNo,
                    name = card.name,
                    category = card.category,
                    colors = card.colors,
                    cores = Cores(action.placeFrom.sumOf { it.normal }, action.placeFrom.sumOf { it.soul }),
                    lvCosts = card.lvCosts,
                    baseSymbols = card.symbols,
                    lvBps = card.lvBps,
                    systems = card.systems
                )
            )

            val fieldPaid = (action.payFrom + action.placeFrom).filter { it.sourceId != "Reserve" }
            val fieldNote = if (fieldPaid.isEmpty()) "" else
                " ※" + fieldPaid.joinToString(", ") { "${it.label}から${it.normal + it.soul}個" }
            messages.add("🎴 「${card.name}」を召喚 (コスト${action.costToPay}をトラッシュへ)$fieldNote")

            // コアを抜かれてLvコストを満たさなくなったカードを消滅させる
            resolveVanish(p, messages)
        }

        "MOVE_CORE" -> {
            if (action.moves.isEmpty()) return
            // 交換のように双方向の移動を含む場合があるため、
            // 先に全部の移動元から取り出してから、まとめて移動先に置く
            val takes = action.moves.map { CoreAlloc(it.fromId, it.fromLabel, it.normal, it.soul) }
            if (!takeCores(p, takes)) return

            for (m in action.moves) {
                if (m.toId == "Reserve") {
                    p.reserve.add(m.normal, m.soul)
                } else {
                    p.field.find { it.instanceId == m.toId }?.cores?.add(m.normal, m.soul)
                }
            }

            messages.add(
                "🔷 " + action.moves.joinToString(" / ") {
                    "「${it.fromLabel}」➔「${it.toLabel}」 " + describeMovedCores(it.normal, it.soul)
                }
            )
            resolveVanish(p, messages)
        }

        "USE_MAGIC" -> {
            if (action.handIndex < 0 || action.handIndex >= p.hand.size) return
            val card = p.hand[action.handIndex]

            if (!takeCores(p, action.payFrom)) {
                messages.add("⚠️ コアが不足しているため「${card.name}」を使用できません")
                return
            }
            // 使用コストのコアはトラッシュへ。マジックはフィールドに置かれず、使用後はトラッシュへ送られる
            p.trash.add(action.payFrom.sumOf { it.normal }, action.payFrom.sumOf { it.soul })
            p.hand.removeAt(action.handIndex)
            p.trashCards.add(card)
            messages.add("✨ 「${card.name}」を使用 (コスト${coreIcons(action.costToPay, 0)}をトラッシュへ) ※効果は未実装")

            resolveVanish(p, messages)
        }

        "ATTACK" -> {
            val attacker = p.field.find { it.instanceId == action.targetInstanceId } ?: return

            // アタック宣言でアタックするスピリットを疲労させ、ブロック宣言へ進む (総合ルール 8-1-1-1)
            attacker.isExhausted = true
            state.attackingCardId = attacker.instanceId
            state.step = Step.BLOCK_DECLARATION
            messages.add("🗡️ 「${attacker.name}」がアタック宣言 (BP${attacker.currentBp})")
        }

        "BLOCK" -> {
            // ブロックするスピリットも疲労する (総合ルール 8-1-3-1)
            val blocker = p.field.find { it.instanceId == action.targetInstanceId } ?: return
            val attackerOwner = state.notChoosingPlayer
            val attacker = attackerOwner.field.find { it.instanceId == state.attackingCardId }
            blocker.isExhausted = true
            messages.add("🛡️ 「${blocker.name}」でブロック")

            if (attacker != null) {
                // BPが低い方が破壊され、同じ場合は両方破壊される (総合ルール 8-1-6-1)
                when {
                    attacker.currentBp > blocker.currentBp -> destroySpirit(p, blocker, messages)
                    attacker.currentBp < blocker.currentBp -> destroySpirit(attackerOwner, attacker, messages)
                    else -> {
                        destroySpirit(p, blocker, messages)
                        destroySpirit(attackerOwner, attacker, messages)
                    }
                }
            }
            // ブロックが成立したのでライフは減らない
            state.attackingCardId = null
            state.step = Step.ATTACK_DECLARATION
        }

        "PASS_BLOCK" -> {
            // ブロックされなかった場合、減るライフはアタックしているスピリットのシンボル数 (総合ルール 8-1-5-1)。
            // 減ったライフのコアは防御側のリザーブへ移る (公式ルールマニュアル page08)
            val attackerOwner = state.notChoosingPlayer
            val attacker = attackerOwner.field.find { it.instanceId == state.attackingCardId }
            val symbolCount = maxOf(1, attacker?.symbols?.size ?: 1)
            val lost = minOf(symbolCount, p.life)
            p.life -= lost
            p.reserve.add(lost)
            messages.add("💔 ブロックせず: プレイヤー${p.playerId} のライフ-${lost} (コアはリザーブへ)")

            if (p.life <= 0) {
                state.winner = attackerOwner.playerId
                messages.add("🏁 プレイヤー${p.playerId} のライフが0になりました")
            }
            state.attackingCardId = null
            state.step = Step.ATTACK_DECLARATION
        }

    }
}
