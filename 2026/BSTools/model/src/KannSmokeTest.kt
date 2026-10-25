package bstools.model

import bstools.kann.*
import kotlinx.cinterop.*

/**
 * cinterop経由でKANN(C言語のNNライブラリ)を実際にリンクできているか、および
 * `KannValueNetwork.trainStep` (BPTT誤差逆伝播) がクラッシュせず安定動作するかを確認するテスト。
 */
@OptIn(ExperimentalForeignApi::class)
fun kannSmokeTest(): String {
    val net = KannValueNetwork()
    val state = createInitialGameState(seed = 1L)
    val tokens = buildTokenSequence(state.player1, state.player2)
    val globals = buildGlobalFeatures(state, state.player1, state.player2)

    val beforeEval = net.evaluate(tokens, globals)
    var lastCost = 0f
    for (step in 1..100) {
        lastCost = net.trainStep(tokens, globals, label = 1.0, lr = 0.01f)
    }
    val afterEval = net.evaluate(tokens, globals)
    net.delete()
    return "OK: BPTT 100 steps completed! eval: $beforeEval -> $afterEval (lastCost=$lastCost)"
}


