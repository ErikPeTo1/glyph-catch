package dev.equalparts.glyph_catch.gameplay.animation

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DimFrameTest {
    @Test
    fun `dimming keeps brighter LEDs brighter`() {
        val frame = IntArray(MAX_LED_BRIGHTNESS + 1) { it }
        val dimmed = dimFrame(frame, 0.45f)
        for (i in 1 until dimmed.size) assertTrue("LED $i got darker than ${i - 1}", dimmed[i] >= dimmed[i - 1])
        assertTrue(dimmed.last() in 900..925)
    }

    @Test
    fun `full brightness leaves the frame alone`() {
        val frame = intArrayOf(0, 767, 768, 2047)
        assertArrayEquals(frame, dimFrame(frame, 1f))
    }
}
