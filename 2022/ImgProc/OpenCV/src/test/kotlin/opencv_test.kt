import org.junit.jupiter.api.Test
import org.opencv.core.Core
import org.opencv.core.Core.hconcat
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.highgui.HighGui.imshow
import org.opencv.imgcodecs.Imgcodecs.imread
import org.opencv.imgcodecs.Imgcodecs.imwrite
import java.io.File


@Suppress("TestFunctionName", "NonAsciiCharacters")
class TestOpenCV {
    @Test
    fun 画像型_二次元配列() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
        val mat = Mat.eye(3, 3, CvType.CV_8UC1)
        System.out.println("mat = " + mat.dump())
    }

    @Test
    fun 画像表示_失敗() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
        val mat = Mat.eye(3, 3, CvType.CV_8UC1)
        imshow("A", mat)
    }

    @Test
    fun 画像切抜x3() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
        File("\\\\192.168.11.14\\public\\tmp\\aaaa").walk().filter { it.name.endsWith(".out.png") }.forEach {
            println(it.name)
            val srcImg = imread(it.path)!!
            val dstImg = srcImg.trim(41).tile3()
            imwrite("build/${it.name}", dstImg)
        }
    }

    fun Mat.tile3(): Mat {
        val dst = clone()!!
        hconcat(listOf(this, this, this), dst)
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

    @Test
    fun 文字認識() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
        //TODO
    }
}