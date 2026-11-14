package dev.equalparts.glyph_catch.gameplay.animation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.DrawableRes
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.set
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixObject
import com.nothing.ketchum.GlyphMatrixUtils
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.util.PokemonSpriteUtils

/** Around the Poké Ball, clockwise from the top: just outside it, just inside the round matrix. */
private val DOTS_13 = listOf(6 to 1, 10 to 2, 11 to 6, 10 to 10, 6 to 11, 2 to 10, 1 to 6, 2 to 2)
private val DOTS_25 = listOf(12 to 0, 20 to 3, 23 to 12, 20 to 20, 12 to 23, 3 to 20, 0 to 12, 3 to 3)

/** Brightest value of a Glyph Matrix LED: frames hold one brightness per LED, not a color. */
internal const val MAX_LED_BRIGHTNESS = 2047

/**
 * Dims every LED of a frame by [factor]. Each value is scaled as a whole; treating it as a color
 * mixed up its bits and turned highlights into shadows.
 */
internal fun dimFrame(frame: IntArray, factor: Float): IntArray {
    if (factor >= 0.999f) return frame
    return IntArray(frame.size) { (frame[it] * factor).toInt().coerceIn(0, MAX_LED_BRIGHTNESS) }
}

/**
 * Provides various utility methods for drawing frames for the Glyph Matrix.
 */
internal class GlyphMatrixHelper(private val context: Context, val matrixSize: Int) {
    private val matrixCenter = matrixSize / 2

    /**
     * Load a Pokémon sprite as a [Bitmap].
     */
    fun getPokemonBitmap(pokemonId: Int, form: String? = null): Bitmap {
        val resourceId = PokemonSpriteUtils.getSpriteResourceId(context, pokemonId, form)
        return loadBitmap(resourceId)
    }

    /**
     * Render a Pokémon sprite to display on the Glyph Matrix.
     */
    fun renderPokemonFrame(pokemonId: Int, invertColors: Boolean = false, form: String? = null): IntArray {
        val base = getPokemonBitmap(pokemonId, form)
        val bitmap = if (invertColors) inverted(base) else base
        return renderBitmapFrame(bitmap)
    }

    /**
     * Render a [Bitmap] to display on the Glyph Matrix.
     */
    fun renderBitmapFrame(bitmap: Bitmap): IntArray {
        val sprite = GlyphMatrixObject.Builder()
            .setImageSource(bitmap)
            .build()

        return GlyphMatrixFrame.Builder()
            .addTop(sprite)
            .build(context)
            .render()
    }

    /**
     * Fill a circle for display on the Glyph Matrix.
     */
    fun renderCircleFrame(diameter: Int, color: Int = Color.WHITE): IntArray =
        renderBitmapFrame(createCircleBitmap(diameter, color))

    /**
     * Render a blank frame to display on the Glyph Matrix.
     */
    fun renderBlankFrame(): IntArray = IntArray(matrixSize * matrixSize)

    /**
     * Load the Pokémon catch animation frames.
     */
    fun loadCatchAnimationFrames(): List<IntArray> {
        val frameResources = intArrayOf(
            R.drawable.catch_frame_0,
            R.drawable.catch_frame_1,
            R.drawable.catch_frame_2,
            R.drawable.catch_frame_3,
            R.drawable.catch_frame_4,
            R.drawable.catch_frame_5,
            R.drawable.catch_frame_6,
            R.drawable.catch_frame_7
        )

        return frameResources.map { renderBitmapFrame(loadBitmap(it)) }
    }

    /**
     * Helper for inverting a [Bitmap].
     */
    fun inverted(bitmap: Bitmap): Bitmap {
        val inverted = createBitmap(bitmap.width, bitmap.height)
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val color = bitmap[x, y]
                val alpha = Color.alpha(color)
                inverted[x, y] = if (alpha == 0) {
                    color
                } else {
                    Color.argb(
                        alpha,
                        255 - Color.red(color),
                        255 - Color.green(color),
                        255 - Color.blue(color)
                    )
                }
            }
        }
        return inverted
    }

    class SpriteFrames(val primaryFrame: IntArray, val flashFrame: IntArray)

    fun createSpawnFrames(pokemonId: Int, isShiny: Boolean = false, form: String? = null): SpriteFrames {
        val primary = renderPokemonFrame(pokemonId, invertColors = isShiny, form = form)
        val flash = renderPokemonFrame(pokemonId, invertColors = !isShiny, form = form)
        return SpriteFrames(primaryFrame = primary, flashFrame = flash)
    }

    fun adjustBrightness(frame: IntArray, factor: Float): IntArray = dimFrame(frame, factor)

    /**
     * One dot per Pokémon waiting around the Poké Ball, clockwise from the top, up to eight and at
     * half brightness. The dot at [blinking] (a shiny) is left out when [blinkOn] is false.
     */
    fun withWaitingDots(frame: IntArray, count: Int, blinking: Int? = null, blinkOn: Boolean = true): IntArray {
        val dots = if (matrixSize <= 13) DOTS_13 else DOTS_25
        val size = if (matrixSize <= 13) 1 else 2
        val result = frame.copyOf()
        dots.take(count.coerceAtMost(dots.size)).forEachIndexed { index, (x, y) ->
            if (index == blinking && !blinkOn) return@forEachIndexed
            for (dy in 0 until size) {
                for (dx in 0 until size) {
                    val px = x + dx
                    val py = y + dy
                    if (px in 0 until matrixSize && py in 0 until matrixSize) {
                        result[py * matrixSize + px] = MAX_LED_BRIGHTNESS / 2
                    }
                }
            }
        }
        return result
    }

    /** Equal Parts' Poké Ball from the catch animation, at its true size on the small matrix. */
    fun renderPokeBallFrame(inverted: Boolean): IntArray {
        val bitmap = loadBitmap(if (matrixSize <= 13) R.drawable.glyph_pokeball_13 else R.drawable.glyph_pokeball_25)
        return renderBitmapFrame(if (inverted) inverted(bitmap) else bitmap)
    }

    /**
     * Loads a [Bitmap] from a resource ID using the Glyph Matrix SDK.
     */
    private fun loadBitmap(@DrawableRes resourceId: Int): Bitmap {
        val drawable = context.resources.getDrawable(resourceId, null)
        val base = GlyphMatrixUtils.drawableToBitmap(drawable)
        return base.copy(Bitmap.Config.ARGB_8888, true) ?: base
    }

    /**
     * Fills a circle on a [Bitmap] sized for the Glyph Matrix display.
     */
    private fun createCircleBitmap(diameter: Int, color: Int): Bitmap {
        val bitmap = createBitmap(matrixSize, matrixSize)
        val radius = diameter / 2.0
        for (x in 0 until matrixSize) {
            for (y in 0 until matrixSize) {
                val dx = x - matrixCenter
                val dy = y - matrixCenter
                if (dx * dx + dy * dy <= radius * radius) {
                    bitmap[x, y] = color
                }
            }
        }
        return bitmap
    }
}
