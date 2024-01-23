package it.vfsfitvnm.vimusic.utils

inline val String.version get() = Version(value = this)

@JvmInline
value class Version(private val parts: List<Int>) {
    constructor(value: String) : this(value.split(".").mapNotNull { it.toIntOrNull() })

    val major get() = parts.firstOrNull()
    val minor get() = parts.getOrNull(1)
    val patch get() = parts.getOrNull(2)

    companion object {
        private val comparator = compareBy<Version> { it.major } then
                compareBy { it.minor } then
                compareBy { it.patch }
    }

    operator fun compareTo(other: Version) = comparator.compare(this, other)
}
