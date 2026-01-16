package com.example.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SampleItemTest {
    @Test
    fun testSampleItemCreation() {
        val item = SampleItem(name = "Test Item", createdAt = "2026-01-01")
        assertEquals("Test Item", item.name)
        assertEquals("2026-01-01", item.createdAt)
    }
}
