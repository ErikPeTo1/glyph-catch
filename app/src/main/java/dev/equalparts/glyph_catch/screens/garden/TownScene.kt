package dev.equalparts.glyph_catch.screens.garden

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.LocalColorSprites
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.GardenZone
import dev.equalparts.glyph_catch.data.PlayerLook
import dev.equalparts.glyph_catch.data.PreferencesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The town's places: their name, the zone they open (null while not built yet) and their spot on the map. */
private enum class TownPlace(@param:StringRes val nameRes: Int, val zone: GardenZone?, key: String) {
    SPRINGS(R.string.zone_springs, GardenZone.SPRINGS, "springs"),
    GYM(R.string.zone_gym, GardenZone.GYM, "gym"),
    KITCHEN(R.string.zone_kitchen, null, "kitchen"),
    MEADOW(R.string.zone_meadow, GardenZone.MEADOW, "meadow"),
    ORCHARD(R.string.zone_orchard, GardenZone.ORCHARD, "orchard");

    val area: TownArea = TownMapData.PLACES.getValue(key)
}

/** Milliseconds per tile, like walking in the games. */
private const val STEP_MILLIS = 220L

/** A step on grass costs this many steps on the path, so the hero keeps to the paths like people do. */
private const val GRASS_COST = 3

/**
 * The way between two tiles over walkable ground (4 directions), keeping to the paths where it can,
 * as the tiles to step on, or null if there's none.
 */
internal fun townPath(fromX: Int, fromY: Int, toX: Int, toY: Int): List<Pair<Int, Int>>? {
    if (!TownMapData.walkable(toX, toY)) return null
    val start = fromX to fromY
    val goal = toX to toY
    val came = HashMap<Pair<Int, Int>, Pair<Int, Int>>()
    val cost = HashMap<Pair<Int, Int>, Int>()
    came[start] = start
    cost[start] = 0
    val queue = java.util.PriorityQueue<Pair<Int, Pair<Int, Int>>>(compareBy { it.first })
    queue += 0 to start
    while (queue.isNotEmpty()) {
        val (spent, at) = queue.poll()!!
        if (at == goal) break
        if (spent > cost.getValue(at)) continue
        for ((dx, dy) in listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)) {
            val next = (at.first + dx) to (at.second + dy)
            if (!TownMapData.walkable(next.first, next.second)) continue
            val total = spent + if (TownMapData.isPath(next.first, next.second)) 1 else GRASS_COST
            if (total < (cost[next] ?: Int.MAX_VALUE)) {
                cost[next] = total
                came[next] = at
                queue += total to next
            }
        }
    }
    if (goal !in came) return null
    val steps = mutableListOf<Pair<Int, Int>>()
    var at = goal
    while (at != start) {
        steps += at
        at = came.getValue(at)
    }
    return steps.reversed()
}

/** The walkable tile nearest to a tap on a tree or a roof. */
private fun nearestWalkable(x: Int, y: Int): Pair<Int, Int>? = (0..6).asSequence().flatMap { r ->
    (-r..r).asSequence().flatMap { dx -> (-r..r).asSequence().map { dy -> (x + dx) to (y + dy) } }
}.firstOrNull { (tx, ty) -> TownMapData.walkable(tx, ty) }

/** The hero's walking frames (from FireRed and Emerald): down, up, left, then two steps of each. */
private object HeroFrames {
    const val WIDTH = 16
    const val HEIGHT = 32
    fun idle(facing: Facing) = facing.ordinal.coerceAtMost(2)
    fun step(facing: Facing, step: Int) = 3 + facing.ordinal.coerceAtMost(2) * 2 + step % 2
}

/** Which way the hero looks; right is left mirrored. */
internal enum class Facing { DOWN, UP, LEFT, RIGHT }

/** The tile the hero stands on and which way it looks. Kept while you visit a place. */
internal class HeroSpot(val tileX: Int = 9, val tileY: Int = 21, val facing: Facing = Facing.DOWN)

/** Feet in map pixels for a tile: its middle, at its bottom. */
private fun feetX(tileX: Int) = tileX * TownMapData.TILE + 8f
private fun feetY(tileY: Int) = tileY * TownMapData.TILE + 15f

/**
 * The town as an Emerald map: tap a place and the hero walks there tile by tile along the
 * paths and grass, around trees, buildings and fences, then goes in. Tap the grass to stroll.
 * Places still being built say so.
 */
@Composable
internal fun TownScene(
    hero: HeroSpot,
    onHeroMoved: (HeroSpot) -> Unit,
    onOpen: (GardenZone) -> Unit,
    onKitchen: () -> Unit
) {
    val color = LocalColorSprites.current
    val gray = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val heroSheet = ImageBitmap.imageResource(
        remember {
            PlayerLook.of(PreferencesManager(context).playerLook).walking
        }
    )
    var tile by remember { mutableStateOf(hero.tileX to hero.tileY) }
    var x by remember { mutableStateOf(feetX(hero.tileX)) }
    var y by remember { mutableStateOf(feetY(hero.tileY)) }
    var facing by remember { mutableStateOf(hero.facing) }
    var walking by remember { mutableStateOf(false) }
    var step by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var walk by remember { mutableStateOf<Job?>(null) }
    val comingSoon = stringResource(R.string.zone_coming_soon)
    val names = TownPlace.entries.associateWith { stringResource(it.nameRes) }

    LaunchedEffect(walking) {
        while (walking) {
            delay(STEP_MILLIS / 2)
            step++
        }
    }
    LaunchedEffect(message) {
        if (message != null) {
            delay(2_000)
            message = null
        }
    }

    fun goTo(toX: Int, toY: Int, then: (() -> Unit)? = null) {
        val steps = townPath(tile.first, tile.second, toX, toY) ?: return
        walk?.cancel()
        walk = scope.launch {
            walking = true
            try {
                for ((tx, ty) in steps) {
                    val (fx, fy) = tile
                    facing = when {
                        tx > fx -> Facing.RIGHT
                        tx < fx -> Facing.LEFT
                        ty > fy -> Facing.DOWN
                        else -> Facing.UP
                    }
                    val fromX = x
                    val fromY = y
                    val frames = (STEP_MILLIS / 16).toInt()
                    for (i in 1..frames) {
                        delay(16)
                        x = fromX + (feetX(tx) - fromX) * i / frames
                        y = fromY + (feetY(ty) - fromY) * i / frames
                    }
                    tile = tx to ty
                }
            } finally {
                walking = false
                onHeroMoved(HeroSpot(tile.first, tile.second, facing))
            }
            then?.invoke()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val crop = remember(maxWidth, maxHeight) {
            SceneCrop(
                maxWidth.value,
                maxHeight.value,
                (TownMapData.WIDTH * TownMapData.TILE).toFloat(),
                (TownMapData.HEIGHT * TownMapData.TILE).toFloat(),
                fit = true
            )
        }
        // The whole town is always in view; any room left is more of the same forest, on the same grid
        val forest = ImageBitmap.imageResource(R.drawable.garden_town_forest)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val block = TownMapData.TILE * 2 * crop.scale * density
            val startX = crop.screenX(0f) * density
            val startY = crop.screenY(0f) * density
            var by = startY - kotlin.math.ceil(startY / block) * block
            while (by < size.height) {
                var bx = startX - kotlin.math.ceil(startX / block) * block
                while (bx < size.width) {
                    drawImage(
                        image = forest,
                        dstOffset = IntOffset(bx.toInt(), by.toInt()),
                        dstSize = IntSize(kotlin.math.ceil(block).toInt(), kotlin.math.ceil(block).toInt()),
                        filterQuality = FilterQuality.None,
                        colorFilter = if (color) null else gray
                    )
                    bx += block
                }
                by += block
            }
        }
        Image(
            bitmap = ImageBitmap.imageResource(R.drawable.garden_scene_town),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None,
            colorFilter = if (color) null else gray,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(crop) {
                    detectTapGestures { tap ->
                        val tx = (crop.toImageX(tap.x / density) / TownMapData.TILE).toInt()
                        val ty = (crop.toImageY(tap.y / density) / TownMapData.TILE).toInt()
                        val place = TownPlace.entries.firstOrNull { it.area.contains(tx, ty) }
                        if (place == null) {
                            nearestWalkable(tx, ty)?.let { (wx, wy) -> goTo(wx, wy) }
                        } else {
                            goTo(place.area.doorX, place.area.doorY) {
                                val zone = place.zone
                                if (place == TownPlace.KITCHEN) {
                                    onKitchen()
                                } else if (zone != null && zone.open) {
                                    onOpen(zone)
                                } else {
                                    message = "${names[place]} · $comingSoon"
                                }
                            }
                        }
                    }
                }
        )

        // The hero, feet on its spot
        val scale = crop.scale
        Canvas(
            modifier = Modifier
                .zIndex(1f)
                .offset(
                    (crop.screenX(x) - HeroFrames.WIDTH * scale / 2).dp,
                    (crop.screenY(y) - HeroFrames.HEIGHT * scale).dp
                )
                .size((HeroFrames.WIDTH * scale).dp, (HeroFrames.HEIGHT * scale).dp)
        ) {
            val frame = if (walking) HeroFrames.step(facing, step) else HeroFrames.idle(facing)
            scale(scaleX = if (facing == Facing.RIGHT) -1f else 1f, scaleY = 1f) {
                drawImage(
                    image = heroSheet,
                    srcOffset = IntOffset(frame * HeroFrames.WIDTH, 0),
                    srcSize = IntSize(HeroFrames.WIDTH, HeroFrames.HEIGHT),
                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                    filterQuality = FilterQuality.None,
                    colorFilter = if (color) null else gray
                )
            }
        }

        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(3f)
                    .padding(horizontal = AppSizes.spacingLarge, vertical = AppSizes.spacingSmall)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(AppSizes.spacingMedium))
                    .padding(horizontal = AppSizes.spacingMedium, vertical = AppSizes.spacingSmall)
            )
        }
    }
}
