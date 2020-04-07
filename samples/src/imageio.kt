import java.io.File
import javax.imageio.ImageIO

// TODO

fun main(args: Array<String>) {
    val inputname = "入力ファイル.bmp"
    val outputname = "出力ファイル.png"

    val bImage = ImageIO.read(File(inputname))
    ImageIO.write(bImage, "png", File(outputname))
}
