package dev.equalparts.glyph_catch.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Pokémon cries as they sounded up to Black and White, from PokeAPI's cries. Like the Garden's
 * sprites, each one is downloaded the first time it's played and kept on the phone.
 */
object PokemonCries {
    private const val TAG = "PokemonCries"
    private const val BASE_URL = "https://raw.githubusercontent.com/PokeAPI/cries/main/cries/pokemon/legacy"
    private const val TIMEOUT_MS = 10_000

    private val mutex = Mutex()
    private var player: MediaPlayer? = null

    /** Plays the cry; false if it couldn't be downloaded (no connection, for example). */
    suspend fun play(context: Context, speciesId: Int): Boolean {
        val file = file(context, speciesId) ?: return false
        return withContext(Dispatchers.Main) {
            runCatching {
                player?.release()
                player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(file.path)
                    setOnCompletionListener {
                        it.release()
                        if (player === it) player = null
                    }
                    prepare()
                    start()
                }
                true
            }.onFailure {
                Log.w(TAG, "Couldn't play the cry of #$speciesId", it)
                file.delete()
            }.getOrDefault(false)
        }
    }

    private suspend fun file(context: Context, speciesId: Int): File? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val folder = File(context.filesDir, "cries").apply { mkdirs() }
            val cached = File(folder, "$speciesId.ogg")
            if (cached.length() > 0 || download("$BASE_URL/$speciesId.ogg", cached)) cached else null
        }
    }

    private fun download(url: String, target: File): Boolean = runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching false
            val partial = File(target.parentFile, "${target.name}.part")
            connection.inputStream.use { input -> partial.outputStream().use { input.copyTo(it) } }
            partial.renameTo(target)
        } finally {
            connection.disconnect()
        }
    }.onFailure { Log.w(TAG, "Couldn't download $url", it) }.getOrDefault(false)
}
