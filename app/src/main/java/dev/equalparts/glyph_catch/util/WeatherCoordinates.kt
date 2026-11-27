package dev.equalparts.glyph_catch.util

/**
 * Coarsens coordinates before they leave the device for weather requests.
 * One decimal place is roughly 11 km — enough for city-scale conditions without
 * exposing precise GPS.
 */
object WeatherCoordinates {
    fun quantize(value: Double): Double = Math.round(value * 10.0) / 10.0

    fun quantize(value: Float): Float = quantize(value.toDouble()).toFloat()
}
