import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem

@OptIn(DelicateCoroutinesApi::class)
fun main() {
    GlobalScope.launch {
        appMain()
    }
}

