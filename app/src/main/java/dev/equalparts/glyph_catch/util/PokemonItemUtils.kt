package dev.equalparts.glyph_catch.util

import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.EvolutionRequirement
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonSpecies

const val MAX_POKEMON_LEVEL = 100

/**
 * Species whose evolution the player picks in the app: Tyrogue (by level) and Clamperl
 * (with the Linking Cord). Wurmple and Burmy stay random, like in the main games.
 */
val CHOICE_EVOLUTIONS = setOf(236, 366)

private fun PokemonSpecies.evolutionTargets(): List<PokemonSpecies> = evolvesTo.mapNotNull { Pokemon[it] }

/**
 * The automatic level-up evolution, if any. Pokémon holding an Everstone and those whose
 * evolution is chosen by the player never evolve automatically.
 */
fun findLevelEvolutionTarget(pokemon: CaughtPokemon): PokemonSpecies? {
    if (pokemon.evolutionLocked || pokemon.speciesId in CHOICE_EVOLUTIONS) return null
    val species = Pokemon[pokemon.speciesId] ?: return null
    return species.evolutionTargets().filter { candidate ->
        val requirement = candidate.evolutionRequirement as? EvolutionRequirement.Level
        requirement != null && pokemon.level >= requirement.level
    }.randomOrNull()
}

fun findStoneEvolutionTarget(pokemon: CaughtPokemon, stone: Item): PokemonSpecies? {
    val species = Pokemon[pokemon.speciesId] ?: return null
    return species.evolutionTargets().firstOrNull { candidate ->
        val requirement = candidate.evolutionRequirement as? EvolutionRequirement.Stone
        requirement?.item == stone
    }
}

fun findTradeEvolutionTarget(pokemon: CaughtPokemon): PokemonSpecies? {
    val species = Pokemon[pokemon.speciesId] ?: return null
    return species.evolutionTargets().filter { candidate ->
        candidate.evolutionRequirement == EvolutionRequirement.Trade
    }.randomOrNull()
}

fun canUseItemOn(item: Item, pokemon: CaughtPokemon): Boolean = when (item) {
    Item.RARE_CANDY -> pokemon.level < MAX_POKEMON_LEVEL
    Item.LINKING_CORD -> findTradeEvolutionTarget(pokemon) != null
    Item.FIRE_STONE,
    Item.WATER_STONE,
    Item.THUNDER_STONE,
    Item.LEAF_STONE,
    Item.MOON_STONE,
    Item.SUN_STONE,
    Item.SHINY_STONE,
    Item.DUSK_STONE,
    Item.DAWN_STONE,
    Item.ICE_STONE -> findStoneEvolutionTarget(pokemon, item) != null
    else -> false
}

/**
 * Level-up evolutions the player can choose from right now (Tyrogue at level 20).
 */
fun levelChoiceTargets(pokemon: CaughtPokemon): List<PokemonSpecies> {
    if (pokemon.speciesId !in CHOICE_EVOLUTIONS) return emptyList()
    val species = Pokemon[pokemon.speciesId] ?: return emptyList()
    return species.evolutionTargets().filter { candidate ->
        val requirement = candidate.evolutionRequirement as? EvolutionRequirement.Level
        requirement != null && pokemon.level >= requirement.level
    }
}

/**
 * All evolutions reachable with the Linking Cord (two for Clamperl).
 */
fun tradeTargets(pokemon: CaughtPokemon): List<PokemonSpecies> {
    val species = Pokemon[pokemon.speciesId] ?: return emptyList()
    return species.evolutionTargets().filter { it.evolutionRequirement == EvolutionRequirement.Trade }
}

/**
 * Whether the Everstone toggle makes sense for this Pokémon.
 */
fun canHoldEverstone(pokemon: CaughtPokemon): Boolean {
    if (pokemon.speciesId in CHOICE_EVOLUTIONS) return false
    val species = Pokemon[pokemon.speciesId] ?: return false
    return species.evolutionTargets().any { it.evolutionRequirement is EvolutionRequirement.Level }
}
