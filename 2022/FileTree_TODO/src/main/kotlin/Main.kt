import org.apache.commons.compress.archivers.jar.JarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.File
import java.util.*

fun main(args: Array<String>) {
    //val file = File("./build/libs/filetree-1.0-SNAPSHOT.jar")
    //val zis = ZipArchiveInputStream(file.inputStream(), null)
    //file.unjar().forEach { println("name:${it.name} dir:${it.isDirectory}") }
    tree(File(args.getOrNull(0) ?: "."))
}

fun tree(file: File) {
    //println(file.path)
    val ext = file.extension.lowercase(Locale.getDefault())
    when {
        ext == "jar" -> {
            println("jar: ${file.path}")
            file.unjar().forEach { ent ->
                // println("${file.path} : ${ent.name}")
            }
        }
        ext == "zip" || ext == "tar" || ext == "ar" -> {

        }
        file.isDirectory -> {
            file.listFiles().forEach { child -> tree(child) }
        }
    }
}

fun unzip(file: File) = ZipArchiveInputStream(file.inputStream(), null).let { generateSequence { it.nextZipEntry } }
fun File.unjar() = JarArchiveInputStream(inputStream(), null).let { generateSequence { it.nextJarEntry } }

