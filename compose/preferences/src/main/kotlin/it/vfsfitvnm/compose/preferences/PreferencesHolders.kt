package it.vfsfitvnm.compose.preferences

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private val coroutineScope = CoroutineScope(Dispatchers.IO + CoroutineName("PreferencesHolders"))

private val canWriteState get() = !Snapshot.current.readOnly && !Snapshot.current.root.readOnly

@Stable
data class SharedPreferencesProperty<T : Any>(
    private val name: String? = null,
    private val get: SharedPreferences.(key: String) -> T,
    private val set: SharedPreferences.Editor.(key: String, value: T) -> Unit,
    private val default: T
) : ReadWriteProperty<PreferencesHolder, T> {
    private val state = mutableStateOf(default)
    val stateFlow = MutableStateFlow(default)
    private var listener: OnSharedPreferenceChangeListener? = null

    private fun setState(newValue: T) {
        state.value = newValue
        stateFlow.update { newValue }
    }

    private inline val KProperty<*>.key get() = this@SharedPreferencesProperty.name ?: name

    override fun getValue(thisRef: PreferencesHolder, property: KProperty<*>): T {
        if (listener == null && canWriteState) {
            setState(thisRef.get(property.key))

            listener = OnSharedPreferenceChangeListener { preferences, key ->
                if (key != property.key || !canWriteState) return@OnSharedPreferenceChangeListener

                preferences.get(property.key).let {
                    if (it != state.value) setState(it)
                }
            }

            thisRef.registerOnSharedPreferenceChangeListener(listener)
        }
        return state.value
    }

    override fun setValue(thisRef: PreferencesHolder, property: KProperty<*>, value: T) =
        coroutineScope.launch {
            thisRef.edit(commit = true) {
                set(property.key, value)
            }
        }.let { }
}

/**
 * A snapshottable, thread-safe, compose-first, extensible SharedPreferences wrapper that supports
 * virtually all types, and if it doesn't, one could simply type
 * `fun myNewType(...) = SharedPreferencesProperty(...)` and start implementing. Starts off as given
 * defaultValue until we are allowed to subscribe to SharedPreferences. Caution: the type of the
 * preference has to be [Stable], otherwise UB will occur.
 */
open class PreferencesHolder(
    application: Application,
    name: String,
    mode: Int = Context.MODE_PRIVATE
) : SharedPreferences by application.getSharedPreferences(name, mode) {
    fun boolean(defaultValue: Boolean) = SharedPreferencesProperty(
        get = { getBoolean(it, defaultValue) },
        set = { k, v -> putBoolean(k, v) },
        default = defaultValue
    )

    fun string(defaultValue: String) = SharedPreferencesProperty(
        get = { getString(it, null) ?: defaultValue },
        set = { k, v -> putString(k, v) },
        default = defaultValue
    )

    fun int(defaultValue: Int) = SharedPreferencesProperty(
        get = { getInt(it, defaultValue) },
        set = { k, v -> putInt(k, v) },
        default = defaultValue
    )

    fun float(defaultValue: Float) = SharedPreferencesProperty(
        get = { getFloat(it, defaultValue) },
        set = { k, v -> putFloat(k, v) },
        default = defaultValue
    )

    fun long(defaultValue: Long) = SharedPreferencesProperty(
        get = { getLong(it, defaultValue) },
        set = { k, v -> putLong(k, v) },
        default = defaultValue
    )

    inline fun <reified T : Enum<T>> enum(defaultValue: T) = SharedPreferencesProperty(
        get = {
            getString(it, null)
                ?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
                ?: defaultValue
        },
        set = { k, v -> putString(k, v.name) },
        default = defaultValue
    )

    fun stringSet(defaultValue: Set<String>) = SharedPreferencesProperty(
        get = { getStringSet(it, null) ?: defaultValue },
        set = { k, v -> putStringSet(k, v) },
        default = defaultValue
    )
}
