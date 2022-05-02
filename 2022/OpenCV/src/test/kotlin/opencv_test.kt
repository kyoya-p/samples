import org.junit.jupiter.api.Test
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.imgcodecs.Imgcodecs.imread
import org.opencv.imgcodecs.Imgcodecs.imwrite


class TestOpenCVtext {
    @Test
    fun test1() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
        val mat = Mat.eye(3, 3, CvType.CV_8UC1)
        System.out.println("mat = " + mat.dump())
    }

    @Test
    fun test2() {
        val x = 41
        val y = 41
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
        val srcImg = imread("samples/5.jpg")!!
        val width = srcImg.width() - 41 - x
        val height = srcImg.height() - 41 - y
        val rect = Rect(x, y, width, height)
        val cloppedImg = Mat(srcImg, rect)
        imwrite("build/out.png", cloppedImg)
    }
}