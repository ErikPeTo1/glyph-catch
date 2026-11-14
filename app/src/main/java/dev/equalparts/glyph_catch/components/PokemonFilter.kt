package dev.equalparts.glyph_catch.components

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.displayName

/**
 * Sort orders for Pokémon lists, modelled after the box sorting options of the main games.
 */
enum class PokemonSort(@StringRes val labelRes: Int) {
    RECENT(R.string.pokemon_sort_recent),
    OLDEST(R.string.pokemon_sort_oldest),
    POKEDEX(R.string.pokemon_sort_pokedex),
    LEVEL_HIGH(R.string.pokemon_sort_level_high),
    LEVEL_LOW(R.string.pokemon_sort_level_low),
    NAME(R.string.pokemon_sort_name),
    TYPE(R.string.pokemon_sort_type),
    FAVORITES(R.string.pokemon_sort_favorites),
    DUPLICATES(R.string.pokemon_sort_duplicates)
}

data class PokemonFilterState(
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false,
    val showShinyOnly: Boolean = false,
    val showDuplicatesOnly: Boolean = false,
    val showTeamOnly: Boolean = false,
    /** The team's ids, for [showTeamOnly]. */
    val team: Set<String> = emptySet(),
    val sort: PokemonSort = PokemonSort.RECENT
) {
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() || showFavoritesOnly || showShinyOnly || showDuplicatesOnly || showTeamOnly
}

private fun CaughtPokemon.sortName(): String = nickname ?: Pokemon[speciesId]?.displayName.orEmpty()

/** The same species and form: West Sea and East Sea Shellos aren't duplicates of each other. */
internal val CaughtPokemon.copyKey: Pair<Int, String?> get() = speciesId to form

/** [copies] is how many of each species and form are in the PC, for sorting by duplicates. */
private fun PokemonSort.comparator(copies: Map<Pair<Int, String?>, Int>): Comparator<CaughtPokemon> {
    val newestFirst = compareByDescending<CaughtPokemon> { it.caughtAt }
    val byPokedex = compareBy<CaughtPokemon> { it.speciesId }
    return when (this) {
        PokemonSort.RECENT -> newestFirst
        PokemonSort.OLDEST -> compareBy { it.caughtAt }
        PokemonSort.POKEDEX -> byPokedex.thenByDescending { it.level }
        PokemonSort.LEVEL_HIGH -> compareByDescending<CaughtPokemon> { it.level }.then(byPokedex)
        PokemonSort.LEVEL_LOW -> compareBy<CaughtPokemon> { it.level }.then(byPokedex)
        PokemonSort.NAME -> compareBy<CaughtPokemon, String>(String.CASE_INSENSITIVE_ORDER) { it.sortName() }
            .then(byPokedex)
        PokemonSort.TYPE -> compareBy<CaughtPokemon> { Pokemon[it.speciesId]?.type1?.ordinal ?: Int.MAX_VALUE }
            .then(byPokedex)
        PokemonSort.FAVORITES -> compareByDescending<CaughtPokemon> { it.isFavorite }.then(newestFirst)
        PokemonSort.DUPLICATES -> compareByDescending<CaughtPokemon> { copies[it.copyKey] ?: 0 }
            .then(byPokedex)
            .thenBy { it.form.orEmpty() }
            .thenByDescending { it.level }
    }
}

fun List<CaughtPokemon>.applyFilters(state: PokemonFilterState): List<CaughtPokemon> {
    // Counted over the whole PC, so filters don't change what counts as a duplicate
    val copies = groupingBy { it.copyKey }.eachCount()
    val search = PokemonSearch.matcher(state.searchQuery)
    return filter { pokemon ->
        val matchesSearch = search(pokemon)
        val matchesFavorite = !state.showFavoritesOnly || pokemon.isFavorite
        val matchesShiny = !state.showShinyOnly || pokemon.isShiny
        val matchesDuplicates = !state.showDuplicatesOnly || (copies[pokemon.copyKey] ?: 0) > 1
        val matchesTeam = !state.showTeamOnly || pokemon.id in state.team
        matchesSearch && matchesFavorite && matchesShiny && matchesDuplicates && matchesTeam
    }.sortedWith(state.sort.comparator(copies))
}

@Composable
fun PokemonFilterControls(
    state: PokemonFilterState,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleFavorites: () -> Unit,
    onToggleShiny: () -> Unit,
    onSortChange: (PokemonSort) -> Unit,
    modifier: Modifier = Modifier,
    onToggleDuplicates: (() -> Unit)? = null,
    onToggleTeam: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier) {
        PokemonFilterSearchField(
            query = state.searchQuery,
            onQueryChange = onSearchChange,
            onClearQuery = onClearSearch,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(AppSizes.spacingSmall))

        // The chips scroll; whatever goes at the end (the list/grid switch) stays put
        Row(verticalAlignment = Alignment.CenterVertically) {
            PokemonFilterChips(
                showFavoritesOnly = state.showFavoritesOnly,
                onToggleFavorites = onToggleFavorites,
                showShinyOnly = state.showShinyOnly,
                onToggleShiny = onToggleShiny,
                showDuplicatesOnly = state.showDuplicatesOnly,
                onToggleDuplicates = onToggleDuplicates,
                showTeamOnly = state.showTeamOnly,
                onToggleTeam = onToggleTeam,
                sort = state.sort,
                onSortChange = onSortChange,
                modifier = Modifier.weight(1f)
            )
            trailing?.invoke()
        }
    }
}

@Composable
private fun PokemonFilterSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHelp by remember { mutableStateOf(false) }
    if (showHelp) SearchHelpDialog(onDismiss = { showHelp = false })
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.caught_screen_search_placeholder)) },
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(AppSizes.cardCornerRadius),
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = onClearQuery) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.caught_screen_clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            {
                IconButton(onClick = { showHelp = true }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.search_help_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

/** How to search like in Pokémon GO, with examples. */
@Composable
private fun SearchHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.search_help_title)) },
        text = {
            val rows = stringResource(R.string.search_help_body).lines().mapNotNull { line ->
                val colon = line.indexOf(':')
                if (colon < 0) null else line.take(colon).trim() to line.drop(colon + 1).trim()
            }
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
            ) {
                rows.forEach { (concept, example) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = concept,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = example,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } }
    )
}

@Composable
private fun PokemonFilterChips(
    showFavoritesOnly: Boolean,
    onToggleFavorites: () -> Unit,
    showShinyOnly: Boolean,
    onToggleShiny: () -> Unit,
    showDuplicatesOnly: Boolean,
    onToggleDuplicates: (() -> Unit)?,
    showTeamOnly: Boolean,
    onToggleTeam: (() -> Unit)?,
    sort: PokemonSort,
    onSortChange: (PokemonSort) -> Unit,
    modifier: Modifier = Modifier
) {
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
    ) {
        SortChip(
            selected = sort,
            options = PokemonSort.entries,
            label = { stringResource(it.labelRes) },
            onSelect = onSortChange
        )

        if (onToggleTeam != null) {
            FilterChip(
                selected = showTeamOnly,
                onClick = onToggleTeam,
                label = { Text(stringResource(R.string.caught_screen_filter_team)) },
                colors = chipColors
            )
        }

        // The heart and the sparkles say it all, like on each Pokémon: gray when off, lit when on
        LightChip(selected = showFavoritesOnly, onClick = onToggleFavorites) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = stringResource(R.string.caught_screen_filter_favorites),
                tint = if (showFavoritesOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(AppSizes.iconSizeSmall)
            )
        }

        LightChip(selected = showShinyOnly, onClick = onToggleShiny) {
            Text(text = "✨", modifier = Modifier.alpha(if (showShinyOnly) 1f else 0.35f))
        }

        // Only where it makes sense: the PC
        if (onToggleDuplicates != null) {
            FilterChip(
                selected = showDuplicatesOnly,
                onClick = onToggleDuplicates,
                label = { Text(stringResource(R.string.caught_screen_filter_duplicates_short)) },
                colors = chipColors
            )
        }
    }
}

/** A chip whose icon lights up when it's on, instead of the whole chip filling in. */
@Composable
private fun LightChip(selected: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    val border = MaterialTheme.colorScheme.outlineVariant
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = icon,
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.Transparent),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = border,
            selectedBorderColor = border,
            selectedBorderWidth = 1.dp
        )
    )
}
