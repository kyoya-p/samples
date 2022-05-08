import org.opencv.core.Core
import org.opencv.imgcodecs.Imgcodecs
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

val File.attrs get() = Files.readAttributes(this.toPath(), BasicFileAttributes::class.java)

fun main(args: Array<String>) {
    val interval = args.getOrNull(0)?.toInt() ?: 0
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

    val newExt = "x3.png"
    fun File.targetFile() = File("$path.$newExt")
    do {
        File(".").walk().onEach {}.filter { file ->
            file.isFile()
                    && file.extension == "png"
                    && !file.name.endsWith(newExt)
                    && (!file.targetFile().exists()
                    || file.attrs.lastModifiedTime() > file.targetFile().attrs.lastModifiedTime()
                    || file.attrs.creationTime() > file.targetFile().attrs.lastModifiedTime()
                    )
        }.forEach { file ->
            val srcImg = Imgcodecs.imread(file.path)!!
            val dstImg = srcImg.trim(41).tile3()
            Imgcodecs.imwrite(file.targetFile().path, dstImg)
            println("${file.path} => ${file.targetFile().name}")
        }
        if (interval == 0) break
        Thread.sleep(interval * 1000L)
    } while (true)
}
