package dev.equalparts.glyph_catch.util

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.DrawableRes
import dev.equalparts.glyph_catch.data.Forms

/**
 * Helpers for dynamically getting Pokémon sprite resources.
 */
object PokemonSpriteUtils {

    @DrawableRes
    fun getMatrixResourceId(context: Context, pokemonId: Int, form: String? = null): Int =
        find(context, "matrix", pokemonId, form)

    @DrawableRes
    fun getColorResourceId(context: Context, pokemonId: Int, shiny: Boolean = false, form: String? = null): Int =
        find(context, if (shiny) "shiny" else "color", pokemonId, form)

    @DrawableRes
    fun getSpriteResourceId(context: Context, pokemonId: Int, form: String? = null): Int {
        val resourceId = find(context, "sprite", pokemonId, form)
        return if (resourceId == 0) identifier(context, "sprite_0000") else resourceId
    }

    /**
     * Looks up `<prefix>_<id>_<form>`, falling back to the regular `<prefix>_<id>` sprite.
     */
    private fun find(context: Context, prefix: String, pokemonId: Int, form: String?): Int {
        val base = "${prefix}_${pokemonId.toString().padStart(4, '0')}"
        val suffix = Forms.spriteSuffix(pokemonId, form)
        if (suffix != null) {
            identifier(context, "${base}_$suffix").takeIf { it != 0 }?.let { return it }
        }
        return identifier(context, base)
    }

    @SuppressLint("DiscouragedApi")
    private fun identifier(context: Context, name: String): Int =
        context.resources.getIdentifier(name, "drawable", context.packageName)
}
