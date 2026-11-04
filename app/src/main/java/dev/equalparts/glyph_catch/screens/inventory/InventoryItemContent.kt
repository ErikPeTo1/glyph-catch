package dev.equalparts.glyph_catch.screens.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.ItemGlyphCircle
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.ActiveItem
import dev.equalparts.glyph_catch.data.FISHING_RODS
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.LegendaryItems
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.Region
import dev.equalparts.glyph_catch.data.descriptionRes
import dev.equalparts.glyph_catch.data.effectDurationMinutes
import dev.equalparts.glyph_catch.data.isFishingRod
import dev.equalparts.glyph_catch.data.isKeyItem
import dev.equalparts.glyph_catch.data.nameRes
import dev.equalparts.glyph_catch.ndotFontFamily
import dev.equalparts.glyph_catch.screens.professor.ownedItems
import dev.equalparts.glyph_catch.util.EventHelpers
import dev.equalparts.glyph_catch.util.rememberActiveItemStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun InventoryItemContent(
    db: PokemonDatabase,
    item: Item,
    quantity: Int,
    preferencesManager: PreferencesManager,
    onSelectPokemonClick: (Item) -> Unit
) {
    val name = stringResource(item.nameRes())
    val description = itemDescription(item)
    val durationMinutes = item.effectDurationMinutes()

    InventoryItemInfoCard(
        item = item,
        name = name,
        description = description,
        quantity = quantity
    )

    when {
        durationMinutes != null -> TimedItemActions(db, item, name, durationMinutes)
        item == Item.REPEL -> RepelToggleCard(preferencesManager)
        item == Item.TOWN_MAP -> RegionFilterCard(db, preferencesManager)
        item == Item.SHINY_CHARM -> ShinyCharmToggleCard(preferencesManager)
        item.isKeyItem -> Unit
        else -> UseOnPokemonButton(name, quantity) { onSelectPokemonClick(item) }
    }
}

@Composable
private fun InventoryItemInfoCard(item: Item, name: String, description: String, quantity: Int) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSizes.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ItemGlyphCircle(
                item = item,
                contentDescription = name,
                modifier = Modifier.size(120.dp)
            )

            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (item.isKeyItem) {
                    stringResource(R.string.inventory_key_item)
                } else {
                    stringResource(R.string.inventory_item_quantity, quantity)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = ndotFontFamily
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TimedItemActions(db: PokemonDatabase, item: Item, itemName: String, durationMinutes: Int) {
    val status by rememberActiveItemStatus(db, item)
    val scope = rememberCoroutineScope()
    var isActivating by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (!status.isActive) {
                scope.launch {
                    isActivating = true
                    try {
                        activateItem(db, item, durationMinutes)
                    } finally {
                        isActivating = false
                    }
                }
            }
        },
        enabled = !status.isActive && !isActivating,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (status.isActive) {
                stringResource(R.string.inventory_item_using_button, itemName)
            } else {
                stringResource(R.string.inventory_item_use_button, itemName)
            }
        )
    }

    if (status.isActive) {
        OutlinedButton(
            onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        db.activeItemDao().deactivateItem(item.ordinal)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.inventory_item_cancel_button))
        }
    }
}

@Composable
private fun RepelToggleCard(preferencesManager: PreferencesManager) {
    val flow = remember(preferencesManager) { preferencesManager.watchRepelActive() }
    val repelActive by flow.collectAsStateWithLifecycle(preferencesManager.isRepelActive)

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AppSizes.spacingLarge)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.item_repel_toggle_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(AppSizes.spacingTiny))
                    Text(
                        text = stringResource(R.string.item_repel_toggle_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = repelActive,
                    onCheckedChange = { isEnabled ->
                        preferencesManager.isRepelActive = isEnabled
                    }
                )
            }
        }
    }
}

@Composable
private fun UseOnPokemonButton(itemName: String, quantity: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = quantity > 0,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.inventory_item_use_on_pokemon_button, itemName))
    }
}

private suspend fun activateItem(db: PokemonDatabase, item: Item, durationMinutes: Int) {
    val now = System.currentTimeMillis()
    val expiresAt = now + durationMinutes * 60_000L
    withContext(Dispatchers.IO) {
        if (item.isFishingRod) {
            FISHING_RODS.filter { it != item }.forEach { db.activeItemDao().deactivateItem(it.ordinal) }
        }
        db.activeItemDao().activateItem(
            ActiveItem(
                itemId = item.ordinal,
                activatedAt = now,
                expiresAt = expiresAt
            )
        )
    }
}

/**
 * Key items explain who gave them and what they're for.
 */
@Composable
private fun itemDescription(item: Item): String {
    val pokedexRegion = Region.ofPokedex(item)
    return when {
        pokedexRegion != null -> stringResource(
            item.descriptionRes(),
            stringResource(pokedexRegion.professor.nameRes),
            stringResource(pokedexRegion.nameRes)
        )
        item in LegendaryItems.species -> stringResource(
            item.descriptionRes(),
            stringResource(LegendaryItems.region(item).professor.nameRes),
            LegendaryItems.speciesNames(item)
        )
        else -> stringResource(item.descriptionRes())
    }
}

/**
 * The Town Map: choose which of the open regions' Pokémon can appear.
 */
@Composable
private fun RegionFilterCard(db: PokemonDatabase, preferencesManager: PreferencesManager) {
    val inventory by db.inventoryDao().watchAllItems().collectAsStateWithLifecycle(emptyList())
    val owned = inventory.ownedItems()
    val unlocked = Region.entries.filter { it.pokedex == null || it.pokedex in owned }
    var disabled by remember { mutableStateOf(preferencesManager.disabledRegions) }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSizes.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
        ) {
            Text(text = stringResource(R.string.region_filter_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.region_filter_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            unlocked.forEach { region ->
                val enabled = region.name !in disabled
                val isLast = enabled && unlocked.count { it.name !in disabled } == 1
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(region.nameRes),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = enabled,
                        enabled = !isLast,
                        onCheckedChange = { on ->
                            disabled = if (on) disabled - region.name else disabled + region.name
                            preferencesManager.disabledRegions = disabled
                        }
                    )
                }
            }
        }
    }
}

/**
 * The Shiny Charm can be switched off, for anyone who prefers the usual odds.
 */
@Composable
private fun ShinyCharmToggleCard(preferencesManager: PreferencesManager) {
    var enabled by remember { mutableStateOf(preferencesManager.shinyCharmEnabled) }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSizes.spacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.item_shiny_charm_toggle_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.item_shiny_charm_toggle_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { on ->
                    enabled = on
                    preferencesManager.shinyCharmEnabled = on
                    EventHelpers.hasShinyCharm = on
                }
            )
        }
    }
}
