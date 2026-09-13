package bstools.model

import bstools.kann.kann_layer_input
import bstools.kann.kann_layer_dense
import bstools.kann.kann_layer_cost
import bstools.kann.KANN_C_MSE
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * cinterop経由でKANN(C言語のNNライブラリ)を実際にリンクできているかを確認する
 * 最小限のスモークテスト。入力層→全結合層→コスト層のグラフを組み立て、
 * 生成されたノードがnullでないことだけを確認する(学習・推論はまだ行わない)。
 */
@OptIn(ExperimentalForeignApi::class)
fun kannSmokeTest(): String {
    val input = kann_layer_input(4)
        ?: return "NG: kann_layer_input returned null"
    val dense = kann_layer_dense(input, 3)
        ?: return "NG: kann_layer_dense returned null"
    val cost = kann_layer_cost(dense, 3, KANN_C_MSE)
        ?: return "NG: kann_layer_cost returned null"
    return "OK: input=$input dense=$dense cost=$cost"
}
