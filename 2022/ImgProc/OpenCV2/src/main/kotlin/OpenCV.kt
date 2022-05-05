import org.opencv.core.*
import org.opencv.core.Core.hconcat
import org.opencv.highgui.HighGui.imshow
import org.opencv.imgcodecs.Imgcodecs.imread
import org.opencv.imgcodecs.Imgcodecs.imwrite
import org.opencv.imgproc.Imgproc
import org.opencv.ximgproc.Ximgproc.createFastLineDetector
import java.io.File


fun main() {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    val srcImgFile = "build/samples/s1.jpg"
    val srcImg = imread("build/samples/s1.jpg")!!

    // 前処理
    val grayImg = srcImg.グレースケール().エッジ抽出()

    val r = grayImg.円弧抽出()
    (0 until r.cols()).map { r[0, it] }.forEach {
        val p = Point(it)
        val rho = it[2].toInt()
        Imgproc.circle(grayImg, p, rho, Scalar(255.0, 0.0, 0.0), 2)
    }

    //grayImg.線分検出()

    imwrite(srcImgFile + ".out.jpg", grayImg)
}

@Suppress("FunctionName", "unused", "NonAsciiCharacters")
fun Mat.円弧抽出(): Mat {
    val circles = Mat()
    Imgproc.HoughCircles(this, circles, Imgproc.CV_HOUGH_GRADIENT, 100.0, 1.0, 190.0, 180.0, 25, 55)
    return circles
}

@Suppress("FunctionName", "unused", "NonAsciiCharacters")
fun Mat.線分検出0_TODO(): Mat {
    // TODO 実行時エラー
    val d = createFastLineDetector(20, 1.5F, 50.0, 50.0, 3, false)!!
    val lines = Mat()
    d.detect(this, lines)
    val result = Mat()
    d.drawSegments(this, result)
    return result
}

@Suppress("FunctionName", "unused", "NonAsciiCharacters")
fun Mat.エッジ抽出(): Mat {
    val edge = Mat()
    Imgproc.Canny(this, edge, 10.0, 160.0)
    return edge
}

@Suppress("FunctionName", "unused", "NonAsciiCharacters")
fun Mat.グレースケール(): Mat {
    val gray = Mat()
    Imgproc.cvtColor(this, gray, Imgproc.COLOR_RGB2GRAY)
    return gray
}

@Suppress("FunctionName", "unused", "NonAsciiCharacters")
fun 画像型_二次元配列() {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    val mat = Mat.eye(3, 3, CvType.CV_8UC1)
    System.out.println("mat = " + mat.dump())
}

@Suppress("FunctionName", "unused", "NonAsciiCharacters")
fun 画像表示_失敗() {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    val mat = Mat.eye(3, 3, CvType.CV_8UC1)
    imshow("A", mat)
}

@Suppress("FunctionName", "unused", "NonAsciiCharacters")
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
