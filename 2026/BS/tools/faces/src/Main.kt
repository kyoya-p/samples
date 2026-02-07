package faces

import bsDetail
import createClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: <card-id> [<card-id> ...]")
        return
    }

    ImageIO.scanForPlugins()

    runBlocking {
        createClient().use { client ->
            val chunks = args.toList().chunked(3)
            val allGridRows = mutableListOf<BufferedImage>()
            val allSuccessfulIds = mutableListOf<String>()

            for (chunk in chunks) {
                val images = mutableListOf<BufferedImage>()
                val successfulIds = mutableListOf<String>()
                for (rawCardId in chunk) {
                    val cardId = normalizeCardId(rawCardId)
                    try {
                        println("Fetching detail for $cardId...")
                        val (card, _) = bsDetail(client, cardId)
                        val imageUrl = card.sideA.imageUrl
                        println("Downloading image from $imageUrl...")
                        val imageBytes = client.get(imageUrl).readRawBytes()
                        val img = ImageIO.read(ByteArrayInputStream(imageBytes))
                        if (img != null) {
                            images.add(img)
                            successfulIds.add(cardId)
                        } else {
                            println("Failed to decode image for $cardId (format may not be supported)")
                        }
                    } catch (e: Exception) {
                        println("Error processing $cardId: ${e.message}")
                    }
                }

                if (images.isNotEmpty()) {
                    val gridRow = create3x3Block(images)
                    allGridRows.add(gridRow)
                    allSuccessfulIds.addAll(successfulIds)
                }
            }

            if (allGridRows.isNotEmpty()) {
                saveFinalImage(allGridRows, allSuccessfulIds)
            }
        }
    }
}

fun create3x3Block(images: List<BufferedImage>): BufferedImage {
    val count = images.size
    val width = images[0].width
    val height = images[0].height

    val totalWidth = width * count
    val rowHeight = height
    val blockHeight = rowHeight * 3

    val block = BufferedImage(totalWidth, blockHeight, BufferedImage.TYPE_INT_ARGB)
    val g = block.createGraphics()

    // 1行目
    for (i in 0 until count) {
        g.drawImage(images[i], i * width, 0, null)
    }

    // 2, 3行目にコピー
    val firstRow = block.getSubimage(0, 0, totalWidth, rowHeight)
    g.drawImage(firstRow, 0, rowHeight, null)
    g.drawImage(firstRow, 0, rowHeight * 2, null)

    g.dispose()
    return block
}

fun saveFinalImage(blocks: List<BufferedImage>, cardIds: List<String>) {
    val maxWidth = blocks.maxOf { it.width }
    val totalHeight = blocks.sumOf { it.height }

    val finalImage = BufferedImage(maxWidth, totalHeight, BufferedImage.TYPE_INT_ARGB)
    val g = finalImage.createGraphics()

    var currentY = 0
    for (block in blocks) {
        g.drawImage(block, 0, currentY, null)
        currentY += block.height
    }
    g.dispose()

    val fileName = "combined_" + cardIds.take(5).joinToString("_").replace(":", "-").replace("/", "-") + (if (cardIds.size > 5) "_etc" else "") + ".png"
    val outputDir = File("output")
    if (!outputDir.exists()) outputDir.mkdirs()
    val outputFile = File(outputDir, fileName)
    ImageIO.write(finalImage, "png", outputFile)
    println("Saved ${outputFile.absolutePath}")
}

fun normalizeCardId(id: String): String {
    val upperId = id.uppercase()
    val parts = upperId.split("-")
    if (parts.size == 2) {
        val numberPart = parts[1]
        // 数字のみで構成され、かつ長さが3未満の場合にゼロ埋め
        if (numberPart.all { it.isDigit() } && numberPart.length < 3) {
            return "${parts[0]}-${numberPart.padStart(3, '0')}"
        }
    }
    return upperId
}
