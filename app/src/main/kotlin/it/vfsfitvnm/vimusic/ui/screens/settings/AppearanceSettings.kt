package it.vfsfitvnm.vimusic.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.enums.ColorPaletteName
import it.vfsfitvnm.vimusic.preferences.AppearancePreferences
import it.vfsfitvnm.vimusic.preferences.PlayerPreferences
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid13

@Composable
fun AppearanceSettings() {
    val (colorPalette) = LocalAppearance.current

    with(AppearancePreferences) {
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
            Header(title = "Appearance")

            SettingsEntryGroupText(title = "COLORS")

            EnumValueSelectorSettingsEntry(
                title = "Theme",
                selectedValue = colorPaletteName,
                onValueSelected = { colorPaletteName = it }
            )

            EnumValueSelectorSettingsEntry(
                title = "Theme mode",
                selectedValue = colorPaletteMode,
                isEnabled = colorPaletteName != ColorPaletteName.PureBlack,
                onValueSelected = { colorPaletteMode = it }
            )

            SettingsGroupSpacer()

            SettingsEntryGroupText(title = "SHAPES")

            EnumValueSelectorSettingsEntry(
                title = "Thumbnail roundness",
                selectedValue = thumbnailRoundness,
                onValueSelected = { thumbnailRoundness = it },
                trailingContent = {
                    Spacer(
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = colorPalette.accent,
                                shape = thumbnailRoundness.shape
                            )
                            .background(
                                color = colorPalette.background1,
                                shape = thumbnailRoundness.shape
                            )
                            .size(36.dp)
                    )
                },
                valueText = { it.desc(it) }
            )

            SettingsGroupSpacer()

            SettingsEntryGroupText(title = "TEXT")

            SwitchSettingEntry(
                title = "Use system font",
                text = "Use the font applied by the system",
                isChecked = useSystemFont,
                onCheckedChange = { useSystemFont = it }
            )

            SwitchSettingEntry(
                title = "Apply font padding",
                text = "Add spacing around texts",
                isChecked = applyFontPadding,
                onCheckedChange = { applyFontPadding = it }
            )

            if (!isAtLeastAndroid13) {
                SettingsGroupSpacer()

                SettingsEntryGroupText(title = "LOCKSCREEN")

                SwitchSettingEntry(
                    title = "Show song cover",
                    text = "Use the playing song cover as the lockscreen wallpaper",
                    isChecked = isShowingThumbnailInLockscreen,
                    onCheckedChange = { isShowingThumbnailInLockscreen = it }
                )
            }

            SettingsGroupSpacer()

            SettingsEntryGroupText(title = "PLAYER")

            SwitchSettingEntry(
                title = "Previous button while collapsed",
                text = "Shows the previous song button while the player is collapsed",
                isChecked = PlayerPreferences.isShowingPrevButtonCollapsed,
                onCheckedChange = { PlayerPreferences.isShowingPrevButtonCollapsed = it }
            )

            SwitchSettingEntry(
                title = "Swipe horizontally to close",
                text = "Closes the player when swiping left/right on the collapsed player. Useful for users with Android's one-handed mode enabled.",
                isChecked = PlayerPreferences.horizontalSwipeToClose,
                onCheckedChange = { PlayerPreferences.horizontalSwipeToClose = it }
            )

            SwitchSettingEntry(
                title = "Show like button",
                text = "Show the like button directly in the player",
                isChecked = PlayerPreferences.showLike,
                onCheckedChange = { PlayerPreferences.showLike = it }
            )
        }
    }
}
