package dev.equalparts.glyph_catch.util

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ShinyOddsTest {

    @Test
    fun `normal days use the base odds`() {
        assertEquals(512, EventHelpers.shinyOdds(LocalDate.of(2026, 3, 10)))
        assertEquals(512, EventHelpers.shinyOdds(LocalDate.of(2026, 10, 24)))
        assertEquals(512, EventHelpers.shinyOdds(LocalDate.of(2026, 12, 27)))
    }

    @Test
    fun `event weeks double the odds`() {
        assertEquals(256, EventHelpers.shinyOdds(LocalDate.of(2026, 10, 25)))
        assertEquals(256, EventHelpers.shinyOdds(LocalDate.of(2026, 10, 30)))
        assertEquals(256, EventHelpers.shinyOdds(LocalDate.of(2026, 12, 19)))
        assertEquals(256, EventHelpers.shinyOdds(LocalDate.of(2026, 12, 26)))
    }

    @Test
    fun `big days quadruple the odds`() {
        assertEquals(128, EventHelpers.shinyOdds(LocalDate.of(2026, 10, 31)))
        assertEquals(128, EventHelpers.shinyOdds(LocalDate.of(2026, 12, 24)))
        assertEquals(128, EventHelpers.shinyOdds(LocalDate.of(2026, 12, 25)))
        assertEquals(128, EventHelpers.shinyOdds(LocalDate.of(2027, 1, 1)))
        assertEquals(128, EventHelpers.shinyOdds(LocalDate.of(2027, 2, 27)))
    }

    @Test
    fun `the Shiny Charm triples the odds, holidays included`() {
        assertEquals(170, EventHelpers.shinyOdds(LocalDate.of(2026, 3, 10), charm = true))
        assertEquals(85, EventHelpers.shinyOdds(LocalDate.of(2026, 10, 25), charm = true))
        assertEquals(42, EventHelpers.shinyOdds(LocalDate.of(2026, 10, 31), charm = true))
    }

    @Test
    fun `catching a species many times makes its shiny more likely`() {
        assertEquals(1, EventHelpers.catchShinyMultiplier(0))
        assertEquals(1, EventHelpers.catchShinyMultiplier(9))
        assertEquals(2, EventHelpers.catchShinyMultiplier(10))
        assertEquals(3, EventHelpers.catchShinyMultiplier(25))
        assertEquals(4, EventHelpers.catchShinyMultiplier(500))
        assertEquals(3, EventHelpers.catchesToNextStep(22))
        assertEquals(null, EventHelpers.catchesToNextStep(50))
    }
}
