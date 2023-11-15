package it.vfsfitvnm.vimusic.ui.screens.player

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import com.valentinilk.shimmer.shimmer
import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.models.bodies.NextBody
import it.vfsfitvnm.innertube.requests.lyrics
import it.vfsfitvnm.kugou.KuGou
import it.vfsfitvnm.lrclib.LrcLib
import it.vfsfitvnm.lrclib.models.Track
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.models.Lyrics
import it.vfsfitvnm.vimusic.preferences.PlayerPreferences
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.transaction
import it.vfsfitvnm.vimusic.ui.components.LocalMenuState
import it.vfsfitvnm.vimusic.ui.components.themed.DefaultDialog
import it.vfsfitvnm.vimusic.ui.components.themed.Menu
import it.vfsfitvnm.vimusic.ui.components.themed.MenuEntry
import it.vfsfitvnm.vimusic.ui.components.themed.TextFieldDialog
import it.vfsfitvnm.vimusic.ui.components.themed.TextPlaceholder
import it.vfsfitvnm.vimusic.ui.components.themed.ValueSelectorDialogBody
import it.vfsfitvnm.vimusic.ui.styling.DefaultDarkColorPalette
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.ui.styling.PureBlackColorPalette
import it.vfsfitvnm.vimusic.ui.styling.onOverlayShimmer
import it.vfsfitvnm.vimusic.utils.SynchronizedLyrics
import it.vfsfitvnm.vimusic.utils.center
import it.vfsfitvnm.vimusic.utils.color
import it.vfsfitvnm.vimusic.utils.medium
import it.vfsfitvnm.vimusic.utils.semiBold
import it.vfsfitvnm.vimusic.utils.toast
import it.vfsfitvnm.vimusic.utils.verticalFadingEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun Lyrics(
    mediaId: String,
    isDisplayed: Boolean,
    onDismiss: () -> Unit,
    size: Dp,
    mediaMetadataProvider: () -> MediaMetadata,
    durationProvider: () -> Long,
    ensureSongInserted: () -> Unit,
    modifier: Modifier = Modifier
) = with(PlayerPreferences) {
    AnimatedVisibility(
        visible = isDisplayed,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val (colorPalette, typography) = LocalAppearance.current
        val context = LocalContext.current
        val menuState = LocalMenuState.current
        val currentView = LocalView.current
        val binder = LocalPlayerServiceBinder.current

        var isEditing by remember(mediaId, isShowingSynchronizedLyrics) { mutableStateOf(false) }
        var isPicking by remember(mediaId, isShowingSynchronizedLyrics) { mutableStateOf(false) }
        var lyrics by remember { mutableStateOf<Lyrics?>(null) }
        val text = if (isShowingSynchronizedLyrics) lyrics?.synced else lyrics?.fixed
        var isError by remember(mediaId, isShowingSynchronizedLyrics) { mutableStateOf(false) }

        LaunchedEffect(mediaId, isShowingSynchronizedLyrics) {
            withContext(Dispatchers.IO) {
                Database.lyrics(mediaId).collect { currentLyrics ->
                    when {
                        isShowingSynchronizedLyrics && currentLyrics?.synced == null -> {
                            lyrics = null
                            val mediaMetadata = mediaMetadataProvider()
                            var duration = withContext(Dispatchers.Main) {
                                durationProvider()
                            }

                            while (duration == C.TIME_UNSET) {
                                delay(100)
                                duration = withContext(Dispatchers.Main) {
                                    durationProvider()
                                }
                            }

                            val album = mediaMetadata.albumTitle?.toString()
                            val artist = mediaMetadata.artist?.toString() ?: ""
                            val title = mediaMetadata.title?.toString() ?: ""

                            LrcLib.lyrics(
                                artist = artist,
                                title = title,
                                duration = duration.milliseconds,
                                album = album
                            )?.onSuccess {
                                Database.upsert(
                                    Lyrics(
                                        songId = mediaId,
                                        fixed = currentLyrics?.fixed,
                                        synced = it?.text ?: ""
                                    )
                                )
                            }?.onFailure {
                                KuGou.lyrics(
                                    artist = artist,
                                    title = title,
                                    duration = duration / 1000
                                )?.onSuccess {
                                    Database.upsert(
                                        Lyrics(
                                            songId = mediaId,
                                            fixed = currentLyrics?.fixed,
                                            synced = it?.value ?: ""
                                        )
                                    )
                                }?.onFailure {
                                    isError = true
                                }
                            }
                        }

                        !isShowingSynchronizedLyrics && currentLyrics?.fixed == null -> {
                            lyrics = null
                            Innertube.lyrics(NextBody(videoId = mediaId))?.onSuccess {
                                Database.upsert(
                                    Lyrics(
                                        songId = mediaId,
                                        fixed = it ?: "",
                                        synced = currentLyrics?.synced
                                    )
                                )
                            }?.onFailure {
                                isError = true
                            }
                        }

                        else -> lyrics = currentLyrics
                    }
                }
            }
        }

        if (isEditing) TextFieldDialog(
            hintText = "Enter the lyrics",
            initialTextInput = text ?: "",
            singleLine = false,
            maxLines = 10,
            isTextInputValid = { true },
            onDismiss = { isEditing = false },
            onDone = {
                query {
                    ensureSongInserted()
                    Database.upsert(
                        Lyrics(
                            songId = mediaId,
                            fixed = if (isShowingSynchronizedLyrics) lyrics?.fixed else it,
                            synced = if (isShowingSynchronizedLyrics) it else lyrics?.synced,
                        )
                    )
                }
            }
        )

        if (isPicking && isShowingSynchronizedLyrics) DefaultDialog(onDismiss = {
            isPicking = false
        }) {
            val tracks = remember { mutableStateListOf<Track>() }
            var loading by remember { mutableStateOf(true) }
            var error by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                val mediaMetadata = mediaMetadataProvider()

                LrcLib.lyrics(
                    artist = mediaMetadata.artist?.toString() ?: "",
                    title = mediaMetadata.title?.toString() ?: ""
                )?.onSuccess {
                    tracks.clear()
                    tracks.addAll(it)
                    loading = false
                    error = false
                }?.onFailure {
                    loading = false
                    error = true
                } ?: run { loading = false }
            }

            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                error || tracks.isEmpty() -> BasicText(
                    text = "No lyric tracks could be found",
                    style = typography.s.semiBold.center,
                    modifier = Modifier
                        .padding(all = 24.dp)
                        .align(Alignment.CenterHorizontally)
                )

                else -> ValueSelectorDialogBody(
                    onDismiss = { isPicking = false },
                    title = "Choose lyric track",
                    selectedValue = null,
                    values = tracks,
                    onValueSelected = {
                        transaction {
                            Database.upsert(
                                Lyrics(
                                    songId = mediaId,
                                    fixed = lyrics?.fixed,
                                    synced = it.syncedLyrics ?: ""
                                )
                            )
                            isPicking = false
                        }
                    },
                    valueText = {
                        "${it.artistName} - ${it.trackName} (${
                            it.duration.seconds.toComponents { minutes, seconds, _ -> 
                                "$minutes:${seconds.toString().padStart(2, '0')}" 
                            }
                        })"
                    }
                )
            }
        }

        if (isShowingSynchronizedLyrics) DisposableEffect(Unit) {
            currentView.keepScreenOn = true
            onDispose {
                currentView.keepScreenOn = false
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onDismiss() }
                    )
                }
                .fillMaxSize()
                .background(Color.Black.copy(0.8f))
        ) {
            AnimatedVisibility(
                visible = isError && text == null,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                BasicText(
                    text = "An error has occurred while fetching the ${if (isShowingSynchronizedLyrics) "synchronized " else ""}lyrics",
                    style = typography.xs.center.medium.color(PureBlackColorPalette.text),
                    modifier = Modifier
                        .background(Color.Black.copy(0.4f))
                        .padding(all = 8.dp)
                        .fillMaxWidth()
                )
            }

            AnimatedVisibility(
                visible = text?.isEmpty() ?: false,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                BasicText(
                    text = "${if (isShowingSynchronizedLyrics) "Synchronized l" else "L"}yrics are not available for this song",
                    style = typography.xs.center.medium.color(PureBlackColorPalette.text),
                    modifier = Modifier
                        .background(Color.Black.copy(0.4f))
                        .padding(all = 8.dp)
                        .fillMaxWidth()
                )
            }

            if (text?.isNotEmpty() == true) {
                if (isShowingSynchronizedLyrics) {
                    val density = LocalDensity.current
                    val player = LocalPlayerServiceBinder.current?.player
                        ?: return@AnimatedVisibility

                    val synchronizedLyrics = remember(text) {
                        SynchronizedLyrics(LrcLib.Lyrics(text).sentences) {
                            player.currentPosition + 50L - (lyrics?.startTime ?: 0L)
                        }
                    }

                    val lazyListState = rememberLazyListState(
                        initialFirstVisibleItemIndex = synchronizedLyrics.index,
                        initialFirstVisibleItemScrollOffset = with(density) { size.roundToPx() } / 6
                    )

                    LaunchedEffect(synchronizedLyrics) {
                        val center = with(density) { size.roundToPx() } / 6

                        while (isActive) {
                            delay(50)
                            if (synchronizedLyrics.update())
                                lazyListState.animateScrollToItem(synchronizedLyrics.index, center)
                        }
                    }

                    LazyColumn(
                        state = lazyListState,
                        userScrollEnabled = false,
                        contentPadding = PaddingValues(vertical = size / 2),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.verticalFadingEdge()
                    ) {
                        itemsIndexed(items = synchronizedLyrics.sentences.values.toList()) { index, sentence ->
                            BasicText(
                                text = sentence,
                                style = typography.xs.center.medium.color(if (index == synchronizedLyrics.index) PureBlackColorPalette.text else PureBlackColorPalette.textDisabled),
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 32.dp)
                            )
                        }
                    }
                } else BasicText(
                    text = text,
                    style = typography.xs.center.medium.color(PureBlackColorPalette.text),
                    modifier = Modifier
                        .verticalFadingEdge()
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                        .padding(vertical = size / 4, horizontal = 32.dp)
                )
            }

            if (text == null && !isError) Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.shimmer()
            ) {
                repeat(4) {
                    TextPlaceholder(
                        color = colorPalette.onOverlayShimmer,
                        modifier = Modifier.alpha(1f - it * 0.2f)
                    )
                }
            }

            Image(
                painter = painterResource(R.drawable.ellipsis_horizontal),
                contentDescription = null,
                colorFilter = ColorFilter.tint(DefaultDarkColorPalette.text),
                modifier = Modifier
                    .padding(all = 4.dp)
                    .clickable(
                        indication = rememberRipple(bounded = false),
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {
                            menuState.display {
                                Menu {
                                    MenuEntry(
                                        icon = R.drawable.time,
                                        text = "Show ${if (isShowingSynchronizedLyrics) "un" else ""}synchronized lyrics",
                                        secondaryText = if (isShowingSynchronizedLyrics) null else "Provided by lrclib.net & kugou.com",
                                        onClick = {
                                            menuState.hide()
                                            isShowingSynchronizedLyrics =
                                                !isShowingSynchronizedLyrics
                                        }
                                    )

                                    MenuEntry(
                                        icon = R.drawable.pencil,
                                        text = "Edit lyrics",
                                        onClick = {
                                            menuState.hide()
                                            isEditing = true
                                        }
                                    )

                                    MenuEntry(
                                        icon = R.drawable.search,
                                        text = "Search lyrics online",
                                        onClick = {
                                            menuState.hide()
                                            val mediaMetadata = mediaMetadataProvider()

                                            try {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_WEB_SEARCH).apply {
                                                        putExtra(
                                                            SearchManager.QUERY,
                                                            "${mediaMetadata.title} ${mediaMetadata.artist} lyrics"
                                                        )
                                                    }
                                                )
                                            } catch (e: ActivityNotFoundException) {
                                                context.toast("Couldn't find an application to browse the Internet")
                                            }
                                        }
                                    )

                                    MenuEntry(
                                        icon = R.drawable.sync,
                                        text = "Fetch lyrics again",
                                        enabled = lyrics != null,
                                        onClick = {
                                            menuState.hide()
                                            query {
                                                Database.upsert(
                                                    Lyrics(
                                                        songId = mediaId,
                                                        fixed = if (isShowingSynchronizedLyrics) lyrics?.fixed else null,
                                                        synced = if (isShowingSynchronizedLyrics) null else lyrics?.synced,
                                                    )
                                                )
                                            }
                                        }
                                    )

                                    if (isShowingSynchronizedLyrics) {
                                        MenuEntry(
                                            icon = R.drawable.download,
                                            text = "Pick lyrics from lrclib.net",
                                            onClick = {
                                                menuState.hide()
                                                isPicking = true
                                            }
                                        )
                                        MenuEntry(
                                            icon = R.drawable.play_skip_forward,
                                            text = "Set start offset",
                                            secondaryText = "Offsets the synchronized lyrics by the current playback time",
                                            onClick = {
                                                menuState.hide()
                                                lyrics?.let {
                                                    val startTime = binder?.player?.currentPosition
                                                    query {
                                                        Database.upsert(it.copy(startTime = startTime))
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                    .padding(all = 8.dp)
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}
