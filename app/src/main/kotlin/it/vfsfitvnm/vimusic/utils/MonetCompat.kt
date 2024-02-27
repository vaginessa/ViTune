package it.vfsfitvnm.vimusic.utils

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kieronquinn.monetcompat.core.MonetCompat
import it.vfsfitvnm.vimusic.ui.styling.ColorPalette
import it.vfsfitvnm.vimusic.ui.styling.DefaultLightColorPalette
import kotlinx.coroutines.launch

val LocalMonetCompat = compositionLocalOf { MonetCompat.getInstance() }

context(LifecycleOwner)
inline fun MonetCompat.invokeOnReady(
    state: Lifecycle.State = Lifecycle.State.CREATED,
    crossinline block: () -> Unit
) = lifecycleScope.launch {
    repeatOnLifecycle(state) {
        awaitMonetReady()
        block()
    }
}

fun MonetCompat.setDefaultPalette(palette: ColorPalette = DefaultLightColorPalette) {
    defaultAccentColor = palette.accent.toArgb()
    defaultBackgroundColor = palette.background0.toArgb()
    defaultPrimaryColor = palette.background1.toArgb()
    defaultSecondaryColor = palette.background2.toArgb()
}
