import java.io.File
import java.util.*

fun main(args: Array<String>) {
    File(args.getOrNull(0) ?: ".").walk().forEach { file ->
        when (file.extension.lowercase(Locale.getDefault())) {
            "jar" -> {}
            "zip" -> {
                
            }
        }
    }
}