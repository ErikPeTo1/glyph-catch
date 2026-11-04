package dev.equalparts.glyph_catch.data

import dev.equalparts.glyph_catch.util.EventHelpers
import kotlin.random.Random

/**
 * Eggs: the only way to get baby Pokémon. They're found when catching, advance while the
 * phone is locked (alongside training) and hatch onto the Glyph Matrix like a wild Pokémon.
 */
object Eggs {
    const val MAX_EGGS = 3
    const val DROP_CHANCE = 0.08
    const val POOL_NAME = "Egg"

    /**
     * Hours with the phone locked that each baby needs to hatch.
     */
    val hatchHours: Map<Int, Int> = mapOf(
        172 to 2, // Pichu
        173 to 2, // Cleffa
        174 to 2, // Igglybuff
        298 to 2, // Azurill
        360 to 2, // Wynaut
        406 to 2, // Budew
        438 to 2, // Bonsly
        175 to 5, // Togepi
        238 to 5, // Smoochum
        239 to 5, // Elekid
        240 to 5, // Magby
        433 to 5, // Chingling
        439 to 5, // Mime Jr.
        458 to 5, // Mantyke
        236 to 10, // Tyrogue
        440 to 10, // Happiny
        446 to 10, // Munchlax
        447 to 10 // Riolu
    )

    val species: List<PokemonSpecies> get() = hatchHours.keys.mapNotNull { Pokemon[it] }

    /**
     * A random egg among the babies that [allowed] accepts (those from open regions), or
     * `null` if there are none.
     */
    fun newEgg(random: Random = Random.Default, allowed: (Int) -> Boolean = { true }): Egg? {
        val speciesId = hatchHours.keys.filter(allowed).randomOrNull(random) ?: return null
        return Egg(speciesId = speciesId, minutesNeeded = hatchHours.getValue(speciesId) * 60)
    }

    /**
     * Hatched Pokémon are four times as likely to be shiny as wild ones, holiday boosts included.
     */
    fun shinyOdds(): Int = (EventHelpers.shinyOdds() / 4).coerceAtLeast(1)
}
