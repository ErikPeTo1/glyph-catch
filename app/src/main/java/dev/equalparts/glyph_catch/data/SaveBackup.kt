package dev.equalparts.glyph_catch.data

import android.content.Context
import androidx.core.content.edit
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * A full copy of the player's progress: caught Pokémon, Pokédex, bag and settings.
 */
@Serializable
data class SaveFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: Long,
    val pokemon: List<CaughtPokemon>,
    val pokedex: List<Int>,
    val shinyPokedex: List<Int> = emptyList(),
    val forms: List<PokedexForm> = emptyList(),
    val inventory: List<InventoryItem>,
    val activeItems: List<ActiveItem>,
    val preferences: Map<String, JsonElement>
) {
    companion object {
        const val FORMAT = "glyph-catch-save"
        const val VERSION = 1
    }
}

data class SaveSummary(val pokemon: Int, val pokedex: Int, val items: Int)

class InvalidSaveFileException : Exception()

object SaveBackup {
    private const val PREFS_NAME = "glyph_catch_prefs"
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun export(context: Context, db: PokemonDatabase): String = withContext(Dispatchers.IO) {
        val save = SaveFile(
            exportedAt = System.currentTimeMillis(),
            pokemon = db.pokemonDao().getAllCaught(),
            pokedex = db.pokemonDao().getPokedexSpeciesIds(),
            shinyPokedex = db.pokemonDao().getShinyPokedexSpeciesIds(),
            forms = db.pokemonDao().getAllForms(),
            inventory = db.inventoryDao().getAllItems(),
            activeItems = db.activeItemDao().getAllRaw(),
            preferences = exportPreferences(context)
        )
        json.encodeToString(SaveFile.serializer(), save)
    }

    /**
     * Parses a save file without touching the current progress, so it can be confirmed first.
     */
    fun parse(content: String): SaveFile {
        val save = runCatching { json.decodeFromString(SaveFile.serializer(), content) }
            .getOrElse { throw InvalidSaveFileException() }
        if (save.format != SaveFile.FORMAT || save.version > SaveFile.VERSION) throw InvalidSaveFileException()
        return save
    }

    fun summary(save: SaveFile) = SaveSummary(
        pokemon = save.pokemon.size,
        pokedex = save.pokedex.size,
        items = save.inventory.sumOf { it.quantity }
    )

    /**
     * Replaces all current progress with the given save.
     */
    suspend fun import(context: Context, db: PokemonDatabase, save: SaveFile) = withContext(Dispatchers.IO) {
        val validItems = Item.entries.indices
        db.withTransaction {
            val pokemonDao = db.pokemonDao()
            pokemonDao.deleteAllCaught()
            pokemonDao.deleteAllPokedexRecords()
            pokemonDao.insertAllCaught(save.pokemon.filter { Pokemon[it.speciesId] != null })
            val shinies = save.shinyPokedex.toSet()
            pokemonDao.insertAllPokedexRecords(
                save.pokedex.filter { Pokemon[it] != null }.map { PokedexRecord(it, shinyCaught = it in shinies) }
            )
            pokemonDao.deleteAllForms()
            pokemonDao.insertAllForms(save.forms.filter { it.form in Forms.formsOf(it.speciesId) })

            db.inventoryDao().deleteAllItems()
            db.inventoryDao().insertAllItems(save.inventory.filter { it.itemId in validItems })

            db.activeItemDao().deleteAll()
            db.activeItemDao().insertAll(save.activeItems.filter { it.itemId in validItems })
        }
        importPreferences(context, save.preferences)
    }

    private fun exportPreferences(context: Context): Map<String, JsonElement> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all.mapNotNull { (key, value) ->
            val encoded = when (value) {
                is Boolean -> typed("boolean", JsonPrimitive(value))
                is Int -> typed("int", JsonPrimitive(value))
                is Long -> typed("long", JsonPrimitive(value))
                is Float -> typed("float", JsonPrimitive(value))
                is String -> typed("string", JsonPrimitive(value))
                is Set<*> -> buildJsonObject {
                    put("type", "stringSet")
                    putJsonArray("value") { value.filterIsInstance<String>().forEach { add(JsonPrimitive(it)) } }
                }
                else -> null
            }
            encoded?.let { key to it }
        }.toMap()
    }

    private fun typed(type: String, value: JsonPrimitive) = buildJsonObject {
        put("type", type)
        put("value", value)
    }

    private fun importPreferences(context: Context, preferences: Map<String, JsonElement>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit(commit = true) {
            clear()
            preferences.forEach { (key, element) ->
                runCatching {
                    val entry = element.jsonObject
                    val value = entry.getValue("value")
                    when (entry.getValue("type").jsonPrimitive.content) {
                        "boolean" -> putBoolean(key, value.jsonPrimitive.boolean)
                        "int" -> putInt(key, value.jsonPrimitive.int)
                        "long" -> putLong(key, value.jsonPrimitive.long)
                        "float" -> putFloat(key, value.jsonPrimitive.float)
                        "string" -> putString(key, value.jsonPrimitive.content)
                        "stringSet" -> putStringSet(key, value.jsonArray.map { it.jsonPrimitive.content }.toSet())
                    }
                }
            }
        }
    }
}
