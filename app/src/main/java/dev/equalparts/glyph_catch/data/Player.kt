package dev.equalparts.glyph_catch.data

import androidx.annotation.DrawableRes
import dev.equalparts.glyph_catch.R

/**
 * How the player looks: the heroes of FireRed/LeafGreen and Emerald, with their walking frames
 * (from the games) for the town and their trainer portrait for choosing.
 */
enum class PlayerLook(@param:DrawableRes val walking: Int, @param:DrawableRes val portrait: Int) {
    RED(R.drawable.town_hero_red, R.drawable.hero_portrait_red),
    LEAF(R.drawable.town_hero_leaf, R.drawable.hero_portrait_leaf),
    BRENDAN(R.drawable.town_hero_brendan, R.drawable.hero_portrait_brendan),
    MAY(R.drawable.town_hero_may, R.drawable.hero_portrait_may);

    companion object {
        /** The one saved, or Brendan for whoever hasn't told Oak yet. */
        fun of(name: String?) = entries.firstOrNull { it.name == name } ?: BRENDAN
    }
}

/** The longest name, like the games after Gen III. */
const val PLAYER_NAME_LENGTH = 10
