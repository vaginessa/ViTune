package it.vfsfitvnm.vimusic.ui.components.themed

import androidx.annotation.IntRange
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance

// TODO: due to changes in the Material Slider there are unknown glitches that occur
// Update 12-31-2023: this is likely caused by the fact that the behavior of onValueChangeFinished
// changed and should not update `value`'s state

@Composable
fun Slider(
    state: Float,
    onSlide: (Float) -> Unit,
    onSlideCompleted: () -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    @IntRange(from = 0) steps: Int = 0
) {
    val (colorPalette) = LocalAppearance.current

    androidx.compose.material3.Slider(
        value = state,
        onValueChange = { onSlide(it) },
        valueRange = range,
        steps = steps,
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Release) onSlideCompleted()
                }
            }
        },
        colors = SliderDefaults.colors(
            thumbColor = colorPalette.onAccent,
            activeTrackColor = colorPalette.accent,
            inactiveTrackColor = colorPalette.text.copy(alpha = 0.75f)
        )
    )
}
