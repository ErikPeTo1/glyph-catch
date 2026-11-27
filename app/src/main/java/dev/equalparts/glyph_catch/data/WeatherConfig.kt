package dev.equalparts.glyph_catch.data

data class WeatherConfig(val useLiveWeather: Boolean, val latitude: Float, val longitude: Float) {
    val hasValidCoordinates: Boolean
        get() = latitude != 0f && longitude != 0f

    val isConfigured: Boolean
        get() = useLiveWeather && hasValidCoordinates
}
