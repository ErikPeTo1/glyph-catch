package dev.equalparts.glyph_catch.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppEmptyState
import dev.equalparts.glyph_catch.AppScaffoldWithTopBar
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.PokemonExpChip
import dev.equalparts.glyph_catch.PokemonLevelChip
import dev.equalparts.glyph_catch.PokemonSpriteCircle
import dev.equalparts.glyph_catch.PokemonTypeChips
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.components.formLabel
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.Forms
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDao
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PokemonSpecies
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.Stat
import dev.equalparts.glyph_catch.data.displayName
import dev.equalparts.glyph_catch.data.nameRes
import dev.equalparts.glyph_catch.util.canHoldEverstone
import dev.equalparts.glyph_catch.util.levelChoiceTargets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private data class CaughtPokemonDetailInfo(
    val speciesId: Int?,
    val level: Int,
    val experience: Int,
    val appearedLabel: String?,
    val caughtLabel: String,
    val screenOffLabel: String,
    val spawnPoolName: String?,
    val isSpecialSpawn: Boolean,
    val isConditionalSpawn: Boolean
)

@Composable
fun CaughtPokemonDetailScreen(
    db: PokemonDatabase,
    preferencesManager: PreferencesManager,
    pokemonId: String,
    onNavigateUp: () -> Unit
) {
    val pokemonDao = remember(db) { db.pokemonDao() }
    val caughtPokemon by pokemonDao.watchCaughtPokemon(pokemonId).collectAsStateWithLifecycle(null)

    AppScaffoldWithTopBar(
        title = stringResource(R.string.caught_detail_title),
        onBackClick = onNavigateUp
    ) { paddingValues ->
        CaughtPokemonDetailContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppSizes.spacingLarge),
            pokemon = caughtPokemon,
            pokemonDao = pokemonDao,
            preferencesManager = preferencesManager
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CaughtPokemonDetailContent(
    modifier: Modifier,
    pokemon: CaughtPokemon?,
    pokemonDao: PokemonDao,
    preferencesManager: PreferencesManager
) {
    val scope = rememberCoroutineScope()
    var isStartingTraining by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
    ) {
        if (pokemon == null) {
            AppEmptyState(
                primaryText = stringResource(R.string.caught_detail_empty_title),
                secondaryText = stringResource(R.string.caught_detail_empty_subtitle)
            )
        } else {
            CaughtPokemonSummaryCard(pokemon = pokemon, pokemonDao = pokemonDao)

            val choices = remember(pokemon) { levelChoiceTargets(pokemon) }
            if (choices.isNotEmpty()) {
                EvolutionChoiceCard(choices = choices) { target ->
                    scope.launch {
                        pokemonDao.evolveCaught(pokemon, target.id, pokemon.level, pokemon.exp)
                        preferencesManager.dailyStats.add(Stat.EVOLUTIONS)
                        preferencesManager.enqueueEvolutionNotification(pokemon.speciesId, target.id)
                    }
                }
            }

            if (canHoldEverstone(pokemon)) {
                EverstoneCard(locked = pokemon.evolutionLocked) { locked ->
                    scope.launch { pokemonDao.updateEvolutionLocked(pokemon.id, locked) }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        isStartingTraining = true
                        try {
                            pokemonDao.setActiveTrainingPartner(pokemon.id)
                            preferencesManager.markTrainingPartner(pokemon.id)
                        } finally {
                            isStartingTraining = false
                        }
                    }
                },
                enabled = !pokemon.isTraining && !isStartingTraining,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (pokemon.isTraining) {
                        stringResource(R.string.caught_detail_is_training)
                    } else {
                        stringResource(R.string.caught_detail_start_training)
                    }
                )
            }
            if (pokemon.isTraining) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            pokemonDao.clearTrainingPartner()
                            preferencesManager.clearTrainingPartner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.caught_detail_stop_training))
                }
            }
        }
    }
}

@Composable
private fun EvolutionChoiceCard(choices: List<PokemonSpecies>, onChoose: (PokemonSpecies) -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(AppSizes.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)
        ) {
            Text(
                text = stringResource(R.string.evolution_ready_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.evolution_choose_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            choices.forEach { target ->
                Button(onClick = { onChoose(target) }, modifier = Modifier.fillMaxWidth()) {
                    Text(target.displayName)
                }
            }
        }
    }
}

@Composable
private fun EverstoneCard(locked: Boolean, onToggle: (Boolean) -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSizes.spacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.everstone_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.everstone_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = locked, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun CaughtPokemonSummaryCard(pokemon: CaughtPokemon, pokemonDao: PokemonDao) {
    val species = Pokemon[pokemon.speciesId]
    val formsFound by remember(pokemon.speciesId) { pokemonDao.watchFormsFound(pokemon.speciesId) }
        .collectAsStateWithLifecycle(emptyList())
    val typeLabels = buildList {
        species?.let {
            add(stringResource(it.type1.nameRes()))
            it.type2?.let { secondary -> add(stringResource(secondary.nameRes())) }
        }
    }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val appearedAtFormatted = remember(pokemon.spawnedAt) {
        pokemon.spawnedAt.takeIf { it > 0L }?.let { Date(it) }?.let(dateFormat::format)
    }
    val caughtAtFormatted = remember(pokemon.caughtAt) { dateFormat.format(Date(pokemon.caughtAt)) }
    val screenOffText = pluralStringResource(
        R.plurals.caught_detail_screen_off_minutes,
        pokemon.screenOffDurationMinutes,
        pokemon.screenOffDurationMinutes
    )

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSizes.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CaughtPokemonOverview(
                pokemon = pokemon,
                speciesName = species?.displayName,
                typeLabels = typeLabels
            )

            val info = CaughtPokemonDetailInfo(
                speciesId = species?.id,
                level = pokemon.level,
                experience = pokemon.exp,
                appearedLabel = appearedAtFormatted,
                caughtLabel = caughtAtFormatted,
                screenOffLabel = screenOffText,
                spawnPoolName = pokemon.spawnPoolName,
                isSpecialSpawn = pokemon.isSpecialSpawn,
                isConditionalSpawn = pokemon.isConditionalSpawn
            )

            CaughtPokemonInfoList(info = info)

            if (Forms.hasForms(pokemon.speciesId)) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)) {
                    pokemon.form?.let { InfoRow(label = stringResource(R.string.form_label), value = formLabel(it)) }
                    InfoRow(
                        label = stringResource(R.string.forms_found_label),
                        value = "${formsFound.size}/${Forms.formsOf(pokemon.speciesId).size}"
                    )
                }
            }
        }
    }
}

@Composable
private fun CaughtPokemonOverview(pokemon: CaughtPokemon, speciesName: String?, typeLabels: List<String>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)
    ) {
        PokemonSpriteCircle(
            modifier = Modifier.size(AppSizes.homeTileHeight),
            pokemonId = pokemon.speciesId,
            pokemonName = speciesName ?: stringResource(R.string.common_unknown),
            shiny = pokemon.isShiny,
            form = pokemon.form
        )

        Text(
            text = pokemon.nickname
                ?: speciesName
                ?: stringResource(R.string.common_unknown),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingTiny)
        ) {
            PokemonLevelChip(level = pokemon.level)
            PokemonExpChip(level = pokemon.level, exp = pokemon.exp)
        }

        if (typeLabels.isNotEmpty()) {
            PokemonTypeChips(
                types = typeLabels,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun CaughtPokemonInfoList(info: CaughtPokemonDetailInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)) {
        InfoRow(
            label = stringResource(R.string.caught_detail_info_pokedex),
            value = info.speciesId?.let { stringResource(R.string.pokedex_entry_number, it) }
                ?: stringResource(R.string.common_unknown)
        )
        InfoRow(
            label = stringResource(R.string.caught_detail_info_appeared_on),
            value = info.appearedLabel ?: stringResource(R.string.common_unknown)
        )
        InfoRow(
            label = stringResource(R.string.caught_detail_info_caught_on),
            value = info.caughtLabel
        )
        InfoRow(
            label = stringResource(R.string.caught_detail_info_screen_off_time),
            value = info.screenOffLabel
        )

        when {
            info.isSpecialSpawn -> InfoRow(
                label = stringResource(R.string.caught_detail_info_spawn_pool),
                value = stringResource(R.string.caught_detail_encounter_special)
            )

            info.isConditionalSpawn -> InfoRow(
                label = stringResource(R.string.caught_detail_info_spawn_pool),
                value = stringResource(R.string.caught_detail_encounter_event)
            )

            else -> info.spawnPoolName?.let { pool ->
                InfoRow(
                    label = stringResource(R.string.caught_detail_info_spawn_pool),
                    value = poolLabel(pool)
                )
            }
        }
    }
}

@Composable
private fun poolLabel(pool: String): String = when (pool) {
    "Common" -> stringResource(R.string.pool_common)
    "Uncommon" -> stringResource(R.string.pool_uncommon)
    "Rare" -> stringResource(R.string.pool_rare)
    "Very Rare" -> stringResource(R.string.pool_very_rare)
    "Old Rod" -> stringResource(R.string.item_old_rod_name)
    "Good Rod" -> stringResource(R.string.item_good_rod_name)
    "Super Rod" -> stringResource(R.string.item_super_rod_name)
    else -> stringResource(R.string.caught_detail_encounter_event)
}
