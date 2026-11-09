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
        assertEquals(ProfessorEvent.ExpShare, events[5])
        assertEquals(ProfessorEvent.TownMap, events[6])
        assertEquals(ProfessorEvent.ShinyCharm, events[7])
        val gifts = events.drop(8).flatMap { it.gifts }
        assertEquals(LegendaryItems.species.keys, gifts.toSet())
        events.drop(8).forEach { event ->
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

    @Test
    fun `Oak gives the Exp Share once Unova is unlocked`() {
        val before = setOf(Item.POKEDEX_KANTO, Item.POKEDEX_JOHTO, Item.POKEDEX_HOENN, Item.POKEDEX_SINNOH)
        assertTrue(ProfessorEvent.ExpShare !in ProfessorEvents.pending(true, 299, before))
        assertTrue(ProfessorEvent.ExpShare in ProfessorEvents.pending(true, 300, before))
        assertTrue(
            ProfessorEvent.ExpShare !in ProfessorEvents.pending(true, 300, before + Item.POKEDEX_UNOVA + Item.EXP_SHARE)
        )
    }

    @Test
    fun `legendaries are caught at high levels, gentle mythicals lower`() {
        assertEquals(70..80, legendaryLevels(150))
        assertEquals(80..90, legendaryLevels(493))
        assertEquals(30..45, legendaryLevels(151))
        assertEquals(null, legendaryLevels(25))
    }

    @Test
    fun `Tyrogue can become any of the three Hitmon`() {
        assertEquals(setOf(106, 107, 237), Pokemon[236]!!.evolvesTo.toSet())
        assertEquals(EvolutionRequirement.Level(20), Pokemon[106]!!.evolutionRequirement)
    }

    @Test
    fun `the clerk brings the Super Repel at 150 species`() {
        val bag = setOf(Item.REPEL)
        assertTrue(NpcEvent.SuperRepel !in NpcEvents.pending(0, 149, bag, true))
        assertTrue(NpcEvent.SuperRepel in NpcEvents.pending(0, 150, bag, true))
        assertTrue(NpcEvent.SuperRepel !in NpcEvents.pending(0, 300, bag + Item.SUPER_REPEL, true))
    }

    @Test
    fun `a Bug Catcher brings the Honey five species after Sinnoh opens`() {
        val bag = setOf(Item.POKEDEX_SINNOH)
        assertTrue(NpcEvent.Honey !in NpcEvents.pending(0, 204, bag, true))
        assertTrue(NpcEvent.Honey in NpcEvents.pending(0, 205, bag, true))
        assertTrue(NpcEvent.Honey !in NpcEvents.pending(0, 300, emptySet(), true))
        assertTrue(NpcEvent.Honey !in NpcEvents.pending(0, 300, bag + Item.HONEY, true))
    }
}
