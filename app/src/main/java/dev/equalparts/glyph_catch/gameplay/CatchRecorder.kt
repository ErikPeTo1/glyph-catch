package dev.equalparts.glyph_catch.gameplay

import android.content.Context
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.DailyStats
import dev.equalparts.glyph_catch.data.Eggs
import dev.equalparts.glyph_catch.data.EvolutionRequirement
import dev.equalparts.glyph_catch.data.InventoryItem
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.Stat
import dev.equalparts.glyph_catch.data.legendaryLevels
import dev.equalparts.glyph_catch.gameplay.spawner.GameplayContext
import kotlin.math.floor
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

/**
 * Saves a catch: the Pokémon, its Pokédex entry, stats and the items it may drop. Shared by the
 * Glyph Toy and by catching from the app or a widget, where the toy isn't running.
 */
internal object CatchRecorder {
    /** Catches started from a widget or the app outlive the screen that started them. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val ROD_POOLS = setOf("Old Rod", "Good Rod", "Super Rod")
    private const val EVOLUTION_STONE_DROP_CHANCE = 0.10
    private const val INCENSE_DROP_CHANCE = 0.03
    private const val LINKING_CORD_DROP_CHANCE = 0.02
    private const val LINKING_CORD_MILESTONE = 40

    private val EVOLUTION_STONES = listOf(
        Item.FIRE_STONE,
        Item.WATER_STONE,
        Item.THUNDER_STONE,
        Item.LEAF_STONE,
        Item.MOON_STONE,
        Item.SUN_STONE,
        Item.SHINY_STONE,
        Item.DUSK_STONE,
        Item.DAWN_STONE,
        Item.ICE_STONE
    )

    /**
     * Records the catch and returns the saved Pokémon. [canSpawn] says which babies an Egg can
     * hold (those from open regions); [onAward] hears about each item handed out.
     */
    suspend fun record(
        db: PokemonDatabase,
        preferences: PreferencesManager,
        spawn: PersistentSpawn,
        canSpawn: (Int) -> Boolean,
        onAward: suspend (Item, String) -> Unit = { _, _ -> }
    ): CaughtPokemon = withContext(Dispatchers.IO) {
        val speciesId = spawn.pokemonId
        val alreadyDiscovered = db.pokemonDao().hasPokedexEntry(speciesId)
        val caught = CaughtPokemon(
            speciesId = speciesId,
            spawnedAt = spawn.spawnedAtMillis,
            level = randomLevel(speciesId),
            exp = 0,
            screenOffDurationMinutes = spawn.screenOffDurationMinutes,
            spawnPoolName = spawn.poolName,
            isSpecialSpawn = spawn.poolName.contains("special", ignoreCase = true),
            isConditionalSpawn = spawn.poolName.contains("event", ignoreCase = true),
            isShiny = spawn.isShiny,
            form = spawn.form
        )
        db.pokemonDao().insert(caught)
        db.pokemonDao().recordPokedexEntry(speciesId, spawn.isShiny, spawn.form)
        preferences.addCatch(speciesId)

        awardItems(db, preferences, speciesId, alreadyDiscovered, canSpawn, onAward)
        with(preferences.dailyStats) {
            add(Stat.CATCHES)
            if (!alreadyDiscovered) add(Stat.NEW_SPECIES)
            if (spawn.poolName in ROD_POOLS) add(Stat.ROD_CATCHES)
            if (spawn.poolName == "Super Rod") bump(DailyStats.SUPER_ROD_CATCHES)
            if (spawn.poolName == Eggs.POOL_NAME) bump(DailyStats.EGGS_HATCHED)
        }
        caught
    }

    /**
     * Catches the first Pokémon waiting in the saved queue, for when the Glyph Toy isn't running
     * (the Phone (4a) Pro, whose button never reaches Glyph Toys). Returns `null` if none waits.
     */
    suspend fun catchFromSavedQueue(context: Context): CaughtPokemon? = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val queue = PokemonGlyphToyService.readSavedQueue(app).toMutableList()
        val first = queue.removeFirstOrNull() ?: return@withContext null
        PokemonGlyphToyService.writeSavedQueue(app, queue)
        val gameplay = GameplayContext(app, WeatherProviderFactory.create(app), emptyList())
        gameplay.trainer.refresh()
        record(PokemonDatabase.getInstance(app), PreferencesManager(app), first, { gameplay.trainer.canSpawn(it) })
    }

    private suspend fun awardItems(
        db: PokemonDatabase,
        preferences: PreferencesManager,
        speciesId: Int,
        wasDuplicate: Boolean,
        canSpawn: (Int) -> Boolean,
        onAward: suspend (Item, String) -> Unit
    ) {
        if (Random.nextDouble() < Eggs.DROP_CHANCE) {
            // With a full incubator (or no baby from an open region), the egg becomes a Rare Candy
            val egg = Eggs.newEgg(allowed = canSpawn)
            if (egg != null && db.eggDao().countEggs() < Eggs.MAX_EGGS) {
                db.eggDao().insertEgg(egg)
            } else {
                grant(db, Item.RARE_CANDY)
            }
        }

        if (Random.nextDouble() < INCENSE_DROP_CHANCE) {
            grant(db, Item.INCENSE)
            onAward(Item.INCENSE, "chance")
        }

        if (Random.nextDouble() < EVOLUTION_STONE_DROP_CHANCE) {
            val stone = EVOLUTION_STONES.random()
            grant(db, stone)
            onAward(stone, "stone")
        }

        if (wasDuplicate) {
            grant(db, Item.RARE_CANDY)
            onAward(Item.RARE_CANDY, "duplicate")
        }

        val totalCaught = db.pokemonDao().getTotalCaughtCount() + preferences.releasedCount
        val linkingCordGuaranteed = totalCaught > 0 && totalCaught % LINKING_CORD_MILESTONE == 0
        if (linkingCordGuaranteed || Random.nextDouble() < LINKING_CORD_DROP_CHANCE) {
            grant(db, Item.LINKING_CORD)
            onAward(Item.LINKING_CORD, if (linkingCordGuaranteed) "milestone" else "chance")
        }
    }

    suspend fun grant(db: PokemonDatabase, item: Item, amount: Int = 1) {
        val inventoryDao = db.inventoryDao()
        if (inventoryDao.getItem(item.ordinal) == null) {
            inventoryDao.insertItem(InventoryItem(itemId = item.ordinal, quantity = amount))
        } else {
            inventoryDao.addItems(item.ordinal, amount)
        }
    }

    /**
     * A random level, kept below the next level-up evolution so it doesn't evolve right away.
     */
    fun randomLevel(speciesId: Int): Int {
        legendaryLevels(speciesId)?.let { return it.random() }
        val species = Pokemon[speciesId] ?: return Random.nextInt(18, 41)
        val nextLevelRequirement = species.evolvesTo
            .mapNotNull { Pokemon[it] }
            .mapNotNull { (it.evolutionRequirement as? EvolutionRequirement.Level)?.level }
            .minOrNull()

        if (nextLevelRequirement != null && nextLevelRequirement > 2) {
            val minLevel = (floor(nextLevelRequirement * 0.5).toInt() + 1).coerceAtLeast(1)
            val maxLevel = (nextLevelRequirement - 2).coerceAtLeast(minLevel)
            return Random.nextInt(minLevel, maxLevel + 1)
        }
        return Random.nextInt(18, 41)
    }
}
