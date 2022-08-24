//import java.io.File
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.File
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Paths
import java.util.*


fun main(args: Array<String>) {
    tree(File(args.getOrNull(0) ?: "."))
}

fun tree(file: File) {
    val ext = file.extension.lowercase(Locale.getDefault())
    when {
        ext == "jar" -> {
            println("jar: ${file.path}")
            jarWalk(file)
        }

        ext == "zip" || ext == "tar" || ext == "ar" -> {

        }

        file.isDirectory -> {
            file.listFiles().forEach { child -> tree(child) }
        }
        //else -> println(file.path)
    }
}

fun unzip(file: File) = ZipArchiveInputStream(file.inputStream(), null).let { generateSequence { it.nextZipEntry } }
fun File.unjar() = JarArchiveInputStream(inputStream(), null).let { generateSequence { it.nextJarEntry } }

fun jarWalk(file: File) {
    val uri = URI.create("jar:${Paths.get(file.path).toUri()}")
    val env: Map<String, Any> = HashMap()
    val fs: FileSystem = FileSystems.newFileSystem(uri, env)
    fs.rootDirectories.forEach { d ->
        println(d.nameCount)
        println(d.getName(1))
    }

}
/*
fun zipLines(zipFilePath: Path?, pathPredicate: Predicate<Path?>?): Stream<String?>? {
    val fs: FileSystem = FileSystems.newFileSystem(zipFilePath, ClassLoader.getSystemClassLoader())
    return StreamSupport.stream(fs.getRootDirectories().spliterator(), false)
        .onClose(Runnable {
            // Stream終了時にFileSystemをクローズする
            try {
                fs.close()
            } catch (e: IOException) {
                throw UncheckedIOException(e)
            }
        }).flatMap<Any>(Function<T, Stream<*>> { rootPath: T? ->
            // zipファイル内のエントリー一覧を取得する
            try {
                return@flatMap Files.walk(rootPath)
            } catch (e: IOException) {
                throw UncheckedIOException(e)
            }
        }).filter(Predicate { path: Any? -> !Files.isDirectory(path) })
        .filter(pathPredicate)
        .flatMap<Any>(Function<Any, Stream<*>> { path: Any? ->
            // zipエントリーをUTF-8のテキストファイルとしてオープンし、Stream<String>を返す
            try {
                return@flatMap Files.lines(path)
            } catch (e: IOException) {
                throw UncheckedIOException(e)
            }
        })
}
*/
