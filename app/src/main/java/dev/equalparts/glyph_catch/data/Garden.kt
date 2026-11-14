package dev.equalparts.glyph_catch.data

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import dev.equalparts.glyph_catch.R
import java.time.LocalDate
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The five flavors of the main games, one berry each. Every Pokémon has a favorite, and later
 * on they can go into Pokéblocks or cooking.
 */
enum class Flavor(val berry: String, @param:StringRes val nameRes: Int) {
    SPICY("cheri", R.string.garden_flavor_spicy),
    DRY("chesto", R.string.garden_flavor_dry),
    SWEET("pecha", R.string.garden_flavor_sweet),
    BITTER("rawst", R.string.garden_flavor_bitter),
    SOUR("aspear", R.string.garden_flavor_sour);

    companion object {
        /** A Pokémon's favorite flavor, fixed for that Pokémon. */
        fun favoriteOf(pokemonId: String): Flavor = entries[Math.floorMod(pokemonId.hashCode(), entries.size)]
    }
}

/** A decoration standing in the Garden, its feet at ([x], [y]) as fractions of the scene. */
@Serializable
data class PlacedDecoration(val id: String, val x: Float, val y: Float)

/**
 * A berry growing in one of the Garden's plots: it grows while the phone is locked. Like in
 * Emerald, each stage it's watered in gives one more berry at the end.
 */
@Serializable
data class BerryPlot(
    val flavor: Flavor,
    val plantedAt: Long,
    val minutes: Int = 0,
    val watered: Set<PlotStage> = emptySet()
) {
    val ripe: Boolean get() = minutes >= GROW_MINUTES

    val stage: PlotStage
        get() = when {
            ripe -> PlotStage.BERRIES
            minutes >= GROW_MINUTES * 6 / 10 -> PlotStage.FLOWER
            minutes >= GROW_MINUTES / 4 -> PlotStage.TALL
            else -> PlotStage.SPROUT
        }

    val progress: Float get() = (minutes.toFloat() / GROW_MINUTES).coerceIn(0f, 1f)

    /** Still growing and not watered in this stage yet. */
    val thirsty: Boolean get() = !ripe && stage !in watered

    /** How long it's been in its current stage. */
    val minutesInStage: Int
        get() = minutes - when (stage) {
            PlotStage.SPROUT -> 0
            PlotStage.TALL -> GROW_MINUTES / 4
            PlotStage.FLOWER -> GROW_MINUTES * 6 / 10
            PlotStage.BERRIES -> GROW_MINUTES
        }

    /**
     * How many berries it gives: one or two (decided when it was planted), and one more for each
     * stage it was watered in, up to five.
     */
    val yield: Int get() = 1 + Random(plantedAt).nextInt(2) + watered.size

    companion object {
        /** Eight hours of locked phone: a night. */
        const val GROW_MINUTES = 480

        /** A Water-type in the Garden waters on its own, if nobody did, after this long in a stage. */
        const val AUTO_WATER_AFTER = 30
    }
}

/** The growth stages of Emerald's berry trees, after the soft soil. */
enum class PlotStage { SPROUT, TALL, FLOWER, BERRIES }

/**
 * The Garden's bag: berries, the decorations your Pokémon brought (and the ones out in the
 * meadow), backgrounds and the berry plots. Kept with the rest of the save, so backups include it.
 */
@Serializable
data class GardenState(
    val berries: Map<Flavor, Int> = emptyMap(),
    val decorations: Set<String> = emptySet(),
    val placed: List<PlacedDecoration> = emptyList(),
    val backgrounds: Set<String> = setOf(GardenCatalog.DEFAULT_BACKGROUND),
    val background: String = GardenCatalog.DEFAULT_BACKGROUND,
    val plots: List<BerryPlot?> = List(GardenCatalog.PLOTS) { null },
    /** Who lives in each zone (caught ids), by zone id. The Meadow is the team, so it isn't here. */
    val zones: Map<String, List<String>> = emptyMap()
) {
    fun residents(zone: GardenZone): List<String> = zones[zone.id].orEmpty()

    /** The zone this Pokémon lives in, if it was sent to one. */
    fun zoneOf(pokemonId: String): GardenZone? = GardenZone.entries.firstOrNull { pokemonId in residents(it) }

    /** Moves a Pokémon into [zone] (out of any other one), if there's room. */
    fun moving(pokemonId: String, zone: GardenZone): GardenState? {
        val here = residents(zone)
        if (pokemonId in here || here.size >= zone.capacity) return null
        val others = zones.mapValues { (_, ids) -> ids - pokemonId }
        return copy(zones = others + (zone.id to here + pokemonId))
    }

    fun leaving(pokemonId: String): GardenState = copy(zones = zones.mapValues { (_, ids) -> ids - pokemonId })

    val totalBerries: Int get() = berries.values.sum()

    fun berries(flavor: Flavor): Int = berries[flavor] ?: 0

    fun withBerry(flavor: Flavor, amount: Int): GardenState =
        copy(berries = berries + (flavor to (berries(flavor) + amount).coerceAtLeast(0)))

    fun withGift(gift: GardenGift): GardenState = when (gift) {
        is GardenGift.Berry -> withBerry(gift.flavor, 1)
        is GardenGift.Decoration -> copy(decorations = decorations + gift.id)
    }

    /** Plants a berry from the bag in an empty plot; null if there's no such berry or room. */
    fun planting(plot: Int, flavor: Flavor, now: Long = System.currentTimeMillis()): GardenState? {
        if (berries(flavor) <= 0 || plots.getOrNull(plot) != null || plot !in plots.indices) return null
        return withBerry(flavor, -1).copy(plots = plots.toMutableList().also { it[plot] = BerryPlot(flavor, now) })
    }

    /** Picks a ripe plot's berries, leaving the soil empty again. */
    fun harvesting(plot: Int): GardenState? {
        val grown = plots.getOrNull(plot)?.takeIf { it.ripe } ?: return null
        return withBerry(grown.flavor, grown.yield).copy(plots = plots.toMutableList().also { it[plot] = null })
    }

    /** Waters a plot in its current stage; null if it doesn't need it. */
    fun watering(plot: Int): GardenState? {
        val growing = plots.getOrNull(plot)?.takeIf { it.thirsty } ?: return null
        return copy(
            plots = plots.toMutableList().also {
                it[plot] =
                    growing.copy(watered = growing.watered + growing.stage)
            }
        )
    }

    /** What a Water-type does while you're away: waters whatever has been thirsty a while. */
    fun autoWatered(): GardenState = plots.indices.fold(this) { state, index ->
        val plot = state.plots[index]
        if (plot != null && plot.thirsty && plot.minutesInStage >= BerryPlot.AUTO_WATER_AFTER) {
            state.watering(index) ?: state
        } else {
            state
        }
    }

    /** More locked minutes for everything that's still growing. */
    fun grown(minutes: Int = 1): GardenState =
        copy(plots = plots.map { plot -> plot?.takeIf { !it.ripe }?.copy(minutes = plot.minutes + minutes) ?: plot })

    val growing: Boolean get() = plots.any { it != null && !it.ripe }
}

class GardenStore private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<GardenState> = _state.asStateFlow()

    private fun load(): GardenState = prefs.getString(KEY_STATE, null)
        ?.let { runCatching { json.decodeFromString<GardenState>(it) }.getOrNull() }
        ?.let { it.copy(plots = List(GardenCatalog.PLOTS) { i -> it.plots.getOrNull(i) }) }
        ?: GardenState()

    @Synchronized
    fun update(transform: (GardenState) -> GardenState) {
        val next = transform(_state.value)
        if (next == _state.value) return
        _state.value = next
        prefs.edit { putString(KEY_STATE, json.encodeToString(next)) }
    }

    /** After importing a backup, which rewrites the preferences underneath. */
    @Synchronized
    fun reload() {
        _state.value = load()
    }

    /**
     * Called every locked minute: berries in the Orchard grow, half as fast again with a
     * Grass-type there ([fast], every other minute counts twice), and a Water-type waters them.
     */
    fun grow(waterer: Boolean, fast: Boolean, minute: Long) {
        if (!_state.value.growing) return
        val minutes = if (fast && minute % 2 == 0L) 2 else 1
        update { it.grown(minutes).let { grown -> if (waterer) grown.autoWatered() else grown } }
    }

    companion object {
        private const val PREFS_NAME = "glyph_catch_prefs"
        private const val KEY_STATE = "garden_state"
        const val MAX_PLACED = 8
        private val json = Json { ignoreUnknownKeys = true }

        @Volatile
        private var instance: GardenStore? = null

        fun get(context: Context): GardenStore =
            instance ?: synchronized(this) { instance ?: GardenStore(context).also { instance = it } }
    }
}

/** Something a Pokémon brings you: a berry, or now and then a decoration. */
sealed interface GardenGift {
    data class Berry(val flavor: Flavor) : GardenGift

    data class Decoration(val id: String) : GardenGift
}

/** What a decoration is, which decides what Pokémon do with it. */
enum class DecorationKind { DOLL, BIG_DOLL, CUSHION, MAT, PLANT, TENT, SLIDE, TIRE, OTHER }

/** What it takes to get a Garden background. */
sealed interface BackgroundUnlock {
    /** Registering this many species of a type in the Pokédex. */
    data class TypeCount(val type: Type, val count: Int) : BackgroundUnlock

    /** Getting a region's Pokédex from its professor. */
    data class RegionPokedex(val region: Region) : BackgroundUnlock
}

/** Emerald's Secret Base decorations and Showdown's backgrounds: none of it is sold, it's all earned. */
object GardenCatalog {
    const val DEFAULT_BACKGROUND = "meadow"
    const val PLOTS = 15

    /** The small dolls and the Pokémon each one is. */
    val dolls: Map<String, Int> = mapOf(
        "azurill_doll" to 298, "baltoy_doll" to 343, "chikorita_doll" to 152, "clefairy_doll" to 35,
        "cyndaquil_doll" to 155, "ditto_doll" to 132, "duskull_doll" to 355, "gulpin_doll" to 316,
        "jigglypuff_doll" to 39, "kecleon_doll" to 352, "lotad_doll" to 270, "marill_doll" to 183,
        "meowth_doll" to 52, "mudkip_doll" to 258, "pichu_doll" to 172, "pikachu_doll" to 25,
        "seedot_doll" to 273, "skitty_doll" to 300, "smoochum_doll" to 238, "swablu_doll" to 333,
        "togepi_doll" to 175, "torchic_doll" to 255, "totodile_doll" to 158, "treecko_doll" to 252,
        "wynaut_doll" to 360
    )

    /** The big dolls: a Pokémon of that family brings it once it can't love you more. */
    val bigDolls: Map<String, Int> = mapOf(
        "big_snorlax_doll" to 143, "big_lapras_doll" to 131, "big_wailmer_doll" to 320,
        "big_venusaur_doll" to 3, "big_charizard_doll" to 6, "big_blastoise_doll" to 9,
        "big_rhydon_doll" to 112, "big_regirock_doll" to 377, "big_regice_doll" to 378,
        "big_registeel_doll" to 379
    )

    private val cushions = listOf(
        "ball_cushion", "diamond_cushion", "fire_cushion", "grass_cushion", "kiss_cushion", "pika_cushion",
        "round_cushion", "spin_cushion", "water_cushion", "zigzag_cushion"
    )
    private val mats = listOf(
        "attract_mat",
        "fire_blast_mat",
        "fissure_mat",
        "powder_snow_mat",
        "spikes_mat",
        "surf_mat",
        "thunder_mat"
    )
    private val plants = listOf(
        "pretty_flowers",
        "red_plant",
        "tropical_plant",
        "colorful_plant",
        "big_plant",
        "gorgeous_plant"
    )
    private val others = listOf(
        "glass_ornament", "sand_ornament", "brick_desk", "camp_desk", "comfort_desk", "hard_desk", "heavy_desk",
        "pretty_desk", "ragged_desk", "blue_brick", "red_brick", "yellow_brick", "solid_board", "stand",
        "breakable_door"
    )
    private val tents = listOf("red_tent", "blue_tent")

    /** Everything that isn't a doll: what fond Pokémon bring at random. */
    val furniture: List<String> = cushions + mats + plants + tents + listOf("slide", "tire") + others

    /** Every decoration, in the order the bag shows them. */
    val decorations: List<String> = dolls.keys.toList() + bigDolls.keys + furniture

    fun kindOf(id: String): DecorationKind = when (id) {
        in dolls -> DecorationKind.DOLL
        in bigDolls -> DecorationKind.BIG_DOLL
        in cushions -> DecorationKind.CUSHION
        in mats -> DecorationKind.MAT
        in plants -> DecorationKind.PLANT
        in tents -> DecorationKind.TENT
        "slide" -> DecorationKind.SLIDE
        "tire" -> DecorationKind.TIRE
        else -> DecorationKind.OTHER
    }

    class Background(val id: String, val unlock: BackgroundUnlock?)

    val backgrounds: List<Background> = listOf(
        Background(DEFAULT_BACKGROUND, null),
        Background("route", BackgroundUnlock.RegionPokedex(Region.JOHTO)),
        Background("forest", BackgroundUnlock.TypeCount(Type.GRASS, 25)),
        Background("river", BackgroundUnlock.TypeCount(Type.WATER, 25)),
        Background("beach", BackgroundUnlock.RegionPokedex(Region.HOENN)),
        Background("beachshore", BackgroundUnlock.TypeCount(Type.FLYING, 25)),
        Background("desert", BackgroundUnlock.TypeCount(Type.GROUND, 15)),
        Background("mountain", BackgroundUnlock.TypeCount(Type.ROCK, 15)),
        Background("thunderplains", BackgroundUnlock.TypeCount(Type.ELECTRIC, 15)),
        Background("earthycave", BackgroundUnlock.RegionPokedex(Region.SINNOH)),
        Background("city", BackgroundUnlock.RegionPokedex(Region.UNOVA)),
        Background("icecave", BackgroundUnlock.TypeCount(Type.ICE, 10)),
        Background("dampcave", BackgroundUnlock.TypeCount(Type.POISON, 15)),
        Background("volcanocave", BackgroundUnlock.TypeCount(Type.FIRE, 20)),
        Background("deepsea", BackgroundUnlock.TypeCount(Type.WATER, 60))
    )

    /** How far along a background is, as (done, needed): a region Pokédex counts as one step. */
    fun progress(background: Background, registered: Collection<Int>, items: Set<Item>): Pair<Int, Int> =
        when (val unlock = background.unlock) {
            null -> 1 to 1
            is BackgroundUnlock.RegionPokedex -> (if (unlock.region.pokedex in items) 1 else 0) to 1
            is BackgroundUnlock.TypeCount -> registered.count { id ->
                Pokemon[id]?.let { it.type1 == unlock.type || it.type2 == unlock.type } == true
            }.coerceAtMost(unlock.count) to unlock.count
        }

    /** The backgrounds you've earned but don't have yet. */
    fun newlyEarned(owned: Set<String>, registered: Collection<Int>, items: Set<Item>): List<String> =
        backgrounds.filter { it.id !in owned }
            .filter { progress(it, registered, items).let { (done, needed) -> done >= needed } }
            .map { it.id }

    @SuppressLint("DiscouragedApi")
    @DrawableRes
    fun drawable(context: Context, prefix: String, id: String): Int =
        context.resources.getIdentifier("garden_${prefix}_$id", "drawable", context.packageName)

    /** The tree for a plot: Emerald's soft soil when empty, then the berry's own tree. */
    fun plotDrawable(context: Context, plot: BerryPlot?): Int = when {
        plot == null -> drawable(context, "tree", "dirt")
        plot.stage == PlotStage.SPROUT -> drawable(context, "tree", "sprout")
        else -> drawable(context, "tree", "${plot.flavor.berry}_${plot.stage.name.lowercase()}")
    }

    /** Whether this Pokémon can water the plots on its own. */
    fun waters(pokemon: CaughtPokemon): Boolean =
        Pokemon[pokemon.speciesId]?.let { it.type1 == Type.WATER || it.type2 == Type.WATER } == true

    /** The dolls of this Pokémon's family, from [among]. */
    fun dollsOf(speciesId: Int, among: Map<String, Int>): List<String> {
        val family = evolutionFamily(speciesId)
        return among.filterValues { it in family }.keys.toList()
    }

    /**
     * The berry this Pokémon has for you today, if any: from two hearts on, the fonder it is the
     * likelier. Decided once a day, so it doesn't change by opening the Meadow again.
     */
    fun giftFor(pokemon: CaughtPokemon, date: LocalDate = LocalDate.now()): GardenGift? {
        val hearts = FriendshipRules.hearts(pokemon.friendship)
        if (hearts < 2) return null
        val random = Random(date.toEpochDay() * 31 + pokemon.id.hashCode())
        if (random.nextFloat() >= 0.25f + 0.15f * (hearts - 2)) return null
        return GardenGift.Berry(Flavor.entries[random.nextInt(Flavor.entries.size)])
    }

    /** Where a Pokémon left today's gift, as fractions across the scene and down the grass. */
    fun giftSpot(pokemonId: String, date: LocalDate = LocalDate.now()): Pair<Float, Float> {
        val random = Random(date.toEpochDay() * 17 + pokemonId.hashCode())
        return (0.12f + random.nextFloat() * 0.76f) to (0.15f + random.nextFloat() * 0.75f)
    }
}

/**
 * The town's zones, like the islands of the Poké Pelago: each one has its own scene and its own
 * residents from the PC, who do their thing while the phone is locked. New zones (or more room
 * in one) are a new entry here; [open] = false shows it on the map as coming soon.
 */
enum class GardenZone(
    val id: String,
    @param:StringRes val nameRes: Int,
    @param:StringRes val descriptionRes: Int,
    /** Its scene, one of Showdown's battle backgrounds. */
    val background: String,
    val capacity: Int,
    val open: Boolean = true
) {
    /** Where the team hangs out: pets and gifts. Its residents are the team, not picked here. */
    MEADOW("meadow", R.string.zone_meadow, R.string.zone_meadow_description, "meadow", 0),

    /** Berry plots: Water-types water them, Grass-types make them grow faster. */
    ORCHARD("orchard", R.string.zone_orchard, R.string.zone_orchard_description, "forest", 6),

    /** Friendship while you're away, and Eggs hatch sooner with someone here. */
    SPRINGS("springs", R.string.zone_springs, R.string.zone_springs_description, "river", 6),

    /** Gym battles and badges: not open yet. */
    GYM("gym", R.string.zone_gym, R.string.zone_gym_description, "city", 6, open = false);

    /** Whether its residents are picked from the PC (everywhere but the Meadow). */
    val picksResidents: Boolean get() = capacity > 0
}
