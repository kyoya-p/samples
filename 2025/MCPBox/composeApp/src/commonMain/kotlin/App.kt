import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.io.File

//import mcpbox.composeapp.generated.resources.compose_multiplatform

class AppPreferences {
    companion object {
        val theme = androidx.datastore.preferences.core.stringPreferencesKey("theme")
        val fontSize = androidx.datastore.preferences.core.intPreferencesKey("font_size")
        val isDarkMode = androidx.datastore.preferences.core.booleanPreferencesKey("is_dark_mode")
    }
}

lateinit var dataStore: DataStore<Preferences>

@Composable
fun PreferenceDemo(scope: CoroutineScope) {
    dataStore = createDataStore()
    val theme by dataStore.data
        .catch { exception ->
            emit(emptyPreferences())
        }
        .map { preferences ->
            preferences[AppPreferences.theme] ?: "light"
        }
        .collectAsState(initial = "light")

    val fontSize by dataStore.data
        .map { it[AppPreferences.fontSize] ?: 12 }
        .collectAsState(initial = 12)

    val isDarkMode by dataStore.data
        .map { it[AppPreferences.isDarkMode] ?: false }
        .collectAsState(initial = false)

    Column {
        androidx.compose.material.Text("Theme: $theme")
        androidx.compose.material.Text("Font Size: $fontSize")
        androidx.compose.material.Text("Dark Mode: $isDarkMode")

        androidx.compose.material.Button(onClick = {
            scope.launch {
                dataStore.edit { preferences ->
                    preferences[AppPreferences.theme] = if (theme == "light") "dark" else "light"
                }
            }
        }) {
            Text("Toggle Theme")
        }

        Button(onClick = {
            scope.launch {
                dataStore.edit { preferences ->
                    preferences[AppPreferences.fontSize] = fontSize + 2
                }
            }
        }) {
            Text("Increase Font Size")
        }

        Button(onClick = {
            scope.launch {
                dataStore.edit { preferences ->
                    preferences[AppPreferences.isDarkMode] = !isDarkMode
                }
            }
        }) {
            Text("Toggle Dark Mode")
        }

        // Example of using kotlin.io to write to a file
        Button(onClick = {
            scope.launch(Dispatchers.IO) {
                val dataToWrite = "Current Theme: $theme, Font Size: $fontSize, Dark Mode: $isDarkMode"
                val file = File(System.getProperty("user.dir"), DATA_FILE_NAME)
                file.writeText(dataToWrite)
                println("Data written to ${file.absolutePath}")
            }
        }) {
            Text("Save Data to File (kotlin.io)")
        }

        // Example of using kotlin.io to read from a file (after a delay to ensure it's written)
        LaunchedEffect(theme, fontSize, isDarkMode) { // Re-run when preferences change
            launch(Dispatchers.IO) {
                val file = File(System.getProperty("user.dir"), DATA_FILE_NAME)
                if (file.exists()) {
                    val readData = file.readText()
                    println("Data read from file: $readData")
                }
            }
        }
    }
}

@Composable
@Preview
fun App() {
    val dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
//                val greeting = remember { Greeting().greet() }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
//                    Image(painterResource(Res.drawable.compose_multiplatform), null)
//                    Text("Compose: $greeting")
                }
            }
        }
    }
}

