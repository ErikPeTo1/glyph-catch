package dev.equalparts.glyph_catch.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.equalparts.glyph_catch.AppBadge
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppEmptyState
import dev.equalparts.glyph_catch.AppScaffoldWithTopBar
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.Features
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.components.formLabel
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.Forms
import dev.equalparts.glyph_catch.data.GardenStore
import dev.equalparts.glyph_catch.data.GardenZone
import dev.equalparts.glyph_catch.data.LEGENDARY_SPECIES
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDao
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PokemonSpecies
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.SHEDINJA_POOL
import dev.equalparts.glyph_catch.data.Stat
import dev.equalparts.glyph_catch.data.displayName
import dev.equalparts.glyph_catch.data.releasePokemon
import dev.equalparts.glyph_catch.screens.garden.FriendshipEvolutionHint
import dev.equalparts.glyph_catch.screens.garden.LivesInRow
import dev.equalparts.glyph_catch.screens.garden.friendshipEvolutions
import dev.equalparts.glyph_catch.util.EventHelpers
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
    // Swipe sideways through the list it was opened from: the PC as filtered, the Garden...
    val ids = remember(pokemonId) { PokemonBrowse.ids.takeIf { pokemonId in it } ?: listOf(pokemonId) }
    val pager = rememberPagerState(initialPage = ids.indexOf(pokemonId).coerceAtLeast(0)) { ids.size }
    val scope = rememberCoroutineScope()

    AppScaffoldWithTopBar(
        title = if (ids.size > 1) {
            stringResource(R.string.caught_detail_position, pager.currentPage + 1, ids.size)
        } else {
            stringResource(R.string.caught_detail_title)
        },
        onBackClick = onNavigateUp
    ) { paddingValues ->
        HorizontalPager(
            state = pager,
            key = { ids[it] },
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            val caughtPokemon by pokemonDao.watchCaughtPokemon(ids[page]).collectAsStateWithLifecycle(null)
            CaughtPokemonDetailContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppSizes.spacingLarge),
                pokemon = caughtPokemon,
                pokemonDao = pokemonDao,
                preferencesManager = preferencesManager,
                onRelease = { pokemon ->
                    scope.launch {
                        releasePokemon(db, preferencesManager, listOf(pokemon.id))
                        onNavigateUp()
                    }
                }
            )
        }
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
    preferencesManager: PreferencesManager,
    onRelease: (CaughtPokemon) -> Unit
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
            PokemonHero(
                pokemon = pokemon,
                onToggleFavorite = { scope.launch { pokemonDao.updateFavorite(pokemon.id, !pokemon.isFavorite) } },
                onRename = { nickname -> scope.launch { pokemonDao.updateNickname(pokemon.id, nickname) } }
            )

            // Training, team and the Glyph Matrix: filled when it's on
            val context = LocalContext.current
            val garden = remember { GardenStore.get(context) }
            var team by remember { mutableStateOf(preferencesManager.team) }
            val inTeam = pokemon.id in team
            var companionId by remember { mutableStateOf(preferencesManager.companionId) }
            var idleDisplay by remember { mutableStateOf(preferencesManager.glyphIdleDisplay) }
            // Without a companion picked, the matrix shows the training partner
            val isCompanion = companionId == pokemon.id ||
                (companionId == null && pokemon.isTraining && idleDisplay == PreferencesManager.IDLE_COMPANION)
            Row(horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)) {
                DetailAction(
                    label = stringResource(
                        if (pokemon.isTraining) R.string.detail_action_training else R.string.detail_action_train
                    ),
                    on = pokemon.isTraining,
                    enabled = !isStartingTraining,
                    modifier = Modifier.weight(1f)
                ) {
                    scope.launch {
                        if (pokemon.isTraining) {
                            pokemonDao.clearTrainingPartner()
                            preferencesManager.clearTrainingPartner()
                        } else {
                            isStartingTraining = true
                            try {
                                pokemonDao.setActiveTrainingPartner(pokemon.id)
                                preferencesManager.markTrainingPartner(pokemon.id)
                            } finally {
                                isStartingTraining = false
                            }
                        }
                    }
                }
                DetailAction(
                    label = stringResource(R.string.detail_action_team),
                    on = inTeam,
                    enabled = inTeam || team.size < PreferencesManager.TEAM_SIZE,
                    modifier = Modifier.weight(1f)
                ) {
                    preferencesManager.team = if (inTeam) team - pokemon.id else team + pokemon.id
                    team = preferencesManager.team
                    // The team lives in the Meadow, so joining it means leaving any other zone
                    if (!inTeam) garden.update { it.leaving(pokemon.id) }
                }
                DetailAction(
                    label = stringResource(R.string.detail_action_glyph),
                    on = isCompanion,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isCompanion) {
                        // Back to what it showed before, unless that would still be this Pokémon
                        preferencesManager.companionId = null
                        val before = preferencesManager.idleBeforeCompanion ?: idleDisplay
                        preferencesManager.idleBeforeCompanion = null
                        preferencesManager.glyphIdleDisplay =
                            if (before == PreferencesManager.IDLE_COMPANION && pokemon.isTraining) {
                                PreferencesManager.IDLE_CLOCK
                            } else {
                                before
                            }
                    } else {
                        // Choosing one means you want to see it
                        if (idleDisplay != PreferencesManager.IDLE_COMPANION) {
                            preferencesManager.idleBeforeCompanion = idleDisplay
                        }
                        preferencesManager.companionId = pokemon.id
                        preferencesManager.glyphIdleDisplay = PreferencesManager.IDLE_COMPANION
                    }
                    companionId = preferencesManager.companionId
                    idleDisplay = preferencesManager.glyphIdleDisplay
                }
            }

            if (Features.TOWN) {
                LivesInRow(
                    pokemon = pokemon,
                    inTeam = inTeam,
                    teamFull = team.size >= PreferencesManager.TEAM_SIZE,
                    onMove = { home ->
                        garden.update { state ->
                            when (home) {
                                null, GardenZone.MEADOW -> state.leaving(pokemon.id)
                                else -> state.moving(pokemon.id, home) ?: state
                            }
                        }
                        val joins = home == GardenZone.MEADOW
                        preferencesManager.team = if (joins) (team - pokemon.id) + pokemon.id else team - pokemon.id
                        team = preferencesManager.team
                    }
                )
            }

            ShinyChainCard(pokemon = pokemon)

            val choices = remember(pokemon) { levelChoiceTargets(pokemon) }
            if (choices.isNotEmpty()) {
                EvolutionChoiceCard(choices = choices) { target ->
                    scope.launch {
                        pokemonDao.evolveCaught(pokemon, target.id, pokemon.level, pokemon.exp)
                        preferencesManager.dailyStats.add(Stat.EVOLUTIONS)
                        preferencesManager.enqueueEvolutionNotification(pokemon.speciesId, target.id, pokemon.isShiny)
                    }
                }
            }

            val everstone = canHoldEverstone(pokemon)
            if (everstone || friendshipEvolutions(pokemon).isNotEmpty()) {
                DetailSection(title = stringResource(R.string.detail_section_evolution)) {
                    FriendshipEvolutionHint(pokemon = pokemon)
                    if (everstone) {
                        EverstoneRow(locked = pokemon.evolutionLocked) { locked ->
                            scope.launch { pokemonDao.updateEvolutionLocked(pokemon.id, locked) }
                        }
                    }
                }
            }

            DetailSection(title = stringResource(R.string.detail_section_capture)) {
                CaptureDetails(pokemon = pokemon, pokemonDao = pokemonDao)
            }

            ReleaseButton(pokemon = pokemon, onRelease = { onRelease(pokemon) })
        }
    }
}

/**
 * Letting it go from its details, asked twice since it can't be undone. Favorites and the
 * training partner can't be released, like in the PC.
 */
@Composable
private fun ReleaseButton(pokemon: CaughtPokemon, onRelease: () -> Unit) {
    var step by remember(pokemon.id) { mutableIntStateOf(0) }
    val protected = pokemon.isFavorite || pokemon.isTraining
    val name = pokemon.nickname ?: Pokemon[pokemon.speciesId]?.displayName.orEmpty()
    OutlinedButton(
        onClick = { step = 1 },
        enabled = !protected,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.caught_screen_release_button),
            color = if (protected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
        )
    }
    if (protected) {
        Text(
            text = stringResource(R.string.detail_release_protected),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (step > 0) {
        AlertDialog(
            onDismissRequest = { step = 0 },
            title = {
                Text(
                    stringResource(
                        if (step == 1) R.string.detail_release_title else R.string.detail_release_again_title,
                        name
                    )
                )
            },
            text = {
                Text(
                    if (step == 1) {
                        stringResource(R.string.caught_screen_release_message) + "\n\n" +
                            pluralStringResource(R.plurals.release_candy_note, 1, 1)
                    } else {
                        stringResource(R.string.detail_release_again_message, name)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (step == 1) {
                        step = 2
                    } else {
                        step = 0
                        onRelease()
                    }
                }) {
                    Text(
                        text = stringResource(
                            if (step ==
                                1
                            ) {
                                R.string.caught_screen_release_confirm
                            } else {
                                R.string.detail_release_again_button
                            }
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { step = 0 }) { Text(stringResource(R.string.caught_screen_release_cancel)) }
            }
        )
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
private fun EverstoneRow(locked: Boolean, onToggle: (Boolean) -> Unit) {
    run {
        Row(
            modifier = Modifier.fillMaxWidth(),
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

/** When and how it was caught, and its forms. */
@Composable
private fun CaptureDetails(pokemon: CaughtPokemon, pokemonDao: PokemonDao) {
    val species = Pokemon[pokemon.speciesId]
    val formsFound by remember(pokemon.speciesId) { pokemonDao.watchFormsFound(pokemon.speciesId) }
        .collectAsStateWithLifecycle(emptyList())
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
    CaughtPokemonInfoList(
        info = CaughtPokemonDetailInfo(
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
    )
    if (Forms.hasForms(pokemon.speciesId)) {
        pokemon.form?.let { InfoRow(label = stringResource(R.string.form_label), value = formLabel(it)) }
        InfoRow(
            label = stringResource(R.string.forms_found_label),
            value = "${formsFound.size}/${Forms.formsOf(pokemon.speciesId).size}"
        )
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
    "Very Rare", "Starter Pseudo" -> stringResource(R.string.pool_very_rare)
    "Egg" -> stringResource(R.string.pool_egg)
    SHEDINJA_POOL -> stringResource(R.string.pool_evolution)
    "Old Rod" -> stringResource(R.string.item_old_rod_name)
    "Good Rod" -> stringResource(R.string.item_good_rod_name)
    "Super Rod" -> stringResource(R.string.item_super_rod_name)
    else -> stringResource(R.string.caught_detail_encounter_event)
}

/**
 * The shiny chain, in one thin bar: catches of this species since its last shiny, up to the top
 * multiplier, and the multiplier reached so far.
 */
@Composable
private fun ShinyChainCard(pokemon: CaughtPokemon) {
    val context = LocalContext.current
    val caught = remember(pokemon.speciesId) { PreferencesManager(context).shinyChainCounts[pokemon.speciesId] ?: 0 }
    val legendary = pokemon.speciesId in LEGENDARY_SPECIES
    val multiplier = EventHelpers.catchShinyMultiplier(caught)
    // The bar fills up towards the next step, and stays full at the top
    val steps = listOf(0) + EventHelpers.CATCH_STREAK_STEPS.map { it.first }
    val from = steps.last { caught >= it }
    val to = steps.firstOrNull { caught < it }
    val progress = if (to == null) 1f else (caught - from).toFloat() / (to - from)
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = AppSizes.spacingLarge, vertical = AppSizes.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        if (legendary) R.string.caught_screen_filter_shiny else R.string.shiny_chain_title
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (!legendary && multiplier > 1) AppBadge(text = "×$multiplier")
            }
            if (legendary) {
                Text(
                    text = stringResource(
                        R.string.shiny_odds_legendary,
                        EventHelpers.wildShinyOdds(EventHelpers.LEGENDARY_SHINY_MULTIPLIER)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                drawStopIndicator = { }
            )
        }
    }
}
