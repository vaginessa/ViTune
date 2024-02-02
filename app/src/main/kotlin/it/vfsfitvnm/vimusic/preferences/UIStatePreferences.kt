package it.vfsfitvnm.vimusic.preferences

import it.vfsfitvnm.vimusic.GlobalPreferencesHolder

object UIStatePreferences : GlobalPreferencesHolder() {
    var homeScreenTabIndex by int(0)
    var searchResultScreenTabIndex by int(0)

    var artistScreenTabIndexProperty = int(0)
    var artistScreenTabIndex by artistScreenTabIndexProperty
}
