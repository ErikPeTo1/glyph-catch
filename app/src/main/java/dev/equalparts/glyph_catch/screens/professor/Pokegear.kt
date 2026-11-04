package dev.equalparts.glyph_catch.screens.professor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppScaffoldWithTopBar
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.InventoryItem
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.Professor
import dev.equalparts.glyph_catch.data.ProfessorEvent
import dev.equalparts.glyph_catch.data.ProfessorEvents
import dev.equalparts.glyph_catch.data.Region
import dev.equalparts.glyph_catch.util.EventHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun List<InventoryItem>.ownedItems(): Set<Item> =
    filter { it.quantity > 0 }.mapNotNull { Item.entries.getOrNull(it.itemId) }.toSet()

/**
 * Shows the professors' conversations the player has earned, one after another, and hands
 * over their gifts at the end of each one. Before anything else, asks once about color sprites.
 */
@Composable
fun ProfessorEventHost(db: PokemonDatabase, preferencesManager: PreferencesManager) {
    val pokedexCount by db.pokemonDao().watchPokedexProgress().collectAsStateWithLifecycle(-1)
    val inventory by db.inventoryDao().watchAllItems().collectAsStateWithLifecycle(null)
    var introSeen by remember { mutableStateOf(preferencesManager.professorIntroSeen) }
    var colorAsked by remember { mutableStateOf(preferencesManager.colorChoiceAsked) }
    if (!colorAsked) {
        ColorChoiceDialog { color ->
            preferencesManager.colorSprites = color
            preferencesManager.colorChoiceAsked = true
            colorAsked = true
        }
        return
    }
    var finished by remember { mutableStateOf(emptySet<ProfessorEvent>()) }
    val owned = inventory?.ownedItems() ?: return
    EventHelpers.hasShinyCharm = Item.SHINY_CHARM in owned && preferencesManager.shinyCharmEnabled
    if (pokedexCount < 0) return

    val event = remember(introSeen, pokedexCount, owned, finished) {
        ProfessorEvents.pending(introSeen, pokedexCount, owned).firstOrNull { it !in finished }
    } ?: return
    val context = LocalContext.current
    val pages = remember(event) { ProfessorScripts.event(context, event, pokedexCount) }
    val scope = rememberCoroutineScope()

    ProfessorDialog(professor = event.professor, pages = pages, onFinish = {
        finished = finished + event
        scope.launch {
            withContext(Dispatchers.IO) { ProfessorEvents.complete(event, db, preferencesManager) }
            if (event == ProfessorEvent.Intro) introSeen = true
        }
    })
}

/**
 * The phone from Pokémon Gold and Silver: call a professor to hear how your Pokédex is going
 * and the latest rumors.
 */
@Composable
fun PokegearScreen(db: PokemonDatabase, preferencesManager: PreferencesManager, onBackClick: () -> Unit) {
    val inventory by db.inventoryDao().watchAllItems().collectAsStateWithLifecycle(emptyList())
    val pokedexIds by db.pokemonDao().watchCaughtSpeciesIds().collectAsStateWithLifecycle(emptyList())
    val contacts = remember(inventory) {
        ProfessorEvents.contacts(preferencesManager.professorIntroSeen, inventory.ownedItems())
    }
    var calling by remember { mutableStateOf<Professor?>(null) }
    val context = LocalContext.current

    AppScaffoldWithTopBar(title = stringResource(R.string.pokegear_title), onBackClick = onBackClick) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = AppSizes.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)
        ) {
            items(contacts) { professor ->
                ContactCard(professor = professor, onCall = { calling = professor })
            }
        }
    }

    calling?.let { professor ->
        val pages = remember(professor) {
            ProfessorScripts.call(context, professor, pokedexIds.toSet(), preferencesManager)
        }
        ProfessorDialog(
            professor = professor,
            pages = pages,
            onFinish = { calling = null },
            onDismiss = { calling = null }
        )
    }
}

@Composable
private fun ContactCard(professor: Professor, onCall: () -> Unit) {
    val name = stringResource(professor.nameRes)
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSizes.spacingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)
        ) {
            ProfessorSprite(professor, modifier = Modifier.size(64.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(Region.ofProfessor(professor).nameRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onCall) { Text(stringResource(R.string.pokegear_call)) }
        }
    }
}
