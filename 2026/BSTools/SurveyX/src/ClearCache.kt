package xtool

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context

class ClearCache : CliktCommand(name = "clear-cache") {
    override fun help(context: Context) = ".x キャッシュディレクトリ内のツイートJSONを全削除する"

    override fun run() {
        val deleted = Tweet.clearCache()
        echo("キャッシュをクリアしました (${deleted}件削除)")
    }
}
