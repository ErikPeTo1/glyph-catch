package dev.equalparts.glyph_catch.data

import android.content.Context
import dev.equalparts.glyph_catch.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What the Pokédex says about each species, from PokeAPI (tools/pokedex/fetch.js): category and
 * description in the phone's language, height, weight, gender ratio and whether females look
 * different. Loaded the first time an entry is opened.
 */
object PokedexData {
    @Serializable
    private class StatsJson(val h: Int, val w: Int, val f: Int, val g: Int)

    @Serializable
    private class TextJson(val c: String, val d: String)

    class Entry(
        val category: String,
        val description: String,
        /** Metres and kilograms, like the games show them. */
        val heightMeters: Float,
        val weightKg: Float,
        /** Whether females have their own sprite, like Pikachu's heart-shaped tail. */
        val femaleSprite: Boolean,
        /** Female share in eighths, like the games: -1 genderless, 0 always male, 8 always female. */
        val genderRate: Int
    )

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var entries: Map<Int, Entry>? = null

    /** Kept so genders can be looked up anywhere: the app, the Glyph Toy service, workers. */
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun entry(context: Context, speciesId: Int): Entry? = (entries ?: load(context))[speciesId]

    /** The species' gender ratio, or null before [init] (in unit tests, for example). */
    fun genderRate(speciesId: Int): Int? = appContext?.let { entry(it, speciesId)?.genderRate }

    @Synchronized
    private fun load(context: Context): Map<Int, Entry> {
        entries?.let { return it }
        val resources = context.resources
        val stats = json.decodeFromString<Map<String, StatsJson>>(
            resources.openRawResource(R.raw.pokedex_stats).bufferedReader().use { it.readText() }
        )
        val texts = json.decodeFromString<Map<String, TextJson>>(
            resources.openRawResource(R.raw.pokedex_text).bufferedReader().use { it.readText() }
        )
        return stats.mapNotNull { (key, stat) ->
            val id = key.toIntOrNull() ?: return@mapNotNull null
            val text = texts[key]
            id to Entry(
                category = text?.c.orEmpty(),
                description = text?.d.orEmpty(),
                heightMeters = stat.h / 10f,
                weightKg = stat.w / 10f,
                femaleSprite = stat.f == 1,
                genderRate = stat.g
            )
        }.toMap().also { entries = it }
    }
}

/**
 * How well each type hits the others, as in the current games (Fairy included, since some
 * species here have it). Only what isn't neutral is listed.
 */
object TypeChart {
    private val chart: Map<Type, Map<Type, Float>> =
        mapOf(
            Type.NORMAL to mapOf(Type.ROCK to 0.5f, Type.GHOST to 0f, Type.STEEL to 0.5f),
            Type.FIRE to mapOf(
                Type.FIRE to 0.5f,
                Type.WATER to 0.5f,
                Type.GRASS to 2f,
                Type.ICE to 2f,
                Type.BUG to 2f,
                Type.ROCK to 0.5f,
                Type.DRAGON to 0.5f,
                Type.STEEL to 2f
            ),
            Type.WATER to mapOf(
                Type.FIRE to 2f,
                Type.WATER to 0.5f,
                Type.GRASS to 0.5f,
                Type.GROUND to 2f,
                Type.ROCK to 2f,
                Type.DRAGON to 0.5f
            ),
            Type.ELECTRIC to mapOf(
                Type.WATER to 2f,
                Type.ELECTRIC to 0.5f,
                Type.GRASS to 0.5f,
                Type.GROUND to 0f,
                Type.FLYING to 2f,
                Type.DRAGON to 0.5f
            ),
            Type.GRASS to mapOf(
                Type.FIRE to 0.5f, Type.WATER to 2f, Type.GRASS to 0.5f, Type.POISON to 0.5f, Type.GROUND to 2f,
                Type.FLYING to 0.5f, Type.BUG to 0.5f, Type.ROCK to 2f, Type.DRAGON to 0.5f, Type.STEEL to 0.5f
            ),
            Type.ICE to mapOf(
                Type.FIRE to 0.5f,
                Type.WATER to 0.5f,
                Type.GRASS to 2f,
                Type.ICE to 0.5f,
                Type.GROUND to 2f,
                Type.FLYING to 2f,
                Type.DRAGON to 2f,
                Type.STEEL to 0.5f
            ),
            Type.FIGHTING to mapOf(
                Type.NORMAL to 2f, Type.ICE to 2f, Type.POISON to 0.5f, Type.FLYING to 0.5f, Type.PSYCHIC to 0.5f,
                Type.BUG to 0.5f, Type.ROCK to 2f, Type.GHOST to 0f, Type.DARK to 2f, Type.STEEL to 2f,
                Type.FAIRY to 0.5f
            ),
            Type.POISON to mapOf(
                Type.GRASS to 2f,
                Type.POISON to 0.5f,
                Type.GROUND to 0.5f,
                Type.ROCK to 0.5f,
                Type.GHOST to 0.5f,
                Type.STEEL to 0f,
                Type.FAIRY to 2f
            ),
            Type.GROUND to mapOf(
                Type.FIRE to 2f,
                Type.ELECTRIC to 2f,
                Type.GRASS to 0.5f,
                Type.POISON to 2f,
                Type.FLYING to 0f,
                Type.BUG to 0.5f,
                Type.ROCK to 2f,
                Type.STEEL to 2f
            ),
            Type.FLYING to mapOf(
                Type.ELECTRIC to 0.5f,
                Type.GRASS to 2f,
                Type.FIGHTING to 2f,
                Type.BUG to 2f,
                Type.ROCK to 0.5f,
                Type.STEEL to 0.5f
            ),
            Type.PSYCHIC to mapOf(
                Type.FIGHTING to 2f,
                Type.POISON to 2f,
                Type.PSYCHIC to 0.5f,
                Type.DARK to 0f,
                Type.STEEL to 0.5f
            ),
            Type.BUG to mapOf(
                Type.FIRE to 0.5f, Type.GRASS to 2f, Type.FIGHTING to 0.5f, Type.POISON to 0.5f, Type.FLYING to 0.5f,
                Type.PSYCHIC to 2f, Type.GHOST to 0.5f, Type.DARK to 2f, Type.STEEL to 0.5f, Type.FAIRY to 0.5f
            ),
            Type.ROCK to mapOf(
                Type.FIRE to 2f,
                Type.ICE to 2f,
                Type.FIGHTING to 0.5f,
                Type.GROUND to 0.5f,
                Type.FLYING to 2f,
                Type.BUG to 2f,
                Type.STEEL to 0.5f
            ),
            Type.GHOST to mapOf(Type.NORMAL to 0f, Type.PSYCHIC to 2f, Type.GHOST to 2f, Type.DARK to 0.5f),
            Type.DRAGON to mapOf(Type.DRAGON to 2f, Type.STEEL to 0.5f, Type.FAIRY to 0f),
            Type.DARK to mapOf(
                Type.FIGHTING to 0.5f,
                Type.PSYCHIC to 2f,
                Type.GHOST to 2f,
                Type.DARK to 0.5f,
                Type.FAIRY to 0.5f
            ),
            Type.STEEL to mapOf(
                Type.FIRE to 0.5f,
                Type.WATER to 0.5f,
                Type.ELECTRIC to 0.5f,
                Type.ICE to 2f,
                Type.ROCK to 2f,
                Type.STEEL to 0.5f,
                Type.FAIRY to 2f
            ),
            Type.FAIRY to mapOf(
                Type.FIRE to 0.5f,
                Type.FIGHTING to 2f,
                Type.POISON to 0.5f,
                Type.DRAGON to 2f,
                Type.DARK to 2f,
                Type.STEEL to 0.5f
            )
        )

    /** How much damage an [attack] of this type does to a Pokémon with [defender]'s types. */
    fun effectiveness(attack: Type, defender: List<Type>): Float =
        defender.fold(1f) { total, type -> total * (chart[attack]?.get(type) ?: 1f) }

    /** Every attacking type that isn't neutral against [defender], and by how much. */
    fun against(defender: List<Type>): Map<Type, Float> =
        Type.entries.associateWith { effectiveness(it, defender) }.filterValues { it != 1f }
}

/** The species' types, one or two. */
val PokemonSpecies.types: List<Type> get() = listOfNotNull(type1, type2)

/**
 * A family's evolutions by stage: the base first, then whatever it evolves into, and so on.
 * Each one comes with what it took to evolve into it (nothing for the base).
 */
fun evolutionStages(speciesId: Int): List<List<PokemonSpecies>> {
    val family = evolutionFamily(speciesId)
    val base = family.mapNotNull { Pokemon[it] }.firstOrNull { species ->
        family.none { other -> Pokemon[other]?.evolvesTo?.contains(species.id) == true }
    } ?: return listOfNotNull(Pokemon[speciesId]).map { listOf(it) }
    val stages = mutableListOf(listOf(base))
    while (true) {
        val next = stages.last().flatMap { it.evolvesTo }.distinct().mapNotNull { Pokemon[it] }
        if (next.isEmpty()) break
        stages += next
    }
    return stages
}
