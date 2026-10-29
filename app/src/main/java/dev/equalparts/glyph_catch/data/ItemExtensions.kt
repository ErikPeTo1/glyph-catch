package dev.equalparts.glyph_catch.data

import androidx.annotation.StringRes
import dev.equalparts.glyph_catch.R

const val SUPER_ROD_DURATION_MINUTES = 60

val FISHING_RODS = listOf(Item.OLD_ROD, Item.GOOD_ROD, Item.SUPER_ROD)

val Item.isFishingRod: Boolean get() = this in FISHING_RODS

@StringRes
fun Item.nameRes(): Int = when (this) {
    Item.FIRE_STONE -> R.string.item_fire_stone_name
    Item.WATER_STONE -> R.string.item_water_stone_name
    Item.THUNDER_STONE -> R.string.item_thunder_stone_name
    Item.LEAF_STONE -> R.string.item_leaf_stone_name
    Item.MOON_STONE -> R.string.item_moon_stone_name
    Item.SUN_STONE -> R.string.item_sun_stone_name
    Item.SUPER_ROD -> R.string.item_super_rod_name
    Item.RARE_CANDY -> R.string.item_rare_candy_name
    Item.LINKING_CORD -> R.string.item_linking_cord_name
    Item.REPEL -> R.string.item_repel_name
    Item.SHINY_STONE -> R.string.item_shiny_stone_name
    Item.DUSK_STONE -> R.string.item_dusk_stone_name
    Item.DAWN_STONE -> R.string.item_dawn_stone_name
    Item.ICE_STONE -> R.string.item_ice_stone_name
    Item.OLD_ROD -> R.string.item_old_rod_name
    Item.GOOD_ROD -> R.string.item_good_rod_name
}

@StringRes
fun Item.descriptionRes(): Int = when (this) {
    Item.FIRE_STONE -> R.string.item_fire_stone_description
    Item.WATER_STONE -> R.string.item_water_stone_description
    Item.THUNDER_STONE -> R.string.item_thunder_stone_description
    Item.LEAF_STONE -> R.string.item_leaf_stone_description
    Item.MOON_STONE -> R.string.item_moon_stone_description
    Item.SUN_STONE -> R.string.item_sun_stone_description
    Item.SUPER_ROD -> R.string.item_super_rod_description
    Item.RARE_CANDY -> R.string.item_rare_candy_description
    Item.LINKING_CORD -> R.string.item_linking_cord_description
    Item.REPEL -> R.string.item_repel_description
    Item.SHINY_STONE -> R.string.item_shiny_stone_description
    Item.DUSK_STONE -> R.string.item_dusk_stone_description
    Item.DAWN_STONE -> R.string.item_dawn_stone_description
    Item.ICE_STONE -> R.string.item_ice_stone_description
    Item.OLD_ROD -> R.string.item_old_rod_description
    Item.GOOD_ROD -> R.string.item_good_rod_description
}

fun Item.effectDurationMinutes(): Int? = when (this) {
    Item.OLD_ROD, Item.GOOD_ROD, Item.SUPER_ROD -> SUPER_ROD_DURATION_MINUTES
    else -> null
}

/**
 * Groups items like the Bag pockets of the main games: stones, other evolution items,
 * fishing rods and everything else.
 */
fun Item.category(): Int = when (this) {
    Item.FIRE_STONE, Item.WATER_STONE, Item.THUNDER_STONE, Item.LEAF_STONE, Item.MOON_STONE,
    Item.SUN_STONE, Item.SHINY_STONE, Item.DUSK_STONE, Item.DAWN_STONE, Item.ICE_STONE -> 0
    Item.RARE_CANDY, Item.LINKING_CORD -> 1
    Item.OLD_ROD, Item.GOOD_ROD, Item.SUPER_ROD -> 2
    Item.REPEL -> 3
}
