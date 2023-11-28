package it.vfsfitvnm.vimusic.preferences

import it.vfsfitvnm.vimusic.GlobalPreferencesHolder
import it.vfsfitvnm.vimusic.enums.CoilDiskCacheSize
import it.vfsfitvnm.vimusic.enums.ExoPlayerDiskCacheSize
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

object DataPreferences : GlobalPreferencesHolder() {
    var coilDiskCacheMaxSize by enum(CoilDiskCacheSize.`128MB`)
    var exoPlayerDiskCacheMaxSize by enum(ExoPlayerDiskCacheSize.`2GB`)
    var pauseHistory by boolean(false)
    var pausePlaytime by boolean(false)
    var pauseSearchHistory by boolean(false)
    var topListLength by int(10)
    var topListPeriod by enum(TopListPeriod.AllTime)

    enum class TopListPeriod(val displayName: String, val duration: Duration? = null) {
        PastDay(displayName = "Past 24 hours", duration = 1.days),
        PastWeek(displayName = "Past week", duration = 7.days),
        PastMonth(displayName = "Past month", duration = 30.days),
        PastYear(displayName = "Past year", 365.days),
        AllTime(displayName = "All time")
    }
}
