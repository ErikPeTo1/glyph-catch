package dev.equalparts.glyph_catch.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.equalparts.glyph_catch.R

enum class Professor(@param:DrawableRes val sprite: Int, @param:StringRes val nameRes: Int) {
    OAK(R.drawable.professor_oak, R.string.professor_oak),
    ELM(R.drawable.professor_elm, R.string.professor_elm),
    BIRCH(R.drawable.professor_birch, R.string.professor_birch),
    ROWAN(R.drawable.professor_rowan, R.string.professor_rowan),
    JUNIPER(R.drawable.professor_juniper, R.string.professor_juniper)
}

/**
 * Kanto is open from the start. Every other region opens when its professor hands over the
 * regional Pokédex, once the national Pokédex reaches [unlockAt] entries.
 */
enum class Region(
    val ids: IntRange,
    val professor: Professor,
    val pokedex: Item?,
    val unlockAt: Int,
    @param:StringRes val nameRes: Int
) {
    KANTO(1..151, Professor.OAK, null, 0, R.string.pokedex_region_kanto),
    JOHTO(152..251, Professor.ELM, Item.POKEDEX_JOHTO, 50, R.string.pokedex_region_johto),
    HOENN(252..386, Professor.BIRCH, Item.POKEDEX_HOENN, 120, R.string.pokedex_region_hoenn),
    SINNOH(387..493, Professor.ROWAN, Item.POKEDEX_SINNOH, 200, R.string.pokedex_region_sinnoh),
    UNOVA(494..649, Professor.JUNIPER, Item.POKEDEX_UNOVA, 300, R.string.pokedex_region_unova);

    val size: Int get() = ids.last - ids.first + 1

    companion object {
        fun of(speciesId: Int): Region = entries.firstOrNull { speciesId in it.ids } ?: KANTO

        fun ofProfessor(professor: Professor): Region = entries.first { it.professor == professor }

        fun ofPokedex(item: Item): Region? = when (item) {
            Item.POKEDEX_KANTO -> KANTO
            else -> entries.firstOrNull { it.pokedex == item }
        }

        /**
         * Regions whose Pokémon can appear: the unlocked ones, minus those switched off with
         * the Town Map (at least one always stays on).
         */
        fun active(owned: Set<Item>, disabled: Set<String>): Set<Region> {
            val unlocked = entries.filter { it.pokedex == null || it.pokedex in owned }.toSet()
            if (Item.TOWN_MAP !in owned) return unlocked
            return unlocked.filter { it.name !in disabled }.toSet().ifEmpty { unlocked }
        }
    }
}

/**
 * Key items that legendary and mythical Pokémon need before they appear. Their professor gives
 * them out once the matching rumor is unlocked.
 */
object LegendaryItems {
    val species: Map<Item, List<Int>> = mapOf(
        Item.OLD_SEA_MAP to listOf(151),
        Item.TIDAL_BELL to listOf(249),
        Item.CLEAR_BELL to listOf(250),
        Item.EON_TICKET to listOf(380, 381),
        Item.BLUE_ORB to listOf(382),
        Item.RED_ORB to listOf(383),
        Item.JADE_ORB to listOf(384),
        Item.METEORITE to listOf(386),
        Item.ADAMANT_ORB to listOf(483),
        Item.LUSTROUS_ORB to listOf(484),
        Item.MAGMA_STONE to listOf(485),
        Item.GRISEOUS_ORB to listOf(487),
        Item.LUNAR_WING to listOf(488),
        Item.MEMBER_CARD to listOf(491),
        Item.OAKS_LETTER to listOf(492),
        Item.AZURE_FLUTE to listOf(493),
        Item.LIBERTY_PASS to listOf(494),
        Item.REVEAL_GLASS to listOf(641, 642, 645),
        Item.LIGHT_STONE to listOf(643),
        Item.DARK_STONE to listOf(644),
        Item.DNA_SPLICERS to listOf(646)
    )

    private val itemFor: Map<Int, Item> = species.flatMap { (item, ids) -> ids.map { it to item } }.toMap()

    fun requiredItem(speciesId: Int): Item? = itemFor[speciesId]

    fun region(item: Item): Region = Region.of(species.getValue(item).first())

    /** The Pokédex size at which the first of its rumors unlocks. */
    fun unlockAt(item: Item): Int {
        val ids = species.getValue(item)
        return Rumors.all.filter { it.speciesId in ids }.minOf { it.unlockAt }
    }

    fun rumor(item: Item): Rumor = Rumors.all.filter { it.speciesId in species.getValue(item) }.minBy { it.unlockAt }

    fun speciesNames(item: Item): String =
        species.getValue(item).mapNotNull { Pokemon[it]?.displayName }.joinToString(" / ")
}

/** Legendary and mythical Pokémon: after release they can be met again, one at a time. */
val LEGENDARY_SPECIES: Set<Int> = Rumors.all.map { it.speciesId }.toSet()

/** Mythical Pokémon met at gentle levels in the games: Mew, Celebi, Jirachi, Phione, Manaphy, Shaymin, Victini, Meloetta. */
private val GENTLE_MYTHICALS = setOf(151, 251, 385, 489, 490, 492, 494, 648)

private const val ARCEUS = 493

/**
 * The level a legendary is caught at: strong enough to match what it takes to find them (Mewtwo
 * wants six Pokémon at level 60). `null` for everything else.
 */
fun legendaryLevels(speciesId: Int): IntRange? = when (speciesId) {
    ARCEUS -> 80..90
    in GENTLE_MYTHICALS -> 30..45
    in LEGENDARY_SPECIES -> 70..80
    else -> null
}

val REGION_POKEDEXES = listOf(Item.POKEDEX_JOHTO, Item.POKEDEX_HOENN, Item.POKEDEX_SINNOH, Item.POKEDEX_UNOVA)

val Item.isKeyItem: Boolean get() = this == Item.POKEDEX_KANTO ||
    this in REGION_POKEDEXES ||
    this == Item.TOWN_MAP ||
    this == Item.SHINY_CHARM ||
    this == Item.EXP_SHARE ||
    this in LegendaryItems.species

/**
 * Whether a species can appear right now: its region must be active and, for legendaries
 * with a key item, the player must own that item.
 */
fun canSpawnSpecies(speciesId: Int, owned: Set<Item>, activeRegions: Set<Region>): Boolean {
    if (Region.of(speciesId) !in activeRegions) return false
    val item = LegendaryItems.requiredItem(speciesId) ?: return true
    return item in owned
}
