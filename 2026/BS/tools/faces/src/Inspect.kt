package faces

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: <image-path> <number-of-cards-x>")
        return
    }

    val imagePath = args[0]
    val numCardsX = if (args.size > 1) args[1].toInt() else 3
    
    println("Inspecting image: $imagePath")
    
    val inputFile = File(imagePath)
    if (!inputFile.exists()) {
        println("Error: File not found.")
        return
    }

    val original = ImageIO.read(inputFile)
    
    // 1. 全体縮小画像の生成 (幅800px)
    val overviewWidth = 800
    val overviewHeight = (original.height.toDouble() / original.width * overviewWidth).toInt()
    val overview = BufferedImage(overviewWidth, overviewHeight, BufferedImage.TYPE_INT_ARGB)
    val g = overview.createGraphics()
    g.drawImage(original, 0, 0, overviewWidth, overviewHeight, null)
    g.dispose()
    
    val overviewFile = File(inputFile.parent, "inspect_overview.png")
    ImageIO.write(overview, "png", overviewFile)
    println("Created overview: ${overviewFile.name}")

    // 2. 各カードのIDエリア切り出し
    // 画像は「横3枚」×「縦3リピート」の構成
    // 1行目の3枚を確認すれば十分
    
    val cardWidth = original.width / numCardsX
    // 縦リピートは3回と仮定
    val cardHeight = original.height / 3 
    
    // IDは通常、カードの右下にある (高さの85%~98%, 幅の60%~95%あたりを狙う)
    val cropXRatio = 0.60
    val cropYRatio = 0.88
    val cropWRatio = 0.35
    val cropHRatio = 0.10
    
    val cropX = (cardWidth * cropXRatio).toInt()
    val cropY = (cardHeight * cropYRatio).toInt()
    val cropW = (cardWidth * cropWRatio).toInt()
    val cropH = (cardHeight * cropHRatio).toInt()

    for (i in 0 until numCardsX) {
        val offsetX = i * cardWidth
        // 境界チェック
        val safeW = if (offsetX + cropX + cropW > original.width) original.width - (offsetX + cropX) else cropW
        
        if (safeW > 0) {
            val crop = original.getSubimage(offsetX + cropX, cropY, safeW, cropH)
            val cropFile = File(inputFile.parent, "inspect_crop_$i.png")
            ImageIO.write(crop, "png", cropFile)
            println("Created crop $i: ${cropFile.name}")
        }
    }
}
