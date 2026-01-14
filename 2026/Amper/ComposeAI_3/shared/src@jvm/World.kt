import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.io.FileInputStream

class InMemoryAddressRepository : AddressRepository {
    private val _addresses = MutableStateFlow(
        listOf(
            Address("1", "Value 1", "Value 2"),
            Address("2", "Value 4", "Value 5"),
            Address("3", "Value 7", "Value 8"),
            Address("4", "Value 10", "Value 11")
        )
    )

    override fun getAddresses(): Flow<List<Address>> = _addresses

    override suspend fun addAddress(name: String, mail: String) {
        val current = _addresses.value
        val newId = (current.maxOfOrNull { it.id.toIntOrNull() ?: 0 } ?: 0) + 1
        _addresses.value = current + Address(newId.toString(), name, mail)
    }

    override suspend fun removeAddress(id: String) {
        _addresses.value = _addresses.value.filter { it.id != id }
    }
}

private val _repository: AddressRepository by lazy {
    val serviceAccountPath = "service-account.json"
    val file = File(serviceAccountPath)
    if (file.exists()) {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                val options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(FileInputStream(file)))
                    .build()
                FirebaseApp.initializeApp(options)
                println("Firebase initialized with service-account.json")
            }
            FirestoreAddressRepository()
        } catch (e: Exception) {
            println("Failed to initialize Firebase: ${e.message}")
            e.printStackTrace()
            InMemoryAddressRepository()
        }
    } else {
        println("service-account.json not found. Using InMemoryAddressRepository.")
        InMemoryAddressRepository()
    }
}

actual fun getWorld() = "JVM World"
actual fun getRepository(): AddressRepository = _repository
