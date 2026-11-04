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
}
