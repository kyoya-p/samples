import com.jakewharton.mosaic.layout.padding
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.runMosaic
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Text
import kotlinx.coroutines.runBlocking

suspend fun main(): Unit = runBlocking {
    runMosaic {
        Box(modifier = Modifier. padding(1)) {
            Text("Mosaicで表示された枠線の中のテキスト")
        }
    }
}