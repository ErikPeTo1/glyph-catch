package dev.equalparts.glyph_catch.data

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KitchenTest {
    @Test
    fun `one flavor gives its own color, gold when blended fast`() {
        assertEquals(PokeblockColor.PINK, Kitchen.color(List(4) { Flavor.SWEET }, 50f))
        assertEquals(PokeblockColor.RED, Kitchen.color(listOf(Flavor.SPICY), 20f))
        assertEquals(PokeblockColor.GOLD, Kitchen.color(listOf(Flavor.SOUR, Flavor.SOUR), Kitchen.GOLD_RPM))
    }

    @Test
    fun `two flavors take the stronger one's mix, more go gray then white`() {
        assertEquals(PokeblockColor.BROWN, Kitchen.color(listOf(Flavor.SWEET, Flavor.SWEET, Flavor.DRY), 50f))
        assertEquals(PokeblockColor.INDIGO, Kitchen.color(listOf(Flavor.SWEET, Flavor.DRY, Flavor.DRY), 50f))
        assertEquals(PokeblockColor.GRAY, Kitchen.color(listOf(Flavor.SWEET, Flavor.DRY, Flavor.SOUR), 50f))
        assertEquals(
            PokeblockColor.WHITE,
            Kitchen.color(listOf(Flavor.SWEET, Flavor.DRY, Flavor.SOUR, Flavor.BITTER), 120f)
        )
    }

    @Test
    fun `its favorite flavor and a fast blend give the most friendship`() {
        val plain = Kitchen.friendship(Flavor.SPICY, List(4) { Flavor.SWEET }, 100f)
        val half = Kitchen.friendship(Flavor.SWEET, listOf(Flavor.SWEET, Flavor.SWEET, Flavor.DRY, Flavor.DRY), 100f)
        val slow = Kitchen.friendship(Flavor.SWEET, List(4) { Flavor.SWEET }, 10f)
        val fast = Kitchen.friendship(Flavor.SWEET, List(4) { Flavor.SWEET }, 150f)
        assertEquals(Kitchen.SWEET, plain)
        assertTrue(plain < half && half < fast && slow < fast)
    }

    @Test
    fun `only Pokémon not living in a zone come to eat`() {
        val garden = GardenState(zones = mapOf(GardenZone.ORCHARD.id to listOf("a")))
        assertFalse(Kitchen.canCome(garden, "a"))
        assertTrue(Kitchen.canCome(garden, "b"))
    }

    @Test
    fun `the blender fills up and speeds up with hits`() {
        val game = BerryBlenderGame(players = 4, random = Random(1))
        var frames = 0
        while (!game.done && frames < 60 * 60 * 10) {
            game.step(1f)
            frames++
        }
        assertTrue(game.done)
        assertTrue(game.maxRpm > BerryBlenderGame.rpmOf(128))
    }

    @Test
    fun `pressing outside your window is a miss`() {
        val game = BerryBlenderGame(players = 1)
        assertEquals(BerryBlenderGame.Hit.MISS, game.press())
    }
}
