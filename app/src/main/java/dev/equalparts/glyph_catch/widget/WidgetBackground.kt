package dev.equalparts.glyph_catch.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.Eggs
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.Type
import dev.equalparts.glyph_catch.gameplay.PersistentSpawn
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The PC box wallpapers from Pokémon Emerald. The inside of some of them is a pattern that
 * repeats every [periodX] / [periodY] pixels; the rest are scenes (0), which are cropped to fit:
 * around the middle sideways, and keeping the bottom (shores, clouds, the Poké Ball) up and down,
 * unless the scene is [centered]. [stretched] ones are just frames and grow from their plainest line;
 * [fitHeight] ones have bands at the top and bottom that always show. The ones with an [achievement]
 * stay locked until it's reached.
 */
enum class BoxWallpaper(
    @param:DrawableRes val drawable: Int,
    @param:StringRes val nameRes: Int,
    val periodX: Int,
    val periodY: Int,
    val dark: Boolean,
    val centered: Boolean = false,
    val achievement: Achievement? = null,
    val stretched: Boolean = false,
    val fitHeight: Boolean = false
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

    // Just a frame inside the frame: it stretches from the middle so its lines stay at the edges
    PLAIN(R.drawable.box_wallpaper_plain, R.string.box_wallpaper_plain, 0, 0, false, stretched = true),

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
    STARS(
        R.drawable.box_wallpaper_stars,
        R.string.box_wallpaper_stars,
        0,
        0,
        true,
        true,
        Achievement.SHINY,
        fitHeight = true
    ),
    HEARTS(R.drawable.box_wallpaper_hearts, R.string.box_wallpaper_hearts, 0, 0, true, true, Achievement.LEVEL_100),
    DUSCLOPS(
        R.drawable.box_wallpaper_dusclops,
        R.string.box_wallpaper_dusclops,
        96,
        0,
        false,
        true,
        Achievement.GHOSTS,
        fitHeight = true
    ),
    MEANDER(
        R.drawable.box_wallpaper_meander,
        R.string.box_wallpaper_meander,
        96,
        0,
        false,
        true,
        Achievement.MISSIONS,
        fitHeight = true
    ),
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
            "Honey" to BoxWallpaper.FOREST,
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
 * Fits a box wallpaper to any widget size without stretching its pixels. The frame keeps its look
 * (thinner on narrow widgets); inside, patterns repeat and scenes grow until they cover the widget,
 * then are cropped. Where a scene is still too short, its plainest line repeats.
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

        val options = BitmapFactory.Options().apply { inScaled = false }
        val source = BitmapFactory.decodeResource(context.resources, wallpaper.drawable, options) ?: return null
        val sourcePixels = IntArray(WIDTH * HEIGHT)
        source.getPixels(sourcePixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT)

        val base = max(1, (PIXEL_DP * density).roundToInt())
        val thickest = max(1, (min(widthPx, heightPx) * MAX_FRAME_SHARE / FRAME).toInt())
        val insideWidth = WIDTH - FRAME * 2
        val insideHeight = HEIGHT - FRAME * 2

        // The scene's own pixel size, big enough to cover the inside whatever the widget's shape
        val firstFrame = min(base, thickest)
        var scene = base
        if (wallpaper.periodX == 0) {
            scene = max(scene, ceil((widthPx - 2f * FRAME * firstFrame) / insideWidth).toInt())
        }
        if (wallpaper.periodY == 0) {
            scene = max(scene, ceil((heightPx - 2f * FRAME * firstFrame) / insideHeight).toInt())
        }
        // Bands along the top and bottom: the whole height always shows, however short the widget
        if (wallpaper.fitHeight) scene = max(1, ((heightPx - 2 * FRAME * firstFrame) / insideHeight))
        val frame = scene.coerceIn(1, thickest)

        val edge = FRAME * frame
        val columns = ceil((widthPx - 2f * edge) / scene).toInt()
        val rows = ceil((heightPx - 2f * edge) / scene).toInt()
        val frameColumns = ceil(widthPx / frame.toFloat()).toInt()
        val frameRows = ceil(heightPx / frame.toFloat()).toInt()
        val quietX = quietLine(sourcePixels, alongX = true)
        val quietY = quietLine(sourcePixels, alongX = false)

        val xs = IntArray(widthPx) { x ->
            when {
                wallpaper.stretched || x < edge || x >= widthPx - edge -> stretch(
                    x / frame,
                    frameColumns,
                    WIDTH,
                    quietX
                )
                else -> inside((x - edge) / scene, columns, insideWidth, wallpaper.periodX, keepEnd = false, quietX)
            }
        }
        val frameXs = IntArray(widthPx) { stretch(it / frame, frameColumns, WIDTH, quietX) }
        val pixels = IntArray(widthPx * heightPx)
        for (y in 0 until heightPx) {
            val inFrame = wallpaper.stretched || y < edge || y >= heightPx - edge
            val sy = if (inFrame) {
                stretch(y / frame, frameRows, HEIGHT, quietY)
            } else {
                inside((y - edge) / scene, rows, insideHeight, wallpaper.periodY, !wallpaper.centered, quietY)
            }
            val row = if (inFrame) frameXs else xs
            for (x in 0 until widthPx) pixels[y * widthPx + x] = sourcePixels[sy * WIDTH + row[x]]
        }
        val bitmap = Bitmap.createBitmap(pixels, widthPx, heightPx, Bitmap.Config.ARGB_8888)
        return RenderedWallpaper(bitmap, edge / density)
    }

    /** Where the inside of the source shows at [index] of [size] lines. */
    private fun inside(index: Int, size: Int, sourceSize: Int, period: Int, keepEnd: Boolean, quiet: Int): Int {
        if (period > 0) return FRAME + index % period
        if (size > sourceSize) return FRAME + stretch(index, size, sourceSize, quiet - FRAME)
        val crop = if (keepEnd) sourceSize - size else (sourceSize - size) / 2
        return FRAME + crop + index
    }

    /**
     * Keeps both ends of the source: when [size] is longer, line [quiet] repeats in between;
     * when shorter, the ends meet in the middle.
     */
    private fun stretch(index: Int, size: Int, sourceSize: Int, quiet: Int): Int {
        if (size <= sourceSize) {
            val half = sourceSize / 2
            return if (index < size / 2) min(index, half) else max(sourceSize - (size - index), half)
        }
        val tail = sourceSize - quiet
        return if (index < size - tail) min(index, quiet) else sourceSize - (size - index)
    }

    /** The plainest column (or row) in the middle half, so repeating it doesn't smear a detail. */
    private fun quietLine(pixels: IntArray, alongX: Boolean): Int {
        val length = if (alongX) WIDTH else HEIGHT
        val across = if (alongX) HEIGHT else WIDTH
        val background = pixels[HEIGHT / 2 * WIDTH + WIDTH / 2]
        return (length / 4 until length * 3 / 4).minBy { line ->
            val busy = (FRAME until across - FRAME).count { other ->
                val pixel = if (alongX) pixels[other * WIDTH + line] else pixels[line * WIDTH + other]
                pixel != background
            }
            busy * 1000 + abs(line - length / 2)
        }
    }
}
