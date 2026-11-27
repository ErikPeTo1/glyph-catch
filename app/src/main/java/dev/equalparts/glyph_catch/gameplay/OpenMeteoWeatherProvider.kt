package dev.equalparts.glyph_catch.gameplay

import android.util.Log
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.WeatherConnectionStatus
import dev.equalparts.glyph_catch.gameplay.spawner.Weather
import dev.equalparts.glyph_catch.gameplay.spawner.WeatherProvider
import dev.equalparts.glyph_catch.util.WeatherCoordinates
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Weather provider that fetches live weather from the Open-Meteo Forecast API.
 * No API key is required. Coordinates are quantized before the request.
 */
class OpenMeteoWeatherProvider(
    private val prefs: PreferencesManager,
    private val latitude: Double,
    private val longitude: Double
) : WeatherProvider {

    private var cachedWeather = Weather.CLEAR
    private var lastFetchTime = 0L
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val weatherState = MutableStateFlow(cachedWeather)

    init {
        if (prefs.weatherLastUpdateEpochMillis == 0L) {
            prefs.weatherConnectionStatus = WeatherConnectionStatus.NEVER_CONNECTED
        }
        fetchWeatherAsync()
    }

    override fun getCurrentWeather(): Weather {
        val now = System.currentTimeMillis()
        if (now - lastFetchTime > TTL) {
            fetchWeatherAsync()
        }
        return cachedWeather
    }

    override fun watchWeather(): Flow<Weather> = weatherState

    private fun fetchWeatherAsync() {
        // Counted when it starts, not when it succeeds: otherwise every weather check while a call is
        // in flight or failing fires another one.
        val fetchStartedAt = System.currentTimeMillis()
        lastFetchTime = fetchStartedAt
        coroutineScope.launch {
            try {
                val weather = fetchWeatherFromApi()
                cachedWeather = weather
                weatherState.value = weather
                prefs.weatherConnectionStatus = WeatherConnectionStatus.CONNECTED
                prefs.weatherLastUpdateEpochMillis = fetchStartedAt
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch weather", e)
                if (prefs.weatherLastUpdateEpochMillis == 0L) {
                    prefs.weatherConnectionStatus = WeatherConnectionStatus.FAILED
                }
            }
        }
    }

    private suspend fun fetchWeatherFromApi(): Weather = withContext(Dispatchers.IO) {
        val lat = WeatherCoordinates.quantize(latitude)
        val lon = WeatherCoordinates.quantize(longitude)
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?" +
                "latitude=$lat&longitude=$lon&current=weather_code"
        )

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Unexpected response code ${connection.responseCode}")
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val current = json.optJSONObject("current")
                ?: throw IOException("Missing current object in response")
            if (!current.has("weather_code")) {
                throw IOException("Missing weather_code in response")
            }

            return@withContext mapWeatherCode(current.getInt("weather_code"))
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val TAG = "OpenMeteoWeatherProvider"
        private const val TTL = 60 * 60 * 1000 // 1 hour

        /**
         * Maps WMO weather interpretation codes (as used by Open-Meteo) to game weather.
         */
        fun mapWeatherCode(code: Int): Weather = when (code) {
            0, 1 -> Weather.CLEAR
            2, 3 -> Weather.CLOUDY
            in 51..67, in 80..82 -> Weather.RAIN
            in 71..77, in 85..86 -> Weather.SNOW
            in 95..99 -> Weather.THUNDERSTORM
            else -> Weather.CLEAR // Fog and other atmosphere codes
        }
    }
}
