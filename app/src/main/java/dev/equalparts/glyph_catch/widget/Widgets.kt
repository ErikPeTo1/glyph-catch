package dev.equalparts.glyph_catch.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.scale
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.equalparts.glyph_catch.MainActivity
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.gameplay.PersistentSpawn
import dev.equalparts.glyph_catch.gameplay.PokemonGlyphToyService
import dev.equalparts.glyph_catch.util.PokemonSpriteUtils
import kotlin.math.ceil
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/** A sprite ready to draw: pixel art scaled up without blurring. */
private class WidgetSprite(val bitmap: Bitmap)

private fun loadSprite(context: Context, speciesId: Int, shiny: Boolean, form: String?, color: Boolean): WidgetSprite? {
    val resource = if (color) {
        PokemonSpriteUtils.getColorResourceId(context, speciesId, shiny, form)
    } else {
        PokemonSpriteUtils.getMatrixResourceId(context, speciesId, form)
    }
    if (resource == 0) return null
    val options = BitmapFactory.Options().apply { inScaled = false }
    val source = BitmapFactory.decodeResource(context.resources, resource, options) ?: return null
    // Keep the pixels crisp: scale small sprites up by whole steps, without filtering
    val factor = (SPRITE_PIXELS / source.width).coerceAtLeast(1)
    val bitmap = if (factor > 1) source.scale(source.width * factor, source.height * factor, false) else source
    return WidgetSprite(bitmap)
}

private const val SPRITE_PIXELS = 176

/**
 * Refreshes both widgets, e.g. after the team, the queue or a setting changes.
 */
object WidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Bumped on every change, so widgets that are already showing reload their data. */
    val changes = MutableStateFlow(0)

    fun refresh(context: Context) {
        val app = context.applicationContext
        changes.update { it + 1 }
        scope.launch {
            runCatching { TeamWidget().updateAll(app) }
            runCatching { QueueWidget().updateAll(app) }
        }
    }
}

/**
 * Lays the slots out in the grid (6×1, 3×2, 2×3…) that gives the biggest sprites for the
 * widget's current size.
 */
@Composable
private fun SpriteGrid(
    sprites: List<WidgetSprite?>,
    slots: Int,
    background: WidgetBackground,
    inset: Float,
    reservedHeight: Float = 0f,
    showEmptySlots: Boolean = false
) {
    val size = LocalSize.current
    val width = size.width.value - inset * 2
    val height = size.height.value - inset * 2 - reservedHeight
    val (columns, rows) = (1..slots)
        .map { it to ceil(slots / it.toDouble()).toInt() }
        .maxBy { (c, r) -> min(width / c, height / r) }
    val cell = min(width / columns, height / rows).coerceAtLeast(MIN_CELL).dp
    val emptySlot = if (background.dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        for (row in 0 until rows) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                for (column in 0 until columns) {
                    val index = row * columns + column
                    if (index >= slots) continue
                    val sprite = sprites.getOrNull(index)
                    Box(modifier = GlanceModifier.size(cell).padding(2.dp), contentAlignment = Alignment.Center) {
                        when {
                            sprite != null -> Image(
                                provider = ImageProvider(sprite.bitmap),
                                contentDescription = null,
                                modifier = GlanceModifier.fillMaxSize()
                            )

                            showEmptySlots -> Box(
                                modifier = GlanceModifier.fillMaxSize().background(emptySlot).cornerRadius(12.dp)
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

private const val FRAME_PADDING = 8f
private const val MIN_CELL = 16f

/**
 * The widget's background, either a color or a PC box wallpaper drawn to the widget's size.
 * [content] gets how far it must stay from the edges.
 */
@Composable
private fun WidgetFrame(background: WidgetBackground, content: @Composable (inset: Float) -> Unit) {
    val context = LocalContext.current
    val size = LocalSize.current
    val wallpaper = (background as? WidgetBackground.Wallpaper)?.let {
        remember(it, size) { WallpaperRenderer.render(context, it.wallpaper, size.width.value, size.height.value) }
    }
    val inset = wallpaper?.let { it.frameDp + 2f } ?: FRAME_PADDING
    val base = GlanceModifier.fillMaxSize()
    // Box wallpapers bring their own pixel-art corners; only plain colors get rounded
    val filled = when {
        wallpaper != null -> base.background(ImageProvider(wallpaper.bitmap), ContentScale.FillBounds)
        background is WidgetBackground.Solid -> base.background(Color(background.argb)).cornerRadius(24.dp)
        else -> base
    }
    Box(
        modifier = filled
            .padding(inset.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        content(inset)
    }
}

private fun textColor(background: WidgetBackground) = ColorProvider(if (background.dark) Color.White else Color.Black)

/** What a team member looks like; the widget only redraws when one of these changes. */
private data class TeamLook(val speciesId: Int, val shiny: Boolean, val form: String?)

private data class TeamState(val background: WidgetBackground, val sprites: List<WidgetSprite?>)

/**
 * Your team: up to six favorite Pokémon, picked from their details.
 */
class TeamWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val preferences = PreferencesManager(context)
        val dao = PokemonDatabase.getInstance(context).pokemonDao()
        // Levels change every minute while training, so only watch what the widget shows
        val looks = combine(WidgetUpdater.changes, dao.watchAllCaught()) { _, all ->
            val byId = all.associateBy { it.id }
            Triple(
                WidgetBackground.from(preferences.widgetBackground),
                preferences.colorSprites,
                preferences.team.mapNotNull { byId[it] }.map { TeamLook(it.speciesId, it.isShiny, it.form) }
            )
        }.distinctUntilChanged()
        val states = looks.map { (background, color, team) ->
            TeamState(background, team.map { loadSprite(context, it.speciesId, it.shiny, it.form, color) })
        }
        val initial = states.first()

        provideContent {
            val state by states.collectAsState(initial)
            WidgetFrame(state.background) { inset ->
                if (state.sprites.isEmpty()) {
                    Text(
                        text = LocalContext.current.getString(R.string.widget_team_empty),
                        style = TextStyle(
                            color = textColor(state.background),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                } else {
                    SpriteGrid(
                        state.sprites,
                        PreferencesManager.TEAM_SIZE,
                        state.background,
                        inset,
                        showEmptySlots = true
                    )
                }
            }
        }
    }
}

class TeamWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TeamWidget()
}

private data class QueueState(val background: WidgetBackground, val canCatch: Boolean, val sprites: List<WidgetSprite?>)

/**
 * The Pokémon waiting on the Glyph Matrix. With the hidden "play while in use" setting, they
 * can be caught from here.
 */
class QueueWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val preferences = PreferencesManager(context)
        fun load(): QueueState {
            val color = preferences.colorSprites
            val queue = PokemonGlyphToyService.readSavedQueue(context)
            val background = WidgetBackground.from(preferences.queueWidgetBackground)
            return QueueState(
                background = (background as? WidgetBackground.Auto)?.resolve(queue.firstOrNull()) ?: background,
                canCatch = preferences.playWhileInUse && preferences.catchAllButton,
                sprites = queue.map { loadSprite(context, it.pokemonId, it.isShiny, it.form, color) }
            )
        }
        val initial = load()
        val states = WidgetUpdater.changes.map { load() }

        provideContent {
            val state by states.collectAsState(initial)
            val sprites = state.sprites
            WidgetFrame(state.background) { inset ->
                Column(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // With nobody waiting, the widget is just its background
                    if (sprites.isNotEmpty()) {
                        Text(
                            text = LocalContext.current.getString(R.string.widget_queue_count, sprites.size),
                            style = TextStyle(
                                color = textColor(state.background),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        SpriteGrid(
                            sprites = sprites,
                            slots = sprites.size,
                            background = state.background,
                            inset = inset,
                            reservedHeight = if (state.canCatch) QUEUE_TEXT_AND_BUTTON else QUEUE_TEXT
                        )
                        if (state.canCatch) {
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Button(
                                text = LocalContext.current.getString(R.string.widget_catch_all),
                                onClick = actionRunCallback<CatchAllAction>()
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val QUEUE_TEXT = 24f
private const val QUEUE_TEXT_AND_BUTTON = 76f

class QueueWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QueueWidget()
}

class CatchAllAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val preferences = PreferencesManager(context)
        if (preferences.playWhileInUse && preferences.catchAllButton) {
            PokemonGlyphToyService.catchAllFromWidget()
        }
    }
}

internal val widgetJson = Json { ignoreUnknownKeys = true }

internal fun decodeQueue(json: String?): List<PersistentSpawn> =
    json?.let { runCatching { widgetJson.decodeFromString<List<PersistentSpawn>>(it) }.getOrNull() }.orEmpty()
