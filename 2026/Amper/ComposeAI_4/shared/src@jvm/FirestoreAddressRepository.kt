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
                    doc.data<Address>().copy(id = doc.id)
                } catch (e: Exception) {
                    Address(doc.id, "Error", "Error parsing: ${e.message}")
                }
            }
        }
    }

    override suspend fun addAddress(name: String, mail: String) {
        collection.add(Address(name = name, mail = mail))
    }

            override suspend fun removeAddress(id: String) {

                collection.document(id).delete()

            }

        }

        

    