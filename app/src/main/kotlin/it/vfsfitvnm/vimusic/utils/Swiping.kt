package it.vfsfitvnm.vimusic.utils

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

fun Modifier.onSwipe(
    onSwipeLeft: () -> Unit = { },
    onSwipeRight: () -> Unit = { },
    orientation: Orientation = Orientation.Horizontal,
    decay: Density.() -> DecayAnimationSpec<Float> = { splineBasedDecay(this) }
) = this.composed {
    var offset by remember { mutableFloatStateOf(0f) }

    pointerInput(Unit) {
        coroutineScope {
            // fling loop, doesn't really offset anything but simulates the animation beforehand
            while (isActive) {
                val velocityTracker = VelocityTracker()

                awaitPointerEventScope {
                    val pointer = awaitFirstDown(requireUnconsumed = false).id
                    offset = 0f

                    val onDrag: (PointerInputChange) -> Unit = {
                        val change =
                            if (orientation == Orientation.Horizontal) it.positionChange().x
                            else it.positionChange().y

                        offset += change

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
                    initialValue = offset,
                    initialVelocity = velocityTracker.calculateVelocity()
                        .let { if (orientation == Orientation.Horizontal) it.x else it.y }
                )
                val size =
                    (if (orientation == Orientation.Horizontal) size.width else size.height) / 2

                when {
                    targetOffset >= size -> onSwipeRight()
                    targetOffset <= -size -> onSwipeLeft()
                }
            }
        }
    }
}