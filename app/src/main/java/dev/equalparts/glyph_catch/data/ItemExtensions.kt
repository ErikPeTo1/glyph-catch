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
    Item.POKEDEX_JOHTO -> R.string.item_pokedex_johto_name
    Item.POKEDEX_HOENN -> R.string.item_pokedex_hoenn_name
    Item.POKEDEX_SINNOH -> R.string.item_pokedex_sinnoh_name
    Item.POKEDEX_UNOVA -> R.string.item_pokedex_unova_name
    Item.TOWN_MAP -> R.string.item_town_map_name
    Item.OLD_SEA_MAP -> R.string.item_old_sea_map_name
    Item.TIDAL_BELL -> R.string.item_tidal_bell_name
    Item.CLEAR_BELL -> R.string.item_clear_bell_name
    Item.METEORITE -> R.string.item_meteorite_name
    Item.EON_TICKET -> R.string.item_eon_ticket_name
    Item.BLUE_ORB -> R.string.item_blue_orb_name
    Item.RED_ORB -> R.string.item_red_orb_name
    Item.JADE_ORB -> R.string.item_jade_orb_name
    Item.ADAMANT_ORB -> R.string.item_adamant_orb_name
    Item.LUSTROUS_ORB -> R.string.item_lustrous_orb_name
    Item.GRISEOUS_ORB -> R.string.item_griseous_orb_name
    Item.LUNAR_WING -> R.string.item_lunar_wing_name
    Item.MAGMA_STONE -> R.string.item_magma_stone_name
    Item.MEMBER_CARD -> R.string.item_member_card_name
    Item.OAKS_LETTER -> R.string.item_oaks_letter_name
    Item.AZURE_FLUTE -> R.string.item_azure_flute_name
    Item.LIBERTY_PASS -> R.string.item_liberty_pass_name
    Item.LIGHT_STONE -> R.string.item_light_stone_name
    Item.DARK_STONE -> R.string.item_dark_stone_name
    Item.DNA_SPLICERS -> R.string.item_dna_splicers_name
    Item.REVEAL_GLASS -> R.string.item_reveal_glass_name
    Item.POKEDEX_KANTO -> R.string.item_pokedex_kanto_name
    Item.SHINY_CHARM -> R.string.item_shiny_charm_name
    Item.EXP_SHARE -> R.string.item_exp_share_name
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
    Item.POKEDEX_KANTO, Item.POKEDEX_JOHTO, Item.POKEDEX_HOENN,
    Item.POKEDEX_SINNOH, Item.POKEDEX_UNOVA -> R.string.item_pokedex_description
    Item.TOWN_MAP -> R.string.item_town_map_description
    Item.SHINY_CHARM -> R.string.item_shiny_charm_description
    Item.EXP_SHARE -> R.string.item_exp_share_description
    Item.OLD_SEA_MAP,
    Item.TIDAL_BELL,
    Item.CLEAR_BELL,
    Item.METEORITE,
    Item.EON_TICKET,
    Item.BLUE_ORB,
    Item.RED_ORB,
    Item.JADE_ORB,
    Item.ADAMANT_ORB,
    Item.LUSTROUS_ORB,
    Item.GRISEOUS_ORB,
    Item.LUNAR_WING,
    Item.MAGMA_STONE,
    Item.MEMBER_CARD,
    Item.OAKS_LETTER,
    Item.AZURE_FLUTE,
    Item.LIBERTY_PASS,
    Item.LIGHT_STONE,
    Item.DARK_STONE,
    Item.DNA_SPLICERS,
    Item.REVEAL_GLASS -> R.string.item_legendary_description
}

fun Item.effectDurationMinutes(): Int? = when (this) {
    Item.OLD_ROD, Item.GOOD_ROD, Item.SUPER_ROD -> SUPER_ROD_DURATION_MINUTES
    else -> null
}

/**
 * Groups items like the Bag pockets of the main games: stones, other evolution items,
 * fishing rods, everything else and key items.
 */
fun Item.category(): Int = when (this) {
    Item.FIRE_STONE, Item.WATER_STONE, Item.THUNDER_STONE, Item.LEAF_STONE, Item.MOON_STONE,
    Item.SUN_STONE, Item.SHINY_STONE, Item.DUSK_STONE, Item.DAWN_STONE, Item.ICE_STONE -> 0
    Item.RARE_CANDY, Item.LINKING_CORD -> 1
    Item.OLD_ROD, Item.GOOD_ROD, Item.SUPER_ROD -> 2
    Item.REPEL -> 3
    else -> 4
}
