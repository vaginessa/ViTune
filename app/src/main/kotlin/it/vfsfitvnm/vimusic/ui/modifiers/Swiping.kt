package it.vfsfitvnm.vimusic.ui.modifiers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Density
import it.vfsfitvnm.vimusic.utils.px
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun Modifier.onSwipe(
    animateOffset: Boolean = false,
    orientation: Orientation = Orientation.Horizontal,
    decay: Density.() -> DecayAnimationSpec<Float> = { splineBasedDecay(this) },
    onSwipeOut: () -> Unit
) = onSwipe(
    animateOffset = animateOffset,
    onSwipeLeft = onSwipeOut,
    onSwipeRight = onSwipeOut,
    orientation = orientation,
    decay = decay
)

fun Modifier.onSwipe(
    animateOffset: Boolean = false,
    onSwipeLeft: () -> Unit = { },
    onSwipeRight: () -> Unit = { },
    orientation: Orientation = Orientation.Horizontal,
    decay: Density.() -> DecayAnimationSpec<Float> = { splineBasedDecay(this) }
) = this.composed {
    val offset = remember { Animatable(0f) }

    pointerInput(Unit) {
        coroutineScope {
            // fling loop, doesn't really offset anything but simulates the animation beforehand
            while (isActive) {
                val velocityTracker = VelocityTracker()

                awaitPointerEventScope {
                    val pointer = awaitFirstDown(requireUnconsumed = false).id
                    launch { offset.snapTo(0f) }

                    val onDrag: (PointerInputChange) -> Unit = {
                        val change =
                            if (orientation == Orientation.Horizontal) it.positionChange().x
                            else it.positionChange().y

                        launch { offset.snapTo(offset.value + change) }

                        velocityTracker.addPosition(it.uptimeMillis, it.position)
                        if (change != 0f) it.consume()
                    }

                    if (orientation == Orientation.Horizontal) {
                        awaitHorizontalTouchSlopOrCancellation(pointer) { change, _ -> onDrag(change) }
                            ?: return@awaitPointerEventScope
                        horizontalDrag(pointer, onDrag)
                    } else {
                        awaitVerticalTouchSlopOrCancellation(pointer) { change, _ -> onDrag(change) }
                            ?: return@awaitPointerEventScope
                        verticalDrag(pointer, onDrag)
                    }
                }

                // drag completed, calculate velocity
                val targetOffset = decay().calculateTargetValue(
                    initialValue = offset.value,
                    initialVelocity = velocityTracker.calculateVelocity()
                        .let { if (orientation == Orientation.Horizontal) it.x else it.y }
                )
                val size =
                    (if (orientation == Orientation.Horizontal) size.width else size.height) / 2

                when {
                    targetOffset >= size -> onSwipeRight()
                    targetOffset <= -size -> onSwipeLeft()
                }

                launch {
                    offset.animateTo(targetOffset)
                    offset.animateTo(0f)
                }
            }
        }
    }.let {
        when {
            animateOffset && orientation == Orientation.Horizontal -> it.offset(x = offset.value.px.dp)
            animateOffset && orientation == Orientation.Vertical -> it.offset(y = offset.value.px.dp)
            else -> it
        }
    }
}