// src/jvmMain/kotlin/Main.kt
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import java.io.File


fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )

internal const val dataStoreFileName = "dice.preferences_pb"

fun createDataStore(): DataStore<Preferences> = createDataStore {
    val file = File(System.getProperty("java.io.tmpdir"), dataStoreFileName)
    file.absolutePath
}
