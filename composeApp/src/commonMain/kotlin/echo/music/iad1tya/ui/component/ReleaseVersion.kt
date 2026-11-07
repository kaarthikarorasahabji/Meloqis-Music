package echo.music.iad1tya.ui.component

/** Only stable, numeric release versions can raise the required update floor. */
fun compareReleaseVersions(left: String, right: String): Int? {
    fun parts(value: String): List<Int>? {
        val clean = value.removePrefix("v")
        if (!Regex("[0-9]{1,6}\\.[0-9]{1,6}\\.[0-9]{1,6}").matches(clean)) return null
        return clean.split('.').map { it.toInt() }
    }
    val a = parts(left) ?: return null
    val b = parts(right) ?: return null
    for (i in a.indices) if (a[i] != b[i]) return a[i].compareTo(b[i])
    return 0
}
