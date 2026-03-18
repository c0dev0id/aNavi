package dev.anavi

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun speedConversion_metersPerSecToKmh() {
        val mps = 27.78 // ~100 km/h
        val kmh = (mps * 3.6).toInt()
        assertEquals(100, kmh)
    }
}
