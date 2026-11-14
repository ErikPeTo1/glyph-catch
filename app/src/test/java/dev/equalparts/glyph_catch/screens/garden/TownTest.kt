package dev.equalparts.glyph_catch.screens.garden

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TownTest {
    private val start = HeroSpot()

    @Test
    fun `every door can be reached from where the hero starts`() {
        TownMapData.PLACES.forEach { (name, area) ->
            assertNotNull(name, townPath(start.tileX, start.tileY, area.doorX, area.doorY))
        }
    }

    @Test
    fun `the hero never steps on trees, buildings, fences or water`() {
        TownMapData.PLACES.values.forEach { area ->
            townPath(start.tileX, start.tileY, area.doorX, area.doorY)!!.forEach { (x, y) ->
                assertTrue(TownMapData.walkable(x, y))
            }
        }
        // Inside the Gym or a tree is never a destination
        val gym = TownMapData.PLACES.getValue("gym")
        assertNull(townPath(start.tileX, start.tileY, gym.x + 2, gym.y + 2))
        assertNull(townPath(start.tileX, start.tileY, 0, 0))
    }
}
