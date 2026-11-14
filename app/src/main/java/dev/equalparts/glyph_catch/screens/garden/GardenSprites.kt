package dev.equalparts.glyph_catch.screens.garden

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.viewinterop.AndroidView
import dev.equalparts.glyph_catch.LocalColorSprites
import dev.equalparts.glyph_catch.data.Forms
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.util.PokemonSpriteUtils
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The animated sprites from Pokémon Black and White, for the Garden. All 649 would double the
 * app's size, so each one is downloaded the first time it's needed and kept on the phone.
 * Until then, or without a connection, the Garden shows the regular sprite.
 */
object GardenSprites {
    private const val TAG = "GardenSprites"
    private const val BASE_URL =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/versions/generation-v/black-white/animated"
    private const val TIMEOUT_MS = 10_000

    private val mutex = Mutex()

    /** Downloads the sprites of whoever lives in the Garden, so they're ready when it opens. */
    suspend fun prefetch(context: Context, db: PokemonDatabase) = withContext(Dispatchers.IO) {
        val all = db.pokemonDao().getAllCaught()
        residentsOf(all, PreferencesManager(context).team).forEach { file(context, it.speciesId, it.isShiny, it.form) }
    }

    suspend fun load(
        context: Context,
        speciesId: Int,
        shiny: Boolean,
        form: String?,
        female: Boolean = false
    ): Drawable? = withContext(Dispatchers.IO) {
        val file = file(context, speciesId, shiny, form, female) ?: return@withContext null
        runCatching { ImageDecoder.decodeDrawable(ImageDecoder.createSource(file)) }
            .onFailure {
                Log.w(TAG, "Couldn't decode ${file.name}", it)
                file.delete()
            }
            .getOrNull()
    }

    private suspend fun file(
        context: Context,
        speciesId: Int,
        shiny: Boolean,
        form: String?,
        female: Boolean = false
    ): File? = mutex.withLock {
        val suffix = Forms.spriteSuffix(speciesId, form)
        // Females with their own look are in a folder of their own, like Pikachu's heart-shaped tail
        val names = listOfNotNull(
            "female/$speciesId".takeIf { female },
            suffix?.let { "$speciesId-$it" },
            "$speciesId"
        )
        val folder = File(context.filesDir, "garden").apply { mkdirs() }
        for (name in names) {
            val cached = File(folder, "${if (shiny) "shiny-" else ""}${name.replace('/', '-')}.gif")
            if (cached.length() > 0) return@withLock cached
            if (download("$BASE_URL/${if (shiny) "shiny/" else ""}$name.gif", cached)) return@withLock cached
        }
        null
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

/**
 * The little party icons from Pokémon Black and White, for scenes drawn like a map (the
 * Orchard), where they match the tiles. Downloaded the first time and kept on the phone.
 */
object PokemonIcons {
    private const val TAG = "PokemonIcons"
    private const val BASE_URL =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/versions/generation-v/icons"
    private const val TIMEOUT_MS = 10_000

    private val mutex = Mutex()

    suspend fun load(context: Context, speciesId: Int, form: String?): ImageBitmap? = withContext(Dispatchers.IO) {
        val file = mutex.withLock {
            val folder = File(context.filesDir, "icons").apply { mkdirs() }
            val names = listOfNotNull(Forms.spriteSuffix(speciesId, form)?.let { "$speciesId-$it" }, "$speciesId")
            names.firstNotNullOfOrNull { name ->
                File(folder, "$name.png").takeIf { it.length() > 0 || download("$BASE_URL/$name.png", it) }
            }
        } ?: return@withContext null
        runCatching { BitmapFactory.decodeFile(file.path)?.asImageBitmap() }
            .onFailure { file.delete() }
            .getOrNull()
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

/** Whether the animated sprite was looked for yet, and what was found. */
private class LoadedSprite(val drawable: Drawable?)

/**
 * A Pokémon's animated Black and White sprite, filling [modifier] with sharp pixels. Until it's
 * downloaded, or without a connection, the regular sprite stands in. Greys in black and white.
 */
@Composable
fun AnimatedPokemonSprite(
    speciesId: Int,
    shiny: Boolean,
    form: String?,
    modifier: Modifier = Modifier,
    female: Boolean = false
) {
    val context = LocalContext.current
    val color = LocalColorSprites.current
    val loaded by produceState<LoadedSprite?>(null, speciesId, shiny, form, female) {
        value = LoadedSprite(GardenSprites.load(context, speciesId, shiny, form, female))
    }
    val sprite = loaded ?: return Box(modifier)
    val drawable = sprite.drawable
    if (drawable != null) {
        AndroidView(
            factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
            update = { view ->
                drawable.colorFilter = if (color) {
                    null
                } else {
                    ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                }
                if (view.drawable !== drawable) {
                    drawable.isFilterBitmap = false
                    view.setImageDrawable(drawable)
                    (drawable as? AnimatedImageDrawable)?.start()
                }
            },
            modifier = modifier
        )
    } else {
        Image(
            bitmap = ImageBitmap.imageResource(PokemonSpriteUtils.getColorResourceId(context, speciesId, shiny, form)),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            contentScale = ContentScale.Fit,
            colorFilter = if (color) {
                null
            } else {
                ColorFilter.colorMatrix(
                    androidx.compose.ui.graphics.ColorMatrix().apply {
                        setToSaturation(0f)
                    }
                )
            },
            modifier = modifier
        )
    }
}
