package bstools.model

import bstools.model.effects.CardScriptRegistry
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

/** 召喚・配置時にカード上へ置く必要があるコア数 = 最低LvのLvコスト (総合ルール 11-1-1-6) */
fun requiredCoresOnSummon(card: CardData): Int = card.lvCosts.firstOrNull() ?: 0

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
                label = fc.displayName,
                availNormal = fc.cores.normal,
                availSoul = fc.cores.soul,
                cap = fc.cores.total,
                keep = fc.lvCosts.firstOrNull() ?: 0
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
    /** 評価器 (価値ネットワーク) が付ける評価値。エンジン自身は評価しない (常に0.0で列挙する) */
    val eval: Double = 0.0,
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
    val repeatsPosition: Boolean = false,
    /** アタック時効果等の対象インスタンスID (例: ガリザロトの疲労ブロック強制対象) */
    val effectTargetId: String? = null,
    /** コアの一括再配置 ([enumerateRearrangements]) の最終配置 (場所ごとの通常コア・ソウルコア数)。それ以外の手は空 */
    val targetConfig: List<CoreAlloc> = emptyList(),
    /**
     * コアの再配置の対象キー ([GameState.coreWindow])。カードのLv変更はその instanceId、ソウルコアの移動は "SOUL"、
     * 一括再配置は "ALL"。再配置以外の手は null
     */
    val rearrangeKey: String? = null
)

/**
 * 局面のハッシュ。ステップ・ターン・手番に加え、両プレイヤーの見えている情報をすべて含める。
 *
 * これで「その手を指すと以前と同じ局面に戻るか」を判定できる。
 * コアを A➔B と B➔A に動かし続けるような、盤面が進まない循環を選択肢から除くために使う。
 * 手札とフィールドは並び順に意味がないので、正規化してから畳み込む。
 */
fun positionHash(state: GameState): Long {
    var hash = -3750763034362895579L
    fun mixByte(b: Int) {
        hash = (hash xor (b.toLong() and 0xFFL)) * 1099511628211L
    }
    fun mixInt(v: Int) {
        mixByte(v)
        mixByte(v ushr 8)
        mixByte(v ushr 16)
        mixByte(v ushr 24)
    }
    fun mixLong(v: Long) {
        mixInt(v.toInt())
        mixInt((v ushr 32).toInt())
    }
    fun mixStr(s: String) {
        for (i in 0 until s.length) {
            mixByte(s[i].code)
        }
        mixByte('|'.code)
    }

    mixInt(state.turn)
    mixInt(state.activePlayerId)
    mixInt(state.step.ordinal)
    mixInt(state.format.ordinal)
    mixInt(state.winner ?: -1)
    mixStr(state.attackingCardId ?: "-")

    for (p in listOf(state.player1, state.player2)) {
        mixInt(p.playerId)
        mixInt(p.life)
        mixInt(p.reserve.normal)
        mixInt(p.reserve.soul)
        mixInt(p.trash.normal)
        mixInt(p.trash.soul)
        mixInt(p.deckCount)

        val handHashes = LongArray(p.hand.size) { i ->
            var h = 0L
            val cn = p.hand[i].cardNo
            for (j in 0 until cn.length) h = h * 31L + cn[j].code
            h
        }
        handHashes.sort()
        for (h in handHashes) mixLong(h)

        val fieldHashes = LongArray(p.field.size) { i ->
            val fc = p.field[i]
            var h = 0L
            val cn = fc.cardNo
            for (j in 0 until cn.length) h = h * 31L + cn[j].code
            h = h * 31L + fc.cores.normal
            h = h * 31L + fc.cores.soul
            h = h * 31L + (if (fc.isHeavyExhausted) 2 else if (fc.isExhausted) 1 else 0)
            h
        }
        fieldHashes.sort()
        for (h in fieldHashes) mixLong(h)
    }
    for (eff in state.usedOncePerTurnEffects.sorted()) {
        mixStr(eff)
    }
    mixInt(state.flashPriorityPlayerId ?: -1)
    mixInt(state.flashPassCount)
    if (state.pendingEffect != null) {
        mixStr(state.pendingEffect!!.cardInstanceId)
        mixStr(state.pendingEffect!!.effectType)
    }
    return hash
}

/**
 * 手を試すための局面コピー。デッキもコピーする: エンドステップで「ステップ終了」を試すと
 * advanceStep → beginTurn で次のターンのドローが起きるため、共有すると元の局面のデッキから
 * カードが抜けてしまう (評価するたびに実際のデッキが減る不具合の原因だった)。
 */
fun GameState.snapshot(): GameState = GameState(
    turn = turn,
    activePlayerId = activePlayerId,
    step = step,
    player1 = player1.snapshot(),
    player2 = player2.snapshot(),
    attackingCardId = attackingCardId,
    blockingCardId = blockingCardId,
    mustBlockInstanceId = mustBlockInstanceId,
    winner = winner,
    format = format,
    seed = seed,
    visitedPositions = visitedPositions,
    nextCardSeq = nextCardSeq,
    evaluatedCount = evaluatedCount,
    coreWindow = coreWindow.toMutableSet(),
    usedOncePerTurnEffects = usedOncePerTurnEffects.toMutableSet(),
    openedCards = openedCards.toMutableList(),
    flashPriorityPlayerId = flashPriorityPlayerId,
    flashPassCount = flashPassCount,
    pendingEffect = pendingEffect
)

private fun PlayerState.snapshot(): PlayerState = PlayerState(
    playerId = playerId,
    name = name,
    life = life,
    reserve = Cores(reserve.normal, reserve.soul),
    trash = Cores(trash.normal, trash.soul),
    deckCount = deckCount,
    hand = hand.toMutableList(),
    deck = deck.toMutableList(),
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
fun playOptions(
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

            options.add(
                GameAction(
                    index = 0,
                    type = actionType,
                    category = category,
                    detail = "$costLabel, ${placeLabel}${vanishLabel}, $sourceStr",
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
 * コアが減ってLv1のLvコストを下回るスピリットは消滅する (その旨を detail に明記する)。
 */
private fun coreMoveOptions(p: PlayerState, sources: List<CoreSource>, limit: Int = 120): List<GameAction> {
    val options = mutableListOf<GameAction>()
    val destinations = listOf("Reserve") + p.field.map { it.instanceId }
    fun labelOf(id: String) = if (id == "Reserve") "R" else p.field.first { it.instanceId == id }.displayName

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
                    options.add(
                        GameAction(
                            index = 0,
                            type = "MOVE_CORE",
                            category = "🔷【コア移動】 ${src.label} ➔ $dstLabel",
                            detail = describeCores(normal, soul) + "を移動" +
                                    (if (vanishes) " ⚠️${src.label}消滅" else ""),
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

/**
 * すべての合法手 (件数の上限なし)。学習・自動対戦・Playmat の既定の列挙。
 * メインステップはプレイ ([enumerateAllPlays]) の全組み合わせと、コアの一括再配置 ([enumerateRearrangements]:
 * 最終配置を1手で選ぶ。個別のコア移動・交換の連続と同じ結果をすべて含む)。
 * それ以外のステップ (アタック・ブロック等) はもともとエンティティ単位なので従来の列挙と同じ。
 * index は 1 からの通し番号。同一局面に戻る手には印を付ける。
 */
/**
 * 現在のステップ開始時に発揮する全効果を自陣カードから収集する。
 * 複数存在する場合、プレイヤーはこの中から解決順を選択する (総合ルール 8-1: 同時発揮の解決)。
 */
fun enumerateStepStartEffects(
    state: GameState,
    player: PlayerState,
    step: Step,
    nextIndex: () -> Int
): List<GameAction> {
    if (state.winner != null) return emptyList()
    val acts = mutableListOf<GameAction>()
    for (fc in player.field) {
        val script = CardScriptRegistry.get(fc.cardNo) ?: continue
        acts.addAll(script.enumerateStepStartActions(state, fc, player, step, nextIndex))
    }
    return acts
}

fun enumerateAllActions(state: GameState): List<GameAction> {
    if (state.pendingEffect != null) {
        return markRepetitions(state, enumerateRawActions(state))
    }
    if (state.winner == null && (state.step == Step.MAIN || state.step == Step.MAIN_2)) {
        val stepStartActs = enumerateStepStartEffects(state, state.choosingPlayer, state.step) { 1 }
        if (stepStartActs.isNotEmpty()) {
            return stepStartActs.mapIndexed { i, a -> a.copy(index = i + 1) }
        }
        return (enumerateAllPlays(state) + enumerateRearrangements(state)).mapIndexed { i, a -> a.copy(index = i + 1) }
    }
    return markRepetitions(state, enumerateRawActions(state))
}

/**
 * メインステップのプレイ (召喚・配置・マジック) を件数の上限なしですべて列挙する。
 * 支払い・配置に使うコアの供給元 (リザーブ・フィールドの各カード) ごとの個数、ソウルコアの使い方
 * (使わない / 配置する / 支払う、どこから取るか)、配置するコア数 (最低Lv以上の全枚数) の全組み合わせ。
 * 通常コアはどれも同じなので、同じ供給元の内訳で「どの分を配置に回すか」だけが違う手は1つにまとめる
 * (盤面の結果が同じため)。同一局面に戻る手には印を付ける。
 */
fun enumerateAllPlays(state: GameState): List<GameAction> {
    if (state.winner != null || (state.step != Step.MAIN && state.step != Step.MAIN_2)) return emptyList()
    val p = state.choosingPlayer
    val prefix = if (state.step == Step.MAIN_2) "[第2メイン] " else ""
    val sources = coreSources(p)
    val acts = mutableListOf<GameAction>()
    for (handIdx in p.hand.indices.distinctBy { p.hand[it].cardNo }) {
        val card = p.hand[handIdx]
        val cost = summonCost(card, p)
        val actionType = if (card.category == CardCategory.MAGIC) "USE_MAGIC" else "SUMMON"
        val category = when (card.category) {
            CardCategory.MAGIC -> "✨【${prefix}マジック使用】 ${card.name}"
            CardCategory.NEXUS -> "🌐【${prefix}配置】 ${card.name}"
            else -> "🎴【${prefix}召喚】 ${card.name}"
        }
        for (place in placementOptions(card, cost, sources)) {
            acts.addAll(playOptions(card, handIdx, category, actionType, cost, place, sources, limit = Int.MAX_VALUE))
        }
    }
    return markRepetitions(state, acts)
}

/**
 * メインステップのコア移動・交換を件数の上限なしですべて列挙する (Playmat のカード別メニュー用)。
 * 各カードについて「自身から別の場所 (リザーブ・他のカード) へ」「別の場所から自身へ」(それぞれ1個〜全部)と
 * 「別の場所とのソウルコア⇄コアの交換」がすべて含まれる。同一局面に戻る手には印を付ける。
 */
fun enumerateAllCoreMoves(state: GameState): List<GameAction> {
    if (state.winner != null || (state.step != Step.MAIN && state.step != Step.MAIN_2)) return emptyList()
    val p = state.choosingPlayer
    return markRepetitions(state, coreMoveOptions(p, coreSources(p), limit = Int.MAX_VALUE))
}

/** 供給元から取るコアをアイコンで表す。WebUI 側の parseDetailSources がこの表記を解釈する */
private fun formatAlloc(alloc: CoreAlloc): String = coreIcons(alloc.normal, alloc.soul)

/**
 * その手を指した結果が既出の局面になるものに「同一局面」の印を付ける。
 *
 * コアを A➔B / B➔A と往復させるような手は総合ルール上禁止された手ではないため、**選択は妨げない**。
 * 自動対戦が無限に往復し続けないよう `repeatsPosition` で明示し、エージェント側で候補から除く。
 * (評価値は付けない。評価はすべて価値ネットワークが行う)
 */
internal fun markRepetitions(state: GameState, actions: List<GameAction>): List<GameAction> {
    if (state.visitedPositions.isEmpty()) return actions
    return actions.map { action ->
        // ステップ進行、召喚、アタック等は必ず局面を進める（手札消費・疲労など）ので確認不要
        if (action.type != "MOVE_CORE" && action.type != "REARRANGE") return@map action
        val trial = state.snapshot()
        applyAction(trial, action, mutableListOf())
        if (positionHash(trial) in state.visitedPositions) {
            action.copy(repeatsPosition = true)
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
 * エンジンは評価値を付けない (eval=0.0)。[actionEvaluator] (価値ネットワークによる1手先読み) が
 * 設定されていれば、各選択肢の `eval` をその評価値で埋める。
 */
fun enumerateActions(state: GameState): List<GameAction> {
    val actions = markRepetitions(state, enumerateRawActions(state))
    val evaluator = actionEvaluator
    if (evaluator != null) {
        return actions.map { a ->
            if (a.repeatsPosition) a else a.copy(eval = evaluator(state, a))
        }
    }
    state.evaluatedCount += actions.size
    return actions
}

/**
 * 選択肢評価の差し替え口。評価器(RNN等)はエンジンに依存させず、外側のモジュールから注入する。
 * `@ThreadLocal`: 自己対戦をスレッド並列化しても他スレッドの評価器と競合させないため。
 */
@kotlin.native.concurrent.ThreadLocal
var actionEvaluator: ((GameState, GameAction) -> Double)? = null

internal fun enumerateRawActions(state: GameState): List<GameAction> {
    val acts = mutableListOf<GameAction>()
    if (state.winner != null) return acts

    val p = state.choosingPlayer
    var idx = 1

    if (state.pendingEffect != null) {
        val pending = state.pendingEffect!!
        val card = p.field.find { it.instanceId == pending.cardInstanceId }
        val script = card?.let { CardScriptRegistry.get(it.cardNo) }
        if (card != null && script != null) {
            val pendingActs = script.enumeratePendingEffectActions(state, card, p, pending) { idx++ }
            if (pendingActs.isNotEmpty()) {
                return pendingActs
            }
        }
        state.pendingEffect = null
    }

    if (state.step == Step.MAIN || state.step == Step.MAIN_2) {
        val stepStartActs = enumerateStepStartEffects(state, p, state.step) { idx++ }
        if (stepStartActs.isNotEmpty()) {
            return stepStartActs
        }

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

        // 盤面カードの起動型効果 (例: ネクサスやスピリットの【起動：メイン】など)
        for (fc in p.field) {
            val script = CardScriptRegistry.get(fc.cardNo)
            if (script != null) {
                acts.addAll(script.enumerateFieldActions(state, fc, p) { idx++ })
            }
        }
    }

    if (state.step == Step.ATTACK_DECLARATION) {
        val opponent = state.notChoosingPlayer

        for (fc in p.field) {
            if (fc.isExhausted || fc.level <= 0 || fc.category != CardCategory.SPIRIT) continue
            val symbolCount = maxOf(1, fc.symbols.size)

            // カード固有アタック効果 (例: ガリザロトの疲労ブロック強制指定など) の列挙
            val script = CardScriptRegistry.get(fc.cardNo)
            val customAttacks = script?.enumerateAttackActions(state, fc, opponent) { idx++ }
            if (!customAttacks.isNullOrEmpty()) {
                acts.addAll(customAttacks)
                continue
            }

            acts.add(
                GameAction(
                    index = idx++,
                    type = "ATTACK",
                    category = "🗡️【アタック】 ${fc.displayName}",
                    detail = "${fc.displayName} でアタック宣言 (Lv${fc.level}, BP${fc.effectiveBp(state, isAttackingHypothetical = true)}, 通ればライフ-${coreIcons(symbolCount, 0)})",
                    targetInstanceId = fc.instanceId
                )
            )
        }
    }

    // フラッシュタイミングおよびブロック宣言ステップにおけるフラッシュ効果・マジックの列挙
    if (state.step == Step.FLASH_TIMING || state.step == Step.BLOCK_DECLARATION) {
        val opp = state.notChoosingPlayer

        // 1. 手札のフラッシュマジック (CardCategory.MAGIC かつ effect に「フラッシュ」を含む)
        val distinctHand = p.hand.indices.distinctBy { p.hand[it].cardNo }
        val sources = coreSources(p)
        for (handIdx in distinctHand) {
            val card = p.hand[handIdx]
            if (card.category == CardCategory.MAGIC && card.effect.contains("フラッシュ")) {
                val cost = summonCost(card, p)
                val totalAvail = sources.sumOf { it.availNormal + it.availSoul }
                if (totalAvail >= cost) {
                    val category = "✨【フラッシュ使用】 ${card.name}"
                    for (action in playOptions(card, handIdx, category, "USE_MAGIC", cost, 0, sources)) {
                        acts.add(action.copy(index = idx++))
                    }
                }
            }
        }

        // 2. カード固有スクリプトによるフラッシュ効果 (例: フラッティの手札からのフラッシュ召喚など)
        p.hand.forEachIndexed { hIdx, card ->
            val script = CardScriptRegistry.get(card.cardNo)
            if (script != null) {
                acts.addAll(script.enumerateFlashActions(state, p, opp, hIdx) { idx++ })
            }
        }
        for (fc in p.field) {
            val script = CardScriptRegistry.get(fc.cardNo)
            if (script != null) {
                acts.addAll(script.enumerateFlashActions(state, p, opp, -1) { idx++ })
            }
        }

        // 3. アタック直後・ブロック後のフラッシュタイミングにおけるパス選択肢
        if (state.step == Step.FLASH_TIMING) {
            val nextDesc = if (state.flashPassCount == 0) {
                "フラッシュをパス (相手へ優先権を移行)"
            } else if (state.blockingCardId != null) {
                "フラッシュをパス (バトル解決へ移行)"
            } else {
                "フラッシュをパス (ブロック宣言へ移行)"
            }
            acts.add(
                GameAction(
                    index = idx++,
                    type = "PASS_FLASH",
                    category = "⏩【フラッシュパス】",
                    detail = nextDesc
                )
            )
        }
    }

    // ブロック宣言はアタックされている側が行う (総合ルール 8-1-3-1)。
    // 回復状態のスピリット1体だけがブロックでき、ブロックしない選択もできる。
    if (state.step == Step.BLOCK_DECLARATION) {
        val attacker = state.notChoosingPlayer.field.find { it.instanceId == state.attackingCardId }
        val attackerBp = attacker?.effectiveBp(state) ?: 0
        val attackerName = attacker?.displayName ?: "相手"
        val lifeLoss = maxOf(1, attacker?.symbols?.size ?: 1)

        val mustBlockId = state.mustBlockInstanceId
        val forcedBlocker = p.field.find { it.instanceId == mustBlockId && it.level > 0 && it.category == CardCategory.SPIRIT }

        if (forcedBlocker != null) {
            // ガリザロト等の効果により「疲労状態でもブロックしなければならない」スピリットが存在する場合:
            // そのスピリットによるブロックのみが合法手となり、PASS_BLOCKや他スピリットでのブロックは選べない
            val blockerBp = forcedBlocker.effectiveBp(state)
            val outcome = when {
                blockerBp > attackerBp -> "${attackerName}を破壊"
                blockerBp < attackerBp -> "${forcedBlocker.displayName}が破壊される"
                else -> "相打ち"
            }
            acts.add(
                GameAction(
                    index = idx++,
                    type = "BLOCK",
                    category = "🛡️【強制ブロック】 ${forcedBlocker.displayName}",
                    detail = "${forcedBlocker.displayName} でブロック (疲労強制ブロック, BP${blockerBp} vs BP${attackerBp} → $outcome)",
                    targetInstanceId = forcedBlocker.instanceId
                )
            )
        } else {
            for (fc in p.field) {
                if (fc.isExhausted || fc.level <= 0 || fc.category != CardCategory.SPIRIT) continue
                // BPが低い方が破壊される。同じなら相打ち (総合ルール 8-1-6-1)
                val blockerBp = fc.effectiveBp(state)
                val outcome = when {
                    blockerBp > attackerBp -> "${attackerName}を破壊"
                    blockerBp < attackerBp -> "${fc.displayName}が破壊される"
                    else -> "相打ち"
                }
                acts.add(
                    GameAction(
                        index = idx++,
                        type = "BLOCK",
                        category = "🛡️【ブロック】 ${fc.displayName}",
                        detail = "${fc.displayName} でブロック (BP${blockerBp} vs BP${attackerBp} → $outcome)",
                        targetInstanceId = fc.instanceId
                    )
                )
            }

            acts.add(
                GameAction(
                    index = idx++,
                    type = "PASS_BLOCK",
                    category = "💔【ブロックしない】",
                    detail = "ライフでアタックを受ける (ライフ-${coreIcons(lifeLoss, 0)})"
                )
            )
        }
    }

    return acts
}
/** ターン開始時の非対話ステップ (スタート→コア→ドロー→リフレッシュ) をまとめて処理する */
fun beginTurn(state: GameState, messages: MutableList<String>) {
    val p = state.activePlayer
    state.coreWindow.clear()
    state.usedOncePerTurnEffects.clear()
    state.openedCards.clear()
    state.flashPriorityPlayerId = null
    state.flashPassCount = 0

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
    refreshStep(state, messages)

    state.step = Step.MAIN
}

/** リフレッシュステップ処理 (単体呼び出し・テスト可能) */
fun refreshStep(state: GameState, messages: MutableList<String>) {
    val p = state.activePlayer
    state.step = Step.REFRESH
    if (p.trash.total > 0) {
        messages.add("♻️ リフレッシュステップ: トラッシュのコア${p.trash.total}個をリザーブへ")
        p.reserve.add(p.trash.normal, p.trash.soul)
        p.trash = Cores(0, 0)
    }
    val recovered = p.field.count { it.isExhausted || it.isHeavyExhausted }
    if (recovered > 0) {
        p.field.forEach { it.refresh() }
        messages.add("♻️ リフレッシュステップ: ${recovered}体を回復")
    }
}

/**
 * ブロック後フラッシュタイミング終了後のバトル解決 (総合ルール 8-1-5 / 8-1-6)。
 * スピリット同士のBPを比較し、低い方を破壊する (同値は相打ち)。
 */
fun resolveBattle(state: GameState, messages: MutableList<String>) {
    val attackerOwner = state.activePlayer
    val blockerOwner = state.opponentPlayer
    val attacker = attackerOwner.field.find { it.instanceId == state.attackingCardId }
    val blocker = blockerOwner.field.find { it.instanceId == state.blockingCardId }

    if (attacker != null && blocker != null) {
        val attackerBp = attacker.effectiveBp(state)
        val blockerBp = blocker.effectiveBp(state)
        messages.add("⚔️ バトル解決 (BP勝負): 「${attacker.name}」(BP$attackerBp) vs 「${blocker.name}」(BP$blockerBp)")
        when {
            attackerBp > blockerBp -> destroySpirit(blockerOwner, blocker, messages)
            attackerBp < blockerBp -> destroySpirit(attackerOwner, attacker, messages)
            else -> {
                destroySpirit(blockerOwner, blocker, messages)
                destroySpirit(attackerOwner, attacker, messages)
            }
        }
    } else if (attacker != null && blocker == null) {
        messages.add("ℹ️ ブロッカーがフィールドを離れたためBP比べをスキップ")
    } else if (attacker == null && blocker != null) {
        messages.add("ℹ️ アタッカーがフィールドを離れたためBP比べをスキップ")
    }

    // バトル終了時効果の解決 (総合ルール 8-1-7: バトル終了時)
    val survivingAttacker = attackerOwner.field.find { it.instanceId == state.attackingCardId }
    if (survivingAttacker != null) {
        CardScriptRegistry.get(survivingAttacker.cardNo)?.onBattleEnd(state, survivingAttacker, attackerOwner, messages)
    }

    state.attackingCardId = null
    state.blockingCardId = null
    state.mustBlockInstanceId = null
    state.flashPriorityPlayerId = null
    state.flashPassCount = 0
    state.step = Step.ATTACK_DECLARATION
}

/** ステップ進行 (総合ルール 7-1-1: スタンダードは8ステップ制) */
fun advanceStep(state: GameState, messages: MutableList<String>) {
    state.coreWindow.clear()
    when (state.step) {
        // 先攻1ターン目はアタックステップが存在しないため直接エンドへ
        // (スタンダードでは第2メインステップも同様に存在しない)
        Step.MAIN -> state.step = if (isFirstTurn(state)) Step.END else Step.ATTACK_DECLARATION
        // エターナルにはアタックステップの後の第2メインステップがない
        Step.ATTACK_DECLARATION -> state.step = if (state.format.hasSecondMain) Step.MAIN_2 else Step.END
        Step.FLASH_TIMING -> {
            state.flashPassCount++
            if (state.flashPassCount >= 2) {
                if (state.blockingCardId != null) {
                    messages.add("⏩ 両者がブロック後フラッシュをパス ➔ バトル解決へ")
                    resolveBattle(state, messages)
                } else {
                    state.step = Step.BLOCK_DECLARATION
                    state.flashPriorityPlayerId = null
                    state.flashPassCount = 0
                    messages.add("⏩ 両者がフラッシュをパス ➔ ブロック宣言へ")
                }
            } else {
                val currentChooser = state.choosingPlayerId
                val nextPlayerId = if (currentChooser == 1) 2 else 1
                state.flashPriorityPlayerId = nextPlayerId
                messages.add("⏩ プレイヤー${currentChooser}がフラッシュをパス ➔ プレイヤー${nextPlayerId}のフラッシュへ")
            }
        }
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
    // コアの再配置は対象のキーを記録する (同じ対象の再配置を続けて列挙しないため)。ほかの手を指したら空に戻す
    val key = action.rearrangeKey
    if (key != null) state.coreWindow.add(key) else state.coreWindow.clear()

    when (action.type) {
        "SUMMON", "FLASH_SUMMON" -> {
            if (action.handIndex < 0 || action.handIndex >= p.hand.size) return
            val card = p.hand[action.handIndex]

            if (!takeCores(p, action.payFrom) || !takeCores(p, action.placeFrom)) {
                messages.add("⚠️ コアが不足しているため「${card.name}」を${if (card.category == CardCategory.NEXUS) "配置" else "召喚"}できません")
                return
            }

            // 召喚コストのコアはトラッシュへ (総合ルール 11-1-1-5)
            p.trash.add(action.payFrom.sumOf { it.normal }, action.payFrom.sumOf { it.soul })
            p.hand.removeAt(action.handIndex)

            // 残りは召喚したカードの上へ (総合ルール 11-1-1-6)
            val seq = state.nextCardSeq++
            val newFieldCard = FieldCard(
                instanceId = "fc_${card.cardNo}_${seq}",
                cardNo = card.cardNo,
                name = card.name,
                category = card.category,
                colors = card.colors,
                cores = Cores(action.placeFrom.sumOf { it.normal }, action.placeFrom.sumOf { it.soul }),
                lvCosts = card.lvCosts,
                baseSymbols = card.symbols,
                lvBps = card.lvBps,
                systems = card.systems,
                seq = seq,
                cost = card.cost,
                shinkai = card.shinkai
            )
            p.field.add(newFieldCard)

            val fieldPaid = (action.payFrom + action.placeFrom).filter { it.sourceId != "Reserve" }
            val fieldNote = if (fieldPaid.isEmpty()) "" else
                " ※" + fieldPaid.joinToString(", ") { "${it.label}から${it.normal + it.soul}個" }
            val verb = if (action.type == "FLASH_SUMMON") "⚡ 【フラッシュ】「${card.name}」を召喚"
                       else if (card.category == CardCategory.NEXUS) "🌐 「${card.name}」を配置"
                       else "🎴 「${card.name}」を召喚"
            messages.add("$verb (コスト${action.costToPay}をトラッシュへ)$fieldNote")

            // コアを抜かれてLvコストを満たさなくなったカードを消滅させる
            resolveVanish(p, messages)

            // 召喚時効果の解決 (総合ルール 11-1-1-8: 召喚完了後に召喚時効果が発揮)
            if (newFieldCard.level >= 1 && p.field.any { it.instanceId == newFieldCard.instanceId }) {
                CardScriptRegistry.get(newFieldCard.cardNo)?.onSummon(state, newFieldCard, p, messages)
            }

            // フラッシュ効果フックの解決
            if (action.type == "FLASH_SUMMON") {
                CardScriptRegistry.get(card.cardNo)?.onFlash(state, p, action, messages)
            }

            if (state.step == Step.FLASH_TIMING) {
                state.flashPassCount = 0
                val nextPlayerId = if (p.playerId == 1) 2 else 1
                state.flashPriorityPlayerId = nextPlayerId
                messages.add("⚡ フラッシュ優先権がプレイヤー${nextPlayerId}へ移動")
            }
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

            if (state.step == Step.FLASH_TIMING) {
                state.flashPassCount = 0
                val nextPlayerId = if (p.playerId == 1) 2 else 1
                state.flashPriorityPlayerId = nextPlayerId
                messages.add("⚡ フラッシュ優先権がプレイヤー${nextPlayerId}へ移動")
            }
        }

        "ATTACK" -> {
            val attacker = p.field.find { it.instanceId == action.targetInstanceId } ?: return

            // アタック宣言でアタックするスピリットを疲労させ、フラッシュタイミングへ進む (総合ルール 8-1-1-1 / 8-1-2)
            attacker.isExhausted = true
            state.attackingCardId = attacker.instanceId
            state.mustBlockInstanceId = null
            state.step = Step.FLASH_TIMING
            state.flashPriorityPlayerId = state.opponentPlayer.playerId
            state.flashPassCount = 0
            messages.add("🗡️ 「${attacker.name}」がアタック宣言 (BP${attacker.effectiveBp(state)})")
            messages.add("⚡ フラッシュタイミング開始: 防御側（プレイヤー${state.opponentPlayer.playerId}）から優先権")

            // カード固有アタック時効果の解決 (例: ガリザロトの疲労ブロック強制指定など)
            CardScriptRegistry.get(attacker.cardNo)?.onAttack(state, attacker, action, messages)
        }

        "PASS_FLASH" -> {
            state.flashPassCount++
            if (state.flashPassCount >= 2) {
                if (state.blockingCardId != null) {
                    messages.add("⏩ 両者がブロック後フラッシュをパス ➔ バトル解決へ")
                    resolveBattle(state, messages)
                } else {
                    state.step = Step.BLOCK_DECLARATION
                    state.flashPriorityPlayerId = null
                    state.flashPassCount = 0
                    messages.add("⏩ 両者がフラッシュをパス ➔ ブロック宣言へ")
                }
            } else {
                val nextPlayerId = if (p.playerId == 1) 2 else 1
                state.flashPriorityPlayerId = nextPlayerId
                messages.add("⏩ プレイヤー${p.playerId}がフラッシュをパス ➔ プレイヤー${nextPlayerId}のフラッシュへ")
            }
        }

        "ACTIVATE_FIELD" -> {
            val card = p.field.find { it.instanceId == action.targetInstanceId } ?: return
            CardScriptRegistry.get(card.cardNo)?.onFieldAction(state, card, p, action, messages)
        }

        "FLASH_EFFECT" -> {
            val card = p.field.find { it.instanceId == action.targetInstanceId }
            if (card != null) {
                CardScriptRegistry.get(card.cardNo)?.onFlash(state, p, action, messages)
            }
            if (state.step == Step.FLASH_TIMING) {
                state.flashPassCount = 0
                val nextPlayerId = if (p.playerId == 1) 2 else 1
                state.flashPriorityPlayerId = nextPlayerId
                messages.add("⚡ フラッシュ優先権がプレイヤー${nextPlayerId}へ移動")
            }
        }

        "STEP_START_EFFECT" -> {
            val card = p.field.find { it.instanceId == action.targetInstanceId } ?: return
            CardScriptRegistry.get(card.cardNo)?.onStepStartAction(state, card, p, action, messages)
        }

        "RESOLVE_PENDING_EFFECT" -> {
            val pending = state.pendingEffect
            state.pendingEffect = null
            if (pending != null) {
                val card = p.field.find { it.instanceId == pending.cardInstanceId } ?: return
                val script = CardScriptRegistry.get(card.cardNo)
                script?.onResolvePendingEffect(state, card, p, action, messages)
            }
        }

        "BLOCK" -> {
            // ブロックするスピリットも疲労する (総合ルール 8-1-3-1)
            val blocker = p.field.find { it.instanceId == action.targetInstanceId } ?: return
            blocker.isExhausted = true
            state.blockingCardId = blocker.instanceId
            messages.add("🛡️ 「${blocker.name}」でブロック ➔ ブロック後フラッシュタイミングへ")

            // ブロック時効果の解決 (総合ルール 8-1-3-2)
            CardScriptRegistry.get(blocker.cardNo)?.onBlock(state, blocker, action, messages)

            // ブロック後フラッシュタイミングへ移行 (総合ルール 8-1-4-1: 防御側から優先権)
            state.step = Step.FLASH_TIMING
            state.flashPriorityPlayerId = p.playerId
            state.flashPassCount = 0
            messages.add("⚡ ブロック後フラッシュタイミング開始: 防御側（プレイヤー${p.playerId}）から優先権")
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

            // バトル終了時効果の解決 (総合ルール 8-1-7: バトル終了時)
            val survivingAttacker = attackerOwner.field.find { it.instanceId == state.attackingCardId }
            if (survivingAttacker != null) {
                CardScriptRegistry.get(survivingAttacker.cardNo)?.onBattleEnd(state, survivingAttacker, attackerOwner, messages)
            }

            state.attackingCardId = null
            state.mustBlockInstanceId = null
            state.flashPriorityPlayerId = null
            state.flashPassCount = 0
            state.step = Step.ATTACK_DECLARATION
        }

    }
}
