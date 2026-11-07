package dev.equalparts.glyph_catch.util

import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.Item
import org.junit.Assert.assertEquals
import org.junit.Test

class SnoruntTest {
    @Test
    fun `Snorunt becomes Glalie at 42 and Froslass with a Dawn Stone`() {
        val snorunt = CaughtPokemon(speciesId = 361, level = 41)
        assertEquals(null, findLevelEvolutionTarget(snorunt)?.id)
        assertEquals(362, findLevelEvolutionTarget(snorunt.copy(level = 42))?.id)
        assertEquals(
            CandyEvolution.Evolves(listOf(dev.equalparts.glyph_catch.data.Pokemon[362]!!)),
            candyEvolution(snorunt, 1)
        )
        assertEquals(478, findStoneEvolutionTarget(snorunt, Item.DAWN_STONE)?.id)
    }
}
