package dev.equalparts.glyph_catch.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppScaffoldWithTopBar
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.Stat
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.ceil
import kotlinx.coroutines.delay

private data class DayStats(val date: LocalDate, val minutes: Int, val catches: Int)

/**
 * Daily minutes with the phone locked and catches over the last week.
 */
@Composable
fun StatsScreen(db: PokemonDatabase, preferencesManager: PreferencesManager, onBackClick: () -> Unit) {
    val days by produceState(initialValue = lastWeek(preferencesManager), preferencesManager) {
        while (true) {
            value = lastWeek(preferencesManager)
            delay(30_000)
        }
    }
    val shinies by db.pokemonDao().watchShinySpeciesIds().collectAsStateWithLifecycle(emptyList())
    val pokedex by db.pokemonDao().watchPokedexProgress().collectAsStateWithLifecycle(0)
    var selected by remember { mutableIntStateOf(days.lastIndex) }

    AppScaffoldWithTopBar(title = stringResource(R.string.stats_title), onBackClick = onBackClick) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppSizes.spacingLarge)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
        ) {
            val week = days.sumOf { it.minutes }
            val best = days.maxBy { it.minutes }
            Row(horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)) {
                StatTile(stringResource(R.string.stats_today), formatDuration(days.last().minutes), Modifier.weight(1f))
                StatTile(stringResource(R.string.stats_week), formatDuration(week), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)) {
                StatTile(stringResource(R.string.stats_best_day), formatDuration(best.minutes), Modifier.weight(1f))
                StatTile(
                    stringResource(R.string.stats_catches_week),
                    days.sumOf { it.catches }.toString(),
                    Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)) {
                StatTile(stringResource(R.string.stats_pokedex), pokedex.toString(), Modifier.weight(1f))
                StatTile(stringResource(R.string.stats_shinies), shinies.size.toString(), Modifier.weight(1f))
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(AppSizes.spacingLarge),
                    verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)
                ) {
                    Text(
                        text = stringResource(R.string.stats_chart_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    WeekChart(days = days, selected = selected, onSelect = { selected = it })
                }
            }

            // Table view of the same numbers
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(AppSizes.spacingLarge),
                    verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
                ) {
                    days.asReversed().forEach { day ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = dayLabel(day.date),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatDuration(day.minutes),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = stringResource(R.string.stats_catches_short, day.catches),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun lastWeek(preferencesManager: PreferencesManager): List<DayStats> {
    val today = LocalDate.now()
    return (6 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        DayStats(
            date = date,
            minutes = preferencesManager.dailyStats.get(Stat.MINUTES, date),
            catches = preferencesManager.dailyStats.get(Stat.CATCHES, date)
        )
    }
}

@Composable
fun formatDuration(minutes: Int): String = stringResource(R.string.stats_duration, minutes / 60, minutes % 60)

@Composable
private fun dayLabel(date: LocalDate): String = if (date == LocalDate.now()) {
    stringResource(R.string.stats_today)
} else {
    date.dayOfWeek.getDisplayName(DateTextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.titlecase() }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Column(modifier = Modifier.padding(AppSizes.spacingLarge)) {
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * One bar per day in hours. Tapping a bar shows its value above it; today is selected first.
 */
@Composable
private fun WeekChart(days: List<DayStats>, selected: Int, onSelect: (Int) -> Unit) {
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryText = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val valueStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
    val dayInitials = days.map { it.date.dayOfWeek.getDisplayName(DateTextStyle.NARROW, Locale.getDefault()) }
    val values = days.map { formatDuration(it.minutes) }
    val maxHours = ceil(days.maxOf { it.minutes } / 60.0).toInt().coerceAtLeast(1)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .pointerInput(days.size) {
                detectTapGestures { offset ->
                    val slot = size.width / days.size
                    onSelect((offset.x / slot).toInt().coerceIn(0, days.lastIndex))
                }
            }
    ) {
        val bottomLabels = 20.dp.toPx()
        val topLabel = 20.dp.toPx()
        val chartHeight = size.height - bottomLabels - topLabel
        val baseline = topLabel + chartHeight
        val slot = size.width / days.size
        val barWidth = slot * 0.55f

        // Recessive grid: a line at the top value and the baseline
        listOf(maxHours, 0).forEach { hours ->
            val y = baseline - chartHeight * hours / maxHours
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        drawText(
            textMeasurer,
            "${maxHours}h",
            Offset(0f, baseline - chartHeight - 16.dp.toPx()),
            style = labelStyle.copy(color = mutedText)
        )

        days.forEachIndexed { index, day ->
            val left = slot * index + (slot - barWidth) / 2
            val height = chartHeight * (day.minutes / 60f) / maxHours
            if (height > 0f) {
                drawTopRoundedBar(
                    left,
                    baseline - height,
                    barWidth,
                    height,
                    4.dp.toPx(),
                    barColor,
                    selected == index || selected !in days.indices
                )
            }
            val initial = textMeasurer.measure(dayInitials[index], labelStyle)
            drawText(
                initial,
                color = if (index == selected) primaryText else mutedText,
                topLeft = Offset(slot * index + (slot - initial.size.width) / 2, baseline + 4.dp.toPx())
            )
            if (index == selected) {
                val value = textMeasurer.measure(values[index], valueStyle)
                val x = (slot * index + (slot - value.size.width) / 2).coerceIn(0f, size.width - value.size.width)
                drawText(
                    value,
                    color = primaryText,
                    topLeft = Offset(
                        x,
                        baseline - height - value.size.height - 2.dp.toPx()
                    )
                )
            }
        }
    }
}

private fun DrawScope.drawTopRoundedBar(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    radius: Float,
    color: Color,
    highlighted: Boolean
) {
    val r = radius.coerceAtMost(height).coerceAtMost(width / 2)
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = left,
                top = top,
                right = left + width,
                bottom = top + height,
                topLeftCornerRadius = CornerRadius(r, r),
                topRightCornerRadius = CornerRadius(r, r)
            )
        )
    }
    drawPath(path, color.copy(alpha = if (highlighted) 1f else 0.55f))
}
