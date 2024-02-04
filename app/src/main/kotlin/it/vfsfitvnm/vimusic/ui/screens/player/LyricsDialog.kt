package it.vfsfitvnm.vimusic.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.service.isLocal
import it.vfsfitvnm.vimusic.ui.modifiers.PinchDirection
import it.vfsfitvnm.vimusic.ui.modifiers.pinchToToggle
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.px
import it.vfsfitvnm.vimusic.utils.thumbnail

@Composable
fun LyricsDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) = Dialog(onDismissRequest = onDismiss) {
    val appearance = LocalAppearance.current
    val (colorPalette) = appearance
    val thumbnailShape = appearance.thumbnailShape

    val player = LocalPlayerServiceBinder.current?.player ?: return@Dialog
    val (window, error) = currentWindow()

    LaunchedEffect(window, error) {
        if (window == null || window.mediaItem.isLocal || error != null) onDismiss()
    }

    window ?: return@Dialog

    BoxWithConstraints(
        modifier = modifier
            .padding(all = 36.dp)
            .padding(vertical = 32.dp)
            .clip(thumbnailShape)
            .fillMaxSize()
            .background(colorPalette.background1)
            .pinchToToggle(
                direction = PinchDirection.In,
                threshold = 0.9f,
                onPinch = { onDismiss() }
            )
    ) {
        val thumbnailHeight = maxHeight

        if (window.mediaItem.mediaMetadata.artworkUri != null) AsyncImage(
            model = window.mediaItem.mediaMetadata.artworkUri.thumbnail((thumbnailHeight - 64.dp).px),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .background(colorPalette.background0)
        )

        Lyrics(
            mediaId = window.mediaItem.mediaId,
            isDisplayed = true,
            onDismiss = { },
            height = thumbnailHeight,
            mediaMetadataProvider = window.mediaItem::mediaMetadata,
            durationProvider = player::getDuration,
            ensureSongInserted = { Database.insert(window.mediaItem) },
            onMenuLaunched = onDismiss
        )
    }
}
