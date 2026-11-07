package dev.equalparts.glyph_catch.widget

import android.content.Context
import androidx.annotation.StringRes
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.DailyStats
import dev.equalparts.glyph_catch.data.Eggs
import dev.equalparts.glyph_catch.data.LEGENDARY_SPECIES
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.Stat
import dev.equalparts.glyph_catch.data.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Each achievement unlocks one of the extra PC Box wallpapers, like the hidden ones in Pokémon
 * Emerald. [goalRes] takes [target] as its only argument.
 */
enum class Achievement(val target: Int, @param:StringRes val goalRes: Int) {
    PIKACHU(1, R.string.achievement_pikachu),
    CATCHES(100, R.string.achievement_catches),
    MORE_CATCHES(500, R.string.achievement_catches),
    SPECIES(100, R.string.achievement_species),
    ROD_CATCHES(50, R.string.achievement_rod),
    SUPER_ROD(1, R.string.achievement_super_rod),
    EVOLUTIONS(50, R.string.achievement_evolutions),
    EGGS(10, R.string.achievement_eggs),
    SHINY(1, R.string.achievement_shiny),
    LEVEL_100(100, R.string.achievement_level),
    GHOSTS(25, R.string.achievement_ghosts),
    MISSIONS(30, R.string.achievement_missions),
    DAYS(7, R.string.achievement_days),
    MINUTES(1000, R.string.achievement_minutes),
    LEGENDARIES(10, R.string.achievement_legendaries)
}

/**
 * How far the player is with each achievement, which wallpapers they have and which of those
 * they haven't seen yet.
 */
data class AchievementState(
    val progress: Map<Achievement, Int>,
    val unlocked: Set<BoxWallpaper>,
    val unseen: Set<BoxWallpaper>
) {
    fun isUnlocked(wallpaper: BoxWallpaper) = wallpaper.achievement == null || wallpaper in unlocked

    val total: Int get() = BoxWallpaper.entries.count { it.achievement != null }

    val done: Int get() = unlocked.size
}

object Achievements {
    /**
     * Checks every achievement and remembers the newly reached ones. Wallpapers never lock again,
     * even if, say, the Pokémon that earned them is released.
     */
    suspend fun refresh(context: Context): AchievementState = withContext(Dispatchers.IO) {
        val preferences = PreferencesManager(context)
        val progress = progress(context, preferences)
        val reached = BoxWallpaper.entries
            .filter { wallpaper -> wallpaper.achievement?.let { (progress[it] ?: 0) >= it.target } == true }
            .map { it.name }
        val unlocked = preferences.unlockedWallpapers + reached
        if (unlocked != preferences.unlockedWallpapers) preferences.unlockedWallpapers = unlocked

        val owned = BoxWallpaper.entries.filter { it.name in unlocked }.toSet()
        AchievementState(
            progress = progress,
            unlocked = owned,
            unseen = owned.filter { it.name !in preferences.seenWallpapers }.toSet()
        )
    }

    fun markSeen(context: Context, state: AchievementState) {
        val preferences = PreferencesManager(context)
        preferences.seenWallpapers = preferences.seenWallpapers + state.unlocked.map { it.name }
    }

    private suspend fun progress(context: Context, preferences: PreferencesManager): Map<Achievement, Int> {
        val dao = PokemonDatabase.getInstance(context).pokemonDao()
        val stats = preferences.dailyStats
        val catches = preferences.catchCounts
        val pokedex = dao.getPokedexSpeciesIds()
        val pc = dao.getAllCaught()
        val values = mapOf(
            Achievement.PIKACHU to if (PIKACHU in pokedex) 1 else 0,
            Achievement.CATCHES to catches.values.sum(),
            Achievement.MORE_CATCHES to catches.values.sum(),
            Achievement.SPECIES to pokedex.size,
            Achievement.ROD_CATCHES to stats.total(Stat.ROD_CATCHES),
            // Catches from before these were counted still show in the PC
            Achievement.SUPER_ROD to maxOf(
                stats.counter(DailyStats.SUPER_ROD_CATCHES),
                pc.count { it.spawnPoolName == SUPER_ROD_POOL }
            ),
            Achievement.EVOLUTIONS to stats.total(Stat.EVOLUTIONS),
            Achievement.EGGS to maxOf(
                stats.counter(DailyStats.EGGS_HATCHED),
                pc.count { it.spawnPoolName == Eggs.POOL_NAME }
            ),
            Achievement.SHINY to dao.getShinyPokedexSpeciesIds().size,
            Achievement.LEVEL_100 to (pc.maxOfOrNull { it.level } ?: 0),
            Achievement.GHOSTS to catches.entries.sumOf { (species, count) ->
                val ghost = Pokemon[species]?.let { it.type1 == Type.GHOST || it.type2 == Type.GHOST } == true
                if (ghost) count else 0
            },
            Achievement.MISSIONS to stats.counter(DailyStats.MISSIONS_COMPLETED),
            Achievement.DAYS to stats.daysPlayed,
            Achievement.MINUTES to stats.total(Stat.MINUTES),
            Achievement.LEGENDARIES to pokedex.count { it in LEGENDARY_SPECIES }
        )
        return values.mapValues { (achievement, value) -> value.coerceAtMost(achievement.target) }
    }

    private const val PIKACHU = 25
    private const val SUPER_ROD_POOL = "Super Rod"
}
