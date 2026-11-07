package dev.equalparts.glyph_catch.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.PreferencesManager

/**
 * The settings revealed by tapping the version seven times.
 */
@Composable
fun HiddenSettingsScreen(modifier: Modifier) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var playWhileInUse by remember { mutableStateOf(prefs.playWhileInUse) }
    var catchAllButton by remember { mutableStateOf(prefs.catchAllButton) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
    ) {
        VisualToggleCard(
            title = stringResource(R.string.hidden_play_in_use_title),
            subtitle = stringResource(R.string.hidden_play_in_use_subtitle),
            checked = playWhileInUse,
            onCheckedChange = {
                playWhileInUse = it
                prefs.playWhileInUse = it
            }
        )

        VisualToggleCard(
            title = stringResource(R.string.hidden_catch_all_title),
            subtitle = stringResource(R.string.hidden_catch_all_subtitle),
            checked = catchAllButton,
            onCheckedChange = {
                catchAllButton = it
                prefs.catchAllButton = it
            }
        )
    }
}
