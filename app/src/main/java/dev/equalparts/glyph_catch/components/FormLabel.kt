package dev.equalparts.glyph_catch.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.equalparts.glyph_catch.R

/**
 * Display name of a form: Unown letters as-is, the rest from string resources.
 */
@Composable
fun formLabel(form: String): String = when (form) {
    "exclamation" -> "!"
    "question" -> "?"
    "normal" -> stringResource(R.string.form_normal)
    "sunny" -> stringResource(R.string.form_sunny)
    "rainy" -> stringResource(R.string.form_rainy)
    "snowy" -> stringResource(R.string.form_snowy)
    "spring" -> stringResource(R.string.form_spring)
    "summer" -> stringResource(R.string.form_summer)
    "autumn" -> stringResource(R.string.form_autumn)
    "winter" -> stringResource(R.string.form_winter)
    "plant" -> stringResource(R.string.form_plant)
    "sandy" -> stringResource(R.string.form_sandy)
    "trash" -> stringResource(R.string.form_trash)
    "west" -> stringResource(R.string.form_west)
    "east" -> stringResource(R.string.form_east)
    else -> form.uppercase()
}
