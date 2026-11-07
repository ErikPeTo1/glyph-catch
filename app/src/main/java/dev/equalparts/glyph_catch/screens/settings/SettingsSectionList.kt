package dev.equalparts.glyph_catch.screens.settings

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.PreferencesManager
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SettingsSectionList(modifier: Modifier, onSectionSelected: (SettingsDestination) -> Unit) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val preferences = remember { PreferencesManager(context) }

    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
    ) {
        SettingsSectionCard(
            icon = R.drawable.ic_settings_palette,
            title = stringResource(R.string.settings_appearance_title),
            description = stringResource(R.string.settings_section_appearance_description),
            onClick = { onSectionSelected(SettingsDestination.Appearance) }
        )

        SettingsSectionCard(
            icon = R.drawable.ic_settings_lightbulb,
            title = stringResource(R.string.settings_graphics_title),
            description = stringResource(R.string.settings_section_toy_description),
            onClick = { onSectionSelected(SettingsDestination.Graphics) }
        )

        SettingsSectionCard(
            icon = R.drawable.ic_settings_wb_sunny,
            title = stringResource(R.string.settings_weather_title),
            description = weatherSummary(preferences),
            onClick = { onSectionSelected(SettingsDestination.Weather) }
        )

        SettingsSectionCard(
            icon = R.drawable.ic_settings_bedtime,
            title = stringResource(R.string.settings_sleep_title),
            description = stringResource(R.string.settings_summary_sleep, bedtimeText(preferences.bedtimeMinutes)),
            onClick = { onSectionSelected(SettingsDestination.Sleep) }
        )

        SettingsSectionCard(
            icon = R.drawable.ic_settings_backup,
            title = stringResource(R.string.settings_backup_title),
            description = stringResource(R.string.settings_section_backup_description),
            onClick = { onSectionSelected(SettingsDestination.Backup) }
        )

        SettingsSectionCard(
            icon = R.drawable.ic_settings_bug_report,
            title = stringResource(R.string.settings_debug_title),
            description = stringResource(R.string.settings_section_report_description),
            onClick = { onSectionSelected(SettingsDestination.Debug) }
        )

        VersionAndHiddenSettings()
    }
}

/**
 * The version number. Tapping it seven times reveals a hidden setting.
 */
@Composable
private fun VersionAndHiddenSettings() {
    val context = LocalContext.current
    val preferences = remember { PreferencesManager(context) }
    var unlocked by remember { mutableStateOf(preferences.hiddenSettingsUnlocked) }
    var taps by remember { mutableIntStateOf(0) }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    if (unlocked) {
        var playWhileInUse by remember { mutableStateOf(preferences.playWhileInUse) }
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSizes.spacingLarge),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.hidden_play_in_use_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(AppSizes.spacingTiny))
                    Text(
                        text = stringResource(R.string.hidden_play_in_use_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = playWhileInUse,
                    onCheckedChange = {
                        playWhileInUse = it
                        preferences.playWhileInUse = it
                    }
                )
            }
        }
    }

    Text(
        text = "Glyph Catch $version",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (unlocked) return@clickable
                taps++
                if (taps >= HIDDEN_SETTINGS_TAPS) {
                    unlocked = true
                    preferences.hiddenSettingsUnlocked = true
                    Toast.makeText(context, R.string.hidden_settings_unlocked, Toast.LENGTH_SHORT).show()
                }
            }
            .padding(vertical = AppSizes.spacingMedium)
    )
}

private const val HIDDEN_SETTINGS_TAPS = 7

@Composable
private fun weatherSummary(preferences: PreferencesManager): String {
    val city = preferences.weatherLocationName
    return if (preferences.useOpenWeatherMap && !city.isNullOrBlank()) {
        stringResource(R.string.settings_summary_weather_live, city)
    } else {
        stringResource(R.string.settings_weather_provider_random)
    }
}

private fun bedtimeText(minutes: Int): String =
    LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

@Composable
private fun SettingsSectionCard(@DrawableRes icon: Int, title: String, description: String, onClick: () -> Unit) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSizes.spacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(AppSizes.spacingLarge))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(AppSizes.spacingTiny))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
