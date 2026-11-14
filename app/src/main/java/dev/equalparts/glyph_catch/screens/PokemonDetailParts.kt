package dev.equalparts.glyph_catch.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.PokemonExpChip
import dev.equalparts.glyph_catch.PokemonLevelChip
import dev.equalparts.glyph_catch.PokemonTypeChips
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.components.GenderSymbol
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.FriendshipRules
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.Type
import dev.equalparts.glyph_catch.data.displayName
import dev.equalparts.glyph_catch.data.nameRes
import dev.equalparts.glyph_catch.data.usesFemaleSprite
import dev.equalparts.glyph_catch.screens.garden.AnimatedPokemonSprite
import dev.equalparts.glyph_catch.screens.garden.FriendshipHearts

/** The usual colours of each type in the games, for the background behind the sprite. */
internal fun Type.color(): Color = when (this) {
    Type.NORMAL -> Color(0xFFA8A878)
    Type.FIRE -> Color(0xFFF08030)
    Type.WATER -> Color(0xFF6890F0)
    Type.ELECTRIC -> Color(0xFFF8D030)
    Type.GRASS -> Color(0xFF78C850)
    Type.ICE -> Color(0xFF98D8D8)
    Type.FIGHTING -> Color(0xFFC03028)
    Type.POISON -> Color(0xFFA040A0)
    Type.GROUND -> Color(0xFFE0C068)
    Type.FLYING -> Color(0xFFA890F0)
    Type.PSYCHIC -> Color(0xFFF85888)
    Type.BUG -> Color(0xFFA8B820)
    Type.ROCK -> Color(0xFFB8A038)
    Type.GHOST -> Color(0xFF705898)
    Type.DRAGON -> Color(0xFF7038F8)
    Type.DARK -> Color(0xFF705848)
    Type.STEEL -> Color(0xFFB8B8D0)
    Type.FAIRY -> Color(0xFFEE99AC)
}

/**
 * The top of a Pokémon's details: its animated sprite on its type's colours, its name (tap to
 * give it a nickname), number, types, level and how fond of you it is.
 */
@Composable
fun PokemonHero(pokemon: CaughtPokemon, onToggleFavorite: () -> Unit, onRename: (String?) -> Unit) {
    val species = Pokemon[pokemon.speciesId]
    val speciesName = species?.displayName ?: stringResource(R.string.common_unknown)
    val colors = listOfNotNull(species?.type1?.color(), species?.type2?.color() ?: species?.type1?.color())
    var renaming by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val female = remember(pokemon.id, pokemon.speciesId) { pokemon.usesFemaleSprite(context) }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(colors.map { it.copy(alpha = 0.55f) } + Color.Transparent)
                    )
            ) {
                AnimatedPokemonSprite(
                    speciesId = pokemon.speciesId,
                    shiny = pokemon.isShiny,
                    form = pokemon.form,
                    female = female,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(180.dp)
                )
                IconButton(onClick = onToggleFavorite, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(
                        imageVector = if (pokemon.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (pokemon.isFavorite) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
            }
            Column(
                modifier = Modifier.padding(AppSizes.spacingLarge),
                verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(AppSizes.spacingSmall))
                            .clickable { renaming = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
                    ) {
                        Text(
                            text = (pokemon.nickname ?: speciesName) + if (pokemon.isShiny) " ✨" else "",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        GenderSymbol(pokemon, style = MaterialTheme.typography.headlineSmall)
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.nickname_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    PokemonLevelChip(level = pokemon.level)
                }
                Text(
                    text = buildString {
                        if (pokemon.nickname != null) append(speciesName).append(" · ")
                        append(stringResource(R.string.pokedex_entry_number, pokemon.speciesId))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                species?.let {
                    PokemonTypeChips(
                        types = listOfNotNull(
                            stringResource(it.type1.nameRes()),
                            it.type2?.let { t ->
                                stringResource(t.nameRes())
                            }
                        )
                    )
                }
                PokemonExpChip(level = pokemon.level, exp = pokemon.exp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
                ) {
                    FriendshipHearts(friendship = pokemon.friendship)
                    Text(
                        text = stringResource(FriendshipRules.phrase(pokemon.friendship)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (renaming) {
        NicknameDialog(
            current = pokemon.nickname,
            speciesName = speciesName,
            onSave = {
                onRename(it)
                renaming = false
            },
            onDismiss = { renaming = false }
        )
    }
}

private const val NICKNAME_LENGTH = 12

/** Up to 12 characters, like in the games. Saving it empty goes back to the species name. */
@Composable
private fun NicknameDialog(current: String?, speciesName: String, onSave: (String?) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(current.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nickname_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(NICKNAME_LENGTH) },
                singleLine = true,
                placeholder = { Text(speciesName) },
                supportingText = { Text("${text.length}/$NICKNAME_LENGTH") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.trim().takeIf { it.isNotEmpty() }) }) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            Row {
                if (current != null) {
                    TextButton(onClick = { onSave(null) }) { Text(stringResource(R.string.nickname_remove)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
            }
        }
    )
}

/** One of the three things to do with a Pokémon: filled when it's on. */
@Composable
fun DetailAction(
    label: String,
    on: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (on) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            contentPadding = ButtonDefaults.TextButtonContentPadding
        ) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            contentPadding = ButtonDefaults.TextButtonContentPadding
        ) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A part of the details you only open when you want it. */
@Composable
fun DetailSection(title: String, initiallyOpen: Boolean = false, content: @Composable () -> Unit) {
    var open by rememberSaveable(title) { mutableStateOf(initiallyOpen) }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { open = !open }
                    .padding(AppSizes.spacingLarge),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            AnimatedVisibility(visible = open) {
                Column(
                    modifier = Modifier.padding(
                        start = AppSizes.spacingLarge,
                        end = AppSizes.spacingLarge,
                        bottom = AppSizes.spacingLarge
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)
                ) { content() }
            }
        }
    }
}
