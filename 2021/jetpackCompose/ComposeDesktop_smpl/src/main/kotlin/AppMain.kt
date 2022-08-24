import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Magenta
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.skiko.currentSystemTheme

enum class NavType {
    HOME, SEARCH, LIBRARY
}

@Composable
@Preview
fun AppMain() {
    fun rgb(r: Int, g: Int, b: Int) = Color(r, g, b)
    // https://material.io/design/color/dark-theme.html
    val tealColor = lightColors(
        // https://materialui.co/colors
        primary = rgb(0, 150, 136),
        primaryVariant = rgb(0, 137, 123),
        secondary = rgb(109, 76, 65),
        secondaryVariant = rgb(93, 64, 55),
        background = rgb(236, 239, 241),
        surface = Color.White,
        error = rgb(233, 30, 99),
    )

    MaterialTheme(colors = tealColor) {
        val navItemState = remember { mutableStateOf(NavType.HOME) }

        val scaffoldState = rememberScaffoldState()
        val scope = rememberCoroutineScope()
        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                TopAppBar {
                    IconButton(onClick = {
                        scope.launch {
                            scaffoldState.drawerState.apply {
                                if (isClosed) open() else close()
                            }
                        }
                    }) { Icon(Icons.Filled.Menu, "menu") }
                    Text("Compose Showcase")
                }
            },
            drawerContent = {
                Text("Categories", modifier = Modifier.padding(16.dp))
                Divider()
                Button(onClick = { navItemState.value = NavType.HOME }) { Text("Form/Field") }
                Button(onClick = { navItemState.value = NavType.SEARCH }) { Text("Form/Field2") }
                Button(onClick = { navItemState.value = NavType.LIBRARY }) { Text("Form/Field3") }
            },
        ) {

            when (navItemState.value) {
                NavType.HOME -> SampleField()
            }

            BottomNavigation(backgroundColor = MaterialTheme.colors.surface) {
                BottomNavigationItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    selected = navItemState.value == NavType.HOME,
                    onClick = { navItemState.value = NavType.HOME },
                    label = { Text("Home") },
                )
                BottomNavigationItem(
                    icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    selected = navItemState.value == NavType.SEARCH,
                    onClick = { navItemState.value = NavType.SEARCH },
                    label = { Text("Search") }
                )
                BottomNavigationItem(
                    icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = "LibraryMusic") },
                    selected = navItemState.value == NavType.LIBRARY,
                    onClick = { navItemState.value = NavType.LIBRARY },
                    label = { Text("Your Library") }
                )
            }
        }
    }
}

