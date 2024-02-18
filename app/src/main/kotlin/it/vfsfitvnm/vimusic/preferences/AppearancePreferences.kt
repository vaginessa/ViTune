package it.vfsfitvnm.vimusic.preferences

import it.vfsfitvnm.vimusic.GlobalPreferencesHolder
import it.vfsfitvnm.vimusic.enums.ColorPaletteMode
import it.vfsfitvnm.vimusic.enums.ColorPaletteName
import it.vfsfitvnm.vimusic.enums.ThumbnailRoundness

object AppearancePreferences : GlobalPreferencesHolder() {
    val colorPaletteNameProperty = enum(ColorPaletteName.Dynamic)
    var colorPaletteName by colorPaletteNameProperty
    val colorPaletteModeProperty = enum(ColorPaletteMode.System)
    var colorPaletteMode by colorPaletteModeProperty
    val thumbnailRoundnessProperty = enum(ThumbnailRoundness.Light)
    var thumbnailRoundness by thumbnailRoundnessProperty
    val useSystemFontProperty = boolean(false)
    var useSystemFont by useSystemFontProperty
    val applyFontPaddingProperty = boolean(false)
    var applyFontPadding by applyFontPaddingProperty
    val isShowingThumbnailInLockscreenProperty = boolean(false)
    var isShowingThumbnailInLockscreen by isShowingThumbnailInLockscreenProperty
    var swipeToHideSong by boolean(false)
}
