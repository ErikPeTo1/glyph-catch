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
}
