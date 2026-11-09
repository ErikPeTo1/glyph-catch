package dev.equalparts.glyph_catch.gameplay

import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.gameplay.animation.GlyphMatrixHelper
import dev.equalparts.glyph_catch.util.GlyphDevice
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Draws on the Glyph Matrix from the app while the phone is in use, when the Glyph Toy is off,
 * so a Pokémon caught from the app or a widget still gets its animation on the back.
 */
object AppMatrix {
    private const val TAG = "AppMatrix"
    private const val CONNECT_TIMEOUT_MS = 3000L
    private const val BALL_HOLD_MS = 900L
    private const val FRAME_MS = 100L
    private const val FIRST_FRAME_MS = 500L
    private const val SHOW_CAUGHT_MS = 2500L
    private const val SHAKE_MS = 350L

    private val mutex = Mutex()

    /** Whether a catch is playing; further catches wait until it ends. */
    @Volatile
    var isPlaying = false
        private set

    suspend fun playCatch(context: Context, caught: CaughtPokemon) = withContext(Dispatchers.Default) {
        mutex.withLock {
            isPlaying = true
            try {
                val app = context.applicationContext
                val gmm = connect(app) ?: return@withLock
                try {
                    for ((frame, millis) in catchFrames(GlyphMatrixHelper(app, GlyphDevice.matrixSize), caught)) {
                        gmm.setAppMatrixFrame(frame)
                        delay(millis)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Couldn't draw the catch", e)
                } finally {
                    runCatching { gmm.closeAppMatrix() }
                    runCatching { gmm.unInit() }
                }
            } finally {
                isPlaying = false
            }
        }
    }

    /**
     * Equal Parts' catch animation, then the Pokémon caught. The small matrix of the Phone (4a) Pro
     * can't show either, so its Poké Ball shakes instead.
     */
    private fun catchFrames(helper: GlyphMatrixHelper, caught: CaughtPokemon): List<Pair<IntArray, Long>> {
        if (helper.matrixSize < 25) {
            val ball = helper.renderPokeBallFrame(false)
            val flash = helper.renderPokeBallFrame(true)
            val blank = helper.renderBlankFrame()
            return listOf(ball to SHAKE_MS, blank to SHAKE_MS, ball to SHAKE_MS, blank to SHAKE_MS) +
                listOf(ball to BALL_HOLD_MS, flash to SHAKE_MS, ball to SHOW_CAUGHT_MS)
        }
        val animation = helper.loadCatchAnimationFrames().mapIndexed { index, frame ->
            frame to if (index == 0) FIRST_FRAME_MS else FRAME_MS
        }
        val pokemon = helper.renderPokemonFrame(caught.speciesId, invertColors = caught.isShiny, form = caught.form)
        return animation + (animation.last().first to BALL_HOLD_MS) + (pokemon to SHOW_CAUGHT_MS)
    }

    private suspend fun connect(app: Context): GlyphMatrixManager? = withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
        suspendCancellableCoroutine { continuation ->
            val gmm = GlyphMatrixManager.getInstance(app)
            if (gmm == null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            gmm.init(object : GlyphMatrixManager.Callback {
                override fun onServiceConnected(name: ComponentName?) {
                    runCatching {
                        gmm.register(if (GlyphDevice.isPhone4aPro) Glyph.DEVICE_25111p else Glyph.DEVICE_23112)
                    }
                    if (continuation.isActive) continuation.resume(gmm)
                }

                override fun onServiceDisconnected(name: ComponentName?) {}
            })
        }
    }
}
