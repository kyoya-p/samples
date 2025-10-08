import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

fun main() {
    var game = Game(
        myBoard = Board(
            deck = Deck1.shuffled(),
            hands = listOf(),
            field = Field(objects = listOf()),
            reserve = 4,
            coreTrash = 0,
            cardTrash = emptyList(),
        ),
    ).drow(4)

    var turnCount = 0
    while (!game.isTerminated && turnCount < 10) { // Added a turn limit to prevent infinite loops
        turnCount++
        println("--- Turn $turnCount ---")
        game = game.turn()
        println("Current Hand: ${game.myBoard.hands.map { it.cardName }}")
        println("Current Field: ${game.myBoard.field.objects.map { it.cards.first().cardName + "(" + it.cores + ")" }}")
        println("Reserve: ${game.myBoard.reserve}, Core Trash: ${game.myBoard.coreTrash}")
    }

    println("\n--- Final Game State ---")
    println(Json {}.encodeToString(game))
}
