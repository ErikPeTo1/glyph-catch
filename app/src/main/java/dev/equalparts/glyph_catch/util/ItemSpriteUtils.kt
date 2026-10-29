package dev.equalparts.glyph_catch.util

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.DrawableRes
import dev.equalparts.glyph_catch.data.Item

/**
 * Helpers for loading item glyph matrix sprites.
 */
object ItemSpriteUtils {

    @DrawableRes
    @SuppressLint("DiscouragedApi")
    fun getMatrixResourceId(context: Context, item: Item): Int {
        val resourceName = "matrix_item_${item.name.lowercase()}"
        return context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    }

    @DrawableRes
    @SuppressLint("DiscouragedApi")
    fun getColorResourceId(context: Context, item: Item): Int {
        val resourceName = "item_color_${item.name.lowercase()}"
        return context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    }
}
