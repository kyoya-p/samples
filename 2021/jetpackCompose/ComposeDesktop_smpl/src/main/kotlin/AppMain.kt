import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

enum class NavType {
    HOME, SEARCH, LIBRARY
}

@Composable
@Preview
fun AppMain() {

    MaterialTheme {
        val navItemState = remember { mutableStateOf(NavType.HOME) }
        Column {
            TopAppBar(
                title = { Text(text = "AppMain") },
                elevation = 8.dp,
            )
            {
                when (navItemState.value) {
                    NavType.HOME -> SampleField()
                }
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

