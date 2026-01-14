import androidx.compose.ui.test.*
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalTestApi::class)
class UiTest {
    @Test
    fun testAddAndDeleteAddress() = runComposeUiTest {
        val repo = FakeAddressRepository()
        setContent {
            Screen(repository = repo)
        }

        // Verify initial state (empty)
        onNodeWithTag("InputName").assertExists()
        
        // Add new address
        onNodeWithTag("InputName").performTextInput("Test User")
        onNodeWithTag("InputMail").performTextInput("test@example.com")
        onNodeWithTag("AddButton").performClick()

        // Verify added address appears
        onNodeWithText("Test User").assertExists()
        onNodeWithText("test@example.com").assertExists()

        // Delete the address
        onNodeWithText("✕").performClick()

        // Verify address is removed
        onNodeWithText("Test User").assertDoesNotExist()
    }
}

class FakeAddressRepository : AddressRepository {
    private val _addresses = MutableStateFlow<List<Address>>(emptyList())

    override fun getAddresses(): Flow<List<Address>> = _addresses

    override suspend fun addAddress(name: String, mail: String) {
        val current = _addresses.value
        val newId = (current.size + 1).toString()
        _addresses.value = current + Address(id = newId, name = name, mail = mail)
    }

    override suspend fun removeAddress(id: String) {
        _addresses.value = _addresses.value.filter { it.id != id }
    }
}