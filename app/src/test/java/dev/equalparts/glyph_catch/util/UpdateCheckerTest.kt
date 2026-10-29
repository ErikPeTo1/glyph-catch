package dev.equalparts.glyph_catch.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun `newer versions are detected`() {
        assertTrue(UpdateChecker.isNewer("2.0.1", "2.0.0"))
        assertTrue(UpdateChecker.isNewer("2.1.0", "2.0.9"))
        assertTrue(UpdateChecker.isNewer("2.10.0", "2.9.0"))
        assertTrue(UpdateChecker.isNewer("3.0", "2.9.9"))
    }

    @Test
    fun `same or older versions are not updates`() {
        assertFalse(UpdateChecker.isNewer("2.0.0", "2.0.0"))
        assertFalse(UpdateChecker.isNewer("2.0", "2.0.0"))
        assertFalse(UpdateChecker.isNewer("1.9.9", "2.0.0"))
        assertFalse(UpdateChecker.isNewer("2.0.0", "2.0.0-beta"))
    }
}
