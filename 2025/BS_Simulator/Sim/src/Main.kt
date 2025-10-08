fun main() {
    val initGame = Game(
        myBoard = Board(
            deck = Deck1.shuffled(),
            hands = listOf(),
            field = Field(objects = listOf()),
            trash = listOf()
        ),
    ).drow(4)
    println(Json{}.encodeToString(initGame))
}
