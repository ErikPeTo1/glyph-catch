package dev.equalparts.glyph_catch.util

import dev.equalparts.glyph_catch.data.CaughtPokemon
import org.junit.Assert.assertEquals
import org.junit.Test

class RareCandyTest {

    private fun pokemon(level: Int) = CaughtPokemon(id = "a", speciesId = 4, level = level, caughtAt = 0L)

    @Test
    fun `you can use as many candies as you own`() {
        assertEquals(5, maxRareCandies(pokemon(level = 10), owned = 5))
    }

    @Test
    fun `candies never take a Pokémon past level 100`() {
        assertEquals(3, maxRareCandies(pokemon(level = 97), owned = 20))
        assertEquals(0, maxRareCandies(pokemon(level = 100), owned = 20))
    }

    @Test
    fun `no candies means none can be used`() {
        assertEquals(0, maxRareCandies(pokemon(level = 10), owned = 0))
    }

    @Test
    fun `the candy preview follows the whole evolution line`() {
        assertEquals(CandyEvolution.None, candyEvolution(pokemon(level = 10), 2))
        val evolves = candyEvolution(pokemon(level = 15), 25) as CandyEvolution.Evolves
        assertEquals(listOf(5, 6), evolves.path.map { it.id })
        assertEquals(CandyEvolution.Blocked, candyEvolution(pokemon(level = 15).copy(evolutionLocked = true), 1))
        assertEquals(CandyEvolution.Choice, candyEvolution(pokemon(level = 19).copy(speciesId = 236), 1))
        val wurmple = candyEvolution(pokemon(level = 6).copy(speciesId = 265), 1) as CandyEvolution.Random
        assertEquals(setOf(266, 268), wurmple.options.map { it.id }.toSet())
    }
}
