package it.vfsfitvnm.vimusic.utils

import androidx.compose.runtime.compositionLocalOf
import com.kieronquinn.monetcompat.core.MonetCompat

val LocalMonetCompat = compositionLocalOf { MonetCompat.getInstance() }
