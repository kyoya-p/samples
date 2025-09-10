package v2

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import girder.library.resources.MPLUS1Code_Medium
import girder.library.resources.Res
import org.jetbrains.compose.resources.Font

@Composable
actual fun defaultTextStyle(): TextStyle {
    val customFont = FontFamily(Font(Res.font.MPLUS1Code_Medium, FontWeight.Normal))
    return TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        fontFamily = customFont,
    )
}