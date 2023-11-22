package it.vfsfitvnm.vimusic.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.DatabaseInitializer
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.preferences.DataPreferences
import it.vfsfitvnm.vimusic.preferences.PlayerPreferences
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.service.PlayerMediaBrowserService
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.components.themed.SecondaryTextButton
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.findActivity
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid12
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid6
import it.vfsfitvnm.vimusic.utils.isIgnoringBatteryOptimizations
import it.vfsfitvnm.vimusic.utils.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@SuppressLint("BatteryLife")
@ExperimentalAnimationApi
@Composable
fun OtherSettings() {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val uriHandler = LocalUriHandler.current
    val (colorPalette) = LocalAppearance.current

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var isAndroidAutoEnabled by remember {
        val component = ComponentName(context, PlayerMediaBrowserService::class.java)
        val disabledFlag = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val enabledFlag = PackageManager.COMPONENT_ENABLED_STATE_ENABLED

        mutableStateOf(
            value = context.packageManager.getComponentEnabledSetting(component) == enabledFlag,
            policy = object : SnapshotMutationPolicy<Boolean> {
                override fun equivalent(a: Boolean, b: Boolean): Boolean {
                    context.packageManager.setComponentEnabledSetting(
                        component,
                        if (b) enabledFlag else disabledFlag,
                        PackageManager.DONT_KILL_APP
                    )
                    return a == b
                }
            }
        )
    }

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(context.isIgnoringBatteryOptimizations)
    }

    val activityResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { isIgnoringBatteryOptimizations = context.isIgnoringBatteryOptimizations }
    )

    val queriesCount by remember {
        Database.queriesCount().distinctUntilChanged()
    }.collectAsState(initial = 0)

    Column(
        modifier = Modifier
            .background(colorPalette.background0)
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues()
            )
    ) {
        Header(title = "Other")

        SettingsEntryGroupText(title = "ANDROID AUTO")

        SwitchSettingEntry(
            title = "Android Auto",
            text = "Enable Android Auto support",
            isChecked = isAndroidAutoEnabled,
            onCheckedChange = { isAndroidAutoEnabled = it }
        )

        AnimatedVisibility(visible = isAndroidAutoEnabled) {
            SettingsDescription(text = "Remember to enable \"Unknown sources\" in the Developer Settings of Android Auto.")
        }

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = "SEARCH HISTORY")

        SwitchSettingEntry(
            title = "Pause search history",
            text = "Neither save new searched queries nor show history",
            isChecked = DataPreferences.pauseSearchHistory,
            onCheckedChange = { DataPreferences.pauseSearchHistory = it }
        )

        AnimatedVisibility(visible = !(DataPreferences.pauseSearchHistory && queriesCount == 0)) {
            SettingsEntry(
                title = "Clear search history",
                text = if (queriesCount > 0) {
                    "Delete $queriesCount search queries"
                } else {
                    "History is empty"
                },
                onClick = { query(Database::clearQueries) },
                isEnabled = queriesCount > 0
            )
        }

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = "BUILT-IN PLAYLISTS")

        IntSettingEntry(
            title = "Top list length",
            text = "Limits the length of the 'My top x' playlist",
            currentValue = DataPreferences.topListLength,
            setValue = { DataPreferences.topListLength = it },
            defaultValue = 10,
            range = 1..500
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = "SERVICE LIFETIME")

        AnimatedVisibility(visible = !isIgnoringBatteryOptimizations) {
            ImportantSettingsDescription(text = "If battery optimizations are applied, the playback notification can suddenly disappear when paused.")
        }

        if (isAtLeastAndroid12)
            SettingsDescription(text = "Since Android 12, disabling battery optimizations is required for the invincible service option to be available.")

        SettingsEntry(
            title = "Ignore battery optimizations",
            text = if (isIgnoringBatteryOptimizations) "Restriction already lifted" else "Disable background restrictions",
            onClick = {
                if (!isAtLeastAndroid6) return@SettingsEntry

                try {
                    activityResultLauncher.launch(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                } catch (e: ActivityNotFoundException) {
                    try {
                        activityResultLauncher.launch(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        )
                    } catch (e: ActivityNotFoundException) {
                        context.toast("Couldn't find battery optimization settings, please whitelist ViMusic manually")
                    }
                }
            },
            isEnabled = !isIgnoringBatteryOptimizations
        )

        AnimatedVisibility(!isAtLeastAndroid12 || isIgnoringBatteryOptimizations) {
            SwitchSettingEntry(
                title = "Invincible service",
                text = "Should keep the playback going 99.99% of the time, in case turning off the battery optimizations is not enough",
                isChecked = PlayerPreferences.isInvincibilityEnabled,
                onCheckedChange = { PlayerPreferences.isInvincibilityEnabled = it }
            )
        }

        SettingsEntry(
            title = "Need help?",
            text = "Most of the time, it is not the developer's fault (even after turning on invincible service) that the app stops working properly in the background.\n" +
                    "Check if your device manufacturer kills your apps (click to redirect)",
            onClick = {
                uriHandler.openUri("https://dontkillmyapp.com/")
            }
        )

        SettingsDescription(text = "If you really think there is something wrong with the app itself, hop on to the About tab")

        SettingsGroupSpacer()

        var showTroubleshoot by rememberSaveable { mutableStateOf(false) }

        AnimatedContent(showTroubleshoot, label = "") { show ->
            if (show) Column {
                SettingsEntryGroupText(title = "TROUBLESHOOTING")

                ImportantSettingsDescription(text = "Caution: use these buttons as a last resort when audio playback fails")

                val troubleshootScope = rememberCoroutineScope()
                var reloading by rememberSaveable { mutableStateOf(false) }

                SecondaryTextButton(
                    text = "Reload app internals",
                    onClick = {
                        if (!reloading) troubleshootScope.launch {
                            reloading = true
                            binder?.restartForegroundOrStop()
                            DatabaseInitializer.reload()
                            reloading = false
                        }
                    },
                    enabled = !reloading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                SecondaryTextButton(
                    text = "Kill app",
                    onClick = {
                        binder?.stopRadio()
                        binder?.invincible = false
                        context.findActivity().finishAndRemoveTask()
                        binder?.restartForegroundOrStop()
                        troubleshootScope.launch {
                            delay(500L)
                            Handler(Looper.getMainLooper()).postAtFrontOfQueue { exitProcess(0) }
                        }
                    },
                    enabled = !reloading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .padding(horizontal = 16.dp)
                )

                SettingsGroupSpacer()
            } else {
                SecondaryTextButton(
                    text = "Show troubleshoot section",
                    onClick = {
                        coroutineScope.launch {
                            delay(500)
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                        showTroubleshoot = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, bottom = 16.dp)
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}
