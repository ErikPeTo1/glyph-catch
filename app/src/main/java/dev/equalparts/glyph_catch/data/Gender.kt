package dev.equalparts.glyph_catch.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

enum class Gender(val symbol: String?, val color: Color) {
    MALE("♂", Color(0xFF4F8EF7)),
    FEMALE("♀", Color(0xFFF2689A)),
    UNKNOWN(null, Color.Unspecified);

    companion object {
        /**
         * Rolled once per Pokémon from its id, with its species' ratio like in the games: always
         * the same, even after evolving or restoring a backup, and nothing to store.
         * [genderRate] is the female share in eighths, -1 for genderless species.
         */
        fun roll(pokemonId: String, genderRate: Int): Gender = when {
            genderRate < 0 -> UNKNOWN
            Random(pokemonId.hashCode().toLong() * SALT).nextInt(8) < genderRate -> FEMALE
            else -> MALE
        }

        private const val SALT = 0x5DEECE66DL
    }
}

/** This Pokémon's gender, with its species' ratio from the Pokédex. */
fun CaughtPokemon.gender(context: Context): Gender =
    Gender.roll(id, PokedexData.entry(context, speciesId)?.genderRate ?: -1)

/** The same, once [PokedexData.init] has run; unknown before that. */
val CaughtPokemon.gender: Gender get() = genderAs(speciesId)

/** The gender it would have as [speciesId]: it's rolled again with each species' ratio, like in Black and White. */
fun CaughtPokemon.genderAs(speciesId: Int): Gender =
    PokedexData.genderRate(speciesId)?.let { Gender.roll(id, it) } ?: Gender.UNKNOWN

/** Evolutions only one gender takes, like in the main games: only female Combee become Vespiquen. */
private val GENDERED_EVOLUTIONS = mapOf(
    416 to Gender.FEMALE, // Vespiquen
    413 to Gender.FEMALE, // Wormadam
    414 to Gender.MALE, // Mothim
    475 to Gender.MALE, // Gallade
    478 to Gender.FEMALE // Froslass
)

/** The only gender that evolves into [targetId], if it is one of those. */
fun requiredGender(targetId: Int): Gender? = GENDERED_EVOLUTIONS[targetId]

/** Whether a Pokémon of this gender can evolve into [targetId]. Unknown never blocks anything. */
fun canEvolveInto(targetId: Int, gender: Gender): Boolean =
    GENDERED_EVOLUTIONS[targetId]?.let { gender == Gender.UNKNOWN || it == gender } ?: true

/** Whether it should use its species' female sprite, like Pikachu's heart-shaped tail. */
fun CaughtPokemon.usesFemaleSprite(context: Context): Boolean =
    gender(context) == Gender.FEMALE && PokedexData.entry(context, speciesId)?.femaleSprite == true
