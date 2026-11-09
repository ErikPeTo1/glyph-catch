package dev.equalparts.glyph_catch.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * A save exported by the original Glyph Catch (Equal Parts' 1.x): no shinies, forms or Eggs,
 * and only the first ten items. It must import as is.
 */
class OriginalSaveTest {
    private val original = """
        {"format":"glyph-catch-save","version":1,"exportedAt":1762000000000,
         "pokemon":[{"id":"a1","speciesId":25,"caughtAt":1761000000000,"spawnedAt":0,"nickname":null,
           "isFavorite":true,"isTraining":false,"exp":120,"level":14,"spawnPoolName":"Common",
           "isSpecialSpawn":false,"isConditionalSpawn":false,"screenOffDurationMinutes":42}],
         "pokedex":[1,4,25],
         "inventory":[{"itemId":7,"quantity":3},{"itemId":9,"quantity":1}],
         "activeItems":[],
         "preferences":{"glyph_toy_clock_enabled":{"type":"boolean","value":true}}}
    """.trimIndent()

    @Test
    fun `a save from the original app imports`() {
        val save = SaveBackup.parse(original)
        assertEquals(1, save.pokemon.size)
        val pikachu = save.pokemon.single()
        assertEquals(25, pikachu.speciesId)
        assertEquals(14, pikachu.level)
        assertFalse(pikachu.isShiny)
        assertEquals(listOf(1, 4, 25), save.pokedex)
        assertEquals(Item.RARE_CANDY.ordinal, save.inventory.first().itemId)
        assertEquals(SaveSummary(pokemon = 1, pokedex = 3, items = 4), SaveBackup.summary(save))
    }
}
