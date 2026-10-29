package dev.equalparts.glyph_catch.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppScreenHeader
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.PokedexSpriteCircle
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PokemonSpecies
import dev.equalparts.glyph_catch.data.displayName
import dev.equalparts.glyph_catch.ndotFontFamily

private data class PokedexScreenState(
    val region: PokedexRegion,
    val caughtSpeciesIds: Set<Int>,
    val shinySpeciesIds: Set<Int>
) {
    val progress: Int get() = caughtSpeciesIds.count { it in region.range }
    val shinyProgress: Int get() = shinySpeciesIds.count { it in region.range }
}

private enum class PokedexRegion(@StringRes val labelRes: Int, val range: IntRange) {
    ALL(R.string.pokedex_region_all, 1..Pokemon.POKEDEX_SIZE),
    KANTO(R.string.pokedex_region_kanto, 1..151),
    JOHTO(R.string.pokedex_region_johto, 152..251),
    HOENN(R.string.pokedex_region_hoenn, 252..386),
    SINNOH(R.string.pokedex_region_sinnoh, 387..493),
    UNOVA(R.string.pokedex_region_unova, 494..649)
}

@Composable
fun PokedexScreen(db: PokemonDatabase, onPokemonClick: (Int) -> Unit = {}, onRumorsClick: () -> Unit = {}) {
    var region by rememberSaveable { mutableStateOf(PokedexRegion.ALL) }
    val caughtSpeciesIds by db.pokemonDao().watchCaughtSpeciesIds().collectAsStateWithLifecycle(emptyList())
    val caughtSpeciesSet = remember(caughtSpeciesIds) {
        caughtSpeciesIds.toSet()
    }

    val shinySpeciesIds by db.pokemonDao().watchShinySpeciesIds().collectAsStateWithLifecycle(emptyList())
    val shinySpeciesSet = remember(shinySpeciesIds) { shinySpeciesIds.toSet() }
    val state =
        PokedexScreenState(region = region, caughtSpeciesIds = caughtSpeciesSet, shinySpeciesIds = shinySpeciesSet)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppSizes.spacingXLarge)
    ) {
        AppScreenHeader(
            title = stringResource(R.string.pokedex_title),
            subtitle = stringResource(R.string.pokedex_progress_subtitle, state.progress, region.range.count()) +
                if (state.shinyProgress > 0) "  ✨${state.shinyProgress}" else ""
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
        ) {
            AssistChip(onClick = onRumorsClick, label = { Text(stringResource(R.string.rumors_button)) })
            PokedexRegion.entries.forEach { option ->
                FilterChip(
                    selected = option == region,
                    onClick = { region = option },
                    label = { Text(stringResource(option.labelRes)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(AppSizes.spacingLarge))
        PokedexGrid(state = state, onPokemonClick = onPokemonClick)
    }
}

@Composable
private fun PokedexGrid(state: PokedexScreenState, onPokemonClick: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge),
        verticalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
    ) {
        items(count = state.region.range.count()) { index ->
            val pokemonId = state.region.range.first + index
            val caught = pokemonId in state.caughtSpeciesIds
            val species = Pokemon.all[pokemonId] ?: return@items

            PokedexEntryCard(
                pokemonId = pokemonId,
                species = species,
                caught = caught,
                shinyCaught = pokemonId in state.shinySpeciesIds,
                onClick = if (caught) {
                    { onPokemonClick(pokemonId) }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun PokedexEntryCard(
    pokemonId: Int,
    species: PokemonSpecies,
    caught: Boolean,
    shinyCaught: Boolean,
    onClick: (() -> Unit)?
) {
    val clickModifier = if (caught && onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

    AppCard(
        modifier = Modifier
            .aspectRatio(0.85f)
            .then(clickModifier),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppSizes.spacingExtraSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PokedexSpriteCircle(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .aspectRatio(1f),
                pokemonId = pokemonId,
                pokemonName = species.displayName,
                caught = caught,
                shinyCaught = shinyCaught,
                size = null
            )

            Spacer(modifier = Modifier.height(AppSizes.spacingMicro))

            PokedexEntryLabel(species = species, caught = caught)

            Text(
                text = stringResource(R.string.pokedex_entry_number, pokemonId),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PokedexEntryLabel(species: PokemonSpecies, caught: Boolean) {
    if (caught) {
        val fontSize = when {
            species.displayName.length >= 10 -> MaterialTheme.typography.labelMedium.fontSize
            else -> MaterialTheme.typography.labelLarge.fontSize
        }
        Text(
            text = species.displayName,
            fontFamily = ndotFontFamily,
            fontSize = fontSize,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    } else {
        Text(
            text = "???",
            fontFamily = ndotFontFamily,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
