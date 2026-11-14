package dev.equalparts.glyph_catch.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PokedexDataTest {
    private fun matchups(id: Int) = TypeChart.against(Pokemon[id]!!.types)

    @Test
    fun `weaknesses and resistances multiply across both types`() {
        val charizard = matchups(6)
        assertEquals(4f, charizard[Type.ROCK])
        assertEquals(0.25f, charizard[Type.GRASS])
        assertEquals(0f, charizard[Type.GROUND])
        assertEquals(2f, matchups(25)[Type.GROUND])
        assertEquals(0f, matchups(92)[Type.NORMAL])
        // Gyarados: Electric hits twice as hard on both types
        assertEquals(4f, matchups(130)[Type.ELECTRIC])
    }

    @Test
    fun `neutral types are left out`() {
        assertTrue(matchups(25).values.none { it == 1f })
    }

    @Test
    fun `evolution lines come by stages, babies and branches included`() {
        assertEquals(listOf(listOf(172), listOf(25), listOf(26)), evolutionStages(26).map { s -> s.map { it.id } })
        val eevee = evolutionStages(133)
        assertEquals(listOf(133), eevee[0].map { it.id })
        assertTrue(eevee[1].map { it.id }.containsAll(listOf(134, 135, 136, 196, 197, 470, 471)))
        assertEquals(1, evolutionStages(128).size)
    }

    @Test
    fun `gender is rolled once per Pokémon with its species' ratio`() {
        val ids = (0 until 4_000).map { "pokemon-$it" }
        assertTrue(ids.all { Gender.roll(it, -1) == Gender.UNKNOWN })
        assertTrue(ids.all { Gender.roll(it, 0) == Gender.MALE })
        assertTrue(ids.all { Gender.roll(it, 8) == Gender.FEMALE })
        // Starters are 1 female in 8; Pikachu half and half
        val starters = ids.count { Gender.roll(it, 1) == Gender.FEMALE } / ids.size.toFloat()
        val pikachu = ids.count { Gender.roll(it, 4) == Gender.FEMALE } / ids.size.toFloat()
        assertEquals(0.125f, starters, 0.03f)
        assertEquals(0.5f, pikachu, 0.04f)
        assertEquals(Gender.roll("abc", 4), Gender.roll("abc", 4))
    }

    @Test
    fun `some evolutions only happen with one gender`() {
        assertTrue(canEvolveInto(416, Gender.FEMALE))
        assertTrue(!canEvolveInto(416, Gender.MALE))
        assertTrue(canEvolveInto(414, Gender.MALE) && !canEvolveInto(413, Gender.MALE))
        assertTrue(canEvolveInto(475, Gender.MALE) && !canEvolveInto(478, Gender.MALE))
        assertTrue(canEvolveInto(282, Gender.MALE) && canEvolveInto(282, Gender.FEMALE))
        // Without the Pokédex data nothing is blocked
        assertTrue(canEvolveInto(416, Gender.UNKNOWN))
    }
}
