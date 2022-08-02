import org.apache.commons.compress.archivers.jar.JarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.File
import java.nio.file.*
import java.util.*
import kotlin.io.path.isDirectory


fun main(args: Array<String>) {
    tree(File(args.getOrNull(0) ?: "."))
}

fun tree(file: File) {
    val ext = file.extension.lowercase(Locale.getDefault())
    when {
        ext == "jar" -> {
            println("jar: ${file.path}")
            val path = Paths.get(file.path)
            val fs1: FileSystem = FileSystems.newFileSystem(path, ClassLoader.getSystemClassLoader())
            val fs: FileSystem = FileSystems.newFileSystem(path, null as ClassLoader?)
            fs.getRootDirectories().forEach { path ->
                val r = path.iterator().forEach {
                    println(it.fileName)
                    //TODO
                }
            }
//            file.unjar().forEach { ent -> }
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
