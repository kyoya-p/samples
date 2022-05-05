import net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel.RIL_WORD
import net.sourceforge.tess4j.Tesseract
import java.awt.Color
import java.awt.Image.SCALE_AREA_AVERAGING
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_3BYTE_BGR
import java.io.File
import javax.imageio.ImageIO.read
import javax.imageio.ImageIO.write


fun main() {

    val tess = Tesseract()
    tess.setDatapath("build/tess/Tess4J/tessdata")
    tess.setLanguage("eng")

    tess.setVariable("tessedit_char_whitelist", "SAMPLE")
//    tess.setVariable("crunch_del_high_word", "15.0")
//    tess.setVariable("crunch_del_low_word", "8.0")
    val r1 = tess.doOCR(File("build/samples/s1.jpg"))!!
    println(r1)

    val bi = read(File("build/samples/s1.jpg"))!!
    val r2 = tess.getSegmentedRegions(bi, 0).mapNotNull { it }
    println(r2)

    val w2 = bi.width * 100 / 800
    val h2 = bi.height * 100 / 800
    val scaledImg = BufferedImage(w2, h2, TYPE_3BYTE_BGR)
    scaledImg.createGraphics().drawImage(bi.getScaledInstance(w2, h2, SCALE_AREA_AVERAGING), 0, 0, w2, h2, null)

    val r3 = tess.getWords(scaledImg, RIL_WORD).mapNotNull { it }
    val g = scaledImg.graphics!!
    r3.forEach {
        val r = it.boundingBox
        g.color = Color.RED
        g.drawRect(r.x, r.y, r.width, r.height)
        g.drawString(it.text, r.x, r.y)
    }
    write(scaledImg, "jpg", File("./build/samples/s1.jpg.out.jpg"))
    println(r3)

}
