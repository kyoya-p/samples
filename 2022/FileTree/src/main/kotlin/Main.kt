import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.io.File
import java.util.*

fun main(args: Array<String>) {
    File(args.getOrNull(0) ?: ".").walk().forEach { file ->
        when (file.extension.lowercase(Locale.getDefault())) {
            "jar", "zip" -> {
                println("File: ${file.path}")
                val zipFile = ZipFile(file)
                zipFile.entries.asSequence().forEach { f -> println(f.name) }
            }
        }
    }
}
