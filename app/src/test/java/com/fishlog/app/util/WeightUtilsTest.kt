package com.fishlog.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WeightUtilsTest {

    @Test
    fun testPoundsOuncesToDecimalPounds() {
        assertEquals(1.1875, WeightUtils.poundsOuncesToDecimalPounds(1, 3), 0.0001)
        assertEquals(0.75, WeightUtils.poundsOuncesToDecimalPounds(0, 12), 0.0001)
        assertEquals(2.5, WeightUtils.poundsOuncesToDecimalPounds(2, 8), 0.0001)
    }

    @Test
    fun testDecimalPoundsToPoundsOunces() {
        val po1 = WeightUtils.decimalPoundsToPoundsOunces(1.1875)
        assertEquals(1, po1?.pounds)
        assertEquals(3, po1?.ounces)

        val po2 = WeightUtils.decimalPoundsToPoundsOunces(2.5)
        assertEquals(2, po2?.pounds)
        assertEquals(8, po2?.ounces)

        val po3 = WeightUtils.decimalPoundsToPoundsOunces(0.75)
        assertEquals(0, po3?.pounds)
        assertEquals(12, po3?.ounces)
    }

    @Test
    fun testFormatWeightPoundsOunces() {
        assertEquals("1 lb 3 oz", WeightUtils.formatWeightPoundsOunces(1.1875))
        assertEquals("12 oz", WeightUtils.formatWeightPoundsOunces(0.75))
        assertEquals("2 lb 8 oz", WeightUtils.formatWeightPoundsOunces(2.5))
        assertEquals("1 lb", WeightUtils.formatWeightPoundsOunces(1.0))
        assertEquals("0 lb", WeightUtils.formatWeightPoundsOunces(0.0))
        assertEquals("—", WeightUtils.formatWeightPoundsOunces(null))
    }

    @Test
    fun testRoundingHandlesUnusualDecimals() {
        // 1.2 lb -> 1.2 * 16 = 19.2 oz -> 1 lb 3.2 oz -> 1 lb 3 oz
        assertEquals("1 lb 3 oz", WeightUtils.formatWeightPoundsOunces(1.2))
        
        // 1.24 lb -> 1.24 * 16 = 19.84 oz -> 1 lb 3.84 oz -> 1 lb 4 oz
        assertEquals("1 lb 4 oz", WeightUtils.formatWeightPoundsOunces(1.24))
    }
}
