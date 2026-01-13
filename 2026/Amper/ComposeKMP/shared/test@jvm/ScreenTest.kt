import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

class ScreenTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testListIsDisplayedAndCapture() = runComposeUiTest {
        setContent {
            Screen()
        }

        onNodeWithText("Assets").assertIsDisplayed()
        
        // "Name" appears in Header and Input Label
        onAllNodesWithText("Name").assertCountEquals(2)
        
        // "Mail" appears in Header and Input Label
        onAllNodesWithText("Mail").assertCountEquals(2)

        onNodeWithText("Value 1").assertIsDisplayed()

        // Capture screenshot
        try {
            val bitmap = onRoot().captureToImage()
            val image = bitmap.toAwtImage()
            val tempDir = File("../build/temp")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }
            val file = File(tempDir, "screen_capture.png")
            ImageIO.write(image, "png", file)
            println("Screenshot saved to: ${file.absolutePath}")
        } catch (e: Exception) {
            println("Failed to capture screenshot: ${e.message}")
            e.printStackTrace()
        }
    }
}