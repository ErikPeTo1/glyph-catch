package dev.equalparts.glyph_catch.gameplay

import dev.equalparts.glyph_catch.gameplay.spawner.Weather
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenMeteoWeatherProviderTest {

    @Test
    fun `maps clear and mainly clear to CLEAR`() {
        assertEquals(Weather.CLEAR, OpenMeteoWeatherProvider.mapWeatherCode(0))
        assertEquals(Weather.CLEAR, OpenMeteoWeatherProvider.mapWeatherCode(1))
    }

    @Test
    fun `maps cloudy codes to CLOUDY`() {
        assertEquals(Weather.CLOUDY, OpenMeteoWeatherProvider.mapWeatherCode(2))
        assertEquals(Weather.CLOUDY, OpenMeteoWeatherProvider.mapWeatherCode(3))
    }

    @Test
    fun `maps drizzle rain and showers to RAIN`() {
        assertEquals(Weather.RAIN, OpenMeteoWeatherProvider.mapWeatherCode(51))
        assertEquals(Weather.RAIN, OpenMeteoWeatherProvider.mapWeatherCode(65))
        assertEquals(Weather.RAIN, OpenMeteoWeatherProvider.mapWeatherCode(67))
        assertEquals(Weather.RAIN, OpenMeteoWeatherProvider.mapWeatherCode(80))
        assertEquals(Weather.RAIN, OpenMeteoWeatherProvider.mapWeatherCode(82))
    }

    @Test
    fun `maps snow codes to SNOW`() {
        assertEquals(Weather.SNOW, OpenMeteoWeatherProvider.mapWeatherCode(71))
        assertEquals(Weather.SNOW, OpenMeteoWeatherProvider.mapWeatherCode(77))
        assertEquals(Weather.SNOW, OpenMeteoWeatherProvider.mapWeatherCode(85))
        assertEquals(Weather.SNOW, OpenMeteoWeatherProvider.mapWeatherCode(86))
    }

    @Test
    fun `maps thunderstorm codes to THUNDERSTORM`() {
        assertEquals(Weather.THUNDERSTORM, OpenMeteoWeatherProvider.mapWeatherCode(95))
        assertEquals(Weather.THUNDERSTORM, OpenMeteoWeatherProvider.mapWeatherCode(97))
        assertEquals(Weather.THUNDERSTORM, OpenMeteoWeatherProvider.mapWeatherCode(99))
    }

    @Test
    fun `maps fog and unknown atmosphere to CLEAR`() {
        assertEquals(Weather.CLEAR, OpenMeteoWeatherProvider.mapWeatherCode(45))
        assertEquals(Weather.CLEAR, OpenMeteoWeatherProvider.mapWeatherCode(48))
        assertEquals(Weather.CLEAR, OpenMeteoWeatherProvider.mapWeatherCode(999))
    }
}
