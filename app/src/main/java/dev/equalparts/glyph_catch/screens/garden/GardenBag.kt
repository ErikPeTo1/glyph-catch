package dev.equalparts.glyph_catch.screens.garden

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.Flavor
import dev.equalparts.glyph_catch.data.GardenCatalog
import dev.equalparts.glyph_catch.data.GardenState

/** One berry and how many of it. */
@Composable
fun BerryCount(flavor: Flavor, count: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            bitmap = ImageBitmap.imageResource(GardenCatalog.drawable(context, "berry", flavor.berry)),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier.size(24.dp)
        )
        Text(text = "$count", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * The town's chest: your berries. Pick one to give it to a Pokémon or plant it; opened from an
 * empty plot ([planting]), picking a berry plants it there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenBag(state: GardenState, planting: Boolean, onBerry: (Flavor) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(true)) {
        Column(modifier = Modifier.padding(horizontal = AppSizes.spacingLarge)) {
            Text(
                text = stringResource(R.string.garden_bag),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(
                    when {
                        state.totalBerries == 0 -> R.string.garden_berries_empty
                        planting -> R.string.garden_plant_hint
                        else -> R.string.garden_berries_hint
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = AppSizes.spacingSmall)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall),
                modifier = Modifier.padding(bottom = AppSizes.spacingLarge)
            ) {
                Flavor.entries.forEach { flavor -> BerryRow(flavor, state.berries(flavor)) { onBerry(flavor) } }
            }
        }
    }
}

@Composable
private fun BerryRow(flavor: Flavor, count: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        enabled = count > 0,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (count > 0) 1f else 0.45f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = AppSizes.spacingMedium, vertical = AppSizes.spacingSmall)
        ) {
            Image(
                bitmap = ImageBitmap.imageResource(GardenCatalog.drawable(LocalContext.current, "berry", flavor.berry)),
                contentDescription = null,
                filterQuality = FilterQuality.None,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = stringResource(flavor.nameRes),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AppSizes.spacingMedium)
            )
            Text(text = "×$count", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
