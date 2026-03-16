import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import kotlinx.io.buffered

val outputFile = Path("result.txt")

fun main() = SystemFileSystem.sink(outputFile).buffered().use { sink ->
    var game = Game(
        myBoard = Board(
            deck = Deck1.shuffled(),
            hands = listOf(),
            field = Field(objects = listOf()),
            reserve = 4,
            coreTrash = 0,
            cardTrash = emptyList(),
        ),
        turnCount = 0
    ).drow(4)

    while (game.turnCount < 1) {
        game = game.turn().maxBy {
            sink.writeString("${it.toText()}\n")
            it.myBoard.field.objects.size
        }
    }
}
