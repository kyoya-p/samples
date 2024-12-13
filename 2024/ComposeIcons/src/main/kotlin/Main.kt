import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

@Composable
@Preview
fun App() {
    var text by remember { mutableStateOf("Hello, World!") }

    MaterialTheme {
        Button(onClick = { text = "Hello, Desktop!" }) {
            Text(text)
        }
        Column {
            Icon(Icons.Outlined.AccountBox, "")
            Icon(Icons.Outlined.AddCircle, "")
            Icon(Icons.Outlined.AccountCircle, "")
            Icon(Icons.Outlined.ArrowDropDown, "")
            Icon(Icons.Outlined.Build, "")
            Icon(Icons.Outlined.Create, "")
            Icon(Icons.Outlined.Call, "")
            Icon(Icons.Outlined.Check, "")
            Icon(Icons.Outlined.Clear, "")
            Icon(Icons.Outlined.Close, "")
            Icon(Icons.Outlined.CheckCircle, "")
            Icon(Icons.Outlined.Delete, "")
            Icon(Icons.Outlined.Done, "")
            Icon(Icons.Outlined.DateRange, "")
            Icon(Icons.Outlined.Edit, "")
            Icon(Icons.Outlined.Email, "")
            Icon(Icons.Outlined.Face, "")
            Icon(Icons.Outlined.Favorite, "")
            Icon(Icons.Outlined.FavoriteBorder, "")
            Icon(Icons.Outlined.Home, "")
            Icon(Icons.Outlined.Info, "")
            Icon(Icons.Outlined.KeyboardArrowDown, "")
            Icon(Icons.Outlined.Lock, "")
            Icon(Icons.Outlined.MoreVert, "")
            Icon(Icons.Outlined.Menu, "")
            Icon(Icons.Outlined.MailOutline, "")
            Icon(Icons.Outlined.Notifications, "")
            Icon(Icons.Outlined.PlayArrow, "")
            Icon(Icons.Outlined.Person, "")
            Icon(Icons.Outlined.Phone, "")
            Icon(Icons.Outlined.Place, "")
            Icon(Icons.Outlined.Refresh, "")
            Icon(Icons.Outlined.Star, "")
            Icon(Icons.Outlined.Search, "")
            Icon(Icons.Outlined.Share, "")
            Icon(Icons.Outlined.Settings, "")
            Icon(Icons.Outlined.ThumbUp, "")
            Icon(Icons.Outlined.Warning, "")
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
