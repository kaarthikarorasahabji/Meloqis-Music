package echo.music.iad1tya.ui.component

/** A charging/screen wake is never sufficient to show the app's decoration. */
fun edgeLightingVisible(enabled: Boolean, resumed: Boolean, interactive: Boolean, locked: Boolean, focused: Boolean): Boolean =
    enabled && resumed && interactive && !locked && focused
