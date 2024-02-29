package it.vfsfitvnm.vimusic.utils

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.MutableStateFlow

fun <Type : Any> stateFlowSaver() = stateFlowSaverOf<Type, Type>(
    from = { it },
    to = { it }
)

inline fun <Type, Saveable : Any> stateFlowSaverOf(
    crossinline from: (Saveable) -> Type,
    crossinline to: (Type) -> Saveable
) = object : Saver<MutableStateFlow<Type>, Saveable> {
    override fun restore(value: Saveable) = MutableStateFlow(from(value))
    override fun SaverScope.save(value: MutableStateFlow<Type>) = to(value.value)
}

val Color.Companion.Saver get() = object : Saver<Color, Int> {
    override fun restore(value: Int) = Color(value)
    override fun SaverScope.save(value: Color) = value.toArgb()
}
