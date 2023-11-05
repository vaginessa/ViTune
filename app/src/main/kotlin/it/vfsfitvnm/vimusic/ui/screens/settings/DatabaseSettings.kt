package it.vfsfitvnm.vimusic.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.internal
import it.vfsfitvnm.vimusic.path
import it.vfsfitvnm.vimusic.preferences.DataPreferences
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.service.PlayerService
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.intent
import it.vfsfitvnm.vimusic.utils.toast
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.system.exitProcess

@ExperimentalAnimationApi
@Composable
fun DatabaseSettings() {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current

    val eventsCount by remember { Database.eventsCount().distinctUntilChanged() }.collectAsState(
        initial = 0
    )

    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.sqlite3")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            query {
                Database.checkpoint()

                context.applicationContext.contentResolver.openOutputStream(uri)
                    ?.use { outputStream ->
                        FileInputStream(Database.internal.path).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
            }
        }

    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            query {
                Database.checkpoint()
                Database.internal.close()

                context.applicationContext.contentResolver.openInputStream(uri)
                    ?.use { inputStream ->
                        FileOutputStream(Database.internal.path).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                context.stopService(context.intent<PlayerService>())
                exitProcess(0)
            }
        }
    with(DataPreferences) {
        Column(
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    LocalPlayerAwareWindowInsets.current
                        .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                        .asPaddingValues()
                )
        ) {
            Header(title = "Database")

            SettingsEntryGroupText(title = "CLEANUP")

            SwitchSettingEntry(
                title = "Pause playback history",
                text = "Stops playback events being used for quick picks",
                isChecked = pauseHistory,
                onCheckedChange = { pauseHistory = !pauseHistory }
            )

            AnimatedVisibility(visible = pauseHistory) {
                ImportantSettingsDescription(text = "Please note: this won't affect offline caching!")
            }

            AnimatedVisibility(visible = !(pauseHistory && eventsCount == 0)) {
                SettingsEntry(
                    title = "Reset quick picks",
                    text = if (eventsCount > 0) {
                        "Delete $eventsCount playback events"
                    } else {
                        "Quick picks are cleared"
                    },
                    onClick = { query(Database::clearEvents) },
                    isEnabled = eventsCount > 0
                )
            }

            SwitchSettingEntry(
                title = "Pause playback time",
                text = "Stops playback time from being saved. This pauses the statistics in the 'My Top $topListLength' playlist!",
                isChecked = pausePlaytime,
                onCheckedChange = { pausePlaytime = !pausePlaytime }
            )

            SettingsGroupSpacer()

            SettingsEntryGroupText(title = "BACKUP")

            SettingsDescription(text = "Personal preferences (i.e. the theme mode) and the cache are excluded.")

            SettingsEntry(
                title = "Backup",
                text = "Export the database to the external storage",
                onClick = {
                    @SuppressLint("SimpleDateFormat")
                    val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")

                    try {
                        backupLauncher.launch("vimusic_${dateFormat.format(Date())}.db")
                    } catch (e: ActivityNotFoundException) {
                        context.toast("Couldn't find an application to create documents")
                    }
                }
            )

            SettingsGroupSpacer()

            SettingsEntryGroupText(title = "RESTORE")

            ImportantSettingsDescription(text = "Existing data will be overwritten.\n${context.applicationInfo.nonLocalizedLabel} will automatically close itself after restoring the database.")

            SettingsEntry(
                title = "Restore",
                text = "Import the database from the external storage",
                onClick = {
                    try {
                        restoreLauncher.launch(
                            arrayOf(
                                "application/vnd.sqlite3",
                                "application/x-sqlite3",
                                "application/octet-stream"
                            )
                        )
                    } catch (e: ActivityNotFoundException) {
                        context.toast("Couldn't find an application to open documents")
                    }
                }
            )
        }
    }
}
