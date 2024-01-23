package it.vfsfitvnm.vimusic.ui.components.themed

import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.CircularIndeterminateStrokeCap
) {
    val (colorPalette) = LocalAppearance.current

    if (progress == null) androidx.compose.material3.CircularProgressIndicator(
        modifier = modifier,
        color = colorPalette.accent,
        strokeCap = strokeCap
    ) else androidx.compose.material3.CircularProgressIndicator(
        modifier = modifier,
        color = colorPalette.accent,
        strokeCap = strokeCap,
        progress = { progress }
    )
}
