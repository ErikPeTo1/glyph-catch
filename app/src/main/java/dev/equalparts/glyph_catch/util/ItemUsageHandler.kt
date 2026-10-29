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
    targetSpeciesId: Int? = null
): ItemUsageResult = withContext(Dispatchers.IO) {
    val pokemonDao = db.pokemonDao()
    val current = pokemonDao.getCaughtPokemon(pokemonId)
        ?: return@withContext ItemUsageResult.Error(ItemUsageError.INVALID_POKEMON)

    when (item) {
        Item.RARE_CANDY -> applyRareCandy(db, preferencesManager, current)
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

private suspend fun applyRareCandy(
    db: PokemonDatabase,
    preferencesManager: PreferencesManager,
    current: CaughtPokemon
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

    val newLevel = (refreshed.level + 1).coerceAtMost(MAX_POKEMON_LEVEL)
    val newExp = 0

    inventoryDao.useItem(Item.RARE_CANDY.ordinal)
    pokemonDao.updateTrainingProgress(refreshed.id, newExp, newLevel)
    preferencesManager.dailyStats.add(Stat.ITEMS_USED)
    preferencesManager.dailyStats.add(Stat.LEVELS)

    val evolution = findLevelEvolutionTarget(refreshed.copy(level = newLevel, exp = newExp))
    if (evolution != null) {
        pokemonDao.evolveCaught(refreshed, evolution.id, newLevel, newExp)
        preferencesManager.dailyStats.add(Stat.EVOLUTIONS)
        val updated = pokemonDao.getCaughtPokemon(refreshed.id)
            ?: refreshed.copy(speciesId = evolution.id, level = newLevel, exp = newExp)
        preferencesManager.enqueueEvolutionNotification(refreshed.speciesId, evolution.id)
        ItemUsageResult.Success(
            updatedPokemon = updated,
            event = UsageEvent.Evolution(refreshed.speciesId, evolution.id, newLevel)
        )
    } else {
        val updated = pokemonDao.getCaughtPokemon(refreshed.id)
            ?: refreshed.copy(level = newLevel, exp = newExp)
        ItemUsageResult.Success(
            updatedPokemon = updated,
            event = UsageEvent.LevelUp(newLevel)
        )
    }
}
