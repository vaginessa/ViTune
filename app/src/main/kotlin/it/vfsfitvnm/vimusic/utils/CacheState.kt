package it.vfsfitvnm.vimusic.utils

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.models.Format
import it.vfsfitvnm.vimusic.service.PrecacheService
import it.vfsfitvnm.vimusic.ui.components.themed.HeaderIconButton
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun PlaylistDownloadIcon(songs: List<MediaItem>) {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current

    if (!songs.all { isCached(mediaId = it.mediaId) }) HeaderIconButton(
        icon = R.drawable.download,
        color = colorPalette.text,
        onClick = {
            songs.forEach {
                PrecacheService.scheduleCache(context.applicationContext, it)
            }
        }
    )
}

@OptIn(UnstableApi::class)
@Composable
fun isCached(mediaId: String): Boolean {
    val cache = LocalPlayerServiceBinder.current?.cache ?: return false
    var format: Format? by remember { mutableStateOf(null) }

    LaunchedEffect(mediaId) {
        Database.format(mediaId).distinctUntilChanged().collect { format = it }
    }

    return format?.contentLength?.let { len ->
        cache.isCached(mediaId, 0, len)
    } ?: false
}
