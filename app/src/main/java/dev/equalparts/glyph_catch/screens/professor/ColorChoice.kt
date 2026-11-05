package dev.equalparts.glyph_catch.screens.professor

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.equalparts.glyph_catch.LocalColorSprites
import dev.equalparts.glyph_catch.PokemonSpriteCircle
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.Professor

/** Black and white, like everything else when color sprites are off. */
val MonochromeFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * Character sprites follow the color setting: in color, or black and white like the Glyph Matrix.
 */
@Composable
fun ProfessorSprite(professor: Professor, modifier: Modifier = Modifier) {
    CharacterSprite(professor.sprite, stringResource(professor.nameRes), modifier)
}

@Composable
fun CharacterSprite(@DrawableRes sprite: Int, name: String, modifier: Modifier = Modifier) {
    Image(
        bitmap = ImageBitmap.imageResource(sprite),
        contentDescription = name,
        filterQuality = FilterQuality.None,
        colorFilter = if (LocalColorSprites.current) null else MonochromeFilter,
        modifier = modifier
    )
}

/**
 * Asked once, before meeting Professor Oak: Pokémon in color or in the Glyph Matrix style.
 * It can be changed later in Settings.
 */
@Composable
fun ColorChoiceDialog(onChoice: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.color_choice_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ColorOption(color = true, label = stringResource(R.string.color_choice_color), onClick = {
                        onChoice(true)
                    })
                    ColorOption(color = false, label = stringResource(R.string.color_choice_matrix), onClick = {
                        onChoice(false)
                    })
                }
                Text(
                    text = stringResource(R.string.color_choice_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun RowScope.ColorOption(color: Boolean, label: String, onClick: () -> Unit) {
    CompositionLocalProvider(LocalColorSprites provides color) {
        Column(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                ProfessorSprite(Professor.OAK, modifier = Modifier.size(56.dp))
                PokemonSpriteCircle(pokemonId = 25, pokemonName = "Pikachu", size = 48.dp)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * The Egg sprite from Black and White, in black and white too when color sprites are off.
 */
@Composable
fun EggSprite(modifier: Modifier = Modifier) {
    CharacterSprite(R.drawable.egg, stringResource(R.string.pool_egg), modifier)
}

/**
 * The red dot for waiting calls, in the app's red.
 */
@Composable
fun CallBadge() {
    Badge(containerColor = MaterialTheme.colorScheme.primary)
}
