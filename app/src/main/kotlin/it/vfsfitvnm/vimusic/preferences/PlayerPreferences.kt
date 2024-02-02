package it.vfsfitvnm.vimusic.preferences

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.vfsfitvnm.vimusic.GlobalPreferencesHolder
import it.vfsfitvnm.vimusic.R

object PlayerPreferences : GlobalPreferencesHolder() {
    val isInvincibilityEnabledProperty = boolean(false)
    var isInvincibilityEnabled by isInvincibilityEnabledProperty
    val trackLoopEnabledProperty = boolean(false)
    var trackLoopEnabled by trackLoopEnabledProperty
    val queueLoopEnabledProperty = boolean(true)
    var queueLoopEnabled by queueLoopEnabledProperty
    val skipSilenceProperty = boolean(false)
    var skipSilence by skipSilenceProperty
    val volumeNormalizationProperty = boolean(false)
    var volumeNormalization by volumeNormalizationProperty
    val volumeNormalizationBaseGainProperty = float(5.00f)
    var volumeNormalizationBaseGain by volumeNormalizationBaseGainProperty
    val bassBoostProperty = boolean(false)
    var bassBoost by bassBoostProperty
    val bassBoostLevelProperty = int(5)
    var bassBoostLevel by bassBoostLevelProperty
    val resumePlaybackWhenDeviceConnectedProperty = boolean(false)
    var resumePlaybackWhenDeviceConnected by resumePlaybackWhenDeviceConnectedProperty
    val speedProperty = float(1f)
    var speed by speedProperty

    var minimumSilence by long(2_000_000L)
    var persistentQueue by boolean(false)
    var isShowingLyrics by boolean(false)
    var isShowingSynchronizedLyrics by boolean(false)
    var isShowingPrevButtonCollapsed by boolean(false)
    var stopWhenClosed by boolean(false)
    var horizontalSwipeToClose by boolean(false)
    var horizontalSwipeToRemoveItem by boolean(false)
    var playerLayout by enum(PlayerLayout.New)
    var seekBarStyle by enum(SeekBarStyle.Wavy)
    var showLike by boolean(false)

    enum class PlayerLayout(val displayName: @Composable () -> String) {
        Classic(displayName = { stringResource(R.string.classic_player_layout_name) }),
        New(displayName = { stringResource(R.string.new_player_layout_name) })
    }

    enum class SeekBarStyle(val displayName: @Composable () -> String) {
        Static(displayName = { stringResource(R.string.static_seek_bar_name) }),
        Wavy(displayName = { stringResource(R.string.wavy_seek_bar_name) })
    }

    val volumeNormalizationBaseGainRounded get() = (volumeNormalizationBaseGain * 100).toInt()
}
