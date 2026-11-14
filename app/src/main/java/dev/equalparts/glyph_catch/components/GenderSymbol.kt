package dev.equalparts.glyph_catch.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.gender

/** ♂ in blue or ♀ in pink after a name; nothing for genderless Pokémon. */
@Composable
fun GenderSymbol(pokemon: CaughtPokemon, style: TextStyle = MaterialTheme.typography.titleMedium) {
    val context = LocalContext.current
    val gender = remember(pokemon.id, pokemon.speciesId) { pokemon.gender(context) }
    val symbol = gender.symbol ?: return
    Text(
        text = symbol,
        style = style,
        color = gender.color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp)
    )
}
