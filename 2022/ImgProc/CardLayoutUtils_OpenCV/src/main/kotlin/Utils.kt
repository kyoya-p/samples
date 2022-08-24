import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.imgcodecs.Imgcodecs
import java.io.File

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("usage: ")
        throw Exception("Illegal Parameter")
    }
    val sourceDir = args[0]
    val ext = args[1]
    fun File.targetFile() = File("$path.$ext.png")

    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    File(sourceDir).walk().onEach {}
        .filter { it.isFile() && it.name.endsWith(".png") && !it.name.contains(ext) && !it.targetFile().exists() }
        .forEach {
            println(it.name)
            val srcImg = Imgcodecs.imread(it.path)!!
            val dstImg = repeat(3) { srcImg.trim(41) }.hconcat()
            Imgcodecs.imwrite(it.targetFile().path, dstImg)
        }
}

fun <T> List<T>.repeat(r: Int) = (0 until r).flatMap { this }
fun <R> repeat(r: Int, op: (Int) -> R) = (0 until r).map { op(it) }
fun List<Mat>.hconcat(): Mat {
    val dst = this[0].clone()!!
    Core.hconcat(this, dst)
    return dst
}

fun List<Mat>.vconcat(): Mat {
    val dst = this[0].clone()!!
    Core.vconcat(this, dst)
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
