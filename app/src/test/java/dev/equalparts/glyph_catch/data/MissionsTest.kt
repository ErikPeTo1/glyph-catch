package dev.equalparts.glyph_catch.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionsTest {

    @Test
    fun `missions stay the same during the day`() {
        val date = LocalDate.of(2026, 10, 28)
        assertEquals(Missions.current(date, hasRod = true), Missions.current(date, hasRod = true))
    }

    @Test
    fun `three daily missions with different goals plus one weekly`() {
        (0L until 60L).map { LocalDate.of(2026, 1, 1).plusDays(it) }.forEach { date ->
            val missions = Missions.current(date, hasRod = true)
            val daily = missions.filterNot { it.weekly }
            assertEquals(3, daily.size)
            assertEquals(3, daily.map { it.stat }.toSet().size)
            assertEquals(1, missions.count { it.weekly })
        }
    }

    @Test
    fun `no fishing missions without a rod`() {
        (0L until 60L).map { LocalDate.of(2026, 1, 1).plusDays(it) }.forEach { date ->
            assertTrue(Missions.current(date, hasRod = false).none { it.stat == Stat.ROD_CATCHES })
        }
    }

    @Test
    fun `the weekly mission is shared by the whole week`() {
        val monday = LocalDate.of(2026, 10, 26)
        val weekly = Missions.current(monday, hasRod = true).single { it.weekly }
        (1L..6L).forEach { offset ->
            assertEquals(weekly, Missions.current(monday.plusDays(offset), hasRod = true).single { it.weekly })
        }
    }
}
