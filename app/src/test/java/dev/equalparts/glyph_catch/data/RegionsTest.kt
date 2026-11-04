package dev.equalparts.glyph_catch.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionsTest {

    @Test
    fun `regions cover the whole Pokédex once`() {
        assertEquals(Pokemon.POKEDEX_SIZE, Region.entries.sumOf { it.size })
        assertEquals(Region.JOHTO, Region.of(172))
        assertEquals(Region.UNOVA, Region.of(494))
    }

    @Test
    fun `a new player meets Oak and nobody else`() {
        assertEquals(listOf(ProfessorEvent.Intro), ProfessorEvents.pending(false, 0, emptySet()))
        assertTrue(ProfessorEvents.pending(true, 49, setOf(Item.POKEDEX_KANTO)).isEmpty())
        assertEquals(listOf(ProfessorEvent.KantoPokedex), ProfessorEvents.pending(true, 49, emptySet()))
    }

    @Test
    fun `each professor hands over their Pokédex at the milestone`() {
        val events = ProfessorEvents.pending(true, 120, emptySet()).filterIsInstance<ProfessorEvent.RegionPokedex>()
        assertEquals(
            listOf(ProfessorEvent.RegionPokedex(Region.JOHTO), ProfessorEvent.RegionPokedex(Region.HOENN)),
            events
        )
    }

    @Test
    fun `an advanced save gets everything in order, ending with the legendary items`() {
        val events = ProfessorEvents.pending(false, 649, emptySet())
        assertEquals(ProfessorEvent.Intro, events.first())
        assertEquals(ProfessorEvent.TownMap, events[5])
        assertEquals(ProfessorEvent.ShinyCharm, events[6])
        val gifts = events.drop(7).flatMap { it.gifts }
        assertEquals(LegendaryItems.species.keys, gifts.toSet())
        events.drop(7).forEach { event ->
            event.gifts.forEach { assertEquals(event.professor, LegendaryItems.region(it).professor) }
        }
    }

    @Test
    fun `legendary items wait for their region`() {
        // Victini's rumor is unlocked at 400, but Unova isn't open without its Pokédex
        val owned = setOf(Item.POKEDEX_JOHTO, Item.POKEDEX_HOENN, Item.POKEDEX_SINNOH)
        val gifts = ProfessorEvents.pending(true, 400, owned + Item.TOWN_MAP + Item.POKEDEX_UNOVA).flatMap { it.gifts }
        assertTrue(Item.LIBERTY_PASS in gifts)
        val unovaClosed = Region.active(owned, emptySet())
        assertFalse(Region.UNOVA in unovaClosed)
    }

    @Test
    fun `species need an open region and their key item`() {
        val regions = Region.active(setOf(Item.POKEDEX_HOENN), emptySet())
        assertTrue(canSpawnSpecies(25, emptySet(), regions))
        assertFalse(canSpawnSpecies(152, emptySet(), regions))
        assertFalse(canSpawnSpecies(381, emptySet(), regions))
        assertTrue(canSpawnSpecies(381, setOf(Item.EON_TICKET), regions))
        assertTrue(canSpawnSpecies(377, emptySet(), regions))
    }

    @Test
    fun `the Town Map filter never switches every region off`() {
        val owned = REGION_POKEDEXES.toSet() + Item.TOWN_MAP
        assertEquals(setOf(Region.HOENN), Region.active(owned, Region.entries.map { it.name }.toSet() - "HOENN"))
        assertEquals(Region.entries.toSet(), Region.active(owned, Region.entries.map { it.name }.toSet()))
        // Without the map, switches are ignored
        assertEquals(Region.entries.toSet(), Region.active(REGION_POKEDEXES.toSet(), setOf("KANTO")))
    }
}
