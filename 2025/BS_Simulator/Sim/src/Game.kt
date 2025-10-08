fun Deck.shuffled() = copy(cards = cards.take(1) + cards.drop(1).shuffled())

fun initGame() = Game(
    myBoard = Board(
        deck = Deck1.shuffled(),
        trash = listOf(),
        hands = listOf(),
        field = Field(objects = listOf()),
        reserve = 4,
        coreTrash = 0,
        cardTrash = emptyList(),
    )
).drow(4)


fun Game.drow(n: Int): Game {
    val cards = myBoard.deck.cards.take(n)
    return copy(
        myBoard = myBoard.copy(
            deck = myBoard.deck.copy(cards = myBoard.deck.cards.drop(n)),
            hands = myBoard.hands + cards
        )
    )
}

fun Game.turn(): Game {
    var game = this
    if (game.isTerminated) return game // Stop if already terminated

    game = game.drawStep()
    if (game.isTerminated) return game // Check after draw

    game = game.refreshStep()
    if (game.isTerminated) return game // Check after refresh

    game = game.mainStep()
    return game
}

fun Game.drawStep(): Game {
    val cardToDraw = myBoard.deck.cards.firstOrNull()
    return if (cardToDraw != null) {
        copy(
            myBoard = myBoard.copy(
                deck = myBoard.deck.copy(cards = myBoard.deck.cards.drop(1)),
                hands = myBoard.hands + cardToDraw
            )
        )
    } else {
        this // No cards to draw
    }
}

fun Game.refreshStep(): Game {
    return copy(
        myBoard = myBoard.copy(
            reserve = myBoard.reserve + myBoard.coreTrash,
            coreTrash = 0
        )
    )
}

fun Game.mainStep(): Game {
    // Try to summon the first available spirit card
    val spiritCardInHand = myBoard.hands.firstOrNull { it.cardType == CardType.SPIRIT }

    if (spiritCardInHand != null && myBoard.reserve >= spiritCardInHand.cardCost) {
        var updatedGame = summonSpirit(spiritCardInHand)
        // Check termination condition
        if (spiritCardInHand.cardName.contains("デストロイモード")) {
            println("Termination condition met: 'Destroy Mode' summoned!")
            return updatedGame.copy(isTerminated = true)
        }
        return updatedGame
    }
    return this // No spirit to summon or not enough cores
}

fun Game.summonSpirit(card: Card): Game {
    // Calculate cost reduction
    var totalReduction = 0
    val fieldSymbols = myBoard.field.objects.flatMap { it.cards.first().cardSymbols }
    for (reductionSymbol in card.cardReduction) {
        val matchingSymbolsOnField = fieldSymbols.count { fieldSymbol ->
            fieldSymbol.color.any { it == reductionSymbol.color }
        }
        totalReduction += minOf(matchingSymbolsOnField, reductionSymbol.value)
    }

    val actualCost = maxOf(0, card.cardCost - totalReduction)

    if (myBoard.reserve >= actualCost) {
        val updatedHands = myBoard.hands.toMutableList()
        updatedHands.remove(card)

        val initialCoresOnSpirit = card.lvCosts.firstOrNull() ?: 1
        val coresToMoveFromReserve = maxOf(actualCost, initialCoresOnSpirit)

        if (myBoard.reserve >= coresToMoveFromReserve) {
            val newObject = Object(cards = listOf(card), cores = initialCoresOnSpirit)
            val updatedFieldObjects = myBoard.field.objects + newObject

            return copy(
                myBoard = myBoard.copy(
                    hands = updatedHands,
                    reserve = myBoard.reserve - coresToMoveFromReserve,
                    coreTrash = myBoard.coreTrash + coresToMoveFromReserve,
                    field = myBoard.field.copy(objects = updatedFieldObjects)
                )
            )
        }
    }
    return this // Cannot summon, return current game state
}
