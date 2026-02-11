
expect fun getWorld(): String
// 描画ロジックを渡してアプリを開始する
fun runApp(renderer: () -> String) {
    // 内部でプラットフォーム固有の処理を呼ぶ
    startFtxuiLoop(renderer)
}

expect fun startFtxuiLoop(renderer: () -> String)
