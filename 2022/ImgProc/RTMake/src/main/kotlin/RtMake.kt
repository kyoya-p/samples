import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.imgcodecs.Imgcodecs
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

val File.attrs get() = Files.readAttributes(this.toPath(), BasicFileAttributes::class.java)

fun main(args: Array<String>) {
    val monitoringDir = File(args.getOrNull(0)?: ".").absoluteFile
    val interval = args.getOrNull(1)?.toInt() ?: 3
    val libopencv="opencv_java420" // Core.NATIVE_LIBRARY_NAME
    System.loadLibrary(libopencv)

    println("Start monitoring directory: ${monitoringDir.path}")
    val newExt = "x3.png"
    fun File.targetFile() = File("$path.$newExt")
    do {
        monitoringDir.walk().onEach {}.filter { file ->
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

fun Mat.tile3(): Mat {
    val dst = clone()!!
    Core.hconcat(listOf(this, this, this), dst)
    return dst
}

fun Mat.trim(w: Int): Mat {
    val left = w
    val top = w
    val width = width() - w - left
    val height = height() - w - top
    val rect = Rect(left, top, width, height)
    return Mat(this, rect)
}
