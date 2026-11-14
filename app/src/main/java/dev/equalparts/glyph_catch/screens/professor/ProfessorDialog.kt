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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.equalparts.glyph_catch.Features
import dev.equalparts.glyph_catch.ItemGlyphCircle
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.LegendaryItems
import dev.equalparts.glyph_catch.data.NpcEvent
import dev.equalparts.glyph_catch.data.PLAYER_NAME_LENGTH
import dev.equalparts.glyph_catch.data.PlayerLook
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.Professor
import dev.equalparts.glyph_catch.data.ProfessorEvent
import dev.equalparts.glyph_catch.data.Region
import dev.equalparts.glyph_catch.data.Rumors
import dev.equalparts.glyph_catch.data.nameRes
import dev.equalparts.glyph_catch.ndotFontFamily
import kotlinx.coroutines.delay

/**
 * One box of text. Pages with a [gift] show the item the professor hands over; pages with
 * [choices] wait for the player to pick one.
 */
data class DialoguePage(
    val text: String,
    val gift: Item? = null,
    val egg: Boolean = false,
    val choices: List<DialogueChoice> = emptyList(),
    val ask: DialogueAsk? = null
)

/** A page that waits for the player to answer about themselves. */
sealed class DialogueAsk {
    class Look(val onPick: (PlayerLook) -> Unit) : DialogueAsk()
    class Name(val onDone: (String) -> Unit) : DialogueAsk()
}

/** Stands for the player's name in a page written before they gave it. */
const val PLAYER_NAME = "\u0000player\u0000"

class DialogueChoice(val label: String, val onPick: () -> Unit)

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
    val context = LocalContext.current
    var index by remember(pages) { mutableIntStateOf(0) }
    val page = pages[index.coerceIn(0, pages.lastIndex)].let { raw ->
        val text = remember(pages, index) {
            raw.text.replace(PLAYER_NAME, PreferencesManager(context).playerName.orEmpty())
        }
        raw.copy(text = text)
    }
    fun next() = if (index < pages.lastIndex) index++ else onFinish()
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
                        page.choices.isNotEmpty() || page.ask != null -> Unit
                        else -> next()
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
                if (page.choices.isNotEmpty() && shown >= page.text.length) {
                    ChoiceBox(page.choices) { choice ->
                        choice.onPick()
                        next()
                    }
                }
                if (shown >= page.text.length) {
                    when (val ask = page.ask) {
                        is DialogueAsk.Look -> LookPicker { look ->
                            ask.onPick(look)
                            next()
                        }
                        is DialogueAsk.Name -> NameEntry { name ->
                            ask.onDone(name)
                            next()
                        }
                        null -> Unit
                    }
                }
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
                    if (shown >= page.text.length && page.choices.isEmpty() && page.ask == null) {
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

/** The four looks side by side; tap one. */
@Composable
private fun LookPicker(onPick: (PlayerLook) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PlayerLook.entries.forEach { look ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                    .clickable { onPick(look) }
                    .padding(4.dp)
            ) {
                CharacterSprite(look.portrait, look.name, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/** A name box like the games': up to [PLAYER_NAME_LENGTH] letters, then OK. */
@Composable
private fun NameEntry(onDone: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val done = { if (name.isNotBlank()) onDone(name.trim()) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (name.isEmpty()) {
                Text(
                    text = stringResource(R.string.player_name_hint),
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            BasicTextField(
                value = name,
                onValueChange = { name = it.replace("\n", "").take(PLAYER_NAME_LENGTH) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { done() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
            )
        }
        Text(
            text = "▶ " + stringResource(R.string.player_name_done),
            color = if (name.isBlank()) Color.White.copy(alpha = 0.4f) else Color.White,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                .clickable(enabled = name.isNotBlank()) { done() }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

/**
 * The Yes/No box from the main games, on the right above the text.
 */
@Composable
private fun ChoiceBox(choices: List<DialogueChoice>, onPick: (DialogueChoice) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Column(
            modifier = Modifier
                .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                .padding(vertical = 4.dp)
        ) {
            choices.forEach { choice ->
                Text(
                    text = "▶ " + choice.label,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .clickable { onPick(choice) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
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
        EggSprite(modifier = Modifier.size(72.dp))
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
        NpcEvent.SuperRepel -> listOf(
            DialoguePage(context.getString(R.string.npc_clerk_super_1, pokedexCount)),
            DialoguePage(context.getString(R.string.npc_clerk_super_2), gift = Item.SUPER_REPEL),
            DialoguePage(context.getString(R.string.npc_clerk_super_3))
        )
        NpcEvent.Honey -> listOf(
            DialoguePage(context.getString(R.string.npc_bug_honey_1)),
            DialoguePage(context.getString(R.string.npc_bug_honey_2), gift = Item.HONEY),
            DialoguePage(context.getString(R.string.npc_bug_honey_3))
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

    /**
     * Oak asks the look, then the name, then says it back. Until the town is out there's nowhere
     * to see the look, so he only asks the name, with [nameOnlyRes].
     */
    private fun aboutYou(
        context: Context,
        lookRes: Int,
        nameRes: Int,
        nameOnlyRes: Int,
        thanksRes: Int
    ): List<DialoguePage> {
        val preferences = PreferencesManager(context)
        val name = DialogueAsk.Name { preferences.playerName = it }
        return if (Features.TOWN) {
            listOf(
                DialoguePage(context.getString(lookRes), ask = DialogueAsk.Look { preferences.playerLook = it.name }),
                DialoguePage(context.getString(nameRes), ask = name),
                DialoguePage(context.getString(thanksRes, PLAYER_NAME))
            )
        } else {
            listOf(
                DialoguePage(context.getString(nameOnlyRes), ask = name),
                DialoguePage(context.getString(thanksRes, PLAYER_NAME))
            )
        }
    }

    /** For players Oak welcomed before he asked their name. */
    fun forgotName(context: Context): List<DialoguePage> =
        listOf(DialoguePage(context.getString(R.string.oak_forgot_name_1))) +
            aboutYou(
                context,
                R.string.oak_forgot_look,
                R.string.oak_forgot_name_2,
                R.string.oak_forgot_name_only,
                R.string.oak_forgot_name_3
            )

    /** How to play on the Phone (4a) Pro, whose button can't catch. */
    fun pokeBallTip(context: Context): List<DialoguePage> = listOf(
        R.string.oak_poke_ball_1,
        R.string.oak_poke_ball_2,
        R.string.oak_poke_ball_3,
        R.string.oak_poke_ball_4
    ).map { DialoguePage(context.getString(it)) }

    fun event(context: Context, event: ProfessorEvent, pokedexCount: Int): List<DialoguePage> {
        val name = context.getString(event.professor.nameRes)
        return when (event) {
            ProfessorEvent.Intro -> introLines.map { DialoguePage(context.getString(it)) }.toMutableList().apply {
                add(5, DialoguePage(context.getString(R.string.oak_kanto_gift), gift = Item.POKEDEX_KANTO))
                addAll(
                    3,
                    aboutYou(
                        context,
                        R.string.oak_ask_look,
                        R.string.oak_ask_name,
                        R.string.oak_ask_name_only,
                        R.string.oak_nice_to_meet
                    )
                )
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
            ProfessorEvent.ExpShare -> {
                val preferences = PreferencesManager(context)
                listOf(
                    DialoguePage(context.getString(R.string.oak_exp_share_1)),
                    DialoguePage(context.getString(R.string.oak_exp_share_2), gift = Item.EXP_SHARE),
                    DialoguePage(
                        context.getString(R.string.oak_exp_share_ask),
                        choices = listOf(
                            DialogueChoice(context.getString(R.string.oak_exp_share_on)) {
                                preferences.expShareEnabled = true
                            },
                            DialogueChoice(context.getString(R.string.oak_exp_share_off)) {
                                preferences.expShareEnabled = false
                            }
                        )
                    ),
                    DialoguePage(context.getString(R.string.oak_exp_share_3))
                )
            }
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
