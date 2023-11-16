package it.vfsfitvnm.vimusic.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

@JvmInline
value class Px(val value: Int) {
    val dp @Composable get() = with(LocalDensity.current) { value.toDp() }
}

val Int.px get() = Px(value = this)