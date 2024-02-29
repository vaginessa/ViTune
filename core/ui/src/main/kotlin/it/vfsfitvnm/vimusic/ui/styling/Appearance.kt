package it.vfsfitvnm.vimusic.ui.styling

import android.app.Activity
import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import it.vfsfitvnm.vimusic.enums.ColorPaletteMode
import it.vfsfitvnm.vimusic.enums.ColorPaletteName
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid6
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid8
import it.vfsfitvnm.vimusic.utils.isCompositionLaunched
import it.vfsfitvnm.vimusic.utils.roundedShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Appearance(
    val colorPalette: ColorPalette,
    val typography: Typography,
    val thumbnailShapeCorners: Dp
) {
    val thumbnailShape = thumbnailShapeCorners.roundedShape
    operator fun component4() = thumbnailShape

    companion object AppearanceSaver : Saver<Appearance, List<Any>> {
        @Suppress("UNCHECKED_CAST")
        override fun restore(value: List<Any>) = Appearance(
            colorPalette = ColorPalette.restore(value[0] as List<Any>),
            typography = Typography.restore(value[1] as List<Any>),
            thumbnailShapeCorners = (value[2] as Float).dp
        )

        override fun SaverScope.save(value: Appearance) = listOf(
            with(ColorPalette.Companion) { save(value.colorPalette) },
            with(Typography.Companion) { save(value.typography) },
            value.thumbnailShapeCorners.value
        )
    }
}

val LocalAppearance = staticCompositionLocalOf<Appearance> { error("No appearance provided") }

@Composable
inline fun rememberAppearance(
    vararg keys: Any = arrayOf(Unit),
    saver: Saver<Appearance, out Any> = Appearance.AppearanceSaver,
    isDark: Boolean = isSystemInDarkTheme(),
    crossinline provide: (isSystemInDarkTheme: Boolean) -> Appearance
) = rememberSaveable(
    keys,
    isCompositionLaunched(),
    isDark,
    stateSaver = saver
) {
    mutableStateOf(provide(isDark))
}

@Composable
fun appearance(
    name: ColorPaletteName,
    mode: ColorPaletteMode,
    materialAccentColor: Color?,
    sampleBitmap: Bitmap?,
    useSystemFont: Boolean,
    applyFontPadding: Boolean,
    thumbnailRoundness: Dp
): Appearance {
    val isSystemInDarkTheme = isSystemInDarkTheme()

    val isDark by remember {
        derivedStateOf {
            mode == ColorPaletteMode.Dark || (mode == ColorPaletteMode.System && isSystemInDarkTheme)
        }
    }

    val defaultTheme by remember {
        derivedStateOf {
            val colorPalette = colorPaletteOf(
                name = ColorPaletteName.Default,
                mode = mode,
                isDark = isSystemInDarkTheme
            )

            Appearance(
                colorPalette = colorPalette,
                typography = typographyOf(
                    color = colorPalette.text,
                    useSystemFont = useSystemFont,
                    applyFontPadding = applyFontPadding
                ),
                thumbnailShapeCorners = thumbnailRoundness
            )
        }
    }

    var dynamicAccentColor by rememberSaveable(stateSaver = Hsl.Saver) {
        mutableStateOf(defaultTheme.colorPalette.accent.hsl)
    }

    LaunchedEffect(sampleBitmap, name) {
        if (!name.isDynamic) return@LaunchedEffect

        dynamicAccentColor = sampleBitmap?.let {
            dynamicAccentColorOf(
                bitmap = it,
                isDark = isDark
            )
        } ?: defaultTheme.colorPalette.accent.hsl
    }

    val colorPalette by remember(name) {
        derivedStateOf {
            when (name) {
                ColorPaletteName.Default -> defaultTheme.colorPalette
                ColorPaletteName.Dynamic -> dynamicColorPaletteOf(
                    hsl = dynamicAccentColor,
                    isDark = isDark,
                    isAmoled = false
                )

                ColorPaletteName.MaterialYou -> if (materialAccentColor == null) defaultTheme.colorPalette
                else dynamicColorPaletteOf(
                    accentColor = materialAccentColor,
                    isDark = isDark,
                    isAmoled = false
                )

                ColorPaletteName.PureBlack -> PureBlackColorPalette
                ColorPaletteName.AMOLED -> dynamicColorPaletteOf(
                    hsl = dynamicAccentColor,
                    isDark = true,
                    isAmoled = true
                )
            }
        }
    }

    return rememberAppearance(
        colorPalette,
        defaultTheme,
        thumbnailRoundness,
        isDark = isDark
    ) {
        Appearance(
            colorPalette = colorPalette,
            typography = defaultTheme.typography.copy(color = colorPalette.text),
            thumbnailShapeCorners = thumbnailRoundness
        )
    }.value
}

fun Activity.setSystemBarAppearance(isDark: Boolean) {
    with(WindowCompat.getInsetsController(window, window.decorView.rootView)) {
        isAppearanceLightStatusBars = !isDark
        isAppearanceLightNavigationBars = !isDark
    }

    val color = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()

    if (!isAtLeastAndroid6) window.statusBarColor = color
    if (!isAtLeastAndroid8) window.navigationBarColor = color
}

@Composable
fun Activity.SystemBarAppearance(palette: ColorPalette) = LaunchedEffect(palette) {
    withContext(Dispatchers.Main) {
        setSystemBarAppearance(palette.isDark)
    }
}
