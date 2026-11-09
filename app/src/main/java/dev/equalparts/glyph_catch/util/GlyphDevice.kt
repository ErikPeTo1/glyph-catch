package dev.equalparts.glyph_catch.util

import com.nothing.ketchum.Common
import dev.equalparts.glyph_catch.data.PreferencesManager

/**
 * Which phone the Glyph Toy runs on. The Phone (4a) Pro has a 13×13 Glyph Matrix and its button
 * never reaches Glyph Toys, so there Pokémon are caught from the app and the widgets, and the
 * matrix shows a Poké Ball whenever one is waiting.
 */
object GlyphDevice {
    val isPhone4aPro: Boolean get() = runCatching { Common.is25111p() }.getOrDefault(false)

    val matrixSize: Int get() = if (isPhone4aPro) 13 else 25

    /** The (4a) Pro way of playing, also available on other phones from the hidden settings to try it. */
    fun pokeBallMode(preferences: PreferencesManager): Boolean = isPhone4aPro || preferences.simulatePhone4aPro
}
