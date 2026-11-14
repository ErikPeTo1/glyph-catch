package dev.equalparts.glyph_catch.data

import dev.equalparts.glyph_catch.screens.garden.skyTint
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GardenTest {
    private fun pokemon(speciesId: Int, friendship: Int, id: String = "p$speciesId") =
        CaughtPokemon(id = id, speciesId = speciesId, friendship = friendship)

    private val days = (0L until 400L).map { LocalDate.of(2026, 1, 1).plusDays(it) }

    @Test
    fun `families include babies and every evolution`() {
        assertEquals(setOf(172, 25, 26), evolutionFamily(26))
        assertEquals(setOf(298, 183, 184), evolutionFamily(183))
        assertEquals(setOf(446, 143), evolutionFamily(143))
        assertEquals(listOf("pichu_doll", "pikachu_doll"), GardenCatalog.dollsOf(26, GardenCatalog.dolls).sorted())
    }

    @Test
    fun `no gifts below two hearts`() {
        val shy = pokemon(25, friendship = 99)
        assertTrue(days.all { GardenCatalog.giftFor(shy, it) == null })
    }

    @Test
    fun `fond Pokémon bring berries, more often the fonder they are`() {
        val two = days.mapNotNull { GardenCatalog.giftFor(pokemon(1, friendship = 100), it) }
        val four = days.mapNotNull { GardenCatalog.giftFor(pokemon(1, friendship = 200), it) }
        assertTrue(two.isNotEmpty() && two.all { it is GardenGift.Berry })
        assertTrue(four.size > two.size && four.all { it is GardenGift.Berry })
    }

    @Test
    fun `each Pokémon lives in one zone, with room for six`() {
        var state = GardenState()
        state = state.moving("a", GardenZone.ORCHARD)!!
        assertEquals(GardenZone.ORCHARD, state.zoneOf("a"))
        state = state.moving("a", GardenZone.SPRINGS)!!
        assertEquals(emptyList<String>(), state.residents(GardenZone.ORCHARD))
        assertEquals(GardenZone.SPRINGS, state.zoneOf("a"))
        (1..5).forEach { state = state.moving("p$it", GardenZone.SPRINGS)!! }
        assertNull(state.moving("full", GardenZone.SPRINGS))
        assertNull(state.zoneOf("full"))
        assertNull(state.leaving("a").zoneOf("a"))
        assertTrue(!GardenZone.GYM.open && !GardenZone.MEADOW.picksResidents)
    }

    @Test
    fun `the same gift all day`() {
        val pokemon = pokemon(7, friendship = 180)
        assertEquals(
            GardenCatalog.giftFor(pokemon, days[3]),
            GardenCatalog.giftFor(pokemon, days[3])
        )
        assertEquals(GardenCatalog.giftSpot(pokemon.id, days[3]), GardenCatalog.giftSpot(pokemon.id, days[3]))
    }

    @Test
    fun `berries planted grow with locked minutes and give one or two without water`() {
        var state = GardenState().withBerry(Flavor.SWEET, 1)
        assertNull(state.planting(0, Flavor.SPICY))
        state = state.planting(1, Flavor.SWEET, now = 42L)!!
        assertEquals(0, state.berries(Flavor.SWEET))
        assertNull(state.planting(1, Flavor.SWEET))
        assertEquals(PlotStage.SPROUT, state.plots[1]!!.stage)
        assertNull(state.harvesting(1))
        state = state.grown(BerryPlot.GROW_MINUTES / 4)
        assertEquals(PlotStage.TALL, state.plots[1]!!.stage)
        state = state.grown(BerryPlot.GROW_MINUTES)
        assertTrue(state.plots[1]!!.ripe)
        assertFalse(state.growing)
        assertEquals(state, state.grown())
        val picked = state.harvesting(1)!!
        assertNull(picked.plots[1])
        assertTrue(picked.berries(Flavor.SWEET) in 1..2)
    }

    @Test
    fun `each watered stage gives one more berry`() {
        var state = GardenState().withBerry(Flavor.SOUR, 1).planting(0, Flavor.SOUR, now = 1L)!!
        assertTrue(state.plots[0]!!.thirsty)
        state = state.watering(0)!!
        assertNull(state.watering(0))
        state = state.grown(BerryPlot.GROW_MINUTES / 4)
        assertTrue(state.plots[0]!!.thirsty)
        state = state.watering(0)!!.grown(BerryPlot.GROW_MINUTES)
        assertFalse(state.plots[0]!!.thirsty)
        assertNull(state.watering(0))
        assertTrue(state.harvesting(0)!!.berries(Flavor.SOUR) in 3..4)
    }

    @Test
    fun `a Water-type waters on its own after a while in each stage`() {
        var state = GardenState().withBerry(Flavor.DRY, 1).planting(2, Flavor.DRY, now = 1L)!!
        state = state.grown(BerryPlot.AUTO_WATER_AFTER - 1).autoWatered()
        assertTrue(state.plots[2]!!.thirsty)
        state = state.grown().autoWatered()
        assertFalse(state.plots[2]!!.thirsty)
        repeat(BerryPlot.GROW_MINUTES) { state = state.grown().autoWatered() }
        assertTrue(state.harvesting(2)!!.berries(Flavor.DRY) in 4..5)
        assertTrue(GardenCatalog.waters(pokemon(7, 70)))
        assertFalse(GardenCatalog.waters(pokemon(4, 70)))
    }

    @Test
    fun `every decoration has a kind and the catalog has no repeats`() {
        assertEquals(77, GardenCatalog.decorations.size)
        assertEquals(GardenCatalog.decorations.size, GardenCatalog.decorations.toSet().size)
        assertEquals(15, GardenCatalog.backgrounds.size)
        assertEquals(DecorationKind.TENT, GardenCatalog.kindOf("red_tent"))
        assertEquals(DecorationKind.BIG_DOLL, GardenCatalog.kindOf("big_lapras_doll"))
        assertTrue(
            GardenCatalog.decorations.all {
                GardenCatalog.kindOf(it) != DecorationKind.OTHER ||
                    it in GardenCatalog.furniture
            }
        )
    }

    @Test
    fun `the sky changes with the hour`() {
        assertNull(skyTint(LocalTime.of(12, 0), asleep = false))
        assertNotNull(skyTint(LocalTime.of(6, 30), asleep = false))
        assertNotNull(skyTint(LocalTime.of(18, 30), asleep = false))
        assertNotNull(skyTint(LocalTime.of(23, 0), asleep = false))
        assertNotNull(skyTint(LocalTime.of(12, 0), asleep = true))
    }
}
