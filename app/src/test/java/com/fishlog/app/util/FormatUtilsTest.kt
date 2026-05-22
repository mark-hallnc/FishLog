package com.fishlog.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun testFormatWholeNumber() {
        assertEquals("72", FormatUtils.formatWholeNumber(72.428))
        assertEquals("73", FormatUtils.formatWholeNumber(72.5))
        assertEquals("—", FormatUtils.formatWholeNumber(null))
    }

    @Test
    fun testFormatDecimal() {
        assertEquals("14", FormatUtils.formatDecimal(14.0))
        assertEquals("14.26", FormatUtils.formatDecimal(14.2567))
        assertEquals("3.8", FormatUtils.formatDecimal(3.8))
        assertEquals("—", FormatUtils.formatDecimal(null))
    }

    @Test
    fun testFormatLength() {
        assertEquals("14 in", FormatUtils.formatLength(14.0))
        assertEquals("14.26 in", FormatUtils.formatLength(14.2567))
        assertEquals("—", FormatUtils.formatLength(null))
    }

    @Test
    fun testFormatWeight() {
        assertEquals("3 lb", FormatUtils.formatWeight(3.0))
        assertEquals("3 lb 12 oz", FormatUtils.formatWeight(3.7567))
        assertEquals("—", FormatUtils.formatWeight(null))
    }

    @Test
    fun testFormatWaterTemp() {
        assertEquals("72°F", FormatUtils.formatWaterTemp(72.428))
        assertEquals("73°F", FormatUtils.formatWaterTemp(72.5))
        assertEquals("—", FormatUtils.formatWaterTemp(null))
    }

    @Test
    fun testFormatDepth() {
        assertEquals("8 ft", FormatUtils.formatDepth(7.9999))
        assertEquals("5 ft", FormatUtils.formatDepth(5.0))
        assertEquals("—", FormatUtils.formatDepth(null))
    }
}
