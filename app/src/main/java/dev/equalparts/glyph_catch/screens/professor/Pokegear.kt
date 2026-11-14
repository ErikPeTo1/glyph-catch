package dev.equalparts.glyph_catch.screens.professor

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import dev.equalparts.glyph_catch.data.NpcEvent
import dev.equalparts.glyph_catch.data.NpcEvents
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.Professor
import dev.equalparts.glyph_catch.data.ProfessorEvent
import dev.equalparts.glyph_catch.data.ProfessorEvents
import dev.equalparts.glyph_catch.data.Region
import dev.equalparts.glyph_catch.gameplay.WaitingNotification
import dev.equalparts.glyph_catch.util.EventHelpers
import dev.equalparts.glyph_catch.util.GlyphDevice
import dev.equalparts.glyph_catch.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun List<InventoryItem>.ownedItems(): Set<Item> =
    filter { it.quantity > 0 }.mapNotNull { Item.entries.getOrNull(it.itemId) }.toSet()

/**
 * Gifts waiting for the player: every professor event except Oak's welcome. They're picked up
 * by calling the professor on the Pokégear.
 */
@Composable
fun rememberPendingCalls(db: PokemonDatabase, preferencesManager: PreferencesManager): List<ProfessorEvent> {
    val pokedexCount by db.pokemonDao().watchPokedexProgress().collectAsStateWithLifecycle(0)
    val inventory by db.inventoryDao().watchAllItems().collectAsStateWithLifecycle(emptyList())
    val introSeen = preferencesManager.professorIntroSeen
    val owned = inventory.ownedItems()
    return remember(introSeen, pokedexCount, owned) {
        if (!introSeen) {
            emptyList()
        } else {
            ProfessorEvents.pending(true, pokedexCount, owned).filter { it != ProfessorEvent.Intro }
        }
    }
}

/**
 * Asks once about color sprites, then shows Professor Oak's welcome. Everything else arrives
 * as a call on the Pokégear.
 */
@Composable
fun ProfessorEventHost(db: PokemonDatabase, preferencesManager: PreferencesManager) {
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
    val owned = inventory?.ownedItems() ?: return
    EventHelpers.hasShinyCharm = Item.SHINY_CHARM in owned && preferencesManager.shinyCharmEnabled
    var pokeBallTipSeen by remember { mutableStateOf(preferencesManager.pokeBallTipSeen) }
    // Settings changes come through here, so switching to the Poké Ball mode brings Oak right away
    val settingsChange by WidgetUpdater.changes.collectAsState()
    val pokeBallMode = remember(settingsChange) { GlyphDevice.pokeBallMode(preferencesManager) }
    if (introSeen && !pokeBallTipSeen && pokeBallMode) {
        // The Phone (4a) Pro's button can't catch, so Oak explains the Poké Ball right away
        val context = LocalContext.current
        val pages = remember { ProfessorScripts.pokeBallTip(context) }
        val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            WidgetUpdater.refresh(context)
        }
        ProfessorDialog(professor = Professor.OAK, pages = pages, onFinish = {
            preferencesManager.pokeBallTipSeen = true
            pokeBallTipSeen = true
            if (preferencesManager.waitingNotification && !WaitingNotification.canPost(context)) {
                askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        })
        return
    }
    var nameKnown by remember { mutableStateOf(preferencesManager.playerName != null) }
    if (introSeen && !nameKnown) {
        val context = LocalContext.current
        val pages = remember { ProfessorScripts.forgotName(context) }
        ProfessorDialog(professor = Professor.OAK, pages = pages, onFinish = { nameKnown = true })
        return
    }
    if (introSeen) {
        NpcEventHost(db, preferencesManager, inventory.orEmpty())
        return
    }

    val context = LocalContext.current
    val pages = remember { ProfessorScripts.event(context, ProfessorEvent.Intro, 0) }
    val scope = rememberCoroutineScope()
    ProfessorDialog(professor = Professor.OAK, pages = pages, onFinish = {
        introSeen = true
        nameKnown = true
        scope.launch {
            withContext(Dispatchers.IO) { ProfessorEvents.complete(ProfessorEvent.Intro, db, preferencesManager) }
        }
    })
}

/**
 * The Fisherman, the clerk and the breeder: each one appears once, when its item is earned.
 */
@Composable
private fun NpcEventHost(db: PokemonDatabase, preferencesManager: PreferencesManager, inventory: List<InventoryItem>) {
    val pokedexCount by db.pokemonDao().watchPokedexProgress().collectAsStateWithLifecycle(-1)
    val totalCaught by db.pokemonDao().watchTotalCaughtCount().collectAsStateWithLifecycle(-1)
    var finished by remember { mutableStateOf(emptySet<NpcEvent>()) }
    if (pokedexCount < 0 || totalCaught < 0) return
    val inBag = inventory.mapNotNull { Item.entries.getOrNull(it.itemId) }.toSet()
    val caughtEver = totalCaught + preferencesManager.releasedCount
    val event = NpcEvents.pending(caughtEver, pokedexCount, inBag, preferencesManager.breederMet)
        .firstOrNull { it !in finished } ?: return

    val context = LocalContext.current
    val npc = remember(event) { NpcEvents.npcFor(event, preferencesManager) }
    val pages = remember(event) { NpcScripts.event(context, event, pokedexCount) }
    val scope = rememberCoroutineScope()
    CharacterDialog(sprite = npc.sprite, name = stringResource(npc.nameRes), pages = pages, onFinish = {
        finished = finished + event
        scope.launch { withContext(Dispatchers.IO) { NpcEvents.complete(event, db, preferencesManager) } }
    })
}

/**
 * The phone from Pokémon Gold and Silver: call a professor to hear how your Pokédex is going
 * and the latest rumors. A professor with something for you has a call waiting.
 */
@Composable
fun PokegearScreen(db: PokemonDatabase, preferencesManager: PreferencesManager, onBackClick: () -> Unit) {
    val inventory by db.inventoryDao().watchAllItems().collectAsStateWithLifecycle(emptyList())
    val pokedexIds by db.pokemonDao().watchCaughtSpeciesIds().collectAsStateWithLifecycle(emptyList())
    val pendingCalls = rememberPendingCalls(db, preferencesManager)
    val waiting = pendingCalls.map { it.professor }.toSet()
    val contacts = remember(inventory, waiting) {
        val known = ProfessorEvents.contacts(preferencesManager.professorIntroSeen, inventory.ownedItems())
        Professor.entries.filter { it in known || it in waiting }
    }
    var calling by remember { mutableStateOf<Professor?>(null) }
    var giftCall by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(emptySet<ProfessorEvent>()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AppScaffoldWithTopBar(title = stringResource(R.string.pokegear_title), onBackClick = onBackClick) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = AppSizes.spacingLarge),
            contentPadding = PaddingValues(vertical = AppSizes.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)
        ) {
            items(contacts) { professor ->
                ContactCard(professor = professor, hasCall = professor in waiting, onCall = {
                    giftCall = professor in waiting
                    calling = professor
                })
            }
        }
    }

    val professor = calling ?: return
    if (giftCall) {
        // Hand over everything this professor has, one conversation after another
        val event = pendingCalls.firstOrNull { it.professor == professor && it !in finished }
        if (event == null) {
            LaunchedEffect(Unit) { calling = null }
            return
        }
        val pages = remember(event) { ProfessorScripts.event(context, event, pokedexIds.size) }
        ProfessorDialog(professor = professor, pages = pages, onFinish = {
            finished = finished + event
            scope.launch { withContext(Dispatchers.IO) { ProfessorEvents.complete(event, db, preferencesManager) } }
        })
    } else {
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
private fun ContactCard(professor: Professor, hasCall: Boolean, onCall: () -> Unit) {
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
                    text = if (hasCall) {
                        stringResource(R.string.pokegear_has_something)
                    } else {
                        stringResource(Region.ofProfessor(professor).nameRes)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasCall) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            BadgedBox(badge = { if (hasCall) CallBadge() }) {
                Button(onClick = onCall) { Text(stringResource(R.string.pokegear_call)) }
            }
        }
    }
}
