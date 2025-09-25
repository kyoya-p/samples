import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toPath
import java.io.File

fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )

internal const val dataStoreFileName = ".modbusdump.preferences_pb"
val dataStoreFile = File(System.getProperty("user.home"), dataStoreFileName)

fun createDataStore(): DataStore<Preferences> = createDataStore(
    producePath = { dataStoreFile.absolutePath }
)

private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
    scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    produceFile = { dataStoreFile }
)
