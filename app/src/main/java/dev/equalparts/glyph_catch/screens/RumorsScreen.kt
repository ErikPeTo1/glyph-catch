package dev.equalparts.glyph_catch.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppScaffoldWithTopBar
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.PokedexSpriteCircle
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.Rumor
import dev.equalparts.glyph_catch.data.Rumors
import dev.equalparts.glyph_catch.data.displayName

/**
 * Hints about legendary and mythical Pokémon, unlocked as the Pokédex grows.
 */
@Composable
fun RumorsScreen(db: PokemonDatabase, onBackClick: () -> Unit) {
    val pokedexCount by db.pokemonDao().watchPokedexProgress().collectAsStateWithLifecycle(0)
    val caughtSpeciesIds by db.pokemonDao().watchCaughtSpeciesIds().collectAsStateWithLifecycle(emptyList())
    val caughtSet = remember(caughtSpeciesIds) { caughtSpeciesIds.toSet() }
    val unlocked = Rumors.all.count { pokedexCount >= it.unlockAt }

    AppScaffoldWithTopBar(
        title = stringResource(R.string.rumors_title),
        onBackClick = onBackClick
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = AppSizes.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)
        ) {
            item {
                Text(
                    text = stringResource(R.string.rumors_progress, unlocked, Rumors.all.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = AppSizes.spacingSmall)
                )
            }
            items(Rumors.all, key = { it.speciesId }) { rumor ->
                RumorCard(
                    rumor = rumor,
                    unlocked = pokedexCount >= rumor.unlockAt,
                    found = rumor.speciesId in caughtSet
                )
            }
            item { Spacer(modifier = Modifier.size(AppSizes.spacingLarge)) }
        }
    }
}

@Composable
private fun RumorCard(rumor: Rumor, unlocked: Boolean, found: Boolean) {
    val species = Pokemon[rumor.speciesId]
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSizes.spacingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)
        ) {
            PokedexSpriteCircle(
                pokemonId = rumor.speciesId,
                pokemonName = species?.displayName.orEmpty(),
                caught = found
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSizes.spacingTiny)) {
                Text(
                    text = if (found) species?.displayName.orEmpty() else stringResource(R.string.rumors_unknown_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (unlocked) {
                        stringResource(rumor.textRes)
                    } else {
                        stringResource(R.string.rumors_locked, rumor.unlockAt)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
