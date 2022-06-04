import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import org.opencv.core.Core
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgcodecs.Imgcodecs.imwrite
import java.io.File

fun main(args: Array<String>) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    val resImg = args.map { bsImage(it) }.repeat(9).take(9)
        .chunked(3)
        .map { img3 -> img3.hconcat() }
        .vconcat()
    imwrite("out.jpg", resImg)
}

fun bsImage(cardId: String) = runBlocking {
    val img = HttpClient().get("https://batspi.com/card/$cardId.jpg").readBytes()
    File("build/tmp.jpg").writeBytes(img)
    Imgcodecs.imread("build/tmp.jpg")!!
}
