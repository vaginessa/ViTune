package it.vfsfitvnm.vimusic.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import it.vfsfitvnm.vimusic.models.ui.UiMedia
import it.vfsfitvnm.vimusic.preferences.PlayerPreferences
import it.vfsfitvnm.vimusic.service.PlayerService
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.formatAsDuration
import it.vfsfitvnm.vimusic.utils.isCompositionLaunched
import it.vfsfitvnm.vimusic.utils.semiBold
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToLong
import kotlin.math.sin

@Composable
fun SeekBar(
    binder: PlayerService.Binder,
    position: Long,
    media: UiMedia,
    modifier: Modifier = Modifier,
    color: Color = LocalAppearance.current.colorPalette.text,
    backgroundColor: Color = LocalAppearance.current.colorPalette.background2,
    shape: Shape = RoundedCornerShape(8.dp),
    isActive: Boolean = binder.player.isPlaying,
    alwaysShowDuration: Boolean = false,
    scrubberRadius: Dp = 6.dp,
    style: PlayerPreferences.SeekBarStyle = PlayerPreferences.seekBarStyle
) {
    val range = 0L..media.duration
    val floatRange = 0f..media.duration.toFloat()

    when (style) {
        PlayerPreferences.SeekBarStyle.Static -> {
            var scrubbingPosition by remember(media) { mutableStateOf<Long?>(null) }

            ClassicSeekBarBody(
                position = scrubbingPosition ?: position,
                duration = media.duration,
                range = range,
                onSeekStart = { scrubbingPosition = it },
                onSeek = { delta ->
                    scrubbingPosition = if (media.duration == C.TIME_UNSET) null
                    else scrubbingPosition?.let { (it + delta).coerceIn(range) }
                },
                onSeekEnd = {
                    scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                },
                color = color,
                backgroundColor = backgroundColor,
                showDuration = alwaysShowDuration || scrubbingPosition != null,
                modifier = modifier,
                scrubberRadius = scrubberRadius,
                shape = shape
            )
        }

        PlayerPreferences.SeekBarStyle.Wavy -> {
            val scope = rememberCoroutineScope()
            val compositionLaunched = isCompositionLaunched()

            val animatedPosition = remember { Animatable(position.toFloat()) }
            var isSeeking by remember { mutableStateOf(false) }

            LaunchedEffect(media) {
                if (compositionLaunched) animatedPosition.animateTo(0f)
            }

            LaunchedEffect(position) {
                if (!isSeeking && !animatedPosition.isRunning) animatedPosition.animateTo(position.toFloat())
            }

            WavySeekBarBody(
                position = animatedPosition.value.roundToLong(),
                duration = media.duration,
                range = range,
                onSeekStart = {
                    isSeeking = true
                    scope.launch { animatedPosition.animateTo(it.toFloat()) }
                },
                onSeek = { delta ->
                    if (media.duration == C.TIME_UNSET) return@WavySeekBarBody

                    isSeeking = true
                    scope.launch {
                        animatedPosition.snapTo(
                            (animatedPosition.value + delta)
                                .coerceIn(floatRange)
                        )
                    }
                },
                onSeekEnd = {
                    isSeeking = false
                    binder.player.seekTo(animatedPosition.targetValue.roundToLong())
                },
                color = color,
                backgroundColor = backgroundColor,
                modifier = modifier,
                scrubberRadius = scrubberRadius,
                shape = shape,
                showDuration = alwaysShowDuration || isSeeking,
                isActive = isActive
            )
        }
    }
}

@Composable
private fun ClassicSeekBarBody(
    position: Long,
    duration: Long,
    range: ClosedRange<Long>,
    onSeekStart: (Long) -> Unit,
    onSeek: (Long) -> Unit,
    onSeekEnd: () -> Unit,
    color: Color,
    backgroundColor: Color,
    scrubberRadius: Dp,
    shape: Shape,
    showDuration: Boolean,
    modifier: Modifier = Modifier,
    barHeight: Dp = 3.dp,
    scrubberColor: Color = color,
    drawSteps: Boolean = false
) = Column {
    val isDragging = remember { MutableTransitionState(false) }
    val transition = updateTransition(transitionState = isDragging, label = null)

    val currentBarHeight by transition.animateDp(label = "") { if (it) scrubberRadius else barHeight }
    val currentScrubberRadius by transition.animateDp(label = "") { if (it) 0.dp else scrubberRadius }

    Box(
        modifier = modifier
            .pointerInput(range) {
                if (range.endInclusive < range.start) return@pointerInput

                var acc = 0f

                detectHorizontalDragGestures(
                    onDragStart = { isDragging.targetState = true },
                    onHorizontalDrag = { _, delta ->
                        acc += delta / size.width * (range.endInclusive - range.start).toFloat()

                        if (acc !in -1f..1f) {
                            onSeek(acc.toLong())
                            acc -= acc.toLong()
                        }
                    },
                    onDragEnd = {
                        isDragging.targetState = false
                        acc = 0f
                        onSeekEnd()
                    },
                    onDragCancel = {
                        isDragging.targetState = false
                        acc = 0f
                        onSeekEnd()
                    }
                )
            }
            .pointerInput(range.start, range.endInclusive) {
                if (range.endInclusive < range.start) return@pointerInput

                detectTapGestures(
                    onPress = { offset ->
                        onSeekStart(
                            (offset.x / size.width * (range.endInclusive - range.start) + range.start).roundToLong()
                        )
                    },
                    onTap = { onSeekEnd() }
                )
            }
            .padding(horizontal = scrubberRadius)
            .drawWithContent {
                drawContent()

                val scrubberPosition =
                    if (range.endInclusive < range.start) 0f
                    else (position.toFloat() - range.start) / (range.endInclusive - range.start) * size.width

                drawCircle(
                    color = scrubberColor,
                    radius = currentScrubberRadius.toPx(),
                    center = center.copy(x = scrubberPosition)
                )

                if (drawSteps) for (i in position + 1..range.endInclusive) {
                    val stepPosition =
                        (i.toFloat() - range.start) / (range.endInclusive - range.start) * size.width

                    drawCircle(
                        color = scrubberColor,
                        radius = scrubberRadius.toPx() / 2,
                        center = center.copy(x = stepPosition)
                    )
                }
            }
            .height(scrubberRadius)
    ) {
        Spacer(
            modifier = Modifier
                .height(currentBarHeight)
                .fillMaxWidth()
                .background(color = backgroundColor, shape = shape)
                .align(Alignment.Center)
        )

        Spacer(
            modifier = Modifier
                .height(currentBarHeight)
                .fillMaxWidth((position.toFloat() - range.start) / (range.endInclusive - range.start).toFloat())
                .background(color = color, shape = shape)
                .align(Alignment.CenterStart)
        )
    }

    Duration(
        position = position,
        duration = duration,
        show = showDuration
    )
}

@Composable
private fun WavySeekBarBody(
    position: Long,
    duration: Long,
    range: ClosedRange<Long>,
    color: Color,
    backgroundColor: Color,
    shape: Shape,
    onSeek: (Long) -> Unit,
    onSeekStart: (Long) -> Unit,
    onSeekEnd: () -> Unit,
    showDuration: Boolean,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    scrubberRadius: Dp = 6.dp
) = Column {
    val isDragging = remember { MutableTransitionState(false) }

    val transition = updateTransition(transitionState = isDragging, label = null)

    val currentAmplitude by transition.animateDp(label = "") { if (it || !isActive) 0.dp else 2.dp }
    val currentScrubberHeight by transition.animateDp(label = "") { if (it) 20.dp else 15.dp }

    Box(
        modifier = modifier
            .pointerInput(range) {
                if (range.endInclusive < range.start) return@pointerInput

                detectDrags(
                    isDragging = isDragging,
                    range = range,
                    onSeek = onSeek,
                    onSeekEnd = onSeekEnd
                )
            }
            .pointerInput(range) {
                detectTaps(
                    range = range,
                    onSeekStart = onSeekStart,
                    onSeekEnd = onSeekEnd
                )
            }
            .padding(horizontal = scrubberRadius)
            .drawWithContent {
                drawContent()

                drawScrubber(
                    range = range,
                    position = position,
                    color = color,
                    height = currentScrubberHeight
                )
            }
    ) {
        WavySeekBarContent(
            backgroundColor = backgroundColor,
            amplitude = { currentAmplitude },
            position = position,
            range = range,
            shape = shape,
            color = color
        )
    }

    Duration(
        position = position,
        duration = duration,
        show = showDuration
    )
}

private suspend fun PointerInputScope.detectDrags(
    isDragging: MutableTransitionState<Boolean>,
    range: ClosedRange<Long>,
    onSeek: (delta: Long) -> Unit,
    onSeekEnd: () -> Unit
) {
    var acc = 0f

    detectHorizontalDragGestures(
        onDragStart = { isDragging.targetState = true },
        onHorizontalDrag = { _, delta ->
            acc += delta / size.width * (range.endInclusive - range.start).toFloat()

            if (acc !in -1f..1f) {
                onSeek(acc.toLong())
                acc -= acc
            }
        },
        onDragEnd = {
            isDragging.targetState = false
            acc = 0f
            onSeekEnd()
        },
        onDragCancel = {
            isDragging.targetState = false
            acc = 0f

            onSeekEnd()
        }
    )
}

private suspend fun PointerInputScope.detectTaps(
    range: ClosedRange<Long>,
    onSeekStart: (updated: Long) -> Unit,
    onSeekEnd: () -> Unit
) {
    if (range.endInclusive < range.start) return

    detectTapGestures(
        onPress = { offset ->
            onSeekStart(
                (offset.x / size.width * (range.endInclusive - range.start).toFloat() + range.start).toLong()
            )
        },
        onTap = { onSeekEnd() }
    )
}

private fun ContentDrawScope.drawScrubber(
    range: ClosedRange<Long>,
    position: Long,
    color: Color,
    height: Dp
) {
    val scrubberPosition = if (range.endInclusive < range.start) 0f
    else (position - range.start) / (range.endInclusive - range.start).toFloat() * size.width

    drawRoundRect(
        color = color,
        topLeft = Offset(
            x = scrubberPosition - 5f,
            y = (size.height - height.toPx()) / 2f
        ),
        size = Size(
            width = 10f,
            height = height.toPx()
        ),
        cornerRadius = CornerRadius(5f)
    )
}

@Composable
private fun WavySeekBarContent(
    backgroundColor: Color,
    amplitude: () -> Dp,
    position: Long,
    range: ClosedRange<Long>,
    shape: Shape,
    color: Color
) {
    val fraction = (position - range.start) / (range.endInclusive - range.start).toFloat()
    val progress by rememberInfiniteTransition(label = "").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(1f - fraction)
                .background(color = backgroundColor, shape = shape)
                .align(Alignment.CenterEnd)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(amplitude())
                .align(Alignment.CenterStart)
        ) {
            drawPath(
                wavePath(size.copy(height = size.height * 2), progress),
                color,
                style = Stroke(width = 5f)
            )
        }
    }
}

@Composable
private fun Duration(
    position: Long,
    duration: Long,
    show: Boolean
) {
    val typography = LocalAppearance.current.typography

    AnimatedVisibility(
        visible = show,
        enter = fadeIn() + expandVertically { -it },
        exit = fadeOut() + shrinkVertically { -it }
    ) {
        Column {
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                BasicText(
                    text = formatAsDuration(position),
                    style = typography.xxs.semiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (duration != C.TIME_UNSET) BasicText(
                    text = formatAsDuration(duration),
                    style = typography.xxs.semiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun wavePath(size: Size, progress: Float) = Path().apply {
    fun f(x: Float) = (sin(x / 15f + progress * 2 * PI.toFloat()) + 1) * size.height / 2f

    moveTo(0f, f(0f))
    var currentX = 0f

    while (currentX < size.width) {
        lineTo(currentX, f(currentX))
        currentX += 1
    }
}
