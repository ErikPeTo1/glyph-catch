package dev.equalparts.glyph_catch.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.equalparts.glyph_catch.R
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Emerald's Pokéblock colors, each with its picture from the games. Which one comes out depends on
 * the flavors that went in, like pokeemerald's CalculatePokeblockColor.
 */
enum class PokeblockColor(@param:DrawableRes val drawable: Int, @param:StringRes val nameRes: Int) {
    RED(R.drawable.pokeblock_red, R.string.pokeblock_red),
    BLUE(R.drawable.pokeblock_blue, R.string.pokeblock_blue),
    PINK(R.drawable.pokeblock_pink, R.string.pokeblock_pink),
    GREEN(R.drawable.pokeblock_green, R.string.pokeblock_green),
    YELLOW(R.drawable.pokeblock_yellow, R.string.pokeblock_yellow),
    PURPLE(R.drawable.pokeblock_purple, R.string.pokeblock_purple),
    INDIGO(R.drawable.pokeblock_indigo, R.string.pokeblock_indigo),
    BROWN(R.drawable.pokeblock_brown, R.string.pokeblock_brown),
    LITE_BLUE(R.drawable.pokeblock_liteblue, R.string.pokeblock_liteblue),
    OLIVE(R.drawable.pokeblock_olive, R.string.pokeblock_olive),
    GRAY(R.drawable.pokeblock_gray, R.string.pokeblock_gray),
    WHITE(R.drawable.pokeblock_white, R.string.pokeblock_white),
    GOLD(R.drawable.pokeblock_gold, R.string.pokeblock_gold)
}

/**
 * The Kitchen: Pokémon come in, eat a Pokéblock made from the berries you blend and go. One berry
 * per Pokémon, up to four at the table, one sweet each a day.
 */
object Kitchen {
    const val SEATS = 4

    /** A Pokéblock is worth more than a raw berry, and much more in its favorite flavor. */
    const val SWEET = 2
    const val FAVORITE = 3

    /** From this top speed on, a single flavor comes out Gold, the best there is. */
    const val GOLD_RPM = 100f

    /** Anyone can come, except those living in a zone of their own like the Orchard. */
    fun canCome(garden: GardenState, pokemonId: String): Boolean =
        garden.zoneOf(pokemonId).let { it == null || it == GardenZone.MEADOW }

    /**
     * The color for these berries: one flavor gives its own, two the stronger one's mix, three Gray
     * and more White. A house rule: the same berry twice doesn't turn it Black like in the games.
     */
    fun color(berries: List<Flavor>, maxRpm: Float): PokeblockColor {
        val counts = Flavor.entries.associateWith { flavor -> berries.count { it == flavor } }.filterValues { it > 0 }
        return when (counts.size) {
            0, 1 -> {
                if (maxRpm >= GOLD_RPM) return PokeblockColor.GOLD
                when (counts.keys.firstOrNull()) {
                    Flavor.SPICY, null -> PokeblockColor.RED
                    Flavor.DRY -> PokeblockColor.BLUE
                    Flavor.SWEET -> PokeblockColor.PINK
                    Flavor.BITTER -> PokeblockColor.GREEN
                    Flavor.SOUR -> PokeblockColor.YELLOW
                }
            }
            2 -> when (counts.maxBy { it.value }.key) {
                Flavor.SPICY -> PokeblockColor.PURPLE
                Flavor.DRY -> PokeblockColor.INDIGO
                Flavor.SWEET -> PokeblockColor.BROWN
                Flavor.BITTER -> PokeblockColor.LITE_BLUE
                Flavor.SOUR -> PokeblockColor.OLIVE
            }
            3 -> PokeblockColor.GRAY
            else -> PokeblockColor.WHITE
        }
    }

    /**
     * Friendship for one Pokémon: the sweet itself, plus its favorite flavor's share of the mix,
     * more the faster it was blended (the games add a third of the top speed to the flavors).
     */
    fun friendship(favorite: Flavor, berries: List<Flavor>, maxRpm: Float): Int {
        if (berries.isEmpty()) return SWEET
        val share = berries.count { it == favorite }.toFloat() / berries.size
        return SWEET + (FAVORITE * share * (1 + maxRpm / 333f)).roundToInt()
    }

    /** The tips only the TV tells, one a day in turn. */
    val TIPS = listOf(
        R.string.kitchen_tv_1,
        R.string.kitchen_tv_2,
        R.string.kitchen_tv_3,
        R.string.kitchen_tv_4,
        R.string.kitchen_tv_5,
        R.string.kitchen_tv_6,
        R.string.kitchen_tv_7,
        R.string.kitchen_tv_8,
        R.string.kitchen_tv_9,
        R.string.kitchen_tv_10,
        R.string.kitchen_tv_11,
        R.string.kitchen_tv_12
    )

    @StringRes
    fun tipOfDay(date: LocalDate = LocalDate.now()): Int =
        TIPS[Math.floorMod(date.toEpochDay(), TIPS.size.toLong()).toInt()]
}

/**
 * Emerald's Berry Blender, by its own numbers: the arrow goes round in 65536 steps, each player's
 * marker catches it in a window of the turn, and hits speed it up while misses slow it down.
 */
class BerryBlenderGame(val players: Int, private val random: kotlin.random.Random = kotlin.random.Random.Default) {
    enum class Hit { BEST, GOOD, MISS }

    var arrowPos = 0f
        private set
    var speed = MIN_SPEED
        private set
    var progress = 0f
        private set
    var maxRpm = 0f
        private set
    val done get() = progress >= MAX_PROGRESS
    val rpm get() = rpmOf(speed)

    private val divisor = SPEED_DIVISOR[players.coerceIn(0, SPEED_DIVISOR.lastIndex)]
    private var slowdown = 0f
    private val tried = BooleanArray(MARKERS)

    /** Moves the game on by this many 60 fps frames. Returns the helpers' hits, by marker. */
    fun step(frames: Float): List<Pair<Int, Hit>> {
        val before = arrowPos
        arrowPos = (arrowPos + speed * frames) % TURN
        slowdown += frames
        while (slowdown > SLOWDOWN_FRAMES) {
            slowdown -= SLOWDOWN_FRAMES
            if (speed > MIN_SPEED) speed--
        }
        maxRpm = maxOf(maxRpm, rpm)
        // Helpers press once each time the arrow comes by their marker, mostly well
        val hits = mutableListOf<Pair<Int, Hit>>()
        for (marker in 1 until players.coerceAtMost(MARKERS)) {
            val inRange = window(marker, arrowPos) != null
            if (!inRange) {
                tried[marker] = false
            } else if (!tried[marker] &&
                window(marker, before) != null &&
                pos(arrowPos) >= RANGE_START[marker] + HELPER_AT
            ) {
                tried[marker] = true
                val roll = random.nextInt(100)
                val hit = when {
                    roll < 45 -> Hit.BEST
                    roll < 90 -> Hit.GOOD
                    else -> Hit.MISS
                }
                apply(hit)
                hits += marker to hit
            }
        }
        if (window(0, arrowPos) == null) tried[0] = false
        return hits
    }

    /** The player presses: only the first press each time the arrow comes by counts. */
    fun press(): Hit {
        val hit = if (tried[0]) Hit.MISS else window(0, arrowPos) ?: Hit.MISS
        if (window(0, arrowPos) != null) tried[0] = true
        apply(hit)
        return hit
    }

    private fun apply(hit: Hit) {
        when (hit) {
            Hit.BEST -> {
                speed += (if (speed < FAST) 384 else 128) / divisor
                progress = (progress + speed / 55f).coerceAtMost(MAX_PROGRESS)
            }
            Hit.GOOD -> {
                if (speed < FAST) speed += 256 / divisor
                progress = (progress + speed / 70f).coerceAtMost(MAX_PROGRESS)
            }
            Hit.MISS -> speed = (speed - 256 / divisor).coerceAtLeast(MIN_SPEED)
        }
        maxRpm = maxOf(maxRpm, rpm)
    }

    private fun pos(arrow: Float) = (arrow / 256).toInt() + 24

    private fun window(marker: Int, arrow: Float): Hit? {
        val pos = pos(arrow)
        val start = RANGE_START[marker]
        return when {
            pos !in start until start + 48 -> null
            pos in start + 20 until start + 28 -> Hit.BEST
            else -> Hit.GOOD
        }
    }

    companion object {
        const val TURN = 65536f
        const val MARKERS = 4
        const val MAX_PROGRESS = 1000f
        private const val MIN_SPEED = 128
        private const val FAST = 1500
        private const val SLOWDOWN_FRAMES = 6f
        private const val HELPER_AT = 24
        private val SPEED_DIVISOR = intArrayOf(1, 1, 2, 3, 4)

        /** Where each marker's window starts, top left, top right, bottom left, bottom right. */
        private val RANGE_START = intArrayOf(32, 224, 96, 160)

        fun rpmOf(speed: Int): Float = 60f * 60f * speed / TURN
    }
}
