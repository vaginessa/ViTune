package it.vfsfitvnm.vimusic.ui

import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import it.vfsfitvnm.vimusic.ui.styling.Appearance
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance

@Composable
fun rippleTheme(appearance: Appearance = LocalAppearance.current) = remember(
    appearance.colorPalette.text,
    appearance.colorPalette.isDark
) {
    object : RippleTheme {
        @Composable
        override fun defaultColor(): Color = RippleTheme.defaultRippleColor(
            contentColor = appearance.colorPalette.text,
            lightTheme = !appearance.colorPalette.isDark
        )

        @Composable
        override fun rippleAlpha(): RippleAlpha = RippleTheme.defaultRippleAlpha(
            contentColor = appearance.colorPalette.text,
            lightTheme = !appearance.colorPalette.isDark
        )
    }
}
