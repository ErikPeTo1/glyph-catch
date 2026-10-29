package dev.equalparts.glyph_catch.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.InvalidSaveFileException
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.SaveBackup
import dev.equalparts.glyph_catch.data.SaveFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupSettingsScreen(modifier: Modifier) {
    val context = LocalContext.current
    val db = remember { PokemonDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<SaveFile?>(null) }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            try {
                val content = SaveBackup.export(context, db)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(content.toByteArray()) }
                        ?: error("Can't open file")
                }
                toast(context.getString(R.string.settings_backup_export_success))
            } catch (_: Exception) {
                toast(context.getString(R.string.settings_backup_export_error))
            } finally {
                busy = false
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            try {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                        ?: error("Can't open file")
                }
                pendingImport = SaveBackup.parse(content)
            } catch (_: InvalidSaveFileException) {
                toast(context.getString(R.string.settings_backup_import_invalid))
            } catch (_: Exception) {
                toast(context.getString(R.string.settings_backup_import_error))
            } finally {
                busy = false
            }
        }
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
    ) {
        BackupCard(
            title = stringResource(R.string.settings_backup_export_title),
            description = stringResource(R.string.settings_backup_export_description)
        ) {
            Button(
                onClick = {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    exportLauncher.launch("glyph-catch-$date.json")
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_backup_export_button))
            }
        }

        BackupCard(
            title = stringResource(R.string.settings_backup_import_title),
            description = stringResource(R.string.settings_backup_import_description)
        ) {
            OutlinedButton(
                onClick = {
                    importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_backup_import_button))
            }
        }
    }

    pendingImport?.let { save ->
        val summary = SaveBackup.summary(save)
        val exportedAt = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(save.exportedAt))
        AlertDialog(
            onDismissRequest = { if (!busy) pendingImport = null },
            title = { Text(stringResource(R.string.settings_backup_import_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.settings_backup_import_confirm_message,
                        exportedAt,
                        summary.pokemon,
                        summary.pokedex,
                        summary.items
                    )
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                SaveBackup.import(context, db, save)
                                toast(context.getString(R.string.settings_backup_import_success))
                            } catch (_: Exception) {
                                toast(context.getString(R.string.settings_backup_import_error))
                            } finally {
                                busy = false
                                pendingImport = null
                            }
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.settings_backup_import_confirm_button),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { pendingImport = null }) {
                    Text(stringResource(R.string.caught_screen_release_cancel))
                }
            }
        )
    }
}

@Composable
private fun BackupCard(title: String, description: String, action: @Composable () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(AppSizes.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            action()
        }
    }
}
