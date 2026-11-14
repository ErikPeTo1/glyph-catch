package dev.equalparts.glyph_catch.screens.garden

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppScreenHeader
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.LocalColorSprites
import dev.equalparts.glyph_catch.PokemonLevelChip
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.components.GenderSymbol
import dev.equalparts.glyph_catch.components.PokemonSearch
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.Flavor
import dev.equalparts.glyph_catch.data.GardenCatalog
import dev.equalparts.glyph_catch.data.GardenState
import dev.equalparts.glyph_catch.data.GardenStore
import dev.equalparts.glyph_catch.data.GardenZone
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.displayName
import dev.equalparts.glyph_catch.util.PokemonSpriteUtils

private val grayscaleMatrix = ColorMatrix().apply { setToSaturation(0f) }

/**
 * The town at a glance: one card per zone with its scene, who lives there and how it's going.
 * Zones that aren't open yet show a padlock.
 */
@Composable
fun TownMap(all: List<CaughtPokemon>, team: List<String>, garden: GardenState, onOpen: (GardenZone) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppSizes.spacingXLarge)
    ) {
        AppScreenHeader(title = stringResource(R.string.garden_title), subtitle = null)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium),
            modifier = Modifier.padding(bottom = AppSizes.spacingLarge)
        ) {
            items(GardenZone.entries, key = { it.id }) { zone ->
                ZoneCard(
                    zone = zone,
                    residents = zoneResidents(zone, all, team, garden),
                    status = zoneStatus(zone, garden),
                    onClick = { onOpen(zone) }
                )
            }
        }
    }
}

/** Who lives in a zone: the team in the Meadow (minus anyone sent elsewhere), the picked ones elsewhere. */
internal fun zoneResidents(
    zone: GardenZone,
    all: List<CaughtPokemon>,
    team: List<String>,
    garden: GardenState
): List<CaughtPokemon> = if (zone == GardenZone.MEADOW) {
    residentsOf(all, team).filter { garden.zoneOf(it.id) == null }
} else {
    val byId = all.associateBy { it.id }
    garden.residents(zone).mapNotNull { byId[it] }
}

@Composable
private fun zoneStatus(zone: GardenZone, garden: GardenState): String? = when (zone) {
    GardenZone.ORCHARD -> {
        val ripe = garden.plots.count { it?.ripe == true }
        val thirsty = garden.plots.count { it?.thirsty == true }
        when {
            ripe > 0 -> stringResource(R.string.zone_ripe, ripe)
            thirsty > 0 -> stringResource(R.string.zone_thirsty, thirsty)
            else -> null
        }
    }
    GardenZone.SPRINGS -> if (garden.residents(zone).isNotEmpty()) stringResource(R.string.zone_eggs_faster) else null
    GardenZone.GYM -> stringResource(R.string.zone_coming_soon)
    GardenZone.MEADOW -> null
}

@Composable
private fun ZoneCard(zone: GardenZone, residents: List<CaughtPokemon>, status: String?, onClick: () -> Unit) {
    val context = LocalContext.current
    val color = LocalColorSprites.current
    Card(
        onClick = onClick,
        enabled = zone.open,
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.78f)
        ) {
            Image(
                bitmap = ImageBitmap.imageResource(
                    if (zone == GardenZone.ORCHARD) {
                        R.drawable.garden_scene_orchard
                    } else {
                        GardenCatalog.drawable(context, "bg", zone.background)
                    }
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.None,
                colorFilter = if (color && zone.open) null else ColorFilter.colorMatrix(grayscaleMatrix),
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (zone.open) 1f else 0.55f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
            )
            if (!zone.open) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(AppSizes.spacingMedium),
                verticalArrangement = Arrangement.spacedBy(AppSizes.spacingTiny)
            ) {
                Text(
                    text = stringResource(zone.nameRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = stringResource(zone.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (residents.isNotEmpty()) {
                    Row {
                        residents.take(6).forEach { pokemon ->
                            Image(
                                bitmap = ImageBitmap.imageResource(
                                    PokemonSpriteUtils.getColorResourceId(
                                        context,
                                        pokemon.speciesId,
                                        pokemon.isShiny,
                                        pokemon.form
                                    )
                                ),
                                contentDescription = null,
                                filterQuality = FilterQuality.None,
                                colorFilter = if (color) null else ColorFilter.colorMatrix(grayscaleMatrix),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                status?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Picks who lives in a zone, from the whole PC. Tapping brings a Pokémon here or sends it back;
 * one living in another zone moves here. Searching works like in the PC.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResidentPicker(
    zone: GardenZone,
    all: List<CaughtPokemon>,
    team: List<String>,
    garden: GardenState,
    onToggle: (CaughtPokemon) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val color = LocalColorSprites.current
    var query by remember { mutableStateOf("") }
    val here = garden.residents(zone)
    val shown = remember(all, query, here) {
        val matches = PokemonSearch.matcher(query)
        all.filter(matches).sortedWith(
            compareByDescending<CaughtPokemon> {
                it.id in here
            }.thenByDescending { it.level }
        )
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(true)) {
        Column(modifier = Modifier.padding(horizontal = AppSizes.spacingLarge)) {
            Text(
                text = stringResource(R.string.zone_pick_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.zone_residents, here.size, zone.capacity) + " · " +
                    stringResource(R.string.zone_pick_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = AppSizes.spacingSmall)
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.caught_screen_search_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(AppSizes.cardCornerRadius),
                modifier = Modifier.fillMaxWidth()
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .padding(top = AppSizes.spacingSmall)
            ) {
                items(shown, key = { it.id }) { pokemon ->
                    val isHere = pokemon.id in here
                    // Where it lives now: a zone, the Meadow with the team, or the PC
                    val home = garden.zoneOf(pokemon.id)
                        ?: if (pokemon.id in team || pokemon.isTraining) GardenZone.MEADOW else null
                    val enabled = isHere || here.size < zone.capacity
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = enabled) { onToggle(pokemon) }
                            .alpha(if (enabled) 1f else 0.4f)
                            .padding(vertical = AppSizes.spacingTiny)
                    ) {
                        Image(
                            bitmap = ImageBitmap.imageResource(
                                PokemonSpriteUtils.getColorResourceId(
                                    context,
                                    pokemon.speciesId,
                                    pokemon.isShiny,
                                    pokemon.form
                                )
                            ),
                            contentDescription = null,
                            filterQuality = FilterQuality.None,
                            colorFilter = if (color) null else ColorFilter.colorMatrix(grayscaleMatrix),
                            modifier = Modifier.size(44.dp)
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = AppSizes.spacingSmall)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = (pokemon.nickname ?: Pokemon[pokemon.speciesId]?.displayName.orEmpty()) +
                                        if (pokemon.isShiny) " ✨" else "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                GenderSymbol(pokemon, style = MaterialTheme.typography.bodyLarge)
                            }
                            val colors = MaterialTheme.colorScheme
                            Text(
                                text = home?.let { stringResource(it.nameRes) } ?: stringResource(R.string.zone_pc),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isHere) colors.onPrimary else colors.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isHere) colors.primary else colors.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 1.dp)
                            )
                        }
                        PokemonLevelChip(level = pokemon.level)
                        Checkbox(checked = isHere, onCheckedChange = null, enabled = enabled)
                    }
                }
            }
        }
    }
}

/**
 * Where this Pokémon lives: the PC, the Meadow (with the team) or one of the town's zones. It's
 * only ever in one; picking another moves it there. Full places can't be picked.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LivesInRow(pokemon: CaughtPokemon, inTeam: Boolean, teamFull: Boolean, onMove: (GardenZone?) -> Unit) {
    val context = LocalContext.current
    val garden by remember { GardenStore.get(context) }.state.collectAsState()
    val home = garden.zoneOf(pokemon.id) ?: if (inTeam || pokemon.isTraining) GardenZone.MEADOW else null
    val places = listOf<GardenZone?>(null) + GardenZone.entries.filter { it.open }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = AppSizes.spacingLarge, vertical = AppSizes.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
        ) {
            Text(
                text = stringResource(R.string.detail_lives_in),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)) {
                places.forEach { place ->
                    val selected = place == home
                    val full = when (place) {
                        null -> false
                        GardenZone.MEADOW -> teamFull && !inTeam
                        else -> garden.residents(place).size >= place.capacity
                    }
                    FilterChip(
                        selected = selected,
                        enabled = selected || !full,
                        onClick = { if (!selected) onMove(place) },
                        label = { Text(place?.let { stringResource(it.nameRes) } ?: stringResource(R.string.zone_pc)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
            Text(
                text = stringResource(
                    R.string.detail_favorite_flavor,
                    stringResource(Flavor.favoriteOf(pokemon.id).nameRes)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
