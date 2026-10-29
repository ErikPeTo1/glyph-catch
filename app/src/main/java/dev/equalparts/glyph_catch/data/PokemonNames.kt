package dev.equalparts.glyph_catch.data

import android.content.Context
import androidx.annotation.StringRes
import dev.equalparts.glyph_catch.R

/**
 * Official Pokémon names in the phone's language (from PokeAPI), loaded when the app starts.
 * Falls back to the English names in [Pokemon].
 */
object PokemonNames {
    @Volatile
    private var names: Array<String>? = null

    fun init(context: Context) {
        names = context.resources.getStringArray(R.array.pokemon_names)
    }

    fun of(species: PokemonSpecies): String = names?.getOrNull(species.id - 1) ?: species.name
}

val PokemonSpecies.displayName: String get() = PokemonNames.of(this)

@StringRes
fun Type.nameRes(): Int = when (this) {
    Type.NORMAL -> R.string.type_normal
    Type.FIRE -> R.string.type_fire
    Type.WATER -> R.string.type_water
    Type.ELECTRIC -> R.string.type_electric
    Type.GRASS -> R.string.type_grass
    Type.ICE -> R.string.type_ice
    Type.FIGHTING -> R.string.type_fighting
    Type.POISON -> R.string.type_poison
    Type.GROUND -> R.string.type_ground
    Type.FLYING -> R.string.type_flying
    Type.PSYCHIC -> R.string.type_psychic
    Type.BUG -> R.string.type_bug
    Type.ROCK -> R.string.type_rock
    Type.GHOST -> R.string.type_ghost
    Type.DRAGON -> R.string.type_dragon
    Type.DARK -> R.string.type_dark
    Type.STEEL -> R.string.type_steel
    Type.FAIRY -> R.string.type_fairy
}
