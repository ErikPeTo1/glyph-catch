package dev.equalparts.glyph_catch.util

import android.content.Context
import androidx.core.content.edit
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * A newer version published on GitHub Releases.
 */
data class AvailableUpdate(val version: String, val url: String)

/**
 * Checks GitHub Releases for a newer version, at most once a day. Only the public release
 * information is requested; nothing about the player is sent.
 */
object UpdateChecker {
    private const val REPOSITORY = "ErikPeTo1/glyph-catch"
    private const val CHECK_INTERVAL_MILLIS = 24L * 60 * 60 * 1000
    private const val PREFS_NAME = "glyph_catch_updates"
    private const val KEY_LAST_CHECK = "last_check"
    private const val KEY_LATEST_VERSION = "latest_version"
    private const val KEY_LATEST_URL = "latest_url"

    /**
     * Returns the newer version if there is one, using the cached answer between checks.
     */
    suspend fun check(context: Context): AvailableUpdate? = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(KEY_LAST_CHECK, 0L) > CHECK_INTERVAL_MILLIS) {
            runCatching { fetchLatest() }.getOrNull()?.let { (version, url) ->
                prefs.edit {
                    putLong(KEY_LAST_CHECK, now)
                    putString(KEY_LATEST_VERSION, version)
                    putString(KEY_LATEST_URL, url)
                }
            }
        }

        val latest = prefs.getString(KEY_LATEST_VERSION, null) ?: return@withContext null
        val url = prefs.getString(KEY_LATEST_URL, null) ?: return@withContext null
        val installed = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: return@withContext null
        if (isNewer(latest, installed)) AvailableUpdate(latest, url) else null
    }

    private fun fetchLatest(): Pair<String, String> {
        val connection = URL(
            "https://api.github.com/repos/$REPOSITORY/releases/latest"
        ).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            return json.getString("tag_name").removePrefix("v") to json.getString("html_url")
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Compares dotted version numbers like "2.10.0" and "2.9.1". Anything after a dash is ignored.
     */
    fun isNewer(candidate: String, current: String): Boolean {
        fun parts(version: String) = version.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val a = parts(candidate)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val difference = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
            if (difference != 0) return difference > 0
        }
        return false
    }
}
