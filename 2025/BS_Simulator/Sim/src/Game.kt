import kotlinx.coroutines.yield

fun Game.start() = generateSequence(this) {
    val game = coreStep()
}

fun Game.selection() = sequence {
    if (turnCount != 1) yield(Event.MoveCore(CorePlace.Boid, CorePlace.Reserve), 1)
}

fun Deck.shuffled() = copy(
    cards = if (fixed == 0) cards.shuffled() else cards.take(fixed) + cards.drop(fixed).shuffled()
)

fun Game.drow(n: Int): Game {
    val cards = myBoard.deck.cards.take(n)
    return copy(
        myBoard = myBoard.copy(
            deck = myBoard.deck.copy(cards = myBoard.deck.cards.drop(n)),
            hands = myBoard.hands + cards
        )
    )
}

fun Game.turn(): Sequence<Game> = sequence {
    var game = copy(turnCount = turnCount + 1)

    if (myBoard.deck.cards.isEmpty()) return@sequence // デッキなし敗北

    game = game.coreStep()
    game = game.drawStep() ?: return@sequence
    game = game.refreshStep()

    // メインステップ
    game.mainStep().forEach { mainStepResult ->
        // 選択された状態に対して、スピリットの消滅チェックを行う
        val updatedFieldObjects = mainStepResult.myBoard.field.objects.filter { obj ->
            val minCoresForLv1 = obj.cards.first().lvCosts.firstOrNull() ?: 1
            obj.cores >= minCoresForLv1
        }
        val annihilatedSpirits = mainStepResult.myBoard.field.objects.filterNot { obj ->
            val minCoresForLv1 = obj.cards.first().lvCosts.firstOrNull() ?: 1
            obj.cores >= minCoresForLv1
        }

        val updatedCardTrash = mainStepResult.myBoard.cardTrash.toMutableList()
        annihilatedSpirits.forEach {
            updatedCardTrash.addAll(it.cards)
        }

        yield(
            mainStepResult.copy(
                myBoard = mainStepResult.myBoard.copy(
                    field = mainStepResult.myBoard.field.copy(objects = updatedFieldObjects),
                    cardTrash = updatedCardTrash
                )
            )
        )
    }
}

fun Game.coreStep(): Game = copy(myBoard = myBoard.copy(reserve = myBoard.reserve + 1))

fun Game.drawStep() = if (myBoard.deck.cards.isEmpty()) null else drow(1)

fun Game.refreshStep(): Game = copy(myBoard = with(myBoard) { copy(reserve = reserve + coreTrash, coreTrash = 0) })

fun Game.toText() = """
    |Turn: $turnCount
    |Hand: ${myBoard.hands.map { it.cardName }}
    |Field: ${myBoard.field.objects.map { it.cards.first().cardName + "(" + it.cores + ")" }}
    |Reserve: ${myBoard.reserve}
    |Core Trash: ${myBoard.coreTrash}
    |Card Trash: ${myBoard.cardTrash.map { it.cardName }}
    |""".trimMargin()

fun Game.mainStep(): Sequence<Game> = sequence {
    // 手札のカードを召喚する
    myBoard.hands.forEach { card ->
        yieldAll(summonSpirit(card))
    }

    // 何もせずにターンを終了する
    yieldAll(sequenceOf())
}


fun Game.summonSpirit(card: Card): Sequence<Game> = sequence {
    // コスト計算
    var totalReduction = 0
    val fieldSymbols = myBoard.field.objects.flatMap { it.cards.first().cardSymbols }
    for (reductionSymbol in card.cardReduction) {
        val matchingSymbolsOnField = fieldSymbols.count { fieldSymbol ->
            fieldSymbol.color.any { it == reductionSymbol.color }
        }
        totalReduction += minOf(matchingSymbolsOnField, reductionSymbol.value)
    }
    val actualCost = maxOf(0, card.cardCost - totalReduction)
    val minCoresOnSpirit = card.lvCosts.firstOrNull() ?: 1
    val totalCoresToPay = actualCost + minCoresOnSpirit

    // 支払い可能なコアの組み合わせを探索する再帰関数
    fun findPaymentCombinations(
        currentReserve: Int,
        currentFieldObjects: List<Object>,
        remainingToPay: Int,
        currentCoreTrash: Int, // 支払いによって増えたコアの合計を保持
        objectIndex: Int // フィールドオブジェクトのインデックス
    ): Sequence<Game> = sequence {
        if (remainingToPay == 0) {
            // 支払い完了
            val updatedHands = myBoard.hands.toMutableList()
            updatedHands.remove(card)

            val newObject = Object(cards = listOf(card), cores = minCoresOnSpirit)
            val finalFieldObjects = currentFieldObjects.filter { obj ->
                val minCoresForLv1 = obj.cards.first().lvCosts.firstOrNull() ?: 1
                obj.cores >= minCoresForLv1
            }
            val annihilatedSpiritsInSummon = currentFieldObjects.filterNot { obj ->
                val minCoresForLv1 = obj.cards.first().lvCosts.firstOrNull() ?: 1
                obj.cores >= minCoresForLv1
            }

            val updatedCardTrash = myBoard.cardTrash.toMutableList()
            annihilatedSpiritsInSummon.forEach {
                updatedCardTrash.addAll(it.cards)
            }

            yield(
                copy(
                    myBoard = myBoard.copy(
                        hands = updatedHands,
                        reserve = currentReserve, // 支払い後のリザーブ
                        coreTrash = currentCoreTrash, // 修正: coresPaidFromReserve + coresPaidFromField はすでにcurrentCoreTrashに含まれている
                        field = myBoard.field.copy(objects = finalFieldObjects + newObject),
                        cardTrash = updatedCardTrash
                    )
                )
            )
            return@sequence
        }

        // 支払い残高がある場合のみ処理を続行
        if (remainingToPay > 0) {
            // リザーブから支払う
            if (currentReserve > 0) {
                val payFromReserve = minOf(currentReserve, remainingToPay)
                yieldAll(
                    findPaymentCombinations(
                        currentReserve - payFromReserve,
                        currentFieldObjects,
                        remainingToPay - payFromReserve,
                        currentCoreTrash + payFromReserve, // 修正: currentCoreTrashに加算
                        objectIndex
                    )
                )
            }

            // フィールド上のスピリットから支払う
            if (objectIndex < currentFieldObjects.size) {
                val obj = currentFieldObjects[objectIndex]
                val minCoresForLv1 = obj.cards.first().lvCosts.firstOrNull() ?: 1
                val availableCoresOnSpirit = obj.cores - minCoresForLv1

                if (availableCoresOnSpirit > 0) {
                    val payFromSpirit = minOf(availableCoresOnSpirit, remainingToPay)
                    val newFieldObjects = currentFieldObjects.toMutableList()
                    newFieldObjects[objectIndex] = obj.copy(cores = obj.cores - payFromSpirit)

                    yieldAll(
                        findPaymentCombinations(
                            currentReserve,
                            newFieldObjects,
                            remainingToPay - payFromSpirit,
                            currentCoreTrash + payFromSpirit, // 修正: currentCoreTrashに加算
                            objectIndex + 1
                        )
                    )
                }
                // このスピリットから支払わない場合、次のスピリットへ
                yieldAll(
                    findPaymentCombinations(
                        currentReserve,
                        currentFieldObjects,
                        remainingToPay,
                        currentCoreTrash,
                        objectIndex + 1
                    )
                )
            }
        }
    }

    // 支払い探索を開始
    yieldAll(
        findPaymentCombinations(
            myBoard.reserve,
            myBoard.field.objects,
            totalCoresToPay,
            myBoard.coreTrash, // 修正: 初期coreTrashを渡す
            0
        )
    )
}
