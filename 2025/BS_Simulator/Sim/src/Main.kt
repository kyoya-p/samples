import kotlinx.serialization.json.Json
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import kotlinx.io.buffered

fun main() {
    val outputFile = Path("result.txt") // 出力ファイル
    SystemFileSystem.sink(outputFile).buffered().use { sink -> // buffered()を追加し、sinkの型をBufferedSinkに変更
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

        while (!game.isTerminated && game.turnCount < 10) {
            sink.writeString("--- Turn: ${game.turnCount} ---\n")

            val nextTurnPossibleStates = game.turn()
            var chosenNextState: Game? = null

            nextTurnPossibleStates.forEachIndexed { index, state ->
                sink.writeString("Choice ${index + 1}:\n")
                sink.writeString("${state.toText()}\n")
                sink.writeString("------------------------------------\n")

                // 優先順位に基づいて次の状態を選択
                if (chosenNextState == null || state.isTerminated) {
                    chosenNextState = state
                }
            }

            if (chosenNextState != null) {
                game = chosenNextState!!.copy(turnCount = game.turnCount + 1)
            } else {
                // 選択肢がない場合はゲームを終了
                game = game.copy(isTerminated = true)
            }
        }
    } // useブロックで自動的にファイルを閉じる
}
