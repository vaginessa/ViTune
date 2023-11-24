package it.vfsfitvnm.vimusic.preferences

import it.vfsfitvnm.vimusic.GlobalPreferencesHolder

object PlayerPreferences : GlobalPreferencesHolder() {
    var isInvincibilityEnabled by boolean(false)
    var trackLoopEnabled by boolean(false)
    var queueLoopEnabled by boolean(true)
    var skipSilence by boolean(false)
    var minimumSilence by long(2_000_000L)
    var volumeNormalization by boolean(false)
    var volumeNormalizationBaseGain by float(5.00f)
    var bassBoost by boolean(false)
    var bassBoostLevel by int(5)
    var resumePlaybackWhenDeviceConnected by boolean(false)
    var persistentQueue by boolean(false)
    var isShowingLyrics by boolean(false)
    var isShowingSynchronizedLyrics by boolean(false)
    var speed by float(1f)
    var isShowingPrevButtonCollapsed by boolean(false)
    var stopWhenClosed by boolean(false)
    var horizontalSwipeToClose by boolean(false)
    var horizontalSwipeToRemoveItem by boolean(false)
    var showLike by boolean(true)

    val volumeNormalizationBaseGainRounded get() = (volumeNormalizationBaseGain * 100).toInt()
}