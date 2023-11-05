package it.vfsfitvnm.vimusic.enums

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.vimusic.roundedShape

@Suppress("unused")
enum class ThumbnailRoundness(val dp: Dp, val desc: ThumbnailRoundness.() -> String = { name }) {
    None(0.dp),
    Light(2.dp),
    Medium(8.dp),
    Heavy(12.dp),
    Heavier(dp = 16.dp, desc = { "Even heavier" }),
    Heaviest(16.dp);

    val shape get() = dp.roundedShape
}