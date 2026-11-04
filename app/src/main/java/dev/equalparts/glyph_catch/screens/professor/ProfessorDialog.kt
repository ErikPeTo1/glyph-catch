package dev.equalparts.glyph_catch.screens.professor

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.equalparts.glyph_catch.ItemGlyphCircle
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.LegendaryItems
import dev.equalparts.glyph_catch.data.NpcEvent
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.Professor
import dev.equalparts.glyph_catch.data.ProfessorEvent
import dev.equalparts.glyph_catch.data.Region
import dev.equalparts.glyph_catch.data.Rumors
import dev.equalparts.glyph_catch.data.nameRes
import dev.equalparts.glyph_catch.ndotFontFamily
import kotlinx.coroutines.delay

/**
 * One box of text. Pages with a [gift] show the item the professor hands over.
 */
data class DialoguePage(val text: String, val gift: Item? = null, val egg: Boolean = false)

/**
 * A professor talking, in the style of the main games: tap to show the whole line, tap again
 * to continue.
 */
@Composable
fun ProfessorDialog(
    professor: Professor,
    pages: List<DialoguePage>,
    onFinish: () -> Unit,
    onDismiss: (() -> Unit)? = null
) = CharacterDialog(professor.sprite, stringResource(professor.nameRes), pages, onFinish, onDismiss)

@Composable
fun CharacterDialog(
    @DrawableRes sprite: Int,
    name: String,
    pages: List<DialoguePage>,
    onFinish: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    var index by remember(pages) { mutableIntStateOf(0) }
    val page = pages[index.coerceIn(0, pages.lastIndex)]
    var shown by remember(pages, index) { mutableIntStateOf(0) }
    LaunchedEffect(pages, index) {
        while (shown < page.text.length) {
            delay(TYPE_DELAY_MILLIS)
            shown++
        }
    }

    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = onDismiss != null,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    when {
                        shown < page.text.length -> shown = page.text.length
                        index < pages.lastIndex -> index++
                        else -> onFinish()
                    }
                }
                .safeDrawingPadding()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                CharacterSprite(sprite, name, modifier = Modifier.size(240.dp))
                page.gift?.let { GiftBadge(it) }
                if (page.egg) EggBadge()
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = name,
                    fontFamily = ndotFontFamily,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 132.dp)
                        .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = page.text.take(shown),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (shown >= page.text.length) {
                        Text(
                            text = "▼",
                            color = Color.White,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GiftBadge(item: Item) {
    val name = stringResource(item.nameRes())
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ItemGlyphCircle(item = item, contentDescription = name, size = 88.dp)
        Text(
            text = stringResource(R.string.professor_gift_received, name),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun EggBadge() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "🥚", style = MaterialTheme.typography.displayMedium)
        Text(
            text = stringResource(R.string.npc_egg_received),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

private const val TYPE_DELAY_MILLIS = 18L

/**
 * What the Fisherman, the clerk and the breeder say.
 */
object NpcScripts {
    fun event(context: Context, event: NpcEvent, pokedexCount: Int): List<DialoguePage> = when (event) {
        is NpcEvent.Rod -> when (event.rod) {
            Item.OLD_ROD -> listOf(
                DialoguePage(context.getString(R.string.npc_fisher_old_1)),
                DialoguePage(context.getString(R.string.npc_fisher_old_2), gift = Item.OLD_ROD),
                DialoguePage(context.getString(R.string.npc_fisher_bye))
            )
            Item.GOOD_ROD -> listOf(
                DialoguePage(context.getString(R.string.npc_fisher_good), gift = Item.GOOD_ROD),
                DialoguePage(context.getString(R.string.npc_fisher_bye))
            )
            else -> listOf(
                DialoguePage(context.getString(R.string.npc_fisher_super), gift = Item.SUPER_ROD),
                DialoguePage(context.getString(R.string.npc_fisher_bye))
            )
        }
        NpcEvent.Repel -> listOf(
            DialoguePage(context.getString(R.string.npc_clerk_1, pokedexCount)),
            DialoguePage(context.getString(R.string.npc_clerk_2), gift = Item.REPEL),
            DialoguePage(context.getString(R.string.npc_clerk_3))
        )
        NpcEvent.Breeder -> listOf(
            DialoguePage(context.getString(R.string.npc_breeder_1)),
            DialoguePage(context.getString(R.string.npc_breeder_2)),
            DialoguePage(context.getString(R.string.npc_breeder_3)),
            DialoguePage(context.getString(R.string.npc_breeder_4)),
            DialoguePage(context.getString(R.string.npc_breeder_5), egg = true)
        )
    }
}

/**
 * What each professor says.
 */
object ProfessorScripts {
    private val introLines = listOf(
        R.string.oak_intro_1,
        R.string.oak_intro_2,
        R.string.oak_intro_3,
        R.string.oak_intro_4,
        R.string.oak_intro_5,
        R.string.oak_intro_6,
        R.string.oak_intro_7,
        R.string.oak_intro_8
    )

    private fun Professor.helloRes(): Int = when (this) {
        Professor.OAK -> R.string.oak_intro_2
        Professor.ELM -> R.string.professor_elm_hello
        Professor.BIRCH -> R.string.professor_birch_hello
        Professor.ROWAN -> R.string.professor_rowan_hello
        Professor.JUNIPER -> R.string.professor_juniper_hello
    }

    fun event(context: Context, event: ProfessorEvent, pokedexCount: Int): List<DialoguePage> {
        val name = context.getString(event.professor.nameRes)
        return when (event) {
            ProfessorEvent.Intro -> introLines.map { DialoguePage(context.getString(it)) }.toMutableList().apply {
                add(5, DialoguePage(context.getString(R.string.oak_kanto_gift), gift = Item.POKEDEX_KANTO))
            }
            ProfessorEvent.KantoPokedex -> listOf(
                DialoguePage(context.getString(R.string.oak_forgot)),
                DialoguePage(context.getString(R.string.oak_kanto_gift), gift = Item.POKEDEX_KANTO)
            )
            is ProfessorEvent.RegionPokedex -> {
                val region = context.getString(event.region.nameRes)
                listOf(
                    DialoguePage(context.getString(event.professor.helloRes())),
                    DialoguePage(context.getString(R.string.professor_pokedex_count, pokedexCount)),
                    DialoguePage(
                        context.getString(R.string.professor_pokedex_gift, region),
                        gift = event.region.pokedex
                    ),
                    DialoguePage(context.getString(R.string.professor_number))
                )
            }
            ProfessorEvent.ShinyCharm -> listOf(
                DialoguePage(context.getString(R.string.oak_charm_1, pokedexCount)),
                DialoguePage(context.getString(R.string.oak_charm_2), gift = Item.SHINY_CHARM),
                DialoguePage(context.getString(R.string.oak_charm_3))
            )
            ProfessorEvent.TownMap -> listOf(
                DialoguePage(context.getString(R.string.oak_map_1)),
                DialoguePage(context.getString(R.string.oak_map_2), gift = Item.TOWN_MAP),
                DialoguePage(context.getString(R.string.oak_map_3))
            )
            is ProfessorEvent.LegendaryGifts -> buildList {
                add(DialoguePage(context.getString(R.string.professor_gift_hello, name)))
                event.gifts.forEach { item ->
                    add(DialoguePage(context.getString(LegendaryItems.rumor(item).textRes)))
                    add(
                        DialoguePage(
                            context.getString(
                                R.string.professor_gift_item,
                                context.getString(item.nameRes()),
                                LegendaryItems.speciesNames(item)
                            ),
                            gift = item
                        )
                    )
                }
                add(DialoguePage(context.getString(R.string.professor_gift_bye)))
            }
        }
    }

    /**
     * A Pokégear call: how the regional Pokédex is going and, if there is one, a rumor.
     * Rumors take turns so every call tells a different one.
     */
    fun call(
        context: Context,
        professor: Professor,
        pokedexIds: Set<Int>,
        preferences: PreferencesManager
    ): List<DialoguePage> {
        val region = Region.ofProfessor(professor)
        val regionName = context.getString(region.nameRes)
        val caught = pokedexIds.count { it in region.ids }
        val evaluation = when {
            caught >= region.size -> R.string.call_eval_complete
            caught * 4 >= region.size * 3 -> R.string.call_eval_high
            caught * 3 >= region.size -> R.string.call_eval_mid
            else -> R.string.call_eval_low
        }
        val rumors = Rumors.all.filter { pokedexIds.size >= it.unlockAt && Region.of(it.speciesId) == region }
        return buildList {
            add(DialoguePage(context.getString(R.string.call_hello, context.getString(professor.nameRes))))
            add(DialoguePage(context.getString(R.string.call_progress, caught, region.size, regionName)))
            add(DialoguePage(context.getString(evaluation)))
            if (rumors.isEmpty()) {
                add(DialoguePage(context.getString(R.string.call_no_rumor)))
            } else {
                val rumor = rumors[preferences.nextRumorTurn(professor.name) % rumors.size]
                add(DialoguePage(context.getString(R.string.call_rumor)))
                add(DialoguePage(context.getString(rumor.textRes)))
            }
            add(DialoguePage(context.getString(R.string.call_bye)))
        }
    }
}
