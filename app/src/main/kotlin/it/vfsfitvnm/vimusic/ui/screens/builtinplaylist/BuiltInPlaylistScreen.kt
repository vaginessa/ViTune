package it.vfsfitvnm.vimusic.ui.screens.builtinplaylist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import it.vfsfitvnm.compose.persist.PersistMapCleanup
import it.vfsfitvnm.compose.routing.RouteHandler
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.enums.BuiltInPlaylist
import it.vfsfitvnm.vimusic.preferences.DataPreferences
import it.vfsfitvnm.vimusic.ui.components.themed.Scaffold
import it.vfsfitvnm.vimusic.ui.screens.GlobalRoutes
import it.vfsfitvnm.vimusic.ui.screens.Route

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Route
@Composable
fun BuiltInPlaylistScreen(builtInPlaylist: BuiltInPlaylist) {
    val saveableStateHolder = rememberSaveableStateHolder()

    val (tabIndex, onTabIndexChanged) = rememberSaveable {
        mutableIntStateOf(
            when (builtInPlaylist) {
                BuiltInPlaylist.Favorites -> 0
                BuiltInPlaylist.Offline -> 1
                BuiltInPlaylist.Top -> 2
            }
        )
    }

    PersistMapCleanup(tagPrefix = "${builtInPlaylist.name}/")

    RouteHandler(listenToGlobalEmitter = true) {
        GlobalRoutes()

        NavHost {
            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = tabIndex,
                onTabChanged = onTabIndexChanged,
                tabColumnContent = { item ->
                    item(0, "Favorites", R.drawable.heart)
                    item(1, "Offline", R.drawable.airplane)
                    item(2, "Top ${DataPreferences.topListLength}", R.drawable.trending_up)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> BuiltInPlaylistSongs(builtInPlaylist = BuiltInPlaylist.Favorites)
                        1 -> BuiltInPlaylistSongs(builtInPlaylist = BuiltInPlaylist.Offline)
                        2 -> BuiltInPlaylistSongs(builtInPlaylist = BuiltInPlaylist.Top)
                    }
                }
            }
        }
    }
}
