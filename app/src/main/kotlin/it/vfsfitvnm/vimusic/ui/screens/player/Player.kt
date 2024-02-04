package it.vfsfitvnm.vimusic.ui.screens.player

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import it.vfsfitvnm.compose.routing.OnGlobalRoute
import it.vfsfitvnm.innertube.models.NavigationEndpoint
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.enums.ThumbnailRoundness
import it.vfsfitvnm.vimusic.models.ui.toUiMedia
import it.vfsfitvnm.vimusic.preferences.PlayerPreferences
import it.vfsfitvnm.vimusic.roundedShape
import it.vfsfitvnm.vimusic.service.PlayerService
import it.vfsfitvnm.vimusic.transaction
import it.vfsfitvnm.vimusic.ui.components.BottomSheet
import it.vfsfitvnm.vimusic.ui.components.BottomSheetState
import it.vfsfitvnm.vimusic.ui.components.LocalMenuState
import it.vfsfitvnm.vimusic.ui.components.rememberBottomSheetState
import it.vfsfitvnm.vimusic.ui.components.themed.BaseMediaItemMenu
import it.vfsfitvnm.vimusic.ui.components.themed.IconButton
import it.vfsfitvnm.vimusic.ui.components.themed.SecondaryTextButton
import it.vfsfitvnm.vimusic.ui.components.themed.SliderDialog
import it.vfsfitvnm.vimusic.ui.modifiers.PinchDirection
import it.vfsfitvnm.vimusic.ui.modifiers.onSwipe
import it.vfsfitvnm.vimusic.ui.modifiers.pinchToToggle
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.ui.styling.collapsedPlayerProgressBar
import it.vfsfitvnm.vimusic.utils.DisposableListener
import it.vfsfitvnm.vimusic.utils.forceSeekToNext
import it.vfsfitvnm.vimusic.utils.forceSeekToPrevious
import it.vfsfitvnm.vimusic.utils.isLandscape
import it.vfsfitvnm.vimusic.utils.positionAndDurationState
import it.vfsfitvnm.vimusic.utils.px
import it.vfsfitvnm.vimusic.utils.seamlessPlay
import it.vfsfitvnm.vimusic.utils.secondary
import it.vfsfitvnm.vimusic.utils.semiBold
import it.vfsfitvnm.vimusic.utils.shouldBePlaying
import it.vfsfitvnm.vimusic.utils.thumbnail
import it.vfsfitvnm.vimusic.utils.toast
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.absoluteValue

private fun onDismiss(binder: PlayerService.Binder) {
    binder.stopRadio()
    binder.player.clearMediaItems()
}

@Composable
fun Player(
    layoutState: BottomSheetState,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
) {
    val menuState = LocalMenuState.current

    val (colorPalette, typography, thumbnailCornerSize) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current

    binder?.player ?: return

    var nullableMediaItem by remember {
        mutableStateOf(binder.player.currentMediaItem, neverEqualPolicy())
    }

    var shouldBePlaying by remember {
        mutableStateOf(binder.player.shouldBePlaying)
    }

    binder.player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                nullableMediaItem = mediaItem
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }
        }
    }

    val mediaItem = nullableMediaItem ?: return

    val positionAndDuration by binder.player.positionAndDurationState()

    val windowInsets = WindowInsets.systemBars

    val horizontalBottomPaddingValues = windowInsets
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom).asPaddingValues()

    OnGlobalRoute {
        layoutState.collapseSoft()
    }

    BottomSheet(
        state = layoutState,
        modifier = modifier,
        onDismiss = { onDismiss(binder) },
        indication = null,
        collapsedContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .let {
                        if (PlayerPreferences.horizontalSwipeToClose) it.onSwipe(
                            animateOffset = true,
                            onSwipeOut = { animationJob ->
                                animationJob.join()
                                layoutState.dismiss()
                                onDismiss(binder)
                            }
                        ) else it
                    }
                    .clip(shape)
                    .background(colorPalette.background1)
                    .fillMaxSize()
                    .padding(horizontalBottomPaddingValues)
                    .drawBehind {
                        val progress =
                            positionAndDuration.first.toFloat() / positionAndDuration.second.absoluteValue

                        drawLine(
                            color = colorPalette.collapsedPlayerProgressBar,
                            start = Offset(x = 0f, y = 1.dp.toPx()),
                            end = Offset(x = size.width * progress, y = 1.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
            ) {
                Spacer(modifier = Modifier.width(2.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.height(Dimensions.items.collapsedPlayerHeight)
                ) {
                    AsyncImage(
                        model = mediaItem.mediaMetadata.artworkUri.thumbnail(Dimensions.thumbnails.song.px),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .clip(thumbnailCornerSize.coerceAtMost(ThumbnailRoundness.Heavy.dp).roundedShape)
                            .background(colorPalette.background0)
                            .size(48.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .height(Dimensions.items.collapsedPlayerHeight)
                        .weight(1f)
                ) {
                    AnimatedContent(
                        targetState = mediaItem.mediaMetadata.title?.toString().orEmpty(),
                        label = "",
                        transitionSpec = { fadeIn() togetherWith fadeOut() }
                    ) { text ->
                        BasicText(
                            text = text,
                            style = typography.xs.semiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    AnimatedVisibility(visible = mediaItem.mediaMetadata.artist != null) {
                        AnimatedContent(
                            targetState = mediaItem.mediaMetadata.artist?.toString().orEmpty(),
                            label = "",
                            transitionSpec = { fadeIn() togetherWith fadeOut() }
                        ) { text ->
                            BasicText(
                                text = text,
                                style = typography.xs.semiBold.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(Dimensions.items.collapsedPlayerHeight)
                ) {
                    AnimatedVisibility(visible = PlayerPreferences.isShowingPrevButtonCollapsed) {
                        IconButton(
                            icon = R.drawable.play_skip_back,
                            color = colorPalette.text,
                            onClick = binder.player::forceSeekToPrevious,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                                .size(20.dp)
                        )
                    }

                    IconButton(
                        icon = if (shouldBePlaying) R.drawable.pause else R.drawable.play,
                        color = colorPalette.text,
                        onClick = {
                            if (shouldBePlaying) binder.player.pause() else {
                                if (binder.player.playbackState == Player.STATE_IDLE) binder.player.prepare()
                                binder.player.play()
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .size(20.dp)
                    )

                    IconButton(
                        icon = R.drawable.play_skip_forward,
                        color = colorPalette.text,
                        onClick = binder.player::forceSeekToNext,
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))
            }
        }
    ) {
        var isShowingStatsForNerds by rememberSaveable { mutableStateOf(false) }
        var isShowingLyricsDialog by rememberSaveable { mutableStateOf(false) }

        if (isShowingLyricsDialog) LyricsDialog(onDismiss = { isShowingLyricsDialog = false })

        val playerBottomSheetState = rememberBottomSheetState(
            dismissedBound = 64.dp + horizontalBottomPaddingValues.calculateBottomPadding(),
            expandedBound = layoutState.expandedBound
        )

        val containerModifier = Modifier
            .clip(shape)
            .background(colorPalette.background1)
            .padding(
                windowInsets
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    .asPaddingValues()
            )
            .padding(bottom = playerBottomSheetState.collapsedBound)

        val thumbnailContent: @Composable (modifier: Modifier) -> Unit = { innerModifier ->
            Thumbnail(
                isShowingLyrics = PlayerPreferences.isShowingLyrics,
                onShowLyrics = { PlayerPreferences.isShowingLyrics = it },
                isShowingStatsForNerds = isShowingStatsForNerds,
                onShowStatsForNerds = { isShowingStatsForNerds = it },
                modifier = innerModifier
                    .nestedScroll(layoutState.preUpPostDownNestedScrollConnection)
                    .pinchToToggle(
                        key = isShowingLyricsDialog,
                        direction = PinchDirection.Out,
                        threshold = 1.05f,
                        onPinch = {
                            if (PlayerPreferences.isShowingLyrics) isShowingLyricsDialog = true
                        }
                    )
            )
        }

        val controlsContent: @Composable (modifier: Modifier) -> Unit = { innerModifier ->
            val media = mediaItem.toUiMedia(positionAndDuration.second)

            Controls(
                media = media,
                shouldBePlaying = shouldBePlaying,
                position = positionAndDuration.first,
                modifier = innerModifier
            )
        }

        if (isLandscape) Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = containerModifier.padding(top = 32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(0.66f)
                    .padding(bottom = 16.dp)
            ) {
                thumbnailContent(Modifier.padding(horizontal = 16.dp))
            }

            controlsContent(
                Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxHeight()
                    .weight(1f)
            )
        } else Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = containerModifier.padding(top = 54.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1.25f)
            ) {
                thumbnailContent(Modifier.padding(horizontal = 32.dp, vertical = 8.dp))
            }

            controlsContent(
                Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        var speedDialogOpen by rememberSaveable { mutableStateOf(false) }

        if (speedDialogOpen) {
            SliderDialog(
                onDismiss = { speedDialogOpen = false },
                title = stringResource(R.string.playback_speed),
                provideState = {
                    remember(PlayerPreferences.speed) {
                        mutableFloatStateOf(PlayerPreferences.speed)
                    }
                },
                onSlideCompleted = { PlayerPreferences.speed = it },
                min = 0f,
                max = 2f,
                toDisplay = {
                    if (it <= 0.01f) stringResource(R.string.minimum_speed_value)
                    else stringResource(R.string.format_speed_multiplier, "%.2f".format(it))
                }
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    SecondaryTextButton(
                        text = stringResource(R.string.reset),
                        onClick = {
                            PlayerPreferences.speed = 1f
                        }
                    )
                }
            }
        }

        var boostDialogOpen by rememberSaveable { mutableStateOf(false) }

        if (boostDialogOpen) {
            fun submit(state: Float) = transaction {
                Database.setLoudnessBoost(
                    songId = mediaItem.mediaId,
                    loudnessBoost = state.takeUnless { it == 0f }
                )
            }

            SliderDialog(
                onDismiss = { boostDialogOpen = false },
                title = stringResource(R.string.song_volume_boost),
                provideState = {
                    val state = remember { mutableFloatStateOf(0f) }

                    LaunchedEffect(mediaItem.mediaId) {
                        Database
                            .loudnessBoost(mediaItem.mediaId)
                            .distinctUntilChanged()
                            .collect { state.floatValue = it ?: 0f }
                    }

                    state
                },
                onSlideCompleted = { submit(it) },
                min = -20f,
                max = 20f,
                toDisplay = { stringResource(R.string.format_db, "%.2f".format(it)) }
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    SecondaryTextButton(
                        text = stringResource(R.string.reset),
                        onClick = { submit(0f) }
                    )
                }
            }
        }

        with(PlayerPreferences) {
            Queue(
                layoutState = playerBottomSheetState,
                beforeContent = {
                    if (playerLayout == PlayerPreferences.PlayerLayout.New) IconButton(
                        onClick = { trackLoopEnabled = !trackLoopEnabled },
                        icon = R.drawable.infinite,
                        color = if (trackLoopEnabled) colorPalette.text else colorPalette.textDisabled,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .size(20.dp)
                    ) else Spacer(modifier = Modifier.width(20.dp))
                },
                afterContent = {
                    IconButton(
                        icon = R.drawable.ellipsis_horizontal,
                        color = colorPalette.text,
                        onClick = {
                            menuState.display {
                                PlayerMenu(
                                    onDismiss = menuState::hide,
                                    mediaItem = mediaItem,
                                    binder = binder,
                                    onShowSpeedDialog = { speedDialogOpen = true },
                                    onShowNormalizationDialog =
                                    if (volumeNormalization) ({ boostDialogOpen = true }) else null
                                )
                            }
                        },
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .size(20.dp)
                    )
                },
                backgroundColorProvider = { colorPalette.background2 },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
@OptIn(UnstableApi::class)
private fun PlayerMenu(
    binder: PlayerService.Binder,
    mediaItem: MediaItem,
    onDismiss: () -> Unit,
    onShowSpeedDialog: (() -> Unit)? = null,
    onShowNormalizationDialog: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    BaseMediaItemMenu(
        mediaItem = mediaItem,
        onStartRadio = {
            binder.stopRadio()
            binder.player.seamlessPlay(mediaItem)
            binder.setupRadio(NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId))
        },
        onGoToEqualizer = {
            try {
                activityResultLauncher.launch(
                    Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, binder.player.audioSessionId)
                        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                    }
                )
            } catch (e: ActivityNotFoundException) {
                context.toast(context.getString(R.string.no_equalizer_installed))
            }
        },
        onShowSleepTimer = {},
        onDismiss = onDismiss,
        onShowSpeedDialog = onShowSpeedDialog,
        onShowNormalizationDialog = onShowNormalizationDialog
    )
}
