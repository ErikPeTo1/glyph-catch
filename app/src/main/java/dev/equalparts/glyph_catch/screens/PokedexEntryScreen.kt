package dev.equalparts.glyph_catch.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppScaffoldWithTopBar
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.LocalColorSprites
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.components.formLabel
import dev.equalparts.glyph_catch.data.DayTime
import dev.equalparts.glyph_catch.data.EvolutionRequirement
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.PokedexData
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PokemonSpecies
import dev.equalparts.glyph_catch.data.Type
import dev.equalparts.glyph_catch.data.TypeChart
import dev.equalparts.glyph_catch.data.displayName
import dev.equalparts.glyph_catch.data.evolutionStages
import dev.equalparts.glyph_catch.data.nameRes
import dev.equalparts.glyph_catch.data.requiredGender
import dev.equalparts.glyph_catch.data.types
import dev.equalparts.glyph_catch.screens.garden.AnimatedPokemonSprite
import dev.equalparts.glyph_catch.util.PokemonCries
import dev.equalparts.glyph_catch.util.PokemonSpriteUtils
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * A Pokédex entry, like in the games: the animated sprite (male and female, shiny once you've
 * caught one, forms you've found), its cry, category and description, types, height, weight,
 * gender ratio, type matchups and evolution line. Swipe sideways through every species you've
 * registered; tapping one of its evolutions you've registered jumps to it.
 */
@Composable
fun PokedexEntryScreen(db: PokemonDatabase, speciesId: Int, onSeeInPc: (Int) -> Unit, onNavigateUp: () -> Unit) {
    val dao = remember(db) { db.pokemonDao() }
    val registered by dao.watchCaughtSpeciesIds().collectAsStateWithLifecycle(null)
    val shinies by dao.watchShinySpeciesIds().collectAsStateWithLifecycle(emptyList())
    val ids = registered?.sorted() ?: return
    // Opened from the grid it's always registered; if not, show it on its own
    val pages = if (speciesId in ids) ids else listOf(speciesId)
    val pager = rememberPagerState(initialPage = pages.indexOf(speciesId).coerceAtLeast(0)) { pages.size }
    val scope = rememberCoroutineScope()
    val registeredSet = remember(ids) { ids.toSet() }
    val shinySet = remember(shinies) { shinies.toSet() }

    AppScaffoldWithTopBar(title = stringResource(R.string.pokedex_title), onBackClick = onNavigateUp) { padding ->
        HorizontalPager(
            state = pager,
            key = { pages[it] },
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            val id = pages[page]
            val forms by dao.watchFormsFound(id).collectAsStateWithLifecycle(emptyList())
            PokedexEntryContent(
                speciesId = id,
                registered = registeredSet,
                shinyCaught = id in shinySet,
                forms = forms,
                onOpen = { target ->
                    pages.indexOf(target).takeIf { it >= 0 }?.let { scope.launch { pager.animateScrollToPage(it) } }
                },
                onSeeInPc = { onSeeInPc(id) }
            )
        }
    }
}

@Composable
private fun PokedexEntryContent(
    speciesId: Int,
    registered: Set<Int>,
    shinyCaught: Boolean,
    forms: List<String>,
    onOpen: (Int) -> Unit,
    onSeeInPc: () -> Unit
) {
    val context = LocalContext.current
    val species = Pokemon[speciesId] ?: return
    val entry = remember(speciesId) { PokedexData.entry(context, speciesId) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppSizes.spacingLarge),
        verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)
    ) {
        EntryHero(species = species, entry = entry, shinyCaught = shinyCaught, forms = forms)
        if (entry != null && entry.description.isNotBlank()) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(AppSizes.spacingLarge)
                )
            }
        }
        entry?.let { Measures(it) }
        Matchups(species.types)
        EvolutionLine(speciesId = speciesId, registered = registered, onOpen = onOpen)
        OutlinedButton(onClick = onSeeInPc, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.pokedex_see_in_pc))
        }
    }
}

/**
 * The sprite on its types' colours, with its cry and the looks it has: male and female, shiny
 * (once caught) and the forms you've found. Then number, name, category and types.
 */
@Composable
private fun EntryHero(species: PokemonSpecies, entry: PokedexData.Entry?, shinyCaught: Boolean, forms: List<String>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var female by rememberSaveable(species.id) { mutableStateOf(false) }
    var shiny by rememberSaveable(species.id) { mutableStateOf(false) }
    var form by rememberSaveable(species.id) { mutableStateOf<String?>(null) }
    val colors = species.types.map { it.color() }.let { if (it.size == 1) it + it else it }
    val femaleOnly = entry?.genderRate == 8
    val maleOrFemale = entry?.femaleSprite == true && !femaleOnly

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Brush.verticalGradient(colors.map { it.copy(alpha = 0.55f) } + Color.Transparent))
            ) {
                AnimatedPokemonSprite(
                    speciesId = species.id,
                    shiny = shiny,
                    form = form ?: forms.firstOrNull(),
                    female = female && maleOrFemale,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(180.dp)
                )
                IconButton(
                    onClick = {
                        scope.launch {
                            if (!PokemonCries.play(context, species.id)) {
                                Toast.makeText(context, R.string.pokedex_cry_failed, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.icon_cry),
                        contentDescription = stringResource(R.string.pokedex_cry)
                    )
                }
            }
            Column(
                modifier = Modifier.padding(AppSizes.spacingLarge),
                verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
            ) {
                // The looks it has, only when there's more than one
                if (maleOrFemale || shinyCaught || forms.size > 1) {
                    LooksRow(
                        maleOrFemale = maleOrFemale,
                        female = female,
                        onFemale = { female = it },
                        shinyCaught = shinyCaught,
                        shiny = shiny,
                        onShiny = { shiny = it },
                        forms = forms,
                        form = form,
                        onForm = { form = it }
                    )
                }
                Text(
                    text = stringResource(R.string.pokedex_entry_number, species.id),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = species.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (!entry?.category.isNullOrBlank()) {
                    Text(
                        text = entry.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)) {
                    species.types.forEach { TypeBadge(it) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LooksRow(
    maleOrFemale: Boolean,
    female: Boolean,
    onFemale: (Boolean) -> Unit,
    shinyCaught: Boolean,
    shiny: Boolean,
    onShiny: (Boolean) -> Unit,
    forms: List<String>,
    form: String?,
    onForm: (String?) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall),
        verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
    ) {
        if (maleOrFemale) {
            LookChip(text = "♂", selected = !female) { onFemale(false) }
            LookChip(text = "♀", selected = female) { onFemale(true) }
        }
        if (shinyCaught) LookChip(text = "✨", selected = shiny) { onShiny(!shiny) }
        if (forms.size > 1) {
            forms.forEach { option ->
                LookChip(text = formLabel(option), selected = (form ?: forms.first()) == option) { onForm(option) }
            }
        }
    }
}

@Composable
private fun LookChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

/** A type in its colour, like in the games. */
@Composable
private fun TypeBadge(type: Type, suffix: String? = null) {
    Text(
        text = stringResource(type.nameRes()) + suffix?.let { " $it" }.orEmpty(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(type.color())
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun EntrySection(title: String, content: @Composable () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(AppSizes.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

/** Height, weight and how often it's male or female. */
@Composable
private fun Measures(entry: PokedexData.Entry) {
    val locale = Locale.getDefault()
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(AppSizes.spacingLarge)) {
            Measure(
                label = stringResource(R.string.pokedex_height),
                value = String.format(locale, "%.1f m", entry.heightMeters),
                modifier = Modifier.weight(1f)
            )
            Measure(
                label = stringResource(R.string.pokedex_weight),
                value = String.format(locale, "%.1f kg", entry.weightKg),
                modifier = Modifier.weight(1f)
            )
            Measure(
                label = stringResource(R.string.pokedex_gender),
                value = if (entry.genderRate < 0) {
                    stringResource(R.string.pokedex_genderless)
                } else {
                    val female = entry.genderRate * 12.5f
                    "♂ ${percent(100f - female, locale)} · ♀ ${percent(female, locale)}"
                },
                modifier = Modifier.weight(1.4f)
            )
        }
    }
}

private fun percent(value: Float, locale: Locale): String =
    if (value % 1f == 0f) String.format(locale, "%d %%", value.toInt()) else String.format(locale, "%.1f %%", value)

@Composable
private fun Measure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

/** Which attack types hit it hard, which barely and which not at all. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Matchups(types: List<Type>) {
    val against = remember(types) { TypeChart.against(types) }
    val groups = listOf(
        R.string.pokedex_weak_to to against.filterValues { it > 1f }.toList().sortedByDescending { it.second },
        R.string.pokedex_resists to against.filterValues { it > 0f && it < 1f }.toList().sortedBy { it.second },
        R.string.pokedex_immune_to to against.filterValues { it == 0f }.toList()
    ).filter { it.second.isNotEmpty() }
    EntrySection(title = stringResource(R.string.pokedex_matchups)) {
        groups.forEach { (title, list) ->
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall),
                verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
            ) {
                list.forEach { (type, times) -> TypeBadge(type, suffix = multiplier(times)) }
            }
        }
    }
}

private fun multiplier(times: Float): String? = when (times) {
    0f -> null
    0.25f -> "×¼"
    0.5f -> "×½"
    else -> "×${times.toInt()}"
}

/**
 * The whole family by stages. Species you've registered show their sprite and open their entry;
 * the rest are silhouettes, like an unseen Pokédex entry.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EvolutionLine(speciesId: Int, registered: Set<Int>, onOpen: (Int) -> Unit) {
    val stages = remember(speciesId) { evolutionStages(speciesId) }
    EntrySection(title = stringResource(R.string.detail_section_evolution)) {
        if (stages.size == 1) {
            Text(
                text = stringResource(R.string.pokedex_no_evolution),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@EntrySection
        }
        stages.forEachIndexed { index, stage ->
            if (index > 0) {
                Text(
                    text = "↓",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall),
                modifier = Modifier.fillMaxWidth()
            ) {
                stage.forEach { member ->
                    EvolutionMember(
                        species = member,
                        known = member.id in registered,
                        current = member.id == speciesId,
                        requirement = if (index > 0) {
                            listOfNotNull(
                                requirementLabel(member.evolutionRequirement),
                                requiredGender(member.id)?.symbol
                            ).joinToString(" · ")
                        } else {
                            null
                        },
                        onClick = { onOpen(member.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EvolutionMember(
    species: PokemonSpecies,
    known: Boolean,
    current: Boolean,
    requirement: String?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val color = LocalColorSprites.current
    val shape = RoundedCornerShape(12.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(92.dp)
            .clip(shape)
            .then(
                if (current) {
                    Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), shape)
                } else {
                    Modifier
                }
            )
            .clickable(enabled = known && !current, onClick = onClick)
            .padding(vertical = AppSizes.spacingSmall)
    ) {
        Image(
            bitmap = ImageBitmap.imageResource(PokemonSpriteUtils.getColorResourceId(context, species.id, false, null)),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            colorFilter = when {
                !known -> ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                !color -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                else -> null
            },
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = if (known) species.displayName else "???",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        requirement?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun requirementLabel(requirement: EvolutionRequirement?): String? = when (requirement) {
    is EvolutionRequirement.Level -> stringResource(R.string.pokedex_evolution_level, requirement.level)
    is EvolutionRequirement.Stone -> stringResource(requirement.item.nameRes())
    EvolutionRequirement.Trade -> stringResource(Item.LINKING_CORD.nameRes())
    is EvolutionRequirement.Friendship -> stringResource(
        when (requirement.time) {
            DayTime.DAY -> R.string.pokedex_evolution_friendship_day
            DayTime.NIGHT -> R.string.pokedex_evolution_friendship_night
            null -> R.string.pokedex_evolution_friendship
        }
    )
    null -> null
}
