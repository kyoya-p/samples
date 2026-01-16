import kotlin.test.Test
import kotlin.test.assertEquals

class BSBCTest {
    @Test
    fun testParseAttributes() {
        // Single Short Code
        assertEquals(listOf("赤"), parseAttributes(listOf("R")))
        assertEquals(listOf("紫"), parseAttributes(listOf("P")))
        
        // Multiple Short Codes in one string
        assertEquals(listOf("赤", "紫"), parseAttributes(listOf("RP")))
        assertEquals(listOf("赤", "白", "青"), parseAttributes(listOf("RWB")))
        
        // Japanese
        assertEquals(listOf("赤"), parseAttributes(listOf("赤")))
        
        // Mixed
        assertEquals(listOf("赤", "紫"), parseAttributes(listOf("R", "紫")))
        
        // Invalid/Unknown (should return as is)
        assertEquals(listOf("Z"), parseAttributes(listOf("Z")))
        // "Red" is not a valid short code key (keys are single chars) nor 'e'/'d' are valid keys? 
        // Wait, "Red" logic: check all chars. 'R' is valid, 'e' is not. So it goes to else -> returns "Red".
        assertEquals(listOf("Red"), parseAttributes(listOf("Red")))
    }

    @Test
    fun testParseCategories() {
        // Single Short Code
        assertEquals(listOf("スピリット"), parseCategories(listOf("S")))
        assertEquals(listOf("アルティメット"), parseCategories(listOf("U")))
        
        // Multiple separate inputs
        assertEquals(listOf("スピリット", "アルティメット"), parseCategories(listOf("S", "U")))
        
        // Japanese
        assertEquals(listOf("スピリット"), parseCategories(listOf("スピリット")))
        
        // Combined Short Codes (Logic check: parseCategories handles single char strings only)
        // If input is "SU", length is 2. Condition `input.length == 1` fails. Returns "SU".
        assertEquals(listOf("SU"), parseCategories(listOf("SU")))
        
        // Invalid
        assertEquals(listOf("Z"), parseCategories(listOf("Z")))
    }
}