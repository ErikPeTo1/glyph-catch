package dev.equalparts.glyph_catch.util

import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.DayTime
import dev.equalparts.glyph_catch.data.EvolutionRequirement
import dev.equalparts.glyph_catch.data.FriendshipRules
import dev.equalparts.glyph_catch.data.Gender
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonSpecies
import dev.equalparts.glyph_catch.data.canEvolveInto
import dev.equalparts.glyph_catch.data.gender
import dev.equalparts.glyph_catch.data.genderAs

const val MAX_POKEMON_LEVEL = 100

/**
 * Species whose evolution the player picks in the app: Tyrogue (by level) and Clamperl
 * (with the Linking Cord). Wurmple stays random, like in the main games; Burmy goes by gender.
 */
val CHOICE_EVOLUTIONS = setOf(236, 366)

/** What this species can evolve into with this gender: only female Combee become Vespiquen. */
private fun PokemonSpecies.evolutionTargets(gender: Gender): List<PokemonSpecies> =
    evolvesTo.filter { canEvolveInto(it, gender) }.mapNotNull { Pokemon[it] }

private fun CaughtPokemon.evolutionTargets(): List<PokemonSpecies> =
    Pokemon[speciesId]?.evolutionTargets(gender).orEmpty()

/** Whether leveling up to [level] with this [friendship] meets this evolution's requirement. */
private fun PokemonSpecies.evolvesOnLevelUp(level: Int, friendship: Int, time: DayTime): Boolean =
    when (val requirement = evolutionRequirement) {
        is EvolutionRequirement.Level -> level >= requirement.level
        is EvolutionRequirement.Friendship ->
            friendship >= FriendshipRules.EVOLVES_AT && (requirement.time == null || requirement.time == time)
        else -> false
    }

/**
 * The automatic level-up evolution, if any. Pokémon holding an Everstone and those whose
 * evolution is chosen by the player never evolve automatically.
 */
fun findLevelEvolutionTarget(pokemon: CaughtPokemon, time: DayTime = DayTime.now()): PokemonSpecies? {
    if (pokemon.evolutionLocked || pokemon.speciesId in CHOICE_EVOLUTIONS) return null
    return pokemon.evolutionTargets()
        .filter { it.evolvesOnLevelUp(pokemon.level, pokemon.friendship, time) }
        .randomOrNull()
}

fun findStoneEvolutionTarget(pokemon: CaughtPokemon, stone: Item): PokemonSpecies? =
    pokemon.evolutionTargets().firstOrNull { candidate ->
        val requirement = candidate.evolutionRequirement as? EvolutionRequirement.Stone
        requirement?.item == stone
    }

fun findTradeEvolutionTarget(pokemon: CaughtPokemon): PokemonSpecies? = pokemon.evolutionTargets().filter { candidate ->
    candidate.evolutionRequirement == EvolutionRequirement.Trade
}.randomOrNull()

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
    return pokemon.evolutionTargets().filter { candidate ->
        val requirement = candidate.evolutionRequirement as? EvolutionRequirement.Level
        requirement != null && pokemon.level >= requirement.level
    }
}

/**
 * All evolutions reachable with the Linking Cord (two for Clamperl).
 */
fun tradeTargets(pokemon: CaughtPokemon): List<PokemonSpecies> =
    pokemon.evolutionTargets().filter { it.evolutionRequirement == EvolutionRequirement.Trade }

/**
 * What the item would evolve this Pokémon into (two options for Clamperl).
 */
fun itemEvolutionTargets(item: Item, pokemon: CaughtPokemon): List<PokemonSpecies> = when {
    item == Item.LINKING_CORD -> tradeTargets(pokemon)
    canUseItemOn(item, pokemon) && item != Item.RARE_CANDY -> listOfNotNull(findStoneEvolutionTarget(pokemon, item))
    else -> emptyList()
}

/**
 * Whether the Everstone toggle makes sense for this Pokémon.
 */
fun canHoldEverstone(pokemon: CaughtPokemon): Boolean {
    if (pokemon.speciesId in CHOICE_EVOLUTIONS) return false
    return pokemon.evolutionTargets().any {
        it.evolutionRequirement is EvolutionRequirement.Level ||
            it.evolutionRequirement is EvolutionRequirement.Friendship
    }
}

/**
 * How many Rare Candies can be used at once: all you have, but never past level 100.
 */
fun maxRareCandies(pokemon: CaughtPokemon, owned: Int): Int =
    minOf(owned, MAX_POKEMON_LEVEL - pokemon.level).coerceAtLeast(0)

/**
 * What happens to a Pokémon's evolution if it gets this many Rare Candies.
 */
sealed class CandyEvolution {
    data object None : CandyEvolution()

    /** It would evolve, but it's holding an Everstone. */
    data object Blocked : CandyEvolution()

    /** Tyrogue: the player picks the evolution in its details. */
    data object Choice : CandyEvolution()

    /** Evolves along this path, e.g. Charmeleon then Charizard. */
    data class Evolves(val path: List<PokemonSpecies>) : CandyEvolution()

    /** Evolves into one of these at random, like Wurmple. */
    data class Random(val path: List<PokemonSpecies>, val options: List<PokemonSpecies>) : CandyEvolution()
}

fun candyEvolution(pokemon: CaughtPokemon, candies: Int, time: DayTime = DayTime.now()): CandyEvolution {
    var species = Pokemon[pokemon.speciesId] ?: return CandyEvolution.None
    val path = mutableListOf<PokemonSpecies>()
    var friendship = pokemon.friendship
    for (level in pokemon.level + 1..minOf(pokemon.level + candies, MAX_POKEMON_LEVEL)) {
        // Each candy is a level up, so it brings them a little closer too
        friendship = FriendshipRules.afterLevelUps(friendship, 1)
        val targets = species.evolutionTargets(pokemon.genderAs(species.id))
            .filter { it.evolvesOnLevelUp(level, friendship, time) }
        if (targets.isEmpty()) continue
        if (pokemon.evolutionLocked) return CandyEvolution.Blocked
        if (species.id in
            CHOICE_EVOLUTIONS
        ) {
            return if (path.isEmpty()) CandyEvolution.Choice else CandyEvolution.Evolves(path)
        }
        if (targets.size > 1) return CandyEvolution.Random(path.toList(), targets)
        species = targets.single()
        path += species
    }
    return if (path.isEmpty()) CandyEvolution.None else CandyEvolution.Evolves(path)
}
