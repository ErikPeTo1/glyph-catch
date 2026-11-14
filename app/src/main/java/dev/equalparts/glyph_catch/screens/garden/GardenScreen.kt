package dev.equalparts.glyph_catch.screens.garden

import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.LocalColorSprites
import dev.equalparts.glyph_catch.PokemonLevelChip
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.BerryPlot
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.DecorationKind
import dev.equalparts.glyph_catch.data.Flavor
import dev.equalparts.glyph_catch.data.FriendshipDaily
import dev.equalparts.glyph_catch.data.FriendshipRules
import dev.equalparts.glyph_catch.data.GardenCatalog
import dev.equalparts.glyph_catch.data.GardenGift
import dev.equalparts.glyph_catch.data.GardenState
import dev.equalparts.glyph_catch.data.GardenStore
import dev.equalparts.glyph_catch.data.GardenZone
import dev.equalparts.glyph_catch.data.PlacedDecoration
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.creditTeamFriendship
import dev.equalparts.glyph_catch.data.displayName
import dev.equalparts.glyph_catch.data.usesFemaleSprite
import dev.equalparts.glyph_catch.ndotFontFamily
import dev.equalparts.glyph_catch.screens.PokemonBrowse
import dev.equalparts.glyph_catch.util.PokemonSpriteUtils
import java.time.LocalTime
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where feet can stand: below the fence, above the bottom edge. */
private const val DEFAULT_GROUND_TOP = 0.46f
private const val DEFAULT_GROUND_BOTTOM = 0.95f

/** Where feet can stand in a scene, as fractions of its height: each scene has its own. */
internal class Ground(val top: Float, val bottom: Float, val left: Float = 0f, val right: Float = 1f)

private val LocalGround = staticCompositionLocalOf { Ground(DEFAULT_GROUND_TOP, DEFAULT_GROUND_BOTTOM) }

/**
 * The Orchard's scene, built from Pokémon Emerald's tiles like the Berry Master's garden
 * (tools: glyphcatch-work/emerald/bake.js): its size and the middle of each soil patch, in
 * scene pixels, and where feet can stand (inside its fence).
 */
private object OrchardScene {
    const val WIDTH = 224f
    const val HEIGHT = 416f
    val SOIL = listOf(108.5f, 156.5f, 204.5f, 252.5f, 300.5f).flatMap { y ->
        listOf(47.5f, 111.5f, 175.5f).map { x ->
            x to
                y
        }
    }

    // Inside the fence
    const val GROUND_TOP = 96f
    const val GROUND_BOTTOM = 382f
    const val GROUND_LEFT = 16f
    const val GROUND_RIGHT = 208f
}

/** A zone drawn like an Emerald map: its picture, its size and where feet can stand, in its pixels. */
private class MapScene(
    @param:DrawableRes val drawable: Int,
    val width: Float,
    val height: Float,
    val groundLeft: Float,
    val groundTop: Float,
    val groundRight: Float,
    val groundBottom: Float
)

private val ORCHARD_SCENE = MapScene(
    R.drawable.garden_scene_orchard,
    OrchardScene.WIDTH,
    OrchardScene.HEIGHT,
    OrchardScene.GROUND_LEFT,
    OrchardScene.GROUND_TOP,
    OrchardScene.GROUND_RIGHT,
    OrchardScene.GROUND_BOTTOM
)

/** Lavaridge's hot spring, built big (tools: glyphcatch-work/emerald/springs.js): below the rock wall, pool included. */
private val SPRINGS_SCENE = MapScene(R.drawable.garden_scene_springs, 224f, 416f, 24f, 56f, 220f, 410f)

/** A scene image cropped to fill the screen (or fitted inside it), centered: from its pixels to the screen's (in dp). */
internal class SceneCrop(
    val width: Float,
    val height: Float,
    imageWidth: Float,
    imageHeight: Float,
    /** The whole image, letterboxed, instead of filling the screen. */
    fit: Boolean = false
) {
    /** Dp per scene pixel. */
    val scale = if (fit) {
        min(
            width / imageWidth,
            height / imageHeight
        )
    } else {
        max(width / imageWidth, height / imageHeight)
    }
    private val left = (width - imageWidth * scale) / 2
    private val top = (height - imageHeight * scale) / 2

    /** As fractions of the screen. */
    fun x(px: Float) = screenX(px) / width
    fun y(py: Float) = screenY(py) / height

    /** In dp from the screen's corner, and back. */
    fun screenX(px: Float) = left + px * scale
    fun screenY(py: Float) = top + py * scale
    fun toImageX(dp: Float) = (dp - left) / scale
    fun toImageY(dp: Float) = (dp - top) / scale
}

/** How many sprite pixels fit across (and down) the meadow, so a big Pokémon fills about a third. */
private const val SPRITE_PIXELS_ACROSS = 300f
private const val SPRITE_PIXELS_DOWN = 480f

/** The black-and-white style: the whole Garden in grays. */
private val grayscale = ColorMatrix().apply { setToSaturation(0f) }

/** Throwing: gravity and limits in dp per second, how much of the speed a bounce keeps. */
private const val GRAVITY_DP = 2_600f
private const val MAX_THROW_DP = 2_200f
private const val MAX_THROW = 2.5f
private const val MIN_BOUNCE_DP = 140f
private const val BOUNCINESS = 0.45f
private const val AIR_FRICTION = 0.6f
private const val GROUND_FRICTION = 5f

/** How high a Pokémon is lifted while you carry it. */
private const val LIFT_DP = -14f

/** Emerald's balloons are 16 pixels; the heart shows up more often, they're happy here. */
private const val EMOTE_PIXELS = 16
private val EMOTES = listOf(
    R.drawable.garden_emote_heart,
    R.drawable.garden_emote_heart,
    R.drawable.garden_emote_exclamation,
    R.drawable.garden_emote_question
)

/** What a Pokémon looks like in the Garden, once its sprite has been looked for. */
private class Look(val animated: Drawable?, val icon: ImageBitmap? = null)

/** Party icons are 32 pixels square, in the scene's own pixels. */
private const val ICON_PIXELS = 32

/**
 * The town: a map of its zones, and each zone's scene once you open it. Back goes to the map.
 */
@Composable
fun GardenScreen(db: PokemonDatabase, preferencesManager: PreferencesManager, onPokemonClick: (CaughtPokemon) -> Unit) {
    val context = LocalContext.current
    val caught by remember(db) { db.pokemonDao().watchAllCaught() }.collectAsStateWithLifecycle(null)
    val all = caught.orEmpty()
    val store = remember { GardenStore.get(context) }
    val garden by store.state.collectAsState()
    // Released Pokémon leave their zone, so they don't take up room
    LaunchedEffect(caught) {
        val ids = caught?.map { it.id }?.toSet() ?: return@LaunchedEffect
        store.update { state -> state.copy(zones = state.zones.mapValues { (_, zone) -> zone.filter { it in ids } }) }
    }
    var zoneId by rememberSaveable { mutableStateOf<String?>(null) }
    var inKitchen by rememberSaveable { mutableStateOf(false) }
    // Where the hero stands in town, so it comes back out of the door it went in
    var hero by remember { mutableStateOf(HeroSpot()) }
    val zone = GardenZone.entries.firstOrNull { it.id == zoneId && it.open }
    if (inKitchen) {
        BackHandler { inKitchen = false }
        KitchenScene(db = db, preferencesManager = preferencesManager, all = all, onBack = { inKitchen = false })
        return
    }
    if (zone == null) {
        TownScene(
            hero = hero,
            onHeroMoved = { hero = it },
            onOpen = { zoneId = it.id },
            onKitchen = { inKitchen = true }
        )
        return
    }
    BackHandler { zoneId = null }
    key(zone) {
        ZoneScene(
            zone = zone,
            db = db,
            preferencesManager = preferencesManager,
            all = all,
            onPokemonClick = onPokemonClick,
            onBack = { zoneId = null }
        )
    }
}

/**
 * One zone, filling the screen: its residents wander around on its scene. Tap one to pet it,
 * double tap to see how it's doing, hold it to carry it around. The Meadow has the team and
 * their gifts; the Orchard its berry plots.
 */
@Composable
private fun ZoneScene(
    zone: GardenZone,
    db: PokemonDatabase,
    preferencesManager: PreferencesManager,
    all: List<CaughtPokemon>,
    onPokemonClick: (CaughtPokemon) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember(db) { db.pokemonDao() }
    val store = remember { GardenStore.get(context) }
    val garden by store.state.collectAsState()
    val residents = remember(all, garden.zones) { zoneResidents(zone, all, preferencesManager.team, garden) }
    val daily = remember { FriendshipDaily(context) }
    val scope = rememberCoroutineScope()
    var petTick by remember { mutableIntStateOf(0) }
    val hint = stringResource(if (zone == GardenZone.MEADOW) R.string.garden_hint else zone.descriptionRes)
    var message by remember { mutableStateOf<String?>(hint) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var giftTick by remember { mutableIntStateOf(0) }
    // What each Pokémon in the Meadow left on the grass for you today
    val gifts = remember(residents, giftTick) {
        if (zone != GardenZone.MEADOW) return@remember emptyList()
        residents.mapNotNull { pokemon ->
            if (daily.giftCollected(pokemon.id)) return@mapNotNull null
            GardenCatalog.giftFor(pokemon)?.let { pokemon to it }
        }
    }
    // The chest, and whether it was opened from an empty plot to plant there
    var bagOpen by remember { mutableStateOf(false) }
    var plantingPlot by remember { mutableStateOf<Int?>(null) }
    var picking by remember { mutableStateOf(false) }
    // A berry taken out of the chest, waiting for a Pokémon or a plot
    var hand by remember { mutableStateOf<Flavor?>(null) }
    // Who's busy with which decoration (or plot), so two don't sit on the same cushion
    val claims = remember { mutableStateMapOf<String, String>() }
    // Plots getting watered right now, while the rain plays
    val watering = remember { mutableStateMapOf<Int, Boolean>() }
    val time by produceState(LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(60_000)
        }
    }
    // They go to bed when you do, with the bedtime from the sleep settings
    val asleep by produceState(isAsleep(preferencesManager.bedtimeMinutes)) {
        while (true) {
            value = isAsleep(preferencesManager.bedtimeMinutes)
            delay(60_000)
        }
    }

    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { creditTeamFriendship(context, db) } }
    LaunchedEffect(message) {
        if (message != null) {
            // The how-to is longer, so it stays a little more
            delay(if (message == hint) 5_000 else 2_500)
            message = null
        }
    }
    fun nameOf(pokemon: CaughtPokemon) = pokemon.nickname ?: Pokemon[pokemon.speciesId]?.displayName.orEmpty()

    /** A berry from the chest: more friendship, and twice as much for its favorite flavor. */
    val feed: (CaughtPokemon, Flavor) -> Boolean = feed@{ pokemon, flavor ->
        val name = nameOf(pokemon)
        if (store.state.value.berries(flavor) <= 0) return@feed false
        if (!daily.eatBerry(pokemon.id)) {
            message = context.getString(R.string.garden_fed_enough, name)
            return@feed false
        }
        store.update { it.withBerry(flavor, -1) }
        val favorite = Flavor.favoriteOf(pokemon.id) == flavor
        scope.launch(Dispatchers.IO) {
            dao.addFriendship(pokemon.id, if (favorite) FriendshipRules.FAVORITE_BERRY else FriendshipRules.BERRY)
        }
        message = context.getString(if (favorite) R.string.garden_fed_favorite else R.string.garden_fed, name)
        petTick++
        true
    }

    /** A tap on a Pokémon: the berry in your hand if there's one, a pet otherwise. */
    val pet: (CaughtPokemon) -> Boolean = pet@{ pokemon ->
        hand?.let { flavor ->
            // If it's had enough for today, the berry stays in your hand for someone else
            val eaten = feed(pokemon, flavor)
            if (eaten) hand = null
            return@pet eaten
        }
        val petted = daily.pet(pokemon.id)
        if (petted) {
            scope.launch(Dispatchers.IO) { dao.addFriendship(pokemon.id, 1) }
        } else {
            message = context.getString(R.string.garden_pets_done, nameOf(pokemon))
        }
        petTick++
        petted
    }

    fun collect(pokemon: CaughtPokemon, gift: GardenGift) {
        if (!daily.collectGift(pokemon.id)) return
        store.update { it.withGift(gift) }
        message = when (gift) {
            is GardenGift.Berry -> context.getString(R.string.garden_gift, nameOf(pokemon))
            is GardenGift.Decoration -> context.getString(R.string.garden_gift_decoration, nameOf(pokemon))
        }
        giftTick++
    }

    /** Waters a thirsty plot: by hand, or by a Water-type ([by]) living in the Garden. */
    fun water(index: Int, by: CaughtPokemon?) {
        if (index in watering || store.state.value.watering(index) == null) return
        store.update { it.watering(index) ?: it }
        watering[index] = true
        message = if (by != null) {
            context.getString(R.string.garden_watered_by, nameOf(by))
        } else {
            context.getString(R.string.garden_watered)
        }
    }

    fun tapPlot(index: Int) {
        val plot = garden.plots.getOrNull(index)
        when {
            plot == null && hand != null -> {
                store.update { it.planting(index, hand!!) ?: it }
                message = context.getString(R.string.garden_planted)
                hand = null
            }
            plot == null -> {
                plantingPlot = index
                bagOpen = true
            }
            plot.ripe -> {
                store.update { it.harvesting(index) ?: it }
                message = context.getString(R.string.garden_harvest, plot.yield)
            }
            plot.thirsty -> water(index, by = null)
            else -> message = context.getString(R.string.garden_growing, (plot.progress * 100).toInt())
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { selectedId = null } }
    ) {
        val sceneWidth = maxWidth
        val sceneHeight = maxHeight
        val color = LocalColorSprites.current
        val orchard = zone == GardenZone.ORCHARD
        val mapScene = when (zone) {
            GardenZone.ORCHARD -> ORCHARD_SCENE
            GardenZone.SPRINGS -> SPRINGS_SCENE
            else -> null
        }
        val crop = remember(sceneWidth, sceneHeight, mapScene) {
            SceneCrop(
                sceneWidth.value,
                sceneHeight.value,
                mapScene?.width ?: OrchardScene.WIDTH,
                mapScene?.height ?: OrchardScene.HEIGHT
            )
        }
        val ground = if (mapScene != null) {
            Ground(
                top = crop.y(mapScene.groundTop),
                bottom = crop.y(mapScene.groundBottom),
                left = crop.x(mapScene.groundLeft),
                right = crop.x(mapScene.groundRight)
            )
        } else {
            LocalGround.current
        }
        // Each soil patch's middle, as fractions of the scene
        val plotSpots = remember(crop) { OrchardScene.SOIL.map { (x, y) -> crop.x(x) to crop.y(y) } }
        CompositionLocalProvider(LocalGround provides ground) {
            Image(
                bitmap = ImageBitmap.imageResource(
                    if (mapScene != null) {
                        mapScene.drawable
                    } else {
                        GardenCatalog.drawable(
                            context,
                            "bg",
                            zone.background
                        )
                    }
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.None,
                colorFilter = if (color) null else ColorFilter.colorMatrix(grayscale),
                modifier = Modifier.fillMaxSize()
            )
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
                    text = stringResource(zone.nameRes),
                    fontFamily = ndotFontFamily,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 6f)
                    ),
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .zIndex(3f)
                    .padding(top = 72.dp, end = AppSizes.spacingLarge),
                horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
            ) {
                if (zone.picksResidents) {
                    ScenePill(
                        text = stringResource(R.string.zone_residents, residents.size, zone.capacity),
                        onClick = { picking = true }
                    )
                }
                ScenePill(text = stringResource(R.string.garden_bag), icon = R.drawable.icon_bag) {
                    plantingPlot = null
                    bagOpen = true
                }
            }

            if (orchard) {
                garden.plots.forEachIndexed { index, plot ->
                    val (x, y) = plotSpots[index]
                    PlotView(
                        plot = plot,
                        x = x,
                        y = y,
                        sceneWidth = sceneWidth,
                        sceneHeight = sceneHeight,
                        scale = crop.scale,
                        watering = index in watering,
                        onWatered = { watering.remove(index) },
                        onTap = { tapPlot(index) }
                    )
                }
            }

            gifts.forEach { (pokemon, gift) ->
                key(pokemon.id) {
                    GroundGift(
                        gift = gift,
                        spot = remember(pokemon.id) { GardenCatalog.giftSpot(pokemon.id) },
                        sceneWidth = sceneWidth,
                        sceneHeight = sceneHeight,
                        onCollect = { collect(pokemon, gift) }
                    )
                }
            }

            var spar by remember { mutableStateOf<Spar?>(null) }
            LaunchedEffect(residents.size, asleep) {
                spar = null
                // No play-fighting among the berry trees
                while (!asleep && mapScene == null && residents.size >= 2) {
                    delay(Random.nextLong(15_000, 35_000))
                    // Nobody busy on a cushion or in a tent gets dragged into a fight
                    val free = residents.filter { it.id !in claims.values }
                    if (free.size < 2) continue
                    val (left, right) = free.shuffled().take(2)
                    spar = Spar(
                        left = left.id,
                        right = right.id,
                        x = 0.25f + Random.nextFloat() * 0.5f,
                        y = ground.top + 0.1f + Random.nextFloat() * (ground.bottom - ground.top - 0.2f)
                    )
                    delay(SPAR_MILLIS)
                    spar = null
                }
            }

            residents.forEachIndexed { index, pokemon ->
                key(pokemon.id) {
                    Wanderer(
                        pokemon = pokemon,
                        slot = index,
                        count = residents.size,
                        sceneWidth = sceneWidth,
                        sceneHeight = sceneHeight,
                        night = asleep,
                        spar = spar,
                        decorations = emptyList(),
                        claims = claims,
                        plotSpots = plotSpots,
                        thirstyPlots = if (orchard) {
                            garden.plots.indices.filter { garden.plots[it]?.thirsty == true && it !in watering }
                        } else {
                            emptyList()
                        },
                        onWater = { water(it, by = pokemon) },
                        onPet = pet,
                        onHold = { selectedId = pokemon.id },
                        iconScale = if (mapScene != null) crop.scale else null
                    )
                }
            }

            skyTint(time, asleep)?.let { tint ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f)
                        .background(if (color) tint else Color.Black.copy(alpha = tint.alpha * 0.7f))
                )
            }

            // Tips and messages sit together just above the bottom bar, out of the way
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(4f)
                    .padding(horizontal = AppSizes.spacingLarge, vertical = AppSizes.spacingSmall),
                verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (residents.isEmpty()) {
                    Overlay(
                        text = stringResource(
                            if (zone == GardenZone.MEADOW) R.string.garden_empty else R.string.zone_empty
                        )
                    )
                }
                val held = hand
                if (held != null) {
                    BerryInHand(flavor = held, onCancel = { hand = null })
                } else {
                    message?.let { Overlay(text = it) }
                }
            }

            if (bagOpen) {
                GardenBag(
                    state = garden,
                    planting = plantingPlot != null,
                    onBerry = { flavor ->
                        val plot = plantingPlot
                        if (plot != null) {
                            store.update { it.planting(plot, flavor) ?: it }
                            message = context.getString(R.string.garden_planted)
                        } else {
                            hand = flavor
                        }
                        plantingPlot = null
                        bagOpen = false
                    },
                    onDismiss = {
                        plantingPlot = null
                        bagOpen = false
                    }
                )
            }

            if (picking) {
                ResidentPicker(
                    zone = zone,
                    all = all,
                    team = preferencesManager.team,
                    garden = garden,
                    onToggle = { pokemon ->
                        val here = pokemon.id in store.state.value.residents(zone)
                        store.update { state ->
                            if (here) {
                                state.leaving(pokemon.id)
                            } else {
                                state.moving(pokemon.id, zone)
                                    ?: state
                            }
                        }
                        // One place at a time: living here means it's no longer in the Meadow with the team
                        if (!here && pokemon.id in store.state.value.residents(zone)) {
                            preferencesManager.team = preferencesManager.team - pokemon.id
                        }
                    },
                    onDismiss = { picking = false }
                )
            }

            residents.firstOrNull { it.id == selectedId }?.let { pokemon ->
                ResidentInfo(
                    pokemon = pokemon,
                    petsLeft = remember(pokemon.id, petTick) { daily.petsLeft(pokemon.id) },
                    garden = garden,
                    berriesLeft = remember(pokemon.id, petTick) { daily.berriesLeft(pokemon.id) },
                    onFeed = { feed(pokemon, it) },
                    onDetails = {
                        PokemonBrowse.ids = residents.map { it.id }
                        onPokemonClick(pokemon)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(4f)
                        .padding(AppSizes.spacingLarge)
                )
            }
        }
    }
}

/**
 * The light over the Garden: warm at dawn and sunset, purple at dusk, dark blue at night (and
 * whenever they're asleep). Nothing during the day.
 */
internal fun skyTint(time: LocalTime, asleep: Boolean): Color? {
    val hour = time.hour + time.minute / 60f
    return when {
        asleep || hour >= 21f || hour < 5.5f -> Color(0xFF0A1440).copy(alpha = 0.45f)
        hour < 7.5f -> Color(0xFFFFA060).copy(alpha = 0.16f)
        hour >= 19.5f -> Color(0xFF4A2A70).copy(alpha = 0.3f)
        hour >= 18f -> Color(0xFFFF8040).copy(alpha = 0.2f)
        else -> null
    }
}

/**
 * The berry trees are baked on a 32×40 canvas (scene pixels) whose point (16, 36) is their foot,
 * which goes on the soil's middle (tools: glyphcatch-work/emerald/bake.js).
 */
private const val TREE_WIDTH = 32f
private const val TREE_HEIGHT = 40f
private const val TREE_FOOT_X = 16f
private const val TREE_FOOT_Y = 36f

/**
 * Soft soil, or a berry growing in it. Tap to plant, water, check on it or pick it. A thirsty
 * one has Emerald's Wailmer Pail over it; watering brings a little of Emerald's rain.
 */
@Composable
private fun PlotView(
    plot: BerryPlot?,
    x: Float,
    y: Float,
    sceneWidth: Dp,
    sceneHeight: Dp,
    scale: Float,
    watering: Boolean,
    onWatered: () -> Unit,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    val color = LocalColorSprites.current
    val filter = if (color) null else ColorFilter.colorMatrix(grayscale)
    // Empty soil is part of the scene; only what grows in it is drawn
    val bitmap = plot?.let { ImageBitmap.imageResource(GardenCatalog.plotDrawable(context, it)) }
    val width = (TREE_WIDTH * scale).dp
    val height = (TREE_HEIGHT * scale).dp
    val pailSize = (16 * scale).dp
    val tap by rememberUpdatedState(onTap)
    // The rain's frames: the drops fall, then splash
    var rainFrame by remember { mutableIntStateOf(-1) }
    LaunchedEffect(watering) {
        if (!watering) return@LaunchedEffect
        repeat(2) {
            RAIN_FRAMES.indices.forEach { frame ->
                rainFrame = frame
                delay(110)
            }
        }
        rainFrame = -1
        onWatered()
    }
    val bob by rememberInfiniteTransition(label = "pail").animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "bob"
    )
    // Ripe berries sway a little, to say they're ready
    val sway by rememberInfiniteTransition(label = "tree").animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "sway"
    )
    Box(
        modifier = Modifier
            .zIndex(y)
            .offset {
                IntOffset(
                    (sceneWidth * x - (TREE_FOOT_X * scale).dp).roundToPx(),
                    (sceneHeight * y - (TREE_FOOT_Y * scale).dp - pailSize).roundToPx()
                )
            }
            .size(width, height + pailSize)
            .pointerInput(Unit) { detectTapGestures { tap() } }
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                filterQuality = FilterQuality.None,
                colorFilter = filter,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(width, height)
                    .graphicsLayer {
                        rotationZ = if (plot.ripe) sway else 0f
                        transformOrigin = TransformOrigin(TREE_FOOT_X / TREE_WIDTH, TREE_FOOT_Y / TREE_HEIGHT)
                    }
            )
        }
        if (plot?.thirsty == true && !watering) {
            Image(
                bitmap = ImageBitmap.imageResource(R.drawable.garden_pail),
                contentDescription = null,
                filterQuality = FilterQuality.None,
                colorFilter = filter,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = bob.dp)
                    .size(pailSize)
            )
        }
        if (rainFrame >= 0) {
            val rain = ImageBitmap.imageResource(RAIN_FRAMES[rainFrame])
            listOf(-0.25f, 0.05f, 0.3f).forEach { dx ->
                Image(
                    bitmap = rain,
                    contentDescription = null,
                    filterQuality = FilterQuality.None,
                    colorFilter = filter,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(x = width * dx, y = -((TREE_HEIGHT - TREE_FOOT_Y) * scale).dp)
                        .size(pailSize, pailSize * 2)
                )
            }
        }
    }
}

private val RAIN_FRAMES = listOf(
    R.drawable.garden_rain_0,
    R.drawable.garden_rain_1,
    R.drawable.garden_rain_2,
    R.drawable.garden_rain_3,
    R.drawable.garden_rain_4,
    R.drawable.garden_rain_5
)

/** Today's gift lying on the grass, bobbing a little until you pick it up. */
@Composable
private fun GroundGift(
    gift: GardenGift,
    spot: Pair<Float, Float>,
    sceneWidth: Dp,
    sceneHeight: Dp,
    onCollect: () -> Unit
) {
    val context = LocalContext.current
    val color = LocalColorSprites.current
    val drawable = when (gift) {
        is GardenGift.Berry -> GardenCatalog.drawable(context, "berry", gift.flavor.berry)
        is GardenGift.Decoration -> GardenCatalog.drawable(context, "deco", gift.id)
    }
    val bitmap = ImageBitmap.imageResource(drawable)
    val ground = LocalGround.current
    val x = spot.first
    val y = ground.top + spot.second * (ground.bottom - ground.top)
    val dpPerPixel =
        pixelSize(y, sceneWidth, sceneHeight, ground) * if (gift is GardenGift.Berry) 1.4f else DECORATION_SCALE
    val width = (bitmap.width * dpPerPixel).dp
    val height = (bitmap.height * dpPerPixel).dp
    val bob by rememberInfiniteTransition(label = "gift").animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "bob"
    )
    Image(
        bitmap = bitmap,
        contentDescription = null,
        filterQuality = FilterQuality.None,
        colorFilter = if (color) null else ColorFilter.colorMatrix(grayscale),
        modifier = Modifier
            .zIndex(y)
            .offset {
                IntOffset(
                    (sceneWidth * x - width / 2).roundToPx(),
                    (sceneHeight * y - height + bob.dp).roundToPx()
                )
            }
            .size(width, height)
            .pointerInput(gift) { detectTapGestures { onCollect() } }
    )
}

/** A dark pill over the scene: the chest, the residents. */
@Composable
private fun ScenePill(text: String, @DrawableRes icon: Int? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(horizontal = AppSizes.spacingMedium, vertical = AppSizes.spacingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
    ) {
        icon?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(text = text, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

/** The berry you took out of the chest, until it goes to a Pokémon or a plot. */
@Composable
private fun BerryInHand(flavor: Flavor, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(AppSizes.spacingMedium))
            .padding(horizontal = AppSizes.spacingMedium, vertical = AppSizes.spacingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
    ) {
        Image(
            bitmap = ImageBitmap.imageResource(GardenCatalog.drawable(context, "berry", flavor.berry)),
            contentDescription = stringResource(flavor.nameRes),
            filterQuality = FilterQuality.None,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = stringResource(R.string.garden_hand_berry),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onCancel) { Text(stringResource(R.string.common_cancel), color = Color.White) }
    }
}

/** How big a sprite pixel is at this depth: further back means a little smaller. */
private fun pixelSize(y: Float, sceneWidth: Dp, sceneHeight: Dp, ground: Ground): Float {
    val depth = (y - ground.top) / (ground.bottom - ground.top)
    return min(sceneWidth.value / SPRITE_PIXELS_ACROSS, sceneHeight.value / SPRITE_PIXELS_DOWN) * (0.8f + 0.35f * depth)
}

/** Whether it's within the eight hours of sleep that start at [bedtimeMinutes]. */
internal fun isAsleep(bedtimeMinutes: Int, now: LocalTime = LocalTime.now()): Boolean {
    val sinceBedtime = (now.hour * 60 + now.minute - bedtimeMinutes + MINUTES_PER_DAY) % MINUTES_PER_DAY
    return sinceBedtime < SLEEP_MINUTES
}

private const val MINUTES_PER_DAY = 24 * 60
private const val SLEEP_MINUTES = 8 * 60

/** The team in its order, plus the training partner if it isn't in it: seven at most. */
internal fun residentsOf(all: List<CaughtPokemon>, team: List<String>): List<CaughtPokemon> {
    val byId = all.associateBy { it.id }
    val members = team.mapNotNull { byId[it] }
    val partner = all.firstOrNull { it.isTraining }?.takeIf { it.id !in team }
    return members + listOfNotNull(partner)
}

@Composable
internal fun Overlay(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Color.White,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(AppSizes.spacingMedium))
            .padding(horizontal = AppSizes.spacingMedium, vertical = AppSizes.spacingSmall)
    )
}

/** Something rising from a Pokémon and fading: a heart when petted, Z's while it sleeps. */
private class Particle(
    val id: Long,
    val dx: Float,
    val sleepy: Boolean = false,
    @param:DrawableRes val drawable: Int? = null
)

/** Two Pokémon play-fighting around ([x], [y]): [left] stands on the left, [right] on the right. */
private class Spar(val left: String, val right: String, val x: Float, val y: Float)

/** Long enough to walk over, size each other up and trade a few playful tackles. */
private const val SPAR_MILLIS = 9_000L
private const val SPAR_GAP = 0.07f
private const val SPAR_TURNS = 6
private const val SPAR_TURN_MILLIS = 650L

/**
 * One Pokémon walking about. Feet are at ([x], [y]), as fractions of the scene; further back
 * means a little smaller. It only appears once its sprite is ready, so it never changes look.
 */
@Composable
private fun Wanderer(
    pokemon: CaughtPokemon,
    slot: Int,
    count: Int,
    sceneWidth: Dp,
    sceneHeight: Dp,
    night: Boolean,
    spar: Spar?,
    decorations: List<PlacedDecoration>,
    claims: MutableMap<String, String>,
    plotSpots: List<Pair<Float, Float>>,
    thirstyPlots: List<Int>,
    onWater: (Int) -> Unit,
    onPet: (CaughtPokemon) -> Boolean,
    onHold: () -> Unit,
    /** In a map scene, its party icon at this many dp per scene pixel instead of the big sprite. */
    iconScale: Float? = null
) {
    val context = LocalContext.current
    val color = LocalColorSprites.current
    val ground = LocalGround.current
    val look by produceState<Look?>(null, pokemon.speciesId, pokemon.isShiny, pokemon.form, iconScale != null) {
        if (iconScale != null) {
            value = Look(animated = null, icon = PokemonIcons.load(context, pokemon.speciesId, pokemon.form))
            return@produceState
        }
        value = Look(
            GardenSprites.load(
                context,
                pokemon.speciesId,
                pokemon.isShiny,
                pokemon.form,
                pokemon.usesFemaleSprite(context)
            )
        )
    }

    // Spread out to start with, so nobody begins on top of someone else
    val x = remember { Animatable((slot + 0.5f) / count * 0.8f + 0.1f) }
    val y = remember { Animatable(ground.top + Random.nextFloat() * (ground.bottom - ground.top)) }
    val hop = remember { Animatable(0f) }
    // Swinging while carried (degrees) and squash and stretch (+ taller, − flatter)
    val swing = remember { Animatable(0f) }
    val squash = remember { Animatable(0f) }
    var facingRight by remember { mutableStateOf(Random.nextBoolean()) }
    var bubble by remember { mutableStateOf<Int?>(null) }
    var showTag by remember { mutableIntStateOf(0) }
    val hearts = remember { mutableStateListOf<Particle>() }
    val scope = rememberCoroutineScope()
    var held by remember { mutableStateOf(false) }
    var sparring by remember { mutableStateOf(false) }
    // Half its width as a share of the scene, so it never walks off the edge
    var margin by remember { mutableFloatStateOf(0.1f) }
    // Tapping a sleeping Pokémon wakes it for a moment
    var awake by remember { mutableStateOf(false) }
    // Inside a tent: it fades out, and back in when it comes out
    var inside by remember { mutableStateOf(false) }
    val shown by animateFloatAsState(if (inside) 0f else 1f, tween(350), label = "tent")
    val latestDecorations by rememberUpdatedState(decorations)
    val latestThirsty by rememberUpdatedState(thirstyPlots)
    val water by rememberUpdatedState(onWater)
    val waterer = remember(pokemon.speciesId) { GardenCatalog.waters(pokemon) }
    val ownDolls = remember(pokemon.speciesId) {
        GardenCatalog.dollsOf(pokemon.speciesId, GardenCatalog.dolls + GardenCatalog.bigDolls).toSet()
    }
    val sleeping = night && !awake

    /** A squish when it lands, then back to shape with a wobble. */
    suspend fun land(amount: Float) {
        squash.snapTo(-amount)
        squash.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMediumLow))
    }

    /** Up and down again, stretching on the way up and squishing on landing. */
    suspend fun bounce(heightDp: Float, millis: Int) {
        coroutineScope {
            launch { squash.animateTo(0.12f, tween(millis / 2)) }
            hop.animateTo(-heightDp, tween(millis, easing = LinearOutSlowInEasing))
        }
        coroutineScope {
            launch { squash.animateTo(0f, tween(millis / 2)) }
            hop.animateTo(0f, tween(millis, easing = FastOutLinearInEasing))
        }
        land(heightDp / 60f)
    }

    suspend fun walkTo(targetX: Float, targetY: Float) {
        val toX = targetX.coerceIn(ground.left + margin, ground.right - margin)
        val toY = targetY.coerceIn(ground.top, ground.bottom)
        if (abs(toX - x.value) > 0.01f) facingRight = toX > x.value
        val millis = (hypot(toX - x.value, toY - y.value) * 9_000).toInt().coerceAtLeast(400)
        coroutineScope {
            launch { y.animateTo(toY, tween(millis, easing = LinearEasing)) }
            x.animateTo(toX, tween(millis, easing = LinearEasing))
        }
    }

    /** A free decoration to go to, its own family's doll more often than not. Claims it. */
    fun pickDecoration(): PlacedDecoration? {
        val free = latestDecorations.filter { it.id !in claims }
        val choice = free.filter { it.id in ownDolls }.takeIf { it.isNotEmpty() && Random.nextFloat() < 0.6f }?.random()
            ?: free.randomOrNull()
            ?: return null
        claims[choice.id] = pokemon.id
        return choice
    }

    /** A thirsty plot nobody else is on its way to water. Claims it. */
    fun pickThirstyPlot(): Int? = latestThirsty.firstOrNull { plotClaim(it) !in claims }
        ?.also { claims[plotClaim(it)] = pokemon.id }

    suspend fun showBubble(@DrawableRes drawable: Int, millis: Long = 1_600) {
        bubble = drawable
        delay(millis)
        bubble = null
    }

    /** Walks up beside the plot, a little hop, and the water comes down on it. */
    suspend fun waterPlot(plot: Int) {
        val (plotX, plotY) = plotSpots[plot]
        val side = if (x.value < plotX) -1f else 1f
        walkTo(plotX + side * (margin + 0.05f), plotY + 0.01f)
        facingRight = side < 0
        bounce(heightDp = 10f, millis = 120)
        water(plot)
        delay(1_500)
        showBubble(R.drawable.garden_emote_heart)
    }

    /**
     * Goes over and plays with it: sits on cushions, dances on mats, bounces on the tire, goes
     * down the slide, naps in the tent, hugs dolls (its own above all) and sniffs the plants.
     */
    suspend fun useDecoration(decoration: PlacedDecoration) {
        val kind = GardenCatalog.kindOf(decoration.id)
        val decoHeightDp =
            decorationPixels(kind) * pixelSize(decoration.y, sceneWidth, sceneHeight, ground) * DECORATION_SCALE
        val decoWidth = decoHeightDp / sceneWidth.value
        // On top of it means just in front, so it's drawn over it
        val onTop = decoration.y + 0.004f
        when (kind) {
            DecorationKind.CUSHION -> {
                walkTo(decoration.x, onTop)
                hop.animateTo(-decoHeightDp * 0.35f, tween(160))
                land(0.12f)
                squash.animateTo(-0.06f, tween(300))
                delay(Random.nextLong(4_000, 8_000))
                if (Random.nextBoolean()) showBubble(R.drawable.garden_emote_heart)
                squash.animateTo(0f, tween(200))
                hop.animateTo(0f, tween(180))
            }
            DecorationKind.MAT -> {
                walkTo(decoration.x, onTop)
                repeat(6) {
                    facingRight = !facingRight
                    bounce(heightDp = 8f, millis = 110)
                    delay(140)
                }
                showBubble(R.drawable.garden_emote_heart)
            }
            DecorationKind.TIRE -> {
                walkTo(decoration.x, onTop)
                val seat = -decoHeightDp * 0.45f
                hop.animateTo(seat, tween(180))
                repeat(Random.nextInt(4, 8)) {
                    coroutineScope {
                        launch { squash.animateTo(0.1f, tween(120)) }
                        hop.animateTo(seat - 22f, tween(180, easing = LinearOutSlowInEasing))
                    }
                    coroutineScope {
                        launch { squash.animateTo(0f, tween(120)) }
                        hop.animateTo(seat, tween(180, easing = FastOutLinearInEasing))
                    }
                    land(0.18f)
                }
                hop.animateTo(0f, tween(180))
            }
            DecorationKind.SLIDE -> {
                // Up the steps on the left, down the slope to the right
                walkTo(decoration.x - decoWidth * 0.4f, onTop)
                facingRight = true
                hop.animateTo(-decoHeightDp * 0.8f, tween(700))
                delay(300)
                coroutineScope {
                    launch { hop.animateTo(0f, tween(420, easing = FastOutLinearInEasing)) }
                    x.animateTo(
                        (decoration.x + decoWidth * 0.6f).coerceIn(ground.left + margin, ground.right - margin),
                        tween(420)
                    )
                }
                land(0.2f)
                showBubble(R.drawable.garden_emote_exclamation, 1_000)
            }
            DecorationKind.TENT -> {
                walkTo(decoration.x, decoration.y + 0.002f)
                inside = true
                delay(Random.nextLong(6_000, 14_000))
                inside = false
                delay(350)
                showBubble(R.drawable.garden_emote_exclamation, 1_000)
            }
            else -> {
                // Stands beside it, looking at it
                val side = if (x.value < decoration.x) -1f else 1f
                walkTo(decoration.x + side * (decoWidth / 2 + margin * 0.7f), decoration.y + 0.002f)
                facingRight = side < 0
                when {
                    decoration.id in ownDolls -> {
                        repeat(2) {
                            hearts += Particle(System.nanoTime(), Random.nextFloat() - 0.5f)
                            bounce(heightDp = 10f, millis = 120)
                            delay(400)
                        }
                        delay(1_500)
                    }
                    kind == DecorationKind.PLANT -> {
                        repeat(3) {
                            squash.animateTo(-0.05f, tween(150))
                            squash.animateTo(0f, tween(150))
                        }
                        showBubble(R.drawable.garden_emote_heart)
                    }
                    kind == DecorationKind.DOLL || kind == DecorationKind.BIG_DOLL ->
                        showBubble(EMOTES.random(), 2_000)
                    else -> showBubble(R.drawable.garden_emote_question)
                }
                delay(Random.nextLong(1_000, 3_000))
            }
        }
    }

    // Asleep already when the Garden opens: they're found curled up, no need to watch them walk over
    var arrived by remember { mutableStateOf(false) }
    LaunchedEffect(pokemon.id, night) {
        if (night) {
            // Bedtime: everyone walks slowly to a little huddle in the middle, and stays there
            val row = slot % 2
            val spotX = (0.5f + (slot - (count - 1) / 2f) * 0.09f).coerceIn(ground.left + margin, ground.right - margin)
            val spotY = 0.68f + row * 0.07f
            hop.snapTo(0f)
            if (!arrived) {
                arrived = true
                x.snapTo(spotX)
                y.snapTo(spotY)
                return@LaunchedEffect
            }
            facingRight = spotX > x.value
            try {
                coroutineScope {
                    launch { y.animateTo(spotY, tween(3_500, easing = LinearEasing)) }
                    x.animateTo(spotX, tween(3_500, easing = LinearEasing))
                }
            } catch (e: CancellationException) {
                if (!isActive) throw e
            }
            return@LaunchedEffect
        }
        arrived = true
        while (true) {
            delay(Random.nextLong(1_500, 4_500))
            if (held || sparring) continue
            val plot = if (waterer) pickThirstyPlot() else null
            val decoration =
                if (plot == null && Random.nextFloat() < USE_DECORATION_CHANCE) pickDecoration() else null
            // Picking it up stops the walk (or the game) midway
            try {
                if (plot != null) {
                    waterPlot(plot)
                } else if (decoration != null) {
                    useDecoration(decoration)
                } else {
                    val targetX = x.value + (Random.nextFloat() - 0.5f) * 0.5f
                    val targetY = y.value + (Random.nextFloat() - 0.5f) * 0.3f
                    walkTo(targetX, targetY)
                }
            } catch (e: CancellationException) {
                if (!isActive) throw e
            } finally {
                inside = false
                decoration?.let { claims.remove(it.id) }
                plot?.let { claims.remove(plotClaim(it)) }
            }
        }
    }
    LaunchedEffect(pokemon.id, night) {
        while (!night) {
            delay(Random.nextLong(6_000, 15_000))
            if (sparring) continue
            bubble = EMOTES.random()
            delay(1_800)
            bubble = null
        }
    }
    // Asleep: Z's float up now and then, and it breathes slowly
    LaunchedEffect(pokemon.id, sleeping) {
        if (!sleeping) return@LaunchedEffect
        launch {
            delay(Random.nextLong(0, 2_000))
            while (true) {
                if (!held) hearts += Particle(System.nanoTime(), 0.3f + Random.nextFloat() * 0.2f, sleepy = true)
                delay(Random.nextLong(2_500, 5_000))
            }
        }
        while (true) {
            try {
                squash.animateTo(0.035f, tween(1_800))
                squash.animateTo(0f, tween(1_800))
            } catch (e: CancellationException) {
                if (!isActive) throw e
            }
        }
    }
    LaunchedEffect(awake) {
        if (awake) {
            delay(4_000)
            awake = false
        }
    }
    LaunchedEffect(showTag) {
        if (showTag > 0) {
            delay(2_000)
            showTag = 0
        }
    }

    fun pickUp() {
        held = true
        bubble = null
        scope.launch { hop.animateTo(LIFT_DP, tween(120)) }
        scope.launch {
            squash.animateTo(0.18f, tween(90))
            squash.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    /** It dangles from your finger, lagging behind and swinging back when you stop. */
    fun carry(dx: Float) {
        scope.launch {
            swing.animateTo((dx * 2.5f).coerceIn(-30f, 30f), spring(stiffness = Spring.StiffnessMedium))
            swing.animateTo(0f, spring(dampingRatio = 0.25f, stiffness = Spring.StiffnessVeryLow))
        }
    }

    /**
     * Let go with the finger's speed: it flies on, falls with gravity, bounces off the ground and
     * the sides, and slides to a stop. [vx] is in scene widths per second, [vyDp] in dp per second.
     */
    fun drop(vx: Float, vyDp: Float) {
        scope.launch {
            launch { swing.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessLow)) }
            // Above the grass, the gap becomes height to fall from
            val floor = y.value.coerceIn(ground.top, ground.bottom)
            var height = hop.value + (y.value - floor) * sceneHeight.value
            y.snapTo(floor)
            hop.snapTo(height)
            var speedX = vx.coerceIn(-MAX_THROW, MAX_THROW)
            var speedY = vyDp.coerceIn(-MAX_THROW_DP, MAX_THROW_DP)
            var last = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val dt = ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                speedY += GRAVITY_DP * dt
                height += speedY * dt
                val grounded = height >= 0f
                if (grounded) {
                    height = 0f
                    if (speedY > MIN_BOUNCE_DP) {
                        launch { land((speedY / 2_500f).coerceIn(0.08f, 0.35f)) }
                        speedY *= -BOUNCINESS
                    } else {
                        speedY = 0f
                    }
                }
                // A little air resistance, and the grass slows it down for real
                speedX *= exp(-(if (grounded) GROUND_FRICTION else AIR_FRICTION) * dt)
                var nextX = x.value + speedX * dt
                if (nextX < ground.left + margin || nextX > ground.right - margin) {
                    nextX = nextX.coerceIn(ground.left + margin, ground.right - margin)
                    speedX *= -BOUNCINESS
                }
                if (abs(speedX) > 0.05f) facingRight = speedX > 0
                x.snapTo(nextX)
                hop.snapTo(height)
                if (grounded && speedY == 0f && abs(speedX) < 0.02f) break
            }
            held = false
        }
    }

    /**
     * Walks up to the other one, a "!" and then tackles in turns: on its turn it lunges, on the
     * other's it gets knocked back a little. Both follow the same clock, so they stay in step.
     */
    LaunchedEffect(spar) {
        val fight = spar ?: return@LaunchedEffect
        val onLeft = when (pokemon.id) {
            fight.left -> true
            fight.right -> false
            else -> return@LaunchedEffect
        }
        if (held || claims.containsValue(pokemon.id)) return@LaunchedEffect
        sparring = true
        try {
            bubble = null
            val spotX = (fight.x + if (onLeft) -SPAR_GAP else SPAR_GAP).coerceIn(
                ground.left + margin,
                ground.right - margin
            )
            facingRight = spotX > x.value
            val millis = (hypot(spotX - x.value, fight.y - y.value) * 9_000).toInt().coerceIn(400, 2_500)
            coroutineScope {
                launch { y.animateTo(fight.y, tween(millis, easing = LinearEasing)) }
                x.animateTo(spotX, tween(millis, easing = LinearEasing))
            }
            delay(2_600L - millis)
            facingRight = onLeft
            bubble = R.drawable.garden_emote_exclamation
            delay(700)
            bubble = null
            val toward = if (onLeft) 1f else -1f
            repeat(SPAR_TURNS) { turn ->
                val mine = (turn % 2 == 0) == onLeft
                if (mine) {
                    // Lunge at the other one and spring back
                    launch { bounce(heightDp = 10f, millis = 110) }
                    x.animateTo(spotX + toward * 0.04f, tween(140, easing = FastOutLinearInEasing))
                    x.animateTo(spotX, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
                } else {
                    // Take the hit: knocked back a bit, with a squish
                    delay(130)
                    launch { land(0.22f) }
                    x.animateTo(spotX - toward * 0.025f, tween(90))
                    x.animateTo(spotX, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
                }
                delay((SPAR_TURN_MILLIS - 400).coerceAtLeast(0))
            }
            // No hard feelings
            bubble = R.drawable.garden_emote_heart
            bounce(heightDp = 14f, millis = 140)
            delay(1_000)
            bubble = null
        } catch (e: CancellationException) {
            // Picked up mid-fight, or the fight ended: just carry on
            if (!isActive) throw e
        } finally {
            sparring = false
        }
    }

    val ready = look ?: return
    val depth = (y.value - ground.top) / (ground.bottom - ground.top)
    val scale = 0.8f + 0.35f * depth
    // Icons are on a map: one size everywhere, the tiles' own
    val dpPerPixel = iconScale
        ?: (min(sceneWidth.value / SPRITE_PIXELS_ACROSS, sceneHeight.value / SPRITE_PIXELS_DOWN) * scale)
    val animated = ready.animated
    val width = if (iconScale != null) {
        (ICON_PIXELS * iconScale).dp
    } else {
        ((animated?.intrinsicWidth?.takeIf { it > 0 } ?: 96) * dpPerPixel).dp
    }
    val height = if (iconScale != null) {
        (ICON_PIXELS * iconScale).dp
    } else {
        ((animated?.intrinsicHeight?.takeIf { it > 0 } ?: 96) * dpPerPixel).dp
    }
    // Party icons bob a pixel up and down, like in the team menu
    val iconBob by rememberInfiniteTransition(label = "icon").animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Restart),
        label = "frame"
    )
    SideEffect { margin = (width / sceneWidth / 2).coerceIn(0.05f, 0.45f) }

    Box(
        modifier = Modifier
            .zIndex(y.value)
            .offset {
                IntOffset(
                    (sceneWidth * x.value - width / 2).roundToPx(),
                    (sceneHeight * y.value - height + hop.value.dp).roundToPx()
                )
            }
            .size(width, height)
            .alpha(shown)
            .pointerInput(pokemon.id) {
                // The finger's path in scene pixels, to know how fast it lets go
                val tracker = VelocityTracker()
                var path = Offset.Zero
                fun release() {
                    val velocity = tracker.calculateVelocity()
                    tracker.resetTracking()
                    drop(vx = velocity.x / sceneWidth.toPx(), vyDp = velocity.y / density)
                }
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        path = Offset.Zero
                        pickUp()
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        path += amount
                        tracker.addPosition(change.uptimeMillis, path)
                        if (abs(amount.x) > 1f) facingRight = amount.x > 0
                        carry(amount.x)
                        scope.launch {
                            x.snapTo(
                                (x.value + amount.x / (sceneWidth.toPx())).coerceIn(
                                    ground.left + margin,
                                    ground.right - margin
                                )
                            )
                            y.snapTo((y.value + amount.y / (sceneHeight.toPx())).coerceIn(0.15f, 1f))
                        }
                    },
                    onDragEnd = { release() },
                    onDragCancel = { release() }
                )
            }
            .pointerInput(pokemon.id) {
                detectTapGestures(
                    onDoubleTap = { onHold() },
                    onTap = {
                        if (night && !awake) {
                            awake = true
                            bubble = R.drawable.garden_emote_exclamation
                            scope.launch {
                                delay(1_500)
                                bubble = null
                            }
                        }
                        val petted = onPet(pokemon)
                        showTag++
                        scope.launch { if (!held) bounce(heightDp = 18f, millis = 140) }
                        if (petted) {
                            hearts += Particle(System.nanoTime(), Random.nextFloat() - 0.5f)
                        } else {
                            bubble = R.drawable.garden_emote_heart
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(width, height)
                .graphicsLayer {
                    rotationZ = swing.value
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
                .graphicsLayer {
                    scaleY = 1f + squash.value
                    scaleX = 1f - squash.value * 0.8f
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
        ) {
            if (iconScale != null) {
                Image(
                    bitmap = ready.icon ?: ImageBitmap.imageResource(
                        PokemonSpriteUtils.getColorResourceId(context, pokemon.speciesId, pokemon.isShiny, pokemon.form)
                    ),
                    contentDescription = Pokemon[pokemon.speciesId]?.displayName,
                    filterQuality = FilterQuality.None,
                    colorFilter = if (color) null else ColorFilter.colorMatrix(grayscale),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Party icons look to the left, like the big sprites
                            scaleX = if (facingRight) -1f else 1f
                            translationY = if (!sleeping && iconBob >= 1f) -iconScale * density else 0f
                        }
                )
            } else if (animated != null) {
                AndroidView(
                    factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
                    update = { view ->
                        animated.colorFilter = if (color) null else ColorMatrixColorFilter(grayscale.values)
                        if (view.drawable !== animated) {
                            animated.isFilterBitmap = false
                            view.setImageDrawable(animated)
                        }
                        (animated as? AnimatedImageDrawable)?.let { if (sleeping) it.stop() else it.start() }
                        // Black and White sprites look to the left
                        view.scaleX = if (facingRight) -1f else 1f
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // No connection the first time: the regular sprite stands in
                Image(
                    bitmap = ImageBitmap.imageResource(
                        PokemonSpriteUtils.getColorResourceId(context, pokemon.speciesId, pokemon.isShiny, pokemon.form)
                    ),
                    contentDescription = Pokemon[pokemon.speciesId]?.displayName,
                    filterQuality = FilterQuality.None,
                    colorFilter = if (color) null else ColorFilter.colorMatrix(grayscale),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = if (facingRight) 1f else -1f }
                )
            }
        }

        if (showTag > 0) {
            NameTag(pokemon = pokemon, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-28).dp))
        } else {
            bubble?.let {
                Emote(
                    drawable = it,
                    size = (EMOTE_PIXELS * dpPerPixel).dp,
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = -(EMOTE_PIXELS * dpPerPixel).dp)
                )
            }
        }
        hearts.forEach { heart ->
            key(heart.id) {
                FloatingParticle(
                    particle = heart,
                    size = (EMOTE_PIXELS * dpPerPixel).dp,
                    modifier = Modifier.align(Alignment.TopCenter)
                ) { hearts.remove(heart) }
            }
        }
    }
}

@Composable
private fun NameTag(pokemon: CaughtPokemon, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .wrapContentSize(unbounded = true)
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = (pokemon.nickname ?: Pokemon[pokemon.speciesId]?.displayName.orEmpty()),
            style = TextStyle(color = Color.White, fontWeight = FontWeight.SemiBold),
            maxLines = 1
        )
        FriendshipHearts(friendship = pokemon.friendship, size = 10.dp)
    }
}

/** The emotion balloons from Pokémon Emerald, at the same pixel size as the sprites. */
@Composable
private fun Emote(@DrawableRes drawable: Int, size: Dp, modifier: Modifier = Modifier) {
    Image(
        bitmap = ImageBitmap.imageResource(drawable),
        contentDescription = null,
        filterQuality = FilterQuality.None,
        modifier = modifier
            .wrapContentSize(unbounded = true)
            .size(size)
    )
}

/**
 * Rises and fades. Z's drift up slowly to the side and grow from the small one to the big one,
 * like when a Pokémon falls asleep in battle.
 */
@Composable
private fun FloatingParticle(particle: Particle, size: Dp, modifier: Modifier = Modifier, onDone: () -> Unit) {
    val rise = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        rise.animateTo(1f, tween(if (particle.sleepy) 2_200 else 900, easing = LinearEasing))
        onDone()
    }
    val drawable = when {
        particle.drawable != null -> particle.drawable
        !particle.sleepy -> R.drawable.garden_emote_heart
        rise.value < 0.45f -> R.drawable.garden_sleep_z_small
        else -> R.drawable.garden_sleep_z_big
    }
    Emote(
        drawable = drawable,
        size = size,
        modifier = modifier
            .offset(
                x = (particle.dx * (if (particle.sleepy) 30 + 30 * rise.value else 30f)).dp,
                y = -size * (if (particle.sleepy) 0.5f else 1f) - (40 * rise.value).dp
            )
            .alpha(1f - rise.value * rise.value)
    )
}

/** Held Pokémon: how it's doing and the way to its details. */
@Composable
private fun ResidentInfo(
    pokemon: CaughtPokemon,
    petsLeft: Int,
    garden: GardenState,
    berriesLeft: Int,
    onFeed: (Flavor) -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val name = pokemon.nickname ?: Pokemon[pokemon.speciesId]?.displayName ?: stringResource(R.string.common_unknown)
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(AppSizes.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
            ) {
                Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                PokemonLevelChip(level = pokemon.level)
                Box(modifier = Modifier.weight(1f))
                FriendshipHearts(friendship = pokemon.friendship)
            }
            Text(
                text = stringResource(FriendshipRules.phrase(pokemon.friendship)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.garden_pets_today,
                    petsLeft.coerceAtLeast(0),
                    FriendshipRules.PETS_PER_DAY
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.garden_feed_hint, berriesLeft.coerceAtLeast(0)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)) {
                Flavor.entries.forEach { flavor ->
                    val count = garden.berries(flavor)
                    val usable = count > 0 && berriesLeft > 0
                    BerryCount(
                        flavor = flavor,
                        count = count,
                        modifier = Modifier
                            .weight(1f)
                            .alpha(if (usable) 1f else 0.4f)
                            .clip(RoundedCornerShape(AppSizes.spacingSmall))
                            .clickable(enabled = usable) { onFeed(flavor) }
                            .padding(AppSizes.spacingTiny)
                    )
                }
            }
            OutlinedButton(onClick = onDetails, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.garden_see_details))
            }
        }
    }
}

/**
 * A decoration out in the meadow. Hold it to move it; double tap puts it back in the shop's
 * storage. It looks bigger at the front, like the Pokémon.
 */
@Composable
private fun DecorationView(
    decoration: PlacedDecoration,
    sceneWidth: Dp,
    sceneHeight: Dp,
    onMove: (Float, Float) -> Unit,
    onPutAway: () -> Unit
) {
    val context = LocalContext.current
    val color = LocalColorSprites.current
    val bitmap = ImageBitmap.imageResource(GardenCatalog.drawable(context, "deco", decoration.id))
    var x by remember(decoration) { mutableFloatStateOf(decoration.x) }
    var y by remember(decoration) { mutableFloatStateOf(decoration.y) }
    val ground = LocalGround.current
    val dpPerPixel = pixelSize(y, sceneWidth, sceneHeight, ground) * DECORATION_SCALE
    val width = (bitmap.width * dpPerPixel).dp
    val height = (bitmap.height * dpPerPixel).dp
    Image(
        bitmap = bitmap,
        contentDescription = null,
        filterQuality = FilterQuality.None,
        colorFilter = if (color) null else ColorFilter.colorMatrix(grayscale),
        modifier = Modifier
            .zIndex(y)
            .offset {
                IntOffset((sceneWidth * x - width / 2).roundToPx(), (sceneHeight * y - height).roundToPx())
            }
            .size(width, height)
            .pointerInput(decoration.id) {
                detectDragGesturesAfterLongPress(
                    onDrag = { change, amount ->
                        change.consume()
                        x = (x + amount.x / sceneWidth.toPx()).coerceIn(0.05f, 0.95f)
                        y = (y + amount.y / sceneHeight.toPx()).coerceIn(ground.top, ground.bottom)
                    },
                    onDragEnd = { onMove(x, y) },
                    onDragCancel = { onMove(x, y) }
                )
            }
            .pointerInput(decoration.id) { detectTapGestures(onDoubleTap = { onPutAway() }) }
    )
}

/** Emerald's decoration icons: dolls and cushions are 16 pixels, big dolls 32, the rest 24. */
private fun decorationPixels(kind: DecorationKind): Int = when (kind) {
    DecorationKind.DOLL, DecorationKind.CUSHION -> 16
    DecorationKind.BIG_DOLL -> 32
    else -> 24
}

private fun plotClaim(plot: Int) = "plot$plot"

/** How often a walk becomes a visit to a decoration. */
private const val USE_DECORATION_CHANCE = 0.35f

/** Emerald's decorations were drawn for its smaller rooms: a bit bigger fits the meadow. */
private const val DECORATION_SCALE = 1.6f
