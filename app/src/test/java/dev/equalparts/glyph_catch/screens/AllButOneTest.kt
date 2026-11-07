package dev.equalparts.glyph_catch.screens

import dev.equalparts.glyph_catch.data.CaughtPokemon
import org.junit.Assert.assertEquals
import org.junit.Test

class AllButOneTest {
    private fun caught(
        id: String,
        species: Int,
        level: Int = 10,
        shiny: Boolean = false,
        favorite: Boolean = false,
        form: String? = null
    ) = CaughtPokemon(id = id, speciesId = species, level = level, isShiny = shiny, isFavorite = favorite, form = form)

    @Test
    fun `keeps the best copy of each species`() {
        val pc = listOf(
            caught("a", 16, level = 5),
            caught("b", 16, level = 30),
            caught("c", 16, level = 12),
            caught("d", 19, level = 50),
            caught("e", 19, level = 3, shiny = true)
        )
        assertEquals(setOf("a", "c", "d"), allButOneOfEach(pc, pc, emptySet()))
    }

    @Test
    fun `a favorite or a team member is the one kept`() {
        val pc = listOf(caught("a", 16, level = 50), caught("b", 16, favorite = true), caught("c", 25), caught("d", 25))
        assertEquals(setOf("a", "c"), allButOneOfEach(pc, pc, setOf("d")))
    }

    @Test
    fun `each form counts on its own`() {
        val pc =
            listOf(caught("a", 422, form = "west"), caught("b", 422, form = "east"), caught("c", 422, form = "east"))
        assertEquals(setOf("c"), allButOneOfEach(pc, pc, emptySet()))
    }
}
