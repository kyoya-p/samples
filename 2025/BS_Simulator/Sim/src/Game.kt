fun Deck.shuffled() = copy(cards = cards.take(1) + cards.drop(1).shuffled())

fun initGame() = Game(
    myBoard = Board(
        deck = Deck1.shuffled(),
        hands = listOf(),
        field = Field(objects = listOf()),
        trash = listOf()
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
