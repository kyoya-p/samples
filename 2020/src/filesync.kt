import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*


// REF: https://docs.oracle.com/cd/E26537_01/tutorial/essential/io/notification.html
// REF: https://docs.oracle.com/javase/tutorial/essential/io/examples/WatchDir.java

fun main(args: Array<String>) {
    val watcher = FileSystems.getDefault().newWatchService()
//    val dir = Paths.get("\\\\win81\\priv1\\priv.2020")
    val dir = Paths.get("test\\test1")
    val key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
    while (true) {
        //監視キーの送信を待機
        var key = watcher.take()
        for (event in key.pollEvents()) {
            val kind = event.kind()
            //この監視キーが登録されるイベントはENTRY_CREATEイベントだけですが、
            //イベントが消失したり破棄されたりした場合は、OVERFLOWイベントが
            //発生することがあります。
            if (kind === OVERFLOW) {
                continue
            }

            //ファイル名はイベントのコンテキストです。
            val ev = event as WatchEvent<Path>
            val filename = ev.context()
            val child: Path = dir.resolve(filename)
            val file = child.toFile()
            println("KIND:${kind.name()}, PATH:${child}, DIR:${file.isDirectory}")
        }
        //監視キーをリセットします。この手順は、この後さらに監視イベントを取得する場合は
        //非常に重要です。 監視キーが有効ではない場合は、ディレクトリに
        //アクセスできないため、ループを終了します。
        val valid = key.reset()
        if (!valid) {
            break
        }
    }
}
