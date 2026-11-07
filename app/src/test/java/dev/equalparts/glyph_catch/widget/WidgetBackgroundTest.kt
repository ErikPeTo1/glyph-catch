package dev.equalparts.glyph_catch.widget

import dev.equalparts.glyph_catch.gameplay.PersistentSpawn
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetBackgroundTest {
    private fun spawn(id: Int, pool: String = "Common") = PersistentSpawn(id, pool, false, false, 0)

    private fun auto(spawn: PersistentSpawn?) = WidgetBackground.Auto.resolve(spawn).wallpaper

    @Test
    fun `the automatic box follows the Pokemon at the front`() {
        assertEquals(BoxWallpaper.PLAIN, auto(null))
        assertEquals(BoxWallpaper.RIVER, auto(spawn(129, "Old Rod")))
        assertEquals(BoxWallpaper.SEAFLOOR, auto(spawn(130, "Super Rod")))
        assertEquals(BoxWallpaper.MACHINE, auto(spawn(595, "Charging Joltik")))
        assertEquals(BoxWallpaper.FOREST, auto(spawn(1)))
        assertEquals(BoxWallpaper.VOLCANO, auto(spawn(4)))
        assertEquals(BoxWallpaper.SKY, auto(spawn(16)))
        assertEquals(BoxWallpaper.SAVANNA, auto(spawn(19)))
    }

    @Test
    fun `saved backgrounds come back, old fixed colors included`() {
        assertEquals(WidgetBackground.Auto, WidgetBackground.from("auto"))
        assertEquals(WidgetBackground.Wallpaper(BoxWallpaper.CAVE), WidgetBackground.from("box:CAVE"))
        val red = WidgetBackground.Solid(0xFFD71921.toInt())
        assertEquals(red, WidgetBackground.from(red.encode()))
        assertEquals(red, WidgetBackground.from("RED"))
        assertEquals(WidgetBackground.DEFAULT, WidgetBackground.from("nonsense"))
    }
}
