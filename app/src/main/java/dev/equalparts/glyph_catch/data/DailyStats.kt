package dev.equalparts.glyph_catch.data

import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Things the player does each day. Used by the statistics screen and by missions.
 */
enum class Stat(val key: String) {
    MINUTES("minutes"), // minutes with the phone locked
    CATCHES("catches"),
    NEW_SPECIES("newSpecies"),
    ROD_CATCHES("rodCatches"),
    LEVELS("levels"),
    EVOLUTIONS("evolutions"),
    ITEMS_USED("itemsUsed")
}

/**
 * Per-day counters stored as one JSON object in the main preferences (so backups include them).
 */
class DailyStats(private val prefs: SharedPreferences) {

    fun add(stat: Stat, amount: Int = 1, date: LocalDate = LocalDate.now()) {
        if (amount <= 0) return
        synchronized(lock) {
            val all = load().toMutableMap()
            val day = all[date.toString()].orEmpty().toMutableMap()
            day[stat.key] = (day[stat.key] ?: 0) + amount
            all[date.toString()] = day
            save(
                all.filterKeys {
                    runCatching { LocalDate.parse(it) }.getOrNull()?.isAfter(date.minusDays(KEEP_DAYS)) ==
                        true
                }
            )
        }
    }

    fun get(stat: Stat, date: LocalDate = LocalDate.now()): Int = load()[date.toString()]?.get(stat.key) ?: 0

    /**
     * Sum of a stat from [from] to [to], both included.
     */
    fun sum(stat: Stat, from: LocalDate, to: LocalDate = LocalDate.now()): Int {
        val all = load()
        return generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .sumOf { all[it.toString()]?.get(stat.key) ?: 0 }
    }

    private fun load(): Map<String, Map<String, Int>> {
        val raw = prefs.getString(KEY, null) ?: return emptyMap()
        return runCatching {
            Json.parseToJsonElement(raw).jsonObject.mapValues { (_, day) ->
                day.jsonObject.mapValues { it.value.jsonPrimitive.int }
            }
        }.getOrDefault(emptyMap())
    }

    private fun save(all: Map<String, Map<String, Int>>) {
        val json = JsonObject(all.mapValues { (_, day) -> JsonObject(day.mapValues { JsonPrimitive(it.value) }) })
        prefs.edit { putString(KEY, json.toString()) }
    }

    companion object {
        const val KEY = "daily_stats"
        private const val KEEP_DAYS = 40L
        private val lock = Any()
    }
}
