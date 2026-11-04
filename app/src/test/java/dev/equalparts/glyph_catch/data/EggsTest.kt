package dev.equalparts.glyph_catch.data

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EggsTest {

    @Test
    fun `every baby Pokémon hatches from an egg`() {
        assertEquals(18, Eggs.species.size)
        Eggs.species.forEach { baby ->
            assertNull("${baby.name} shouldn't evolve from anything", baby.evolutionRequirement)
            assertTrue("${baby.name} should evolve into something", baby.evolvesTo.isNotEmpty())
        }
    }

    @Test
    fun `eggs only hold babies from open regions`() {
        assertNull(Eggs.newEgg { false })
        repeat(20) { assertEquals(172, Eggs.newEgg(Random(it)) { id -> id == 172 }!!.speciesId) }
    }

    @Test
    fun `eggs take 2, 5 or 10 hours to hatch`() {
        assertTrue(Eggs.hatchHours.values.all { it in setOf(2, 5, 10) })
        repeat(50) {
            val egg = Eggs.newEgg(Random(it))!!
            assertEquals(Eggs.hatchHours.getValue(egg.speciesId) * 60, egg.minutesNeeded)
            assertEquals(0, egg.minutesProgress)
        }
    }
}
