package dev.equalparts.glyph_catch.gameplay

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.equalparts.glyph_catch.data.Forms
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.LEGENDARY_SPECIES
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.gameplay.spawner.GameplayContext
import dev.equalparts.glyph_catch.gameplay.spawner.SpawnResult
import dev.equalparts.glyph_catch.gameplay.spawner.SpawnRulesEngine
import dev.equalparts.glyph_catch.gameplay.spawner.createSpawnRules
import dev.equalparts.glyph_catch.util.EventHelpers
import dev.equalparts.glyph_catch.widget.WidgetUpdater
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The hidden "play while in use" setting promises Pokémon while the phone is in use, but Nothing
 * switches Glyph Toys off then. The ones missed are added when the toy comes back, when the app
 * opens and every 15 minutes in the background. Nothing happens with the setting off.
 */
object HiddenCatchUp {
    /** Same pace as the toy itself with the setting on: every 5 minutes, or 2½ with an Incense. */
    private const val SPAWN_EVERY_MILLIS = 5 * 60_000L
    private const val INCENSE_SPAWN_EVERY_MILLIS = 150_000L
    private const val SPAWN_MINUTES = 5
    private const val WORK_NAME = "hidden_catch_up"

    /**
     * Adds the spawns missed since the toy last ticked. [fromToy] is the toy itself coming back;
     * otherwise, nothing is done while it's running, as it keeps its own pace.
     */
    suspend fun run(context: Context, fromToy: Boolean = false) = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val preferences = PreferencesManager(app)
        if (!preferences.playWhileInUse) return@withContext
        if (!fromToy && PokemonGlyphToyService.isRunning) return@withContext
        val last = preferences.lastToyTickAt
        val now = System.currentTimeMillis()
        if (last == 0L) {
            preferences.lastToyTickAt = now
            return@withContext
        }
        val incense = PokemonDatabase.getInstance(app).activeItemDao().getActiveItem(Item.INCENSE.ordinal) != null
        val missed = ((now - last) / if (incense) INCENSE_SPAWN_EVERY_MILLIS else SPAWN_EVERY_MILLIS).toInt()
        if (missed <= 0) return@withContext

        val queue = PokemonGlyphToyService.readSavedQueue(app).toMutableList()
        val room = PokemonGlyphToyService.maxQueueSize(preferences) - queue.size
        if (room > 0) {
            val gameplay = GameplayContext(app, WeatherProviderFactory.create(app), emptyList())
            gameplay.trainer.refresh()
            val engine = SpawnRulesEngine(
                rules = createSpawnRules(gameplay),
                speciesFilter = { gameplay.trainer.canSpawn(it.id) },
                shinyMultiplier = { EventHelpers.catchShinyMultiplier(gameplay.trainer.catchCounts[it.id] ?: 0) }
            )
            val db = PokemonDatabase.getInstance(app)
            repeat(minOf(missed, room)) {
                val spawn = engine.spawn(SPAWN_MINUTES)
                if (isRepelled(spawn, preferences, db)) return@repeat
                queue += PersistentSpawn(
                    pokemonId = spawn.pokemon.id,
                    poolName = spawn.pool.name,
                    isSpecial = spawn.pool.isSpecial,
                    isConditional = spawn.pool.isConditional,
                    screenOffDurationMinutes = SPAWN_MINUTES,
                    spawnedAtMillis = now,
                    isShiny = spawn.isShiny,
                    form = Forms.formFor(spawn.pokemon.id, gameplay.formConditions())
                )
            }
            PokemonGlyphToyService.writeSavedQueue(app, queue)
        }
        preferences.lastToyTickAt = now
        WidgetUpdater.refresh(app)
    }

    /** Runs [run] every 15 minutes while the setting is on, the shortest Android allows. */
    fun schedule(context: Context, enabled: Boolean) {
        val work = WorkManager.getInstance(context.applicationContext)
        if (enabled) {
            work.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<Worker>(15, TimeUnit.MINUTES).build()
            )
        } else {
            work.cancelUniqueWork(WORK_NAME)
        }
    }

    class Worker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            run(applicationContext)
            return Result.success()
        }
    }
}

/**
 * The Repel keeps away species already registered; the Super Repel, those still in the PC.
 * Legendaries aren't scared off; shinies are, or a repel would be a free shiny hunt.
 */
internal suspend fun isRepelled(spawn: SpawnResult, preferences: PreferencesManager, db: PokemonDatabase): Boolean {
    val id = spawn.pokemon.id
    if (id in LEGENDARY_SPECIES) return false
    return when {
        preferences.isRepelActive -> db.pokemonDao().hasPokedexEntry(id)
        preferences.isSuperRepelActive -> db.pokemonDao().isSpeciesOwned(id)
        else -> false
    }
}

/** What decides a Pokémon's form when it appears: weather and season. */
internal fun GameplayContext.formConditions(): Forms.Conditions = Forms.Conditions(
    sunny = weather.clear,
    rainy = weather.rain || weather.thunderstorm,
    snowy = weather.snow,
    season = when {
        season.winter -> "winter"
        season.summer -> "summer"
        season.autumn -> "autumn"
        else -> "spring"
    }
)
