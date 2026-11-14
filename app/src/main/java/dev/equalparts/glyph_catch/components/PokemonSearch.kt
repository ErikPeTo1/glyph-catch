package dev.equalparts.glyph_catch.components

import android.content.Context
import androidx.annotation.StringRes
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.Eggs
import dev.equalparts.glyph_catch.data.Gender
import dev.equalparts.glyph_catch.data.LEGENDARY_SPECIES
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.Region
import dev.equalparts.glyph_catch.data.Type
import dev.equalparts.glyph_catch.data.displayName
import dev.equalparts.glyph_catch.data.evolutionFamily
import dev.equalparts.glyph_catch.data.gender
import dev.equalparts.glyph_catch.data.nameRes
import java.text.Normalizer
import java.util.Locale

/**
 * Searching the PC like in Pokémon GO. A term can be a name or nickname, a Pokédex number or
 * range (25, 1-151), a type (water), a region (kanto), a keyword (shiny, favorite, legendary,
 * mythical, egg, event, training), a level (lv30, lv30-50) or a whole family (+pikachu).
 * "," means or, "&" means and, "!" means not: "water,fire&!shiny".
 *
 * Words work in the phone's language and in English, with or without accents.
 */
object PokemonSearch {
    private enum class Keyword { SHINY, FAVORITE, LEGENDARY, MYTHICAL, EGG, EVENT, TRAINING, MALE, FEMALE }

    private class Vocabulary(
        val types: Map<String, Type>,
        val regions: Map<String, Region>,
        val keywords: Map<String, Keyword>,
        val levelPrefixes: List<String>
    )

    private val english = Vocabulary(
        types = Type.entries.associateBy { it.name.lowercase() },
        regions = Region.entries.associateBy { it.name.lowercase() },
        keywords = mapOf(
            "shiny" to Keyword.SHINY,
            "favorite" to Keyword.FAVORITE,
            "favourite" to Keyword.FAVORITE,
            "legendary" to Keyword.LEGENDARY,
            "mythical" to Keyword.MYTHICAL,
            "egg" to Keyword.EGG,
            "hatched" to Keyword.EGG,
            "event" to Keyword.EVENT,
            "training" to Keyword.TRAINING,
            "male" to Keyword.MALE,
            "female" to Keyword.FEMALE
        ),
        levelPrefixes = listOf("lv", "lvl", "level")
    )

    @Volatile
    private var vocabulary = english

    /** For genders, which come from the Pokédex data. */
    @Volatile
    private var appContext: Context? = null

    /** Mythical Pokémon, the ones the games hand out at events. */
    private val MYTHICAL = setOf(151, 251, 385, 386, 489, 490, 491, 492, 493, 494, 647, 648, 649)

    /** Learns the words in the phone's language. English keeps working too. */
    fun init(context: Context) {
        appContext = context.applicationContext
        fun words(@StringRes res: Int) = context.getString(res).split(',').map(::normalize).filter { it.isNotEmpty() }
        val keywords = listOf(
            R.string.search_keyword_shiny to Keyword.SHINY,
            R.string.search_keyword_favorite to Keyword.FAVORITE,
            R.string.search_keyword_legendary to Keyword.LEGENDARY,
            R.string.search_keyword_mythical to Keyword.MYTHICAL,
            R.string.search_keyword_egg to Keyword.EGG,
            R.string.search_keyword_event to Keyword.EVENT,
            R.string.search_keyword_training to Keyword.TRAINING,
            R.string.search_keyword_male to Keyword.MALE,
            R.string.search_keyword_female to Keyword.FEMALE
        ).flatMap { (res, keyword) -> words(res).map { it to keyword } }.toMap()
        vocabulary = Vocabulary(
            types = english.types + Type.entries.associateBy { normalize(context.getString(it.nameRes())) },
            regions = english.regions + Region.entries.associateBy { normalize(context.getString(it.nameRes)) },
            keywords = english.keywords + keywords,
            levelPrefixes = (english.levelPrefixes + words(R.string.search_level_prefixes)).distinct()
        )
        names.clear()
    }

    /** Species names in both languages, ready to compare. */
    private val names = HashMap<Int, List<String>>()

    private fun namesOf(speciesId: Int): List<String> = names.getOrPut(speciesId) {
        Pokemon[speciesId]?.let { listOf(normalize(it.displayName), normalize(it.name)).distinct() }.orEmpty()
    }

    /** Lowercase, without accents: "Eléctrico" is "electrico". */
    fun normalize(text: String): String = Normalizer.normalize(text.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace("œ", "oe")

    /** What the query lets through; a blank one lets everything through. */
    fun matcher(query: String): (CaughtPokemon) -> Boolean {
        val words = vocabulary
        val clauses = query.split('&').map { clause ->
            clause.split(',', ';', ':').map(::normalize).filter { it.isNotEmpty() }.map { term(it, words) }
        }.filter { it.isNotEmpty() }
        if (clauses.isEmpty()) return { true }
        return { pokemon -> clauses.all { terms -> terms.any { it(pokemon) } } }
    }

    private val number = Regex("^\\d+$")
    private val range = Regex("^(\\d*)-(\\d*)$")

    private fun term(text: String, words: Vocabulary): (CaughtPokemon) -> Boolean {
        if (text.startsWith('!')) {
            val inner = term(text.drop(1).trim(), words)
            return { !inner(it) }
        }
        if (text.startsWith('+')) {
            val name = text.drop(1).trim()
            val family = Pokemon.all.keys.filter { id -> namesOf(id).any { it.startsWith(name) } }
                .flatMap(::evolutionFamily)
                .toSet()
            return { it.speciesId in family }
        }
        numbers(text)?.let { ids -> return { it.speciesId in ids } }
        words.levelPrefixes.sortedByDescending { it.length }.firstOrNull { text.startsWith(it) }
            ?.let { prefix -> numbers(text.removePrefix(prefix).trim()) }
            ?.let { levels -> return { it.level in levels } }
        words.types[text]?.let { type ->
            return { Pokemon[it.speciesId]?.let { species -> species.type1 == type || species.type2 == type } == true }
        }
        words.regions[text]?.let { region -> return { it.speciesId in region.ids } }
        words.keywords[text]?.let { keyword -> return { it.has(keyword) } }
        return { pokemon ->
            pokemon.nickname?.let { normalize(it).contains(text) } == true ||
                namesOf(pokemon.speciesId).any { it.contains(text) }
        }
    }

    /** "25" or "1-151"; open ranges like "100-" or "-50" too. */
    private fun numbers(text: String): IntRange? {
        if (number.matches(text)) return text.toInt().let { it..it }
        val (from, to) = range.matchEntire(text)?.destructured ?: return null
        if (from.isEmpty() && to.isEmpty()) return null
        return (from.toIntOrNull() ?: 0)..(to.toIntOrNull() ?: Int.MAX_VALUE)
    }

    private fun CaughtPokemon.has(keyword: Keyword): Boolean = when (keyword) {
        Keyword.SHINY -> isShiny
        Keyword.FAVORITE -> isFavorite
        Keyword.LEGENDARY -> speciesId in LEGENDARY_SPECIES
        Keyword.MYTHICAL -> speciesId in MYTHICAL
        Keyword.EGG -> spawnPoolName == Eggs.POOL_NAME
        Keyword.EVENT -> isSpecialSpawn || isConditionalSpawn
        Keyword.TRAINING -> isTraining
        Keyword.MALE -> appContext?.let { gender(it) } == Gender.MALE
        Keyword.FEMALE -> appContext?.let { gender(it) } == Gender.FEMALE
    }
}
