import kotlinx.coroutines.flow.Flow

interface AddressRepository {
    fun getAddresses(): Flow<List<Address>>
    suspend fun addAddress(name: String, mail: String)
    suspend fun removeAddress(id: String)
}
