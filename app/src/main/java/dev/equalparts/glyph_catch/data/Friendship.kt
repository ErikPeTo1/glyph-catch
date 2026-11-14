package dev.equalparts.glyph_catch.data

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.edit
import dev.equalparts.glyph_catch.R
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * How close a Pokémon is to its trainer, from 0 to 255 like in the main games. It only goes up:
 * petting it in the Garden, keeping it in the team, training it and leveling it up.
 */
object FriendshipRules {
    const val MAX = 255
    const val CAUGHT = 70
    const val HATCHED = 120
    const val EVOLVES_AT = 220

    const val PETS_PER_DAY = 10
    const val TEAM_PER_DAY = 2
    const val TRAINING_MINUTES_PER_POINT = 60
    const val TRAINING_POINTS_PER_DAY = 6

    /** The Hot Springs: a point every half hour with the phone locked, a few a day. */
    const val SPRINGS_MINUTES_PER_POINT = 30
    const val SPRINGS_POINTS_PER_DAY = 8

    /** A berry from the Garden: more than a pet, double for its favorite flavor, a couple a day. */
    const val BERRY = 1
    const val FAVORITE_BERRY = 1
    const val BERRIES_PER_DAY = 2

    /** From this much on, a Pokémon trains a little harder for you. */
    const val EXP_BONUS_FROM = 200
    const val EXP_BONUS_PERCENT = 10

    /** 0–4 hearts every 50 points, and the fifth only at the very top. */
    fun hearts(friendship: Int): Int = if (friendship >= MAX) 5 else (friendship / 50).coerceIn(0, 4)

    /** Like in the games, each level counts a little less once a Pokémon already loves you. */
    fun levelUpGain(friendship: Int): Int = if (friendship >= EXP_BONUS_FROM) 2 else 3

    fun afterLevelUps(friendship: Int, levels: Int): Int =
        (0 until levels.coerceAtLeast(0)).fold(friendship) { value, _ -> add(value, levelUpGain(value)) }

    fun add(friendship: Int, amount: Int): Int = (friendship + amount).coerceIn(0, MAX)

    fun withExpBonus(friendship: Int, exp: Int): Int =
        if (friendship >= EXP_BONUS_FROM) exp + exp * EXP_BONUS_PERCENT / 100 else exp

    /** What the friendship checker in the games would say. */
    @StringRes
    fun phrase(friendship: Int): Int = when (hearts(friendship)) {
        0 -> R.string.friendship_0
        1 -> R.string.friendship_1
        2 -> R.string.friendship_2
        3 -> R.string.friendship_3
        4 -> R.string.friendship_4
        else -> R.string.friendship_5
    }
}

/** Day or night, for evolutions like Espeon and Umbreon. */
enum class DayTime {
    DAY,
    NIGHT;

    companion object {
        fun at(time: LocalTime): DayTime = if (time.hour in 6..19) DAY else NIGHT

        fun now(): DayTime = at(LocalTime.now())
    }
}

/**
 * Keeps the daily limits: how many times each Pokémon was petted, whether the team got its
 * daily bonus and how long each one trained today. Starts over every day.
 */
class FriendshipDaily(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Serializable
    private data class Day(
        val date: String,
        val pets: Map<String, Int> = emptyMap(),
        val team: Set<String> = emptySet(),
        val trainingMinutes: Map<String, Int> = emptyMap(),
        val gifts: Set<String> = emptySet(),
        val berriesEaten: Map<String, Int> = emptyMap(),
        val springsMinutes: Map<String, Int> = emptyMap(),
        val sweets: Set<String> = emptySet()
    )

    private fun today(): Day {
        val date = LocalDate.now().toString()
        val saved = prefs.getString(KEY_DAY, null)
            ?.let { runCatching { json.decodeFromString<Day>(it) }.getOrNull() }
        return saved?.takeIf { it.date == date } ?: Day(date)
    }

    private fun save(day: Day) = prefs.edit { putString(KEY_DAY, json.encodeToString(day)) }

    fun petsLeft(pokemonId: String): Int = FriendshipRules.PETS_PER_DAY - (today().pets[pokemonId] ?: 0)

    /** Counts a pet; returns `false` once today's pets for this Pokémon are used up. */
    @Synchronized
    fun pet(pokemonId: String): Boolean {
        val day = today()
        val done = day.pets[pokemonId] ?: 0
        if (done >= FriendshipRules.PETS_PER_DAY) return false
        save(day.copy(pets = day.pets + (pokemonId to done + 1)))
        return true
    }

    /** The team members that haven't had today's bonus yet, marked as done. */
    @Synchronized
    fun takeTeamBonus(team: List<String>): List<String> {
        val day = today()
        val due = team.filter { it !in day.team }
        if (due.isNotEmpty()) save(day.copy(team = day.team + due))
        return due
    }

    /** Counts a minute of training; returns `true` when it completes a point under today's limit. */
    @Synchronized
    fun trainMinute(pokemonId: String): Boolean {
        val day = today()
        val minutes = (day.trainingMinutes[pokemonId] ?: 0) + 1
        save(day.copy(trainingMinutes = day.trainingMinutes + (pokemonId to minutes)))
        val maxMinutes = FriendshipRules.TRAINING_MINUTES_PER_POINT * FriendshipRules.TRAINING_POINTS_PER_DAY
        return minutes <= maxMinutes && minutes % FriendshipRules.TRAINING_MINUTES_PER_POINT == 0
    }

    /** Counts a locked minute in the Hot Springs; true when it completes a point under today's limit. */
    @Synchronized
    fun springsMinute(pokemonId: String): Boolean {
        val day = today()
        val minutes = (day.springsMinutes[pokemonId] ?: 0) + 1
        save(day.copy(springsMinutes = day.springsMinutes + (pokemonId to minutes)))
        val maxMinutes = FriendshipRules.SPRINGS_MINUTES_PER_POINT * FriendshipRules.SPRINGS_POINTS_PER_DAY
        return minutes <= maxMinutes && minutes % FriendshipRules.SPRINGS_MINUTES_PER_POINT == 0
    }

    /** Whether today's berry from this Pokémon was already collected. */
    fun giftCollected(pokemonId: String): Boolean = pokemonId in today().gifts

    /** Marks today's berry as collected; false if it already was. */
    @Synchronized
    fun collectGift(pokemonId: String): Boolean {
        val day = today()
        if (pokemonId in day.gifts) return false
        save(day.copy(gifts = day.gifts + pokemonId))
        return true
    }

    fun berriesLeft(pokemonId: String): Int = FriendshipRules.BERRIES_PER_DAY - (today().berriesEaten[pokemonId] ?: 0)

    /** Counts a berry eaten; false once it's had enough for today. */
    @Synchronized
    fun eatBerry(pokemonId: String): Boolean {
        val day = today()
        val eaten = day.berriesEaten[pokemonId] ?: 0
        if (eaten >= FriendshipRules.BERRIES_PER_DAY) return false
        save(day.copy(berriesEaten = day.berriesEaten + (pokemonId to eaten + 1)))
        return true
    }

    /** Whether this Pokémon already had today's sweet from the Kitchen. */
    fun ateSweet(pokemonId: String): Boolean = pokemonId in today().sweets

    /** Counts today's sweet; false if it already had it. */
    @Synchronized
    fun eatSweet(pokemonId: String): Boolean {
        val day = today()
        if (pokemonId in day.sweets) return false
        save(day.copy(sweets = day.sweets + pokemonId))
        return true
    }

    private companion object {
        const val PREFS_NAME = "friendship_daily"
        const val KEY_DAY = "day"
        val json = Json { ignoreUnknownKeys = true }
    }
}

/** Gives the team its daily bonus for being together. */
suspend fun creditTeamFriendship(context: Context, db: PokemonDatabase) {
    val preferences = PreferencesManager(context)
    val dao = db.pokemonDao()
    val team = preferences.team.filter { dao.getCaughtPokemon(it) != null }
    FriendshipDaily(context).takeTeamBonus(team).forEach { dao.addFriendship(it, FriendshipRules.TEAM_PER_DAY) }
}
