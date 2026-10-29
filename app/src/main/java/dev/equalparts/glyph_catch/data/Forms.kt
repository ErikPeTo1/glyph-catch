package dev.equalparts.glyph_catch.data

import kotlin.random.Random

/**
 * Alternate forms. The first form of each species uses the regular sprite; the others have
 * their own sprites named `<prefix>_<id>_<form>`. Forms don't count separately in the Pokédex.
 */
object Forms {
    private val UNOWN = ('a'..'z').map { it.toString() } + listOf("exclamation", "question")
    private val SEASONS = listOf("spring", "summer", "autumn", "winter")
    private val CLOAKS = listOf("plant", "sandy", "trash")
    private val SEAS = listOf("west", "east")

    private val forms: Map<Int, List<String>> = mapOf(
        201 to UNOWN,
        351 to listOf("normal", "sunny", "rainy", "snowy"),
        412 to CLOAKS,
        413 to CLOAKS,
        422 to SEAS,
        423 to SEAS,
        585 to SEASONS,
        586 to SEASONS
    )

    /**
     * Conditions at the moment a Pokémon appears.
     */
    data class Conditions(
        val sunny: Boolean = false,
        val rainy: Boolean = false,
        val snowy: Boolean = false,
        val season: String = "spring"
    )

    fun formsOf(speciesId: Int): List<String> = forms[speciesId].orEmpty()

    fun hasForms(speciesId: Int): Boolean = speciesId in forms

    /**
     * Picks the form of a wild Pokémon: Castform follows the weather, Deerling and Sawsbuck the
     * season, and Unown, Burmy, Wormadam, Shellos and Gastrodon are random.
     */
    fun formFor(speciesId: Int, conditions: Conditions, random: Random = Random.Default): String? = when (speciesId) {
        351 -> when {
            conditions.snowy -> "snowy"
            conditions.rainy -> "rainy"
            conditions.sunny -> "sunny"
            else -> "normal"
        }
        585, 586 -> conditions.season
        else -> forms[speciesId]?.random(random)
    }

    /**
     * Forms carry over when the evolution has the same forms (Burmy → Wormadam,
     * Shellos → Gastrodon, Deerling → Sawsbuck); otherwise they're dropped (Burmy → Mothim).
     */
    fun formAfterEvolution(form: String?, targetSpeciesId: Int): String? =
        form?.takeIf { it in formsOf(targetSpeciesId) }

    /**
     * Suffix for the sprite resource name, or null for the regular sprite.
     */
    fun spriteSuffix(speciesId: Int, form: String?): String? {
        val all = formsOf(speciesId)
        return form?.takeIf { it in all && it != all.first() }
    }
}
