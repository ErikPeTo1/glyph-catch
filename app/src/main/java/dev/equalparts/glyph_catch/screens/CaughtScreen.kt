package dev.equalparts.glyph_catch.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.equalparts.glyph_catch.AppEmptyState
import dev.equalparts.glyph_catch.AppScreenHeader
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.PokemonExpChip
import dev.equalparts.glyph_catch.PokemonLevelChip
import dev.equalparts.glyph_catch.PokemonSpriteCircle
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.components.PokemonFilterControls
import dev.equalparts.glyph_catch.components.PokemonFilterState
import dev.equalparts.glyph_catch.components.PokemonSort
import dev.equalparts.glyph_catch.components.applyFilters
import dev.equalparts.glyph_catch.components.copyKey
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.InventoryItem
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.displayName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private data class CaughtUiState(
    val totalCaught: Int,
    val filterState: PokemonFilterState,
    val caughtPokemon: List<CaughtPokemon>,
    val selectedIds: Set<String>
) {
    val isSelecting: Boolean get() = selectedIds.isNotEmpty()
}

private data class CaughtActions(
    val onSearchChange: (String) -> Unit,
    val onClearSearch: () -> Unit,
    val onToggleFavorites: () -> Unit,
    val onToggleEvent: () -> Unit,
    val onToggleShiny: () -> Unit,
    val onToggleDuplicates: () -> Unit,
    val onSortChange: (PokemonSort) -> Unit,
    val onToggleFavorite: (CaughtPokemon) -> Unit,
    val onPokemonClick: (CaughtPokemon) -> Unit,
    val onToggleSelection: (CaughtPokemon) -> Unit,
    val onSelectAll: () -> Unit,
    val onClearSelection: () -> Unit,
    val onReleaseClick: () -> Unit
)

/**
 * Favorites and the training partner can't be released, so they can't be selected either.
 */
private val CaughtPokemon.isProtected: Boolean get() = isFavorite || isTraining

/**
 * Selects every copy but one of each species (and form) in [shown], so clearing out duplicates never
 * loses one. If a copy is protected or in [keep] (team, companion), that one stays; otherwise the
 * shiny, then the highest level, then the first caught.
 */
internal fun allButOneOfEach(shown: List<CaughtPokemon>, pc: List<CaughtPokemon>, keep: Set<String>): Set<String> {
    val copies = pc.groupBy { it.copyKey }
    val keepers = shown.map { it.copyKey }.distinct().mapNotNull { key ->
        val all = copies[key].orEmpty()
        all.firstOrNull { it.isFavorite || it.isTraining || it.id in keep }
            ?: all.maxWithOrNull(
                compareBy<CaughtPokemon> { it.isShiny }.thenBy { it.level }.thenByDescending { it.caughtAt }
            )
    }.map { it.id }.toSet()
    return shown.filter { !it.isFavorite && !it.isTraining && it.id !in keepers }.map { it.id }.toSet()
}

@Composable
fun CaughtScreen(db: PokemonDatabase, initialSearchQuery: String = "", onPokemonClick: (CaughtPokemon) -> Unit = {}) {
    val context = LocalContext.current
    val pokemonDao = remember(db) { db.pokemonDao() }
    val preferencesManager = remember(context) { PreferencesManager(context) }
    val caughtPokemon by pokemonDao.watchAllCaught().collectAsStateWithLifecycle(emptyList())
    val totalCaught by pokemonDao.watchTotalCaughtCount().collectAsStateWithLifecycle(0)
    val scope = rememberCoroutineScope()

    var searchQuery by rememberSaveable(initialSearchQuery) { mutableStateOf(initialSearchQuery) }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var showEventOnly by remember { mutableStateOf(false) }
    var showShinyOnly by remember { mutableStateOf(false) }
    var showDuplicatesOnly by remember { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf(PokemonSort.RECENT) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var showReleaseConfirmation by remember { mutableStateOf(false) }

    val toggleFavorite: (CaughtPokemon) -> Unit = { pokemon ->
        scope.launch { pokemonDao.updateFavorite(pokemon.id, !pokemon.isFavorite) }
    }

    val filterState = PokemonFilterState(
        searchQuery = searchQuery,
        showFavoritesOnly = showFavoritesOnly,
        showEventOnly = showEventOnly,
        showShinyOnly = showShinyOnly,
        showDuplicatesOnly = showDuplicatesOnly,
        sort = sort
    )

    val filteredPokemon by remember(caughtPokemon, filterState) {
        derivedStateOf { caughtPokemon.applyFilters(filterState) }
    }

    // Drop selections that no longer exist or became protected (e.g. favorited)
    val validSelection = remember(selectedIds, caughtPokemon) {
        val selectable = caughtPokemon.filterNot { it.isProtected }.map { it.id }.toSet()
        selectedIds intersect selectable
    }

    BackHandler(enabled = validSelection.isNotEmpty()) { selectedIds = emptySet() }

    val state = CaughtUiState(
        totalCaught = totalCaught,
        filterState = filterState,
        caughtPokemon = filteredPokemon,
        selectedIds = validSelection
    )

    val actions = CaughtActions(
        onSearchChange = { searchQuery = it },
        onClearSearch = { searchQuery = "" },
        onToggleFavorites = { showFavoritesOnly = !showFavoritesOnly },
        onToggleEvent = { showEventOnly = !showEventOnly },
        onToggleShiny = { showShinyOnly = !showShinyOnly },
        onToggleDuplicates = { showDuplicatesOnly = !showDuplicatesOnly },
        onSortChange = { sort = it },
        onToggleFavorite = toggleFavorite,
        onPokemonClick = onPokemonClick,
        onToggleSelection = { pokemon ->
            if (!pokemon.isProtected) {
                selectedIds =
                    if (pokemon.id in validSelection) validSelection - pokemon.id else validSelection + pokemon.id
            }
        },
        onSelectAll = {
            selectedIds = if (showDuplicatesOnly) {
                val keep = preferencesManager.team.toSet() + listOfNotNull(preferencesManager.companionId)
                allButOneOfEach(filteredPokemon, caughtPokemon, keep)
            } else {
                filteredPokemon.filterNot { it.isProtected }.map { it.id }.toSet()
            }
        },
        onClearSelection = { selectedIds = emptySet() },
        onReleaseClick = { showReleaseConfirmation = true }
    )

    CaughtScreenContent(state = state, actions = actions)

    if (showReleaseConfirmation) {
        val count = validSelection.size
        AlertDialog(
            onDismissRequest = { showReleaseConfirmation = false },
            title = { Text(pluralStringResource(R.plurals.caught_screen_release_title, count, count)) },
            text = {
                Text(
                    stringResource(R.string.caught_screen_release_message) + "\n\n" +
                        pluralStringResource(R.plurals.release_candy_note, count, count)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ids = validSelection.toList()
                        scope.launch {
                            pokemonDao.deleteCaught(ids)
                            preferencesManager.forgetMissingPokemon(db)
                            preferencesManager.releasedCount += ids.size
                            // A Rare Candy for each released Pokémon
                            val inventoryDao = db.inventoryDao()
                            if (inventoryDao.getItem(Item.RARE_CANDY.ordinal) == null) {
                                inventoryDao.insertItem(
                                    InventoryItem(itemId = Item.RARE_CANDY.ordinal, quantity = ids.size)
                                )
                            } else {
                                inventoryDao.addItems(Item.RARE_CANDY.ordinal, ids.size)
                            }
                        }
                        selectedIds = emptySet()
                        showReleaseConfirmation = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.caught_screen_release_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showReleaseConfirmation = false }) {
                    Text(stringResource(R.string.caught_screen_release_cancel))
                }
            }
        )
    }
}

@Composable
private fun CaughtScreenContent(state: CaughtUiState, actions: CaughtActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppSizes.spacingXLarge)
    ) {
        if (state.isSelecting) {
            SelectionHeader(
                selectedCount = state.selectedIds.size,
                keepOne = state.filterState.showDuplicatesOnly,
                onSelectAll = actions.onSelectAll,
                onRelease = actions.onReleaseClick,
                onClose = actions.onClearSelection
            )
        } else {
            AppScreenHeader(
                title = stringResource(R.string.caught_screen_title),
                subtitle = stringResource(R.string.caught_screen_total_caught, state.totalCaught)
            )
        }

        PokemonFilterControls(
            state = state.filterState,
            onSearchChange = actions.onSearchChange,
            onClearSearch = actions.onClearSearch,
            onToggleFavorites = actions.onToggleFavorites,
            onToggleEvent = actions.onToggleEvent,
            onSortChange = actions.onSortChange,
            onToggleShiny = actions.onToggleShiny,
            onToggleDuplicates = actions.onToggleDuplicates
        )

        CaughtResults(state = state, actions = actions)
    }
}

/** With the Repeated filter on, "select all" keeps one of each species. */
@Composable
private fun SelectionHeader(
    selectedCount: Int,
    keepOne: Boolean,
    onSelectAll: () -> Unit,
    onRelease: () -> Unit,
    onClose: () -> Unit
) {
    Column {
        AppScreenHeader(
            title = pluralStringResource(R.plurals.caught_screen_selected_count, selectedCount, selectedCount),
            subtitle = null
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.caught_screen_release_cancel)
                )
            }
            TextButton(onClick = onSelectAll) {
                Text(
                    stringResource(
                        if (keepOne) R.string.caught_screen_select_all_but_one else R.string.caught_screen_select_all
                    )
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onRelease) {
                Text(
                    text = stringResource(R.string.caught_screen_release_button),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(modifier = Modifier.height(AppSizes.spacingSmall))
    }
}

@Composable
private fun CaughtResults(state: CaughtUiState, actions: CaughtActions) {
    if (state.caughtPokemon.isEmpty()) {
        AppEmptyState(
            primaryText = if (state.filterState.hasActiveFilters) {
                stringResource(R.string.caught_screen_empty_filtered_title)
            } else {
                stringResource(R.string.caught_screen_empty_title)
            },
            secondaryText = if (state.filterState.hasActiveFilters) {
                stringResource(R.string.caught_screen_empty_filtered_subtitle)
            } else {
                null
            }
        )
    } else {
        val context = LocalContext.current
        val preferences = remember { PreferencesManager(context) }
        var gridView by remember { mutableStateOf(preferences.caughtGridView) }
        // The first visible Pokémon, shared by both layouts so switching keeps your place
        var anchor by rememberSaveable { mutableIntStateOf(0) }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                gridView = !gridView
                preferences.caughtGridView = gridView
            }) {
                if (gridView) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = stringResource(R.string.caught_screen_list_view)
                    )
                } else {
                    Icon(
                        painterResource(R.drawable.icon_grid_dots),
                        contentDescription = stringResource(R.string.caught_screen_grid_view)
                    )
                }
            }
        }

        val onClick = if (state.isSelecting) actions.onToggleSelection else actions.onPokemonClick
        if (gridView) {
            val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = anchor)
            LaunchedEffect(gridState) { snapshotFlow { gridState.firstVisibleItemIndex }.collect { anchor = it } }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium),
                verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)
            ) {
                items(items = state.caughtPokemon, key = { it.id }) { pokemon ->
                    CaughtPokemonGridCell(
                        pokemon = pokemon,
                        isSelecting = state.isSelecting,
                        isSelected = pokemon.id in state.selectedIds,
                        onClick = onClick,
                        onLongClick = actions.onToggleSelection
                    )
                }
            }
        } else {
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = anchor)
            LaunchedEffect(listState) { snapshotFlow { listState.firstVisibleItemIndex }.collect { anchor = it } }
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
            ) {
                items(
                    items = state.caughtPokemon,
                    key = { it.id }
                ) { pokemon ->
                    CaughtPokemonCard(
                        pokemon = pokemon,
                        isSelecting = state.isSelecting,
                        isSelected = pokemon.id in state.selectedIds,
                        onToggleFavorite = actions.onToggleFavorite,
                        onClick = onClick,
                        onLongClick = actions.onToggleSelection
                    )
                }
            }
        }
    }
}

/**
 * A compact cell for the 3-column grid: sprite, name and level.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CaughtPokemonGridCell(
    pokemon: CaughtPokemon,
    isSelecting: Boolean,
    isSelected: Boolean,
    onClick: (CaughtPokemon) -> Unit,
    onLongClick: (CaughtPokemon) -> Unit
) {
    val species = Pokemon[pokemon.speciesId] ?: return
    val dimmed = isSelecting && pokemon.isProtected
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (dimmed) 0.4f else 1f),
        shape = RoundedCornerShape(AppSizes.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = { onClick(pokemon) }, onLongClick = { onLongClick(pokemon) })
                .padding(AppSizes.spacingSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingTiny)
        ) {
            Box {
                PokemonSpriteCircle(
                    pokemonId = pokemon.speciesId,
                    pokemonName = species.displayName,
                    shiny = pokemon.isShiny,
                    form = pokemon.form
                )
                if (pokemon.isFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                    )
                }
            }
            Text(
                text = pokemon.nickname ?: species.displayName,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            PokemonLevelChip(level = pokemon.level)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CaughtPokemonCard(
    pokemon: CaughtPokemon,
    onToggleFavorite: (CaughtPokemon) -> Unit,
    onClick: (CaughtPokemon) -> Unit,
    onLongClick: (CaughtPokemon) -> Unit = {},
    isSelecting: Boolean = false,
    isSelected: Boolean = false
) {
    val species = Pokemon[pokemon.speciesId]!!
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(pokemon.caughtAt) { dateFormat.format(Date(pokemon.caughtAt)) }
    val dimmed = isSelecting && pokemon.isProtected

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (dimmed) 0.4f else 1f),
        shape = RoundedCornerShape(AppSizes.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onClick(pokemon) },
                    onLongClick = { onLongClick(pokemon) }
                )
                .padding(AppSizes.spacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PokemonSpriteCircle(
                pokemonId = pokemon.speciesId,
                pokemonName = species.displayName,
                shiny = pokemon.isShiny,
                form = pokemon.form
            )

            Spacer(modifier = Modifier.size(AppSizes.spacingMedium))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pokemon.nickname ?: species.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(AppSizes.spacingTiny))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
                ) {
                    PokemonLevelChip(level = pokemon.level)
                    PokemonExpChip(level = pokemon.level, exp = pokemon.exp)
                }

                Spacer(modifier = Modifier.height(AppSizes.spacingTiny))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (pokemon.isSpecialSpawn || pokemon.isConditionalSpawn) {
                        Text(
                            text = "★",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            when {
                isSelecting && pokemon.isProtected -> Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = stringResource(R.string.caught_screen_protected_content_description),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(AppSizes.spacingMedium)
                )
                isSelecting -> Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    },
                    modifier = Modifier.padding(AppSizes.spacingMedium)
                )
                else -> IconButton(onClick = { onToggleFavorite(pokemon) }) {
                    Icon(
                        imageVector = if (pokemon.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = stringResource(R.string.caught_screen_favorite_content_description),
                        tint = if (pokemon.isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}
