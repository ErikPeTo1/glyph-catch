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
            val totals = lifetime(all)
            totals[stat.key] = (totals[stat.key] ?: 0) + amount
            if (date.toString() !in all) totals[DAYS_PLAYED] = (totals[DAYS_PLAYED] ?: 0) + 1
            saveLifetime(totals)
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

    /** All-time total of a stat, for achievements. */
    fun total(stat: Stat): Int = lifetime(load())[stat.key] ?: 0

    /** Days with anything counted. */
    val daysPlayed: Int get() = lifetime(load())[DAYS_PLAYED] ?: 0

    /** All-time counters that aren't daily stats, like hatched Eggs. */
    fun counter(key: String): Int = lifetime(load())[key] ?: 0

    fun bump(key: String, amount: Int = 1) {
        synchronized(lock) {
            val totals = lifetime(load())
            totals[key] = (totals[key] ?: 0) + amount
            saveLifetime(totals)
        }
    }

    /**
     * The all-time totals. The first time, they start from the days still kept, so older saves
     * don't start from zero.
     */
    private fun lifetime(days: Map<String, Map<String, Int>>): MutableMap<String, Int> {
        val raw = prefs.getString(LIFETIME_KEY, null)
        val saved = raw?.let {
            runCatching { Json.parseToJsonElement(it).jsonObject.mapValues { (_, v) -> v.jsonPrimitive.int } }
                .getOrNull()
        }
        if (saved != null) return saved.toMutableMap()
        val seeded = Stat.entries.associate { stat -> stat.key to days.values.sumOf { it[stat.key] ?: 0 } }
        return (seeded + (DAYS_PLAYED to days.size)).toMutableMap()
    }

    private fun saveLifetime(totals: Map<String, Int>) {
        prefs.edit { putString(LIFETIME_KEY, JsonObject(totals.mapValues { JsonPrimitive(it.value) }).toString()) }
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
        private const val LIFETIME_KEY = "lifetime_stats"
        private const val DAYS_PLAYED = "days"
        const val EGGS_HATCHED = "eggsHatched"
        const val SUPER_ROD_CATCHES = "superRodCatches"
        const val MISSIONS_COMPLETED = "missionsCompleted"
        private val lock = Any()
    }
}
