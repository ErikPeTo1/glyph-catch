package dev.equalparts.glyph_catch.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveBackupTest {
    private val json = Json { encodeDefaults = true }

    private val save = SaveFile(
        exportedAt = 1_761_000_000_000L,
        pokemon = listOf(
            CaughtPokemon(
                id = "a",
                speciesId = 258,
                caughtAt = 10L,
                level = 12,
                isFavorite = true,
                spawnPoolName = "Very Rare"
            ),
            CaughtPokemon(id = "b", speciesId = 25, caughtAt = 20L, nickname = "Pika", exp = 120, isTraining = true)
        ),
        pokedex = listOf(25, 258),
        inventory = listOf(InventoryItem(itemId = Item.RARE_CANDY.ordinal, quantity = 3)),
        activeItems = listOf(ActiveItem(itemId = Item.GOOD_ROD.ordinal, activatedAt = 1L, expiresAt = 2L)),
        preferences = mapOf(
            "released_count" to buildJsonObject {
                put("type", "int")
                put("value", JsonPrimitive(4))
            }
        )
    )

    @Test
    fun `save files survive a round trip`() {
        val content = json.encodeToString(SaveFile.serializer(), save)
        assertEquals(save, SaveBackup.parse(content))
        assertEquals(SaveSummary(pokemon = 2, pokedex = 2, items = 3), SaveBackup.summary(save))
    }

    @Test(expected = InvalidSaveFileException::class)
    fun `other json files are rejected`() {
        SaveBackup.parse("""{"hello": "world"}""")
    }

    @Test(expected = InvalidSaveFileException::class)
    fun `saves from a newer version are rejected`() {
        SaveBackup.parse(json.encodeToString(SaveFile.serializer(), save.copy(version = SaveFile.VERSION + 1)))
    }
}
