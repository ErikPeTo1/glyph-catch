package dev.equalparts.glyph_catch.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.graphics.scale
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.Eggs
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.Type
import dev.equalparts.glyph_catch.gameplay.PersistentSpawn
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The PC box wallpapers from Pokémon Emerald. The inside of some of them is a pattern that
 * repeats every [periodX] / [periodY] pixels; the rest are scenes (0), which are cropped to fit:
 * around the middle sideways, and keeping the bottom (shores, clouds, the Poké Ball) up and down,
 * unless the scene is [centered]. The ones with an [achievement] stay locked until it's reached.
 */
enum class BoxWallpaper(
    @param:DrawableRes val drawable: Int,
    @param:StringRes val nameRes: Int,
    val periodX: Int,
    val periodY: Int,
    val dark: Boolean,
    val centered: Boolean = false,
    val achievement: Achievement? = null
) {
    FOREST(R.drawable.box_wallpaper_forest, R.string.box_wallpaper_forest, 32, 16, false),
    CITY(R.drawable.box_wallpaper_city, R.string.box_wallpaper_city, 32, 16, false),
    DESERT(R.drawable.box_wallpaper_desert, R.string.box_wallpaper_desert, 0, 0, false),
    SAVANNA(R.drawable.box_wallpaper_savanna, R.string.box_wallpaper_savanna, 0, 0, false),
    CRAG(R.drawable.box_wallpaper_crag, R.string.box_wallpaper_crag, 32, 32, false),
    VOLCANO(R.drawable.box_wallpaper_volcano, R.string.box_wallpaper_volcano, 0, 0, false, centered = true),
    SNOW(R.drawable.box_wallpaper_snow, R.string.box_wallpaper_snow, 32, 0, false),
    CAVE(R.drawable.box_wallpaper_cave, R.string.box_wallpaper_cave, 16, 16, false),
    BEACH(R.drawable.box_wallpaper_beach, R.string.box_wallpaper_beach, 96, 0, false),
    SEAFLOOR(R.drawable.box_wallpaper_seafloor, R.string.box_wallpaper_seafloor, 96, 0, true),
    RIVER(R.drawable.box_wallpaper_river, R.string.box_wallpaper_river, 24, 0, false),
    SKY(R.drawable.box_wallpaper_sky, R.string.box_wallpaper_sky, 0, 0, false),
    POLKADOT(R.drawable.box_wallpaper_polkadot, R.string.box_wallpaper_polkadot, 32, 64, false),
    POKECENTER(R.drawable.box_wallpaper_pokecenter, R.string.box_wallpaper_pokecenter, 0, 0, false),
    MACHINE(R.drawable.box_wallpaper_machine, R.string.box_wallpaper_machine, 0, 0, false),
    PLAIN(R.drawable.box_wallpaper_plain, R.string.box_wallpaper_plain, 0, 0, false),

    // Earned with achievements
    PIKACHU(R.drawable.box_wallpaper_pikachu, R.string.box_wallpaper_pikachu, 0, 0, false, false, Achievement.PIKACHU),
    ZIGZAG(R.drawable.box_wallpaper_zigzag, R.string.box_wallpaper_zigzag, 48, 0, false, false, Achievement.CATCHES),
    POKE_BALLS(
        R.drawable.box_wallpaper_pokeballs,
        R.string.box_wallpaper_pokeballs,
        0,
        0,
        false,
        true,
        Achievement.MORE_CATCHES
    ),
    CIRCLES(
        R.drawable.box_wallpaper_circles,
        R.string.box_wallpaper_circles,
        80,
        32,
        false,
        false,
        Achievement.SPECIES
    ),
    AZUMARILL(
        R.drawable.box_wallpaper_azumarill,
        R.string.box_wallpaper_azumarill,
        32,
        0,
        false,
        false,
        Achievement.ROD_CATCHES
    ),
    WHISCASH(
        R.drawable.box_wallpaper_whiscash,
        R.string.box_wallpaper_whiscash,
        0,
        0,
        false,
        false,
        Achievement.SUPER_ROD
    ),
    LUDICOLO(
        R.drawable.box_wallpaper_ludicolo,
        R.string.box_wallpaper_ludicolo,
        0,
        0,
        false,
        false,
        Achievement.EVOLUTIONS
    ),
    POKECENTER_2(
        R.drawable.box_wallpaper_pokecenter2,
        R.string.box_wallpaper_pokecenter2,
        0,
        0,
        false,
        false,
        Achievement.EGGS
    ),
    STARS(R.drawable.box_wallpaper_stars, R.string.box_wallpaper_stars, 0, 0, true, true, Achievement.SHINY),
    HEARTS(R.drawable.box_wallpaper_hearts, R.string.box_wallpaper_hearts, 0, 0, true, true, Achievement.LEVEL_100),
    DUSCLOPS(
        R.drawable.box_wallpaper_dusclops,
        R.string.box_wallpaper_dusclops,
        96,
        0,
        false,
        true,
        Achievement.GHOSTS
    ),
    MEANDER(R.drawable.box_wallpaper_meander, R.string.box_wallpaper_meander, 96, 0, false, true, Achievement.MISSIONS),
    STRIPES(R.drawable.box_wallpaper_stripes, R.string.box_wallpaper_stripes, 0, 0, false, true, Achievement.DAYS),
    BRICKS(R.drawable.box_wallpaper_bricks, R.string.box_wallpaper_bricks, 16, 16, true, false, Achievement.MINUTES),
    LEGENDARY(
        R.drawable.box_wallpaper_legendary,
        R.string.box_wallpaper_legendary,
        0,
        0,
        false,
        false,
        Achievement.LEGENDARIES
    )
}

/**
 * Background of the home-screen widgets: any color picked with the RGB sliders, or a PC box
 * wallpaper. Saved as text, e.g. `color:#FF000000` or `box:FOREST`.
 */
sealed interface WidgetBackground {
    /** Whether text and empty slots on it should be light. */
    val dark: Boolean

    fun encode(): String

    data class Solid(val argb: Int) : WidgetBackground {
        override val dark: Boolean
            get() {
                val alpha = argb ushr 24
                val luminance = 0.299 * (argb shr 16 and 0xFF) + 0.587 * (argb shr 8 and 0xFF) + 0.114 * (argb and 0xFF)
                // See-through backgrounds sit on top of the home screen wallpaper, usually dark
                return alpha < 128 || luminance < 140
            }

        override fun encode() = "color:#%08X".format(argb)
    }

    data class Wallpaper(val wallpaper: BoxWallpaper) : WidgetBackground {
        override val dark: Boolean get() = wallpaper.dark

        override fun encode() = "box:${wallpaper.name}"
    }

    /** Waiting Pokémon only: the box that fits the Pokémon at the front of the queue. */
    data object Auto : WidgetBackground {
        override val dark = false

        override fun encode() = "auto"

        fun resolve(spawn: PersistentSpawn?): Wallpaper = Wallpaper(
            spawn?.let { POOLS.entries.firstOrNull { (pool, _) -> it.poolName.startsWith(pool) }?.value }
                ?: spawn?.let { Pokemon[it.pokemonId] }?.let { species ->
                    // Pidgey and friends are Normal first, but they belong in the sky
                    val type = species.type2?.takeIf { species.type1 == Type.NORMAL } ?: species.type1
                    TYPES.getValue(type)
                }
                ?: BoxWallpaper.PLAIN
        )

        private val POOLS = mapOf(
            "Old Rod" to BoxWallpaper.RIVER,
            "Good Rod" to BoxWallpaper.BEACH,
            "Super Rod" to BoxWallpaper.SEAFLOOR,
            Eggs.POOL_NAME to BoxWallpaper.POKECENTER,
            "Charging" to BoxWallpaper.MACHINE,
            "Halloween" to BoxWallpaper.CAVE,
            "Christmas" to BoxWallpaper.SNOW,
            "Valentine" to BoxWallpaper.POLKADOT,
            "Pokémon Day" to BoxWallpaper.POKECENTER,
            "New Year" to BoxWallpaper.CITY,
            "San Juan" to BoxWallpaper.BEACH,
            "Full moon" to BoxWallpaper.SKY
        )

        private val TYPES = mapOf(
            Type.NORMAL to BoxWallpaper.SAVANNA,
            Type.FIRE to BoxWallpaper.VOLCANO,
            Type.WATER to BoxWallpaper.RIVER,
            Type.ELECTRIC to BoxWallpaper.MACHINE,
            Type.GRASS to BoxWallpaper.FOREST,
            Type.ICE to BoxWallpaper.SNOW,
            Type.FIGHTING to BoxWallpaper.CRAG,
            Type.POISON to BoxWallpaper.CAVE,
            Type.GROUND to BoxWallpaper.DESERT,
            Type.FLYING to BoxWallpaper.SKY,
            Type.PSYCHIC to BoxWallpaper.POLKADOT,
            Type.BUG to BoxWallpaper.FOREST,
            Type.ROCK to BoxWallpaper.CRAG,
            Type.GHOST to BoxWallpaper.CAVE,
            Type.DRAGON to BoxWallpaper.SKY,
            Type.DARK to BoxWallpaper.CITY,
            Type.STEEL to BoxWallpaper.MACHINE,
            Type.FAIRY to BoxWallpaper.POLKADOT
        )
    }

    companion object {
        val DEFAULT = Solid(0xFF000000.toInt())

        fun from(value: String?): WidgetBackground = when {
            value == null -> DEFAULT
            value == "auto" -> Auto
            value.startsWith("color:#") -> value.removePrefix("color:#").toLongOrNull(16)?.let { Solid(it.toInt()) }
            value.startsWith("box:") -> BoxWallpaper.entries.firstOrNull { it.name == value.removePrefix("box:") }
                ?.let { Wallpaper(it) }
            // The fixed colors from before the RGB picker
            else -> LEGACY[value]?.let { Solid(it.toInt()) }
        } ?: DEFAULT

        private val LEGACY = mapOf(
            "BLACK" to 0xFF000000,
            "DARK" to 0xFF1C1C1E,
            "TRANSPARENT" to 0x00000000L,
            "WHITE" to 0xFFFFFFFF,
            "RED" to 0xFFD71921
        )
    }
}

/** A box wallpaper drawn at a widget's size, and how thick its frame came out. */
class RenderedWallpaper(val bitmap: Bitmap, val frameDp: Float)

/**
 * Fits a box wallpaper to any widget size without stretching its pixels: the frame stays the
 * same (thinner on narrow widgets), patterns repeat, and scenes are cropped to fit.
 */
object WallpaperRenderer {
    private const val WIDTH = 160
    private const val HEIGHT = 120
    private const val FRAME = 8
    private const val PIXEL_DP = 2.5f

    // The frame is at most this much of the widget's shorter side, on each edge
    private const val MAX_FRAME_SHARE = 0.12f

    fun render(context: Context, wallpaper: BoxWallpaper, widthDp: Float, heightDp: Float): RenderedWallpaper? {
        val density = context.resources.displayMetrics.density
        val widthPx = (widthDp * density).roundToInt()
        val heightPx = (heightDp * density).roundToInt()
        if (widthPx <= 0 || heightPx <= 0) return null

        // Scenes can't repeat, so their pixels grow until the scene covers the widget, but never
        // so much that the frame eats a narrow widget: what's left uncovered extends the scene's edges
        var pixel = max(1, (PIXEL_DP * density).roundToInt())
        if (wallpaper.periodX == 0) pixel = max(pixel, ceil(widthPx / WIDTH.toFloat()).toInt())
        if (wallpaper.periodY == 0) pixel = max(pixel, ceil(heightPx / HEIGHT.toFloat()).toInt())
        val thickest = (min(widthPx, heightPx) * MAX_FRAME_SHARE / FRAME).toInt()
        pixel = pixel.coerceAtMost(thickest).coerceAtLeast(1)
        val width = max(FRAME * 2 + 1, ceil(widthPx / pixel.toFloat()).toInt())
        val height = max(FRAME * 2 + 1, ceil(heightPx / pixel.toFloat()).toInt())

        val options = BitmapFactory.Options().apply { inScaled = false }
        val source = BitmapFactory.decodeResource(context.resources, wallpaper.drawable, options) ?: return null
        val sourcePixels = IntArray(WIDTH * HEIGHT)
        source.getPixels(sourcePixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT)

        val xs = IntArray(width) { mapAxis(it, width, WIDTH, wallpaper.periodX, keepEnd = false) }
        val ys = IntArray(height) { mapAxis(it, height, HEIGHT, wallpaper.periodY, keepEnd = !wallpaper.centered) }
        val pixels = IntArray(width * height) { sourcePixels[ys[it / width] * WIDTH + xs[it % width]] }
        val small = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        return RenderedWallpaper(small.scale(width * pixel, height * pixel, false), FRAME * pixel / density)
    }

    private fun mapAxis(index: Int, size: Int, sourceSize: Int, period: Int, keepEnd: Boolean): Int {
        val inside = sourceSize - FRAME * 2
        val target = size - FRAME * 2
        val crop = if (keepEnd) inside - target else (inside - target) / 2
        return when {
            index < FRAME -> index
            index >= size - FRAME -> sourceSize - (size - index)
            period > 0 -> FRAME + (index - FRAME) % period
            // Past the scene's edge, its last row or column repeats
            else -> (FRAME + crop + (index - FRAME)).coerceIn(FRAME, sourceSize - FRAME - 1)
        }.coerceIn(0, sourceSize - 1)
    }
}
