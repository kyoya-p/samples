import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import your_project_name.composeapp.generated.resources.Res
import your_project_name.composeapp.generated.resources.your_custom_font_regular
import your_project_name.composeapp.generated.resources.your_custom_font_bold

val appFontFamily = FontFamily(
    Font(Res.font.your_custom_font_regular, FontWeight.Normal),
    Font(Res.font.your_custom_font_bold, FontWeight.Bold)
)

val AppTypography = Typography(
    h1 = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp
    ),
    body1 = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    )
    // 他のテキストスタイルも同様に定義
)
