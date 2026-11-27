package dev.equalparts.glyph_catch.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCoordinatesTest {

    @Test
    fun `quantizes doubles to one decimal place`() {
        assertEquals(52.5, WeatherCoordinates.quantize(52.52), 0.0)
        assertEquals(13.4, WeatherCoordinates.quantize(13.41), 0.0)
        assertEquals(-73.9, WeatherCoordinates.quantize(-73.935), 0.0)
        assertEquals(0.0, WeatherCoordinates.quantize(0.04), 0.0)
        assertEquals(0.1, WeatherCoordinates.quantize(0.05), 0.0)
    }

    @Test
    fun `quantizes floats to one decimal place`() {
        assertEquals(40.7f, WeatherCoordinates.quantize(40.7128f), 0.0f)
        assertEquals(-74.0f, WeatherCoordinates.quantize(-74.0060f), 0.0f)
    }
}
