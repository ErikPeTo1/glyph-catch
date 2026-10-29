package dev.equalparts.glyph_catch.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppScaffoldWithTopBar
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.ItemGlyphCircle
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.FISHING_RODS
import dev.equalparts.glyph_catch.data.MissionProgress
import dev.equalparts.glyph_catch.data.Missions
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.Stat
import dev.equalparts.glyph_catch.data.nameRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Current missions, refreshed every few seconds while the screen is open.
 */
@Composable
fun rememberMissionProgress(db: PokemonDatabase, preferencesManager: PreferencesManager, refreshKey: Int = 0) =
    produceState(initialValue = emptyList<MissionProgress>(), db, preferencesManager, refreshKey) {
        while (true) {
            value = withContext(Dispatchers.IO) {
                val hasRod = FISHING_RODS.any { (db.inventoryDao().getItem(it.ordinal)?.quantity ?: 0) > 0 }
                val claimed = preferencesManager.claimedMissions
                Missions.current(hasRod = hasRod).map { Missions.progress(it, preferencesManager.dailyStats, claimed) }
            }
            delay(15_000)
        }
    }

@Composable
fun MissionsScreen(db: PokemonDatabase, preferencesManager: PreferencesManager, onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshKey by remember { mutableIntStateOf(0) }
    val missions by rememberMissionProgress(db, preferencesManager, refreshKey)

    AppScaffoldWithTopBar(title = stringResource(R.string.missions_title), onBackClick = onBackClick) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppSizes.spacingLarge)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)
        ) {
            Text(
                text = stringResource(R.string.missions_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            missions.forEach { progress ->
                MissionCard(progress = progress) {
                    scope.launch {
                        val claimed = withContext(Dispatchers.IO) { Missions.claim(db, preferencesManager, progress) }
                        if (claimed) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.missions_claimed_toast),
                                Toast.LENGTH_SHORT
                            ).show()
                            refreshKey++
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionCard(progress: MissionProgress, onClaim: () -> Unit) {
    val mission = progress.mission
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(AppSizes.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
        ) {
            Text(
                text = stringResource(if (mission.weekly) R.string.missions_weekly else R.string.missions_daily),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = missionTitle(mission.stat, mission.target),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            LinearProgressIndicator(
                progress = { (progress.progress.toFloat() / mission.target).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
            ) {
                ItemGlyphCircle(item = mission.reward, contentDescription = stringResource(mission.reward.nameRes()))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            R.string.missions_reward,
                            mission.rewardAmount,
                            stringResource(mission.reward.nameRes()),
                            Missions.boostMinutes(mission)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = progressLabel(
                            mission.stat,
                            progress.progress.coerceAtMost(mission.target),
                            mission.target
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                when {
                    progress.claimed -> Text(
                        text = stringResource(R.string.missions_done),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    progress.canClaim -> Button(onClick = onClaim) { Text(stringResource(R.string.missions_claim)) }
                }
            }
        }
    }
}

@Composable
fun missionTitle(stat: Stat, target: Int): String = when (stat) {
    Stat.MINUTES -> pluralStringResource(R.plurals.mission_minutes, target / 60, target / 60)
    Stat.CATCHES -> pluralStringResource(R.plurals.mission_catches, target, target)
    Stat.NEW_SPECIES -> pluralStringResource(R.plurals.mission_new_species, target, target)
    Stat.ROD_CATCHES -> pluralStringResource(R.plurals.mission_rod_catches, target, target)
    Stat.LEVELS -> pluralStringResource(R.plurals.mission_levels, target, target)
    Stat.EVOLUTIONS -> pluralStringResource(R.plurals.mission_evolutions, target, target)
    Stat.ITEMS_USED -> pluralStringResource(R.plurals.mission_items, target, target)
}

@Composable
private fun progressLabel(stat: Stat, progress: Int, target: Int): String = if (stat == Stat.MINUTES) {
    "${formatDuration(progress)} / ${formatDuration(target)}"
} else {
    "$progress / $target"
}
