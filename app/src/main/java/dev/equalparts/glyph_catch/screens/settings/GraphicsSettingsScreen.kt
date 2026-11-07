package dev.equalparts.glyph_catch.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.widget.BoxWallpaper
import dev.equalparts.glyph_catch.widget.WidgetBackground
import kotlin.math.roundToInt

@Composable
fun GraphicsSettingsScreen(modifier: Modifier) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scrollState = rememberScrollState()

    val clockEnabled by produceState(initialValue = prefs.glyphToyClockEnabled, key1 = prefs) {
        prefs.watchGlyphToyClockEnabled().collect { value = it }
    }
    val lowerBrightness by produceState(initialValue = prefs.glyphToyLowerBrightness, key1 = prefs) {
        prefs.watchGlyphToyLowerBrightness().collect { value = it }
    }

    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
    ) {
        VisualToggleCard(
            title = stringResource(R.string.settings_graphics_clock_title),
            subtitle = stringResource(R.string.settings_graphics_clock_subtitle),
            checked = clockEnabled,
            onCheckedChange = { enabled -> prefs.glyphToyClockEnabled = enabled }
        )

        VisualToggleCard(
            title = stringResource(R.string.settings_graphics_brightness_title),
            subtitle = stringResource(R.string.settings_graphics_brightness_subtitle),
            checked = lowerBrightness,
            onCheckedChange = { enabled -> prefs.glyphToyLowerBrightness = enabled }
        )

        Text(
            text = stringResource(R.string.settings_graphics_note_cycle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * How Pokémon look in the app and on the home-screen widgets.
 */
@Composable
fun AppearanceSettingsScreen(modifier: Modifier) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val colorSprites by produceState(initialValue = prefs.colorSprites, key1 = prefs) {
        prefs.watchColorSprites().collect { value = it }
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
    ) {
        VisualToggleCard(
            title = stringResource(R.string.settings_graphics_color_title),
            subtitle = stringResource(R.string.settings_graphics_color_subtitle),
            checked = colorSprites,
            onCheckedChange = { enabled -> prefs.colorSprites = enabled }
        )

        WidgetBackgroundCard(prefs)

        Text(
            text = stringResource(R.string.widget_howto),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VisualToggleCard(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSizes.spacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(AppSizes.spacingTiny))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

/**
 * Each widget has its own background; the waiting Pokémon can also pick one on their own.
 */
@Composable
private fun WidgetBackgroundCard(prefs: PreferencesManager) {
    var editingQueue by remember { mutableStateOf(false) }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AppSizes.spacingLarge)) {
            Text(
                text = stringResource(R.string.widget_bg_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(AppSizes.spacingTiny))
            Text(
                text = stringResource(R.string.widget_bg_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(AppSizes.spacingMedium))
            Row(horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)) {
                FilterChip(
                    selected = !editingQueue,
                    onClick = { editingQueue = false },
                    label = { Text(stringResource(R.string.widget_team_name)) }
                )
                FilterChip(
                    selected = editingQueue,
                    onClick = { editingQueue = true },
                    label = { Text(stringResource(R.string.widget_queue_name)) }
                )
            }
            key(editingQueue) {
                if (editingQueue) {
                    BackgroundEditor(prefs.queueWidgetBackground, allowAuto = true) { prefs.queueWidgetBackground = it }
                } else {
                    BackgroundEditor(prefs.widgetBackground, allowAuto = false) { prefs.widgetBackground = it }
                }
            }
        }
    }
}

@Composable
private fun BackgroundEditor(saved: String, allowAuto: Boolean, onSave: (String) -> Unit) {
    var selected by remember { mutableStateOf(WidgetBackground.from(saved)) }
    var color by remember { mutableIntStateOf((selected as? WidgetBackground.Solid)?.argb ?: DEFAULT_COLOR) }
    fun choose(background: WidgetBackground) {
        selected = background
        onSave(background.encode())
    }

    Column {
        Spacer(modifier = Modifier.height(AppSizes.spacingLarge))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Gray)
                    .background(Color(color))
                    .border(
                        width = if (selected is WidgetBackground.Solid) 3.dp else 1.dp,
                        color = if (selected is WidgetBackground.Solid) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { choose(WidgetBackground.Solid(color)) }
            )
            Spacer(modifier = Modifier.width(AppSizes.spacingLarge))
            Column(modifier = Modifier.weight(1f)) {
                COLOR_CHANNELS.forEach { (label, shift) ->
                    ColorSlider(
                        label = stringResource(label),
                        value = color shr shift and 0xFF,
                        onChange = { value -> color = color and (0xFF shl shift).inv() or (value shl shift) },
                        onDone = { choose(WidgetBackground.Solid(color)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppSizes.spacingLarge))
        Text(
            text = stringResource(R.string.widget_bg_boxes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(AppSizes.spacingSmall))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
        ) {
            if (allowAuto) {
                val isSelected = selected == WidgetBackground.Auto
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(76.dp)
                        .clickable { choose(WidgetBackground.Auto) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(width = 76.dp, height = 57.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        // A peek at four of the boxes it can pick
                        Column {
                            AUTO_PREVIEW.chunked(2).forEach { pair ->
                                Row {
                                    pair.forEach { wallpaper ->
                                        Image(
                                            bitmap = ImageBitmap.imageResource(wallpaper.drawable),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            filterQuality = FilterQuality.None,
                                            modifier = Modifier.size(width = 38.dp, height = 28.5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        text = stringResource(R.string.widget_bg_auto),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            BoxWallpaper.entries.forEach { wallpaper ->
                val isSelected = (selected as? WidgetBackground.Wallpaper)?.wallpaper == wallpaper
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(76.dp)
                        .clickable { choose(WidgetBackground.Wallpaper(wallpaper)) }
                ) {
                    Image(
                        bitmap = ImageBitmap.imageResource(wallpaper.drawable),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.None,
                        modifier = Modifier
                            .size(width = 76.dp, height = 57.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                    Text(
                        text = stringResource(wallpaper.nameRes),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
        if (allowAuto) {
            Spacer(modifier = Modifier.height(AppSizes.spacingSmall))
            Text(
                text = stringResource(R.string.widget_bg_auto_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ColorSlider(label: String, value: Int, onChange: (Int) -> Unit, onDone: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(76.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            onValueChangeFinished = onDone,
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Red, green, blue and opacity, with where each one sits in an ARGB color. */
private val COLOR_CHANNELS = listOf(
    R.string.widget_bg_red to 16,
    R.string.widget_bg_green to 8,
    R.string.widget_bg_blue to 0,
    R.string.widget_bg_opacity to 24
)

private val AUTO_PREVIEW = listOf(BoxWallpaper.FOREST, BoxWallpaper.VOLCANO, BoxWallpaper.SEAFLOOR, BoxWallpaper.SKY)

private const val DEFAULT_COLOR = 0xFF000000.toInt()
