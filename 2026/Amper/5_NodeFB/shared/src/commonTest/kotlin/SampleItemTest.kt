import com.example.model.SampleItem
import kotlin.test.Test
import kotlin.test.assertEquals

class SampleItemTest {
    @Test
    fun testSampleItemCreation() {
        val item = SampleItem(null, "Test Name", "2026-01-16T00:00:00Z")
        assertEquals("Test Name", item.name)
        assertEquals("2026-01-16T00:00:00Z", item.createdAt)
    }
}
