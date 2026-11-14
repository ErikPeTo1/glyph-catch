package dev.equalparts.glyph_catch.screens.garden

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.LocalColorSprites
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.components.GenderSymbol
import dev.equalparts.glyph_catch.data.BerryBlenderGame
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.Flavor
import dev.equalparts.glyph_catch.data.FriendshipDaily
import dev.equalparts.glyph_catch.data.GardenCatalog
import dev.equalparts.glyph_catch.data.GardenStore
import dev.equalparts.glyph_catch.data.Kitchen
import dev.equalparts.glyph_catch.data.PokeblockColor
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.displayName
import dev.equalparts.glyph_catch.ndotFontFamily
import dev.equalparts.glyph_catch.util.PokemonSpriteUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The room, in its own pixels (tools: glyphcatch-work/emerald/kitchen2.js). */
private object KitchenRoom {
    const val WIDTH = 128f
    const val HEIGHT = 192f

    /** Where you cook: the fridge, the sink and stove and the cupboard along the wall. */
    fun isCounter(x: Float, y: Float) = x < 80f && y in 12f..52f

    /** The TV in the corner. */
    fun isTv(x: Float, y: Float) = x >= 108f && y in 28f..80f

    /** Feet on the doormat. */
    val DOOR = 64f to 188f

    /** Feet around the table: two on each side. */
    val SEATS = listOf(38f to 116f, 90f to 116f, 38f to 132f, 90f to 132f)

    /** Each Pokéblock on the table, near its seat. */
    val BLOCKS = listOf(56f to 106f, 72f to 106f, 56f to 118f, 72f to 118f)
}

private enum class Cooking { PICK, BERRIES, BLEND }

private const val WALK_MILLIS = 1_400
private const val EAT_MILLIS = 3_000

/** Who sits where and what they're eating, while they eat. */
private class Meal(val pokemon: List<CaughtPokemon>, val block: PokeblockColor, val gains: List<Int>)

/**
 * The house's kitchen: tap the counter to make Pokéblocks in the Berry Blender for up to four
 * Pokémon, who come in, eat at the table and go. The TV has a tip a day you won't hear anywhere else.
 */
@Composable
internal fun KitchenScene(
    db: PokemonDatabase,
    preferencesManager: PreferencesManager,
    all: List<CaughtPokemon>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val color = LocalColorSprites.current
    val gray = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    val scope = rememberCoroutineScope()
    val store = remember { GardenStore.get(context) }
    val garden by store.state.collectAsState()
    val daily = remember { FriendshipDaily(context) }
    var cooking by remember { mutableStateOf<Cooking?>(null) }
    val eaters = remember { mutableStateListOf<CaughtPokemon>() }
    val berries = remember { mutableStateListOf<Flavor>() }
    var meal by remember { mutableStateOf<Meal?>(null) }
    var tv by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val noBerries = stringResource(R.string.kitchen_no_berries)
    val busy = stringResource(R.string.kitchen_busy)

    LaunchedEffect(message) {
        if (message != null) {
            delay(4_000)
            message = null
        }
    }

    fun serve(maxRpm: Float) {
        val mix = berries.toList()
        val block = Kitchen.color(mix, maxRpm)
        val guests = eaters.toList()
        val gains = guests.map { Kitchen.friendship(Flavor.favoriteOf(it.id), mix, maxRpm) }
        // The berries go into the Blender and the friendship counts right away, even if you leave
        store.update { state -> mix.fold(state) { s, flavor -> s.withBerry(flavor, -1) } }
        scope.launch {
            withContext(Dispatchers.IO) {
                guests.zip(gains).forEach { (pokemon, gain) ->
                    if (daily.eatSweet(pokemon.id)) db.pokemonDao().addFriendship(pokemon.id, gain)
                }
            }
        }
        cooking = null
        meal = Meal(guests, block, gains)
        message = context.getString(R.string.kitchen_made, context.getString(block.nameRes)) + "\n" +
            guests.zip(gains).joinToString(" · ") { (pokemon, gain) ->
                context.getString(R.string.kitchen_gain, pokemon.shownName(), gain)
            }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val crop = remember(maxWidth, maxHeight) {
            SceneCrop(maxWidth.value, maxHeight.value, KitchenRoom.WIDTH, KitchenRoom.HEIGHT, fit = true)
        }
        Image(
            bitmap = ImageBitmap.imageResource(R.drawable.garden_scene_kitchen),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None,
            colorFilter = if (color) null else gray,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(crop, meal) {
                    detectTapGestures { tap ->
                        val x = crop.toImageX(tap.x / density)
                        val y = crop.toImageY(tap.y / density)
                        when {
                            KitchenRoom.isTv(x, y) -> tv = true
                            !KitchenRoom.isCounter(x, y) -> Unit
                            meal != null -> message = busy
                            Flavor.entries.none { garden.berries(it) > 0 } -> message = noBerries
                            else -> {
                                eaters.clear()
                                berries.clear()
                                cooking = Cooking.PICK
                            }
                        }
                    }
                }
        )

        meal?.let { current ->
            current.pokemon.forEachIndexed { index, pokemon ->
                key(pokemon.id) {
                    Guest(
                        pokemon = pokemon,
                        seat = KitchenRoom.SEATS[index],
                        block = KitchenRoom.BLOCKS[index] to current.block,
                        crop = crop,
                        onGone = { if (index == current.pokemon.lastIndex) meal = null }
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .zIndex(3f)
                .padding(AppSizes.spacingMedium)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = Color.White
                )
            }
            Text(
                text = stringResource(R.string.zone_kitchen),
                fontFamily = ndotFontFamily,
                style = MaterialTheme.typography.headlineMedium.copy(
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 6f)
                ),
                color = Color.White
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(4f)
                .padding(horizontal = AppSizes.spacingLarge, vertical = AppSizes.spacingSmall),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            message?.let { Overlay(text = it) }
            if (meal == null && message == null) Overlay(text = stringResource(R.string.kitchen_hint))
        }
    }

    when (cooking) {
        Cooking.PICK -> GuestPicker(
            all = all.filter { Kitchen.canCome(garden, it.id) },
            ate = { daily.ateSweet(it) },
            max = minOf(Kitchen.SEATS, Flavor.entries.sumOf { garden.berries(it) }),
            chosen = eaters,
            onNext = { cooking = Cooking.BERRIES },
            onDismiss = { cooking = null }
        )
        Cooking.BERRIES -> BerryChoice(
            needed = eaters.size,
            stock = { garden.berries(it) },
            chosen = berries,
            onBlend = { cooking = Cooking.BLEND },
            onDismiss = { cooking = null }
        )
        Cooking.BLEND -> BlenderDialog(
            player = preferencesManager.playerName ?: stringResource(R.string.kitchen_you),
            helpers = eaters.take(BerryBlenderGame.MARKERS - 1).map { it.shownName() },
            berries = berries.toList(),
            onServe = ::serve,
            onDismiss = { cooking = null }
        )
        null -> Unit
    }

    if (tv) {
        AlertDialog(
            onDismissRequest = { tv = false },
            title = { Text(stringResource(R.string.kitchen_tv_title)) },
            text = { Text(stringResource(Kitchen.tipOfDay())) },
            confirmButton = { TextButton(onClick = { tv = false }) { Text(stringResource(android.R.string.ok)) } }
        )
    }
}

private fun CaughtPokemon.shownName(): String = nickname ?: Pokemon[speciesId]?.displayName.orEmpty()

@Composable
private fun key(id: String, content: @Composable () -> Unit) = androidx.compose.runtime.key(id) { content() }

/** A Pokémon coming in from the door, eating its Pokéblock at the table, and going back out. */
@Composable
private fun Guest(
    pokemon: CaughtPokemon,
    seat: Pair<Float, Float>,
    block: Pair<Pair<Float, Float>, PokeblockColor>,
    crop: SceneCrop,
    onGone: () -> Unit
) {
    val context = LocalContext.current
    val color = LocalColorSprites.current
    val icon by androidx.compose.runtime.produceState<ImageBitmap?>(null, pokemon.id) {
        value = PokemonIcons.load(context, pokemon.speciesId, pokemon.form)
    }
    val walk = remember { Animatable(0f) }
    var eating by remember { mutableStateOf(false) }
    var bob by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        walk.animateTo(1f, tween(WALK_MILLIS, easing = LinearEasing))
        eating = true
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < EAT_MILLIS) {
            delay(250)
            bob++
        }
        eating = false
        walk.animateTo(0f, tween(WALK_MILLIS, easing = LinearEasing))
        onGone()
    }
    val (doorX, doorY) = KitchenRoom.DOOR
    val x = doorX + (seat.first - doorX) * walk.value
    val y = doorY + (seat.second - doorY) * walk.value
    val scale = crop.scale
    val gray = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    val density = LocalDensity.current.density

    if (eating) {
        val (bx, by) = block.first
        Image(
            bitmap = ImageBitmap.imageResource(block.second.drawable),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            colorFilter = if (color) null else gray,
            modifier = Modifier
                .zIndex(1f)
                .offset {
                    IntOffset(
                        ((crop.screenX(bx) - 4 * scale) * density).toInt(),
                        (
                            (crop.screenY(by) - 4 * scale) *
                                density
                            ).toInt()
                    )
                }
                .size((8 * scale).dp)
                .alpha(if (bob > 8) 0f else 1f)
        )
    }
    icon?.let { bitmap ->
        val hop = if (eating && bob % 2 == 1) 1f else 0f
        Image(
            bitmap = bitmap,
            contentDescription = null,
            filterQuality = FilterQuality.None,
            colorFilter = if (color) null else gray,
            modifier = Modifier
                .zIndex(2f)
                .offset {
                    IntOffset(
                        ((crop.screenX(x) - 16 * scale) * density).toInt(),
                        ((crop.screenY(y - hop) - 28 * scale) * density).toInt()
                    )
                }
                .size((32 * scale).dp)
        )
    }
    if (eating && bob in 6..11) {
        Image(
            bitmap = ImageBitmap.imageResource(R.drawable.garden_emote_heart),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            colorFilter = if (color) null else gray,
            modifier = Modifier
                .zIndex(3f)
                .offset {
                    IntOffset(
                        ((crop.screenX(x) - 8 * scale) * density).toInt(),
                        (
                            (crop.screenY(y) - 44 * scale) *
                                density
                            ).toInt()
                    )
                }
                .size((16 * scale).dp)
        )
    }
}

/** Who's coming: anyone not living in a zone, up to four, one berry each. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuestPicker(
    all: List<CaughtPokemon>,
    ate: (String) -> Boolean,
    max: Int,
    chosen: MutableList<CaughtPokemon>,
    onNext: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val color = LocalColorSprites.current
    var query by remember { mutableStateOf("") }
    val shown = remember(all, query) {
        val matches = dev.equalparts.glyph_catch.components.PokemonSearch.matcher(query)
        all.filter(matches).sortedWith(compareBy<CaughtPokemon> { ate(it.id) }.thenByDescending { it.level })
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(true)) {
        Column(modifier = Modifier.padding(horizontal = AppSizes.spacingLarge)) {
            Text(
                text = stringResource(R.string.kitchen_pick_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.kitchen_pick_hint, chosen.size, max),
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
                    .height(400.dp)
                    .padding(top = AppSizes.spacingSmall)
            ) {
                items(shown, key = { it.id }) { pokemon ->
                    val picked = chosen.any { it.id == pokemon.id }
                    val full = ate(pokemon.id)
                    val enabled = picked || (!full && chosen.size < max)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = enabled) {
                                if (picked) chosen.removeAll { it.id == pokemon.id } else chosen += pokemon
                            }
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
                            colorFilter = if (color) {
                                null
                            } else {
                                ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                            },
                            modifier = Modifier.size(44.dp)
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = AppSizes.spacingSmall)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = pokemon.shownName() + if (pokemon.isShiny) " ✨" else "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                GenderSymbol(pokemon, style = MaterialTheme.typography.bodyLarge)
                            }
                            Text(
                                text = if (full) {
                                    stringResource(R.string.kitchen_pick_ate)
                                } else {
                                    stringResource(
                                        R.string.detail_favorite_flavor,
                                        stringResource(Flavor.favoriteOf(pokemon.id).nameRes)
                                    )
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Checkbox(checked = picked, onCheckedChange = null, enabled = enabled)
                    }
                }
            }
            Button(
                onClick = onNext,
                enabled = chosen.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSizes.spacingMedium)
            ) {
                Text(stringResource(R.string.kitchen_next))
            }
        }
    }
}

/** One berry per guest, from the chest: tap a berry to add it, tap it in the pot to take it out. */
@Composable
private fun BerryChoice(
    needed: Int,
    stock: (Flavor) -> Int,
    chosen: MutableList<Flavor>,
    onBlend: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.kitchen_berries_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)) {
                Text(
                    text = stringResource(R.string.kitchen_berries_hint, chosen.size, needed),
                    style = MaterialTheme.typography.bodyMedium
                )
                // The pot
                Row(horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)) {
                    repeat(needed) { index ->
                        val flavor = chosen.getOrNull(index)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable(enabled = flavor != null) { chosen.removeAt(index) }
                        ) {
                            if (flavor != null) BerryIcon(context, flavor, Modifier.size(36.dp))
                        }
                    }
                }
                // The chest
                Row(horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)) {
                    Flavor.entries.forEach { flavor ->
                        val left = stock(flavor) - chosen.count { it == flavor }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = left > 0 && chosen.size < needed) { chosen += flavor }
                                .alpha(if (left > 0) 1f else 0.35f)
                                .padding(vertical = AppSizes.spacingTiny)
                        ) {
                            BerryIcon(context, flavor, Modifier.size(32.dp))
                            Text(text = "×$left", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = stringResource(flavor.nameRes),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onBlend, enabled = chosen.size == needed) {
                Text(stringResource(R.string.kitchen_blend))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } }
    )
}

@Composable
private fun BerryIcon(context: android.content.Context, flavor: Flavor, modifier: Modifier) {
    val color = LocalColorSprites.current
    Image(
        bitmap = ImageBitmap.imageResource(GardenCatalog.drawable(context, "berry", flavor.berry)),
        contentDescription = null,
        filterQuality = FilterQuality.None,
        colorFilter = if (color) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
        modifier = modifier
    )
}

/** A score mark by a marker, for a moment. */
private class Mark(val marker: Int, val hit: BerryBlenderGame.Hit, val until: Long)

/**
 * The Berry Blender from Emerald, with its own pieces: tap when the spinning arrow passes your
 * marker (top left). The guests press at theirs. Hits speed it up and fill the bar; when it's full
 * the Pokéblock is done, better the faster it went.
 */
@Composable
private fun BlenderDialog(
    player: String,
    helpers: List<String>,
    berries: List<Flavor>,
    onServe: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val game = remember { BerryBlenderGame(players = 1 + helpers.size) }
    var frame by remember { mutableIntStateOf(0) }
    val marks = remember { mutableStateListOf<Mark>() }
    var flash by remember { mutableStateOf(0L) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (!game.done) {
            withFrameNanos { now ->
                val frames = ((now - last) / 1_000_000_000f * 60f).coerceAtMost(4f)
                last = now
                game.step(frames).forEach { (marker, hit) ->
                    marks += Mark(marker, hit, System.currentTimeMillis() + 400)
                }
                marks.removeAll { it.until < System.currentTimeMillis() }
                frame++
            }
        }
        finished = true
    }

    Dialog(
        onDismissRequest = { if (!finished) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .safeDrawingPadding()
                .padding(AppSizes.spacingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(240f / 160f)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            if (game.done) return@detectTapGestures
                            marks += Mark(0, game.press(), System.currentTimeMillis() + 400)
                            flash = System.currentTimeMillis() + 160
                        }
                    }
            ) {
                // Read every frame so the arrow keeps turning
                frame.let { }
                val s = maxWidth.value / 240f
                Pixels(R.drawable.kitchen_blender_ring, 0f, 0f, 240f, 160f, s)
                Pixels(
                    R.drawable.kitchen_blender_center,
                    72f,
                    32f,
                    96f,
                    96f,
                    s,
                    rotation =
                    -game.arrowPos / BerryBlenderGame.TURN * 360f
                )
                // The progress bar along the top, and the speed in its box at the bottom
                Box(
                    modifier = Modifier
                        .offset((88 * s).dp, (3 * s).dp)
                        .size((64 * s * game.progress / BerryBlenderGame.MAX_PROGRESS).dp, (8 * s).dp)
                        .background(Color(0xFFF8D030))
                )
                Text(
                    text = "%06.2f".format(game.rpm),
                    color = Color(0xFF5A4A9C),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = (7 * s).sp,
                    modifier = Modifier
                        .offset((99 * s).dp, (134 * s).dp)
                )
                // The markers: yours and the guests'
                val names = listOf(player) + helpers
                names.forEachIndexed { marker, name ->
                    val (mx, my) = MARKER_POS[marker]
                    val lit = marker == 0 && flash > System.currentTimeMillis()
                    Marker(marker, mx, my, s, lit)
                    val (nx, ny) = NAME_POS[marker]
                    Text(
                        text = name,
                        color = Color.White,
                        fontSize = (8 * s).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .offset((nx * s).dp, (ny * s).dp)
                            .size((58 * s).dp, (14 * s).dp)
                    )
                }
                marks.forEach { mark ->
                    val (mx, my) = MARKER_POS[mark.marker]
                    val (qx, qy) = QUADRANT[mark.marker]
                    val frameIndex = when (mark.hit) {
                        BerryBlenderGame.Hit.GOOD -> 0
                        BerryBlenderGame.Hit.MISS -> 1
                        BerryBlenderGame.Hit.BEST -> if ((mark.until / 80) % 2 == 0L) 2 else 3
                    }
                    Sheet(R.drawable.kitchen_blender_marks, frameIndex, 16, mx - 10 * qx - 8, my - 10 * qy - 8, s)
                }
            }
            if (!finished) {
                Text(
                    text = stringResource(R.string.kitchen_blender_hint),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                val block = remember { Kitchen.color(berries, game.maxRpm) }
                Pixels(block.drawable, 0f, 0f, 8f, 8f, 8f)
                Text(
                    text = stringResource(R.string.kitchen_made, stringResource(block.nameRes)),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.kitchen_rpm, "%.2f".format(game.maxRpm)),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = { onServe(game.maxRpm) }) { Text(stringResource(R.string.kitchen_serve)) }
            }
            Spacer(modifier = Modifier.weight(1f).heightIn(min = 0.dp))
        }
    }
}

/** Marker spots on the 240x160 screen: top left (you), top right, bottom left, bottom right. */
private val MARKER_POS = listOf(72f to 32f, 168f to 32f, 72f to 128f, 168f to 128f)
private val QUADRANT = listOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f)

/** The white name boxes on each side. */
private val NAME_POS = listOf(10f to 49f, 177f to 49f, 10f to 97f, 177f to 97f)

/** A marker pointing at the middle: the same picture flipped for each corner, lit when you press. */
@Composable
private fun Marker(marker: Int, x: Float, y: Float, s: Float, lit: Boolean) {
    val (qx, qy) = QUADRANT[marker]
    Sheet(
        R.drawable.kitchen_blender_marker,
        if (lit) 1 else 0,
        32,
        x - 16,
        y - 16,
        s,
        flipX = qx < 0,
        flipY = qy < 0
    )
}

/** Part of a picture, pixel for pixel, at a spot on the 240x160 screen. */
@Composable
private fun Pixels(drawable: Int, x: Float, y: Float, width: Float, height: Float, s: Float, rotation: Float = 0f) {
    val color = LocalColorSprites.current
    Image(
        bitmap = ImageBitmap.imageResource(drawable),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        filterQuality = FilterQuality.None,
        colorFilter = if (color) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
        modifier = Modifier
            .offset((x * s).dp, (y * s).dp)
            .size((width * s).dp, (height * s).dp)
            .rotate(rotation)
    )
}

/** One square frame of a vertical strip, optionally flipped. */
@Composable
private fun Sheet(
    drawable: Int,
    frame: Int,
    side: Int,
    x: Float,
    y: Float,
    s: Float,
    flipX: Boolean = false,
    flipY: Boolean = false
) {
    val color = LocalColorSprites.current
    val bitmap = ImageBitmap.imageResource(drawable)
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .offset((x * s).dp, (y * s).dp)
            .size((side * s).dp)
    ) {
        scale(if (flipX) -1f else 1f, if (flipY) -1f else 1f) {
            drawImage(
                image = bitmap,
                srcOffset = IntOffset(0, frame * side),
                srcSize = IntSize(side, side),
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.None,
                colorFilter = if (color) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            )
        }
    }
}
