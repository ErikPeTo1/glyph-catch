package dev.equalparts.glyph_catch.util

import androidx.room.withTransaction
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PokemonSpecies
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.Stat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ItemUsageResult {
    data class Success(val updatedPokemon: CaughtPokemon, val event: UsageEvent) : ItemUsageResult()
    data class Error(val reason: ItemUsageError, val pokemon: CaughtPokemon? = null) : ItemUsageResult()
}

sealed class UsageEvent {
    data class LevelUp(val newLevel: Int) : UsageEvent()
    data class Evolution(val previousSpeciesId: Int, val newSpeciesId: Int, val newLevel: Int) : UsageEvent()
}

enum class ItemUsageError {
    ITEM_NOT_CONSUMABLE,
    ITEM_NOT_AVAILABLE,
    INVALID_POKEMON,
    ALREADY_MAX_LEVEL,
    NO_APPLICABLE_EVOLUTION
}

suspend fun useItemOnPokemon(
    db: PokemonDatabase,
    preferencesManager: PreferencesManager,
    item: Item,
    pokemonId: String,
    targetSpeciesId: Int? = null,
    amount: Int = 1
): ItemUsageResult = withContext(Dispatchers.IO) {
    val pokemonDao = db.pokemonDao()
    val current = pokemonDao.getCaughtPokemon(pokemonId)
        ?: return@withContext ItemUsageResult.Error(ItemUsageError.INVALID_POKEMON)

    when (item) {
        Item.RARE_CANDY -> applyRareCandy(db, preferencesManager, current, amount)
        Item.LINKING_CORD -> applyEvolutionItem(db, preferencesManager, Item.LINKING_CORD, current) { pokemon ->
            val targets = tradeTargets(pokemon)
            targets.firstOrNull { it.id == targetSpeciesId } ?: findTradeEvolutionTarget(pokemon)
        }
        Item.FIRE_STONE,
        Item.WATER_STONE,
        Item.THUNDER_STONE,
        Item.LEAF_STONE,
        Item.MOON_STONE,
        Item.SUN_STONE,
        Item.SHINY_STONE,
        Item.DUSK_STONE,
        Item.DAWN_STONE,
        Item.ICE_STONE -> applyEvolutionItem(db, preferencesManager, item, current) { pokemon ->
            findStoneEvolutionTarget(pokemon, item)
        }
        else -> ItemUsageResult.Error(ItemUsageError.ITEM_NOT_CONSUMABLE, current)
    }
}

private suspend fun applyEvolutionItem(
    db: PokemonDatabase,
    preferencesManager: PreferencesManager,
    item: Item,
    current: CaughtPokemon,
    targetFor: (CaughtPokemon) -> PokemonSpecies?
): ItemUsageResult = db.withTransaction {
    val pokemonDao = db.pokemonDao()
    val inventoryDao = db.inventoryDao()

    val refreshed = pokemonDao.getCaughtPokemon(current.id)
        ?: return@withTransaction ItemUsageResult.Error(ItemUsageError.INVALID_POKEMON)

    val quantity = inventoryDao.getItem(item.ordinal)?.quantity ?: 0
    if (quantity <= 0) {
        return@withTransaction ItemUsageResult.Error(ItemUsageError.ITEM_NOT_AVAILABLE, refreshed)
    }

    val target = targetFor(refreshed)
        ?: return@withTransaction ItemUsageResult.Error(ItemUsageError.NO_APPLICABLE_EVOLUTION, refreshed)

    inventoryDao.useItem(item.ordinal)
    pokemonDao.evolveCaught(refreshed, target.id, refreshed.level, refreshed.exp)
    preferencesManager.dailyStats.add(Stat.ITEMS_USED)
    preferencesManager.dailyStats.add(Stat.EVOLUTIONS)

    val updated = pokemonDao.getCaughtPokemon(refreshed.id) ?: refreshed.copy(speciesId = target.id)
    preferencesManager.enqueueEvolutionNotification(refreshed.speciesId, target.id)

    ItemUsageResult.Success(
        updatedPokemon = updated,
        event = UsageEvent.Evolution(refreshed.speciesId, target.id, updated.level)
    )
}

/**
 * Uses [amount] Rare Candies one after another, never past level 100. The Pokémon can evolve
 * along the way, just like when the candies are used one at a time.
 */
private suspend fun applyRareCandy(
    db: PokemonDatabase,
    preferencesManager: PreferencesManager,
    current: CaughtPokemon,
    amount: Int
): ItemUsageResult = db.withTransaction {
    val pokemonDao = db.pokemonDao()
    val inventoryDao = db.inventoryDao()

    val refreshed = pokemonDao.getCaughtPokemon(current.id)
        ?: return@withTransaction ItemUsageResult.Error(ItemUsageError.INVALID_POKEMON)

    val quantity = inventoryDao.getItem(Item.RARE_CANDY.ordinal)?.quantity ?: 0
    if (quantity <= 0) {
        return@withTransaction ItemUsageResult.Error(ItemUsageError.ITEM_NOT_AVAILABLE, refreshed)
    }
    if (refreshed.level >= MAX_POKEMON_LEVEL) {
        return@withTransaction ItemUsageResult.Error(ItemUsageError.ALREADY_MAX_LEVEL, refreshed)
    }

    val candies = maxRareCandies(refreshed, quantity).coerceAtMost(amount.coerceAtLeast(1))
    var pokemon = refreshed
    repeat(candies) {
        val newLevel = pokemon.level + 1
        inventoryDao.useItem(Item.RARE_CANDY.ordinal)
        pokemonDao.updateTrainingProgress(pokemon.id, 0, newLevel)
        pokemon = pokemon.copy(level = newLevel, exp = 0)

        val evolution = findLevelEvolutionTarget(pokemon)
        if (evolution != null) {
            pokemonDao.evolveCaught(pokemon, evolution.id, newLevel, 0)
            preferencesManager.dailyStats.add(Stat.EVOLUTIONS)
            preferencesManager.enqueueEvolutionNotification(pokemon.speciesId, evolution.id)
            pokemon = pokemonDao.getCaughtPokemon(pokemon.id) ?: pokemon.copy(speciesId = evolution.id)
        }
    }
    preferencesManager.dailyStats.add(Stat.ITEMS_USED, candies)
    preferencesManager.dailyStats.add(Stat.LEVELS, candies)

    val updated = pokemonDao.getCaughtPokemon(refreshed.id) ?: pokemon
    ItemUsageResult.Success(
        updatedPokemon = updated,
        event = if (updated.speciesId != refreshed.speciesId) {
            UsageEvent.Evolution(refreshed.speciesId, updated.speciesId, updated.level)
        } else {
            UsageEvent.LevelUp(updated.level)
        }
    )
}
