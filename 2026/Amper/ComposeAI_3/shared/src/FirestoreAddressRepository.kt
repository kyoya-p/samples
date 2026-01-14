import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirestoreAddressRepository : AddressRepository {
    private val db = Firebase.firestore
    private val collection = db.collection("addresses")

    override fun getAddresses(): Flow<List<Address>> {
        return collection.snapshots.map { querySnapshot ->
            querySnapshot.documents.map { doc ->
                try {
                    val data = doc.data<Map<String, String>>()
                    Address(
                        id = doc.id,
                        name = data["name"] ?: "",
                        mail = data["mail"] ?: ""
                    )
                } catch (e: Exception) {
                    // Fallback or log error
                    Address(doc.id, "Error", "Error parsing data")
                }
            }
        }
    }

    override suspend fun addAddress(name: String, mail: String) {
        val data = mapOf("name" to name, "mail" to mail)
        collection.add(data)
    }

    override suspend fun removeAddress(id: String) {
        collection.document(id).delete()
    }
}
