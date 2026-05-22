package com.fishlog.app.util

import org.junit.Assert.*
import org.junit.Test

class WaterBodyNameUtilsTest {

    @Test
    fun normalizeTrimsAndIgnoresCase() {
        val input1 = " Oak Hollow "
        val input2 = "oak hollow"
        assertEquals(WaterBodyNameUtils.normalize(input1), WaterBodyNameUtils.normalize(input2))
    }

    @Test
    fun detectsSimilarLakeNames() {
        val a = "Oak Hollow"
        val b = "Oak Hollow Lake"
        assertTrue(WaterBodyNameUtils.areSimilar(a, b))
    }

    @Test
    fun doesNotOvermatchDifferentNames() {
        val a = "Oak Hollow"
        val b = "High Rock Lake"
        assertFalse(WaterBodyNameUtils.areSimilar(a, b))
    }

    @Test
    fun ignoresBlankValues() {
        assertNull(WaterBodyNameUtils.findBestSuggestion("", listOf("Oak Hollow")))
        assertNull(WaterBodyNameUtils.findBestSuggestion("   ", listOf("Oak Hollow")))
    }

    @Test
    fun bestSuggestionReturnsExpectedName() {
        val existing = listOf("Oak Hollow Lake", "High Rock Lake")
        val input = "Oak Hollow"
        val suggestion = WaterBodyNameUtils.findBestSuggestion(input, existing)
        assertEquals("Oak Hollow Lake", suggestion)
    }
}
