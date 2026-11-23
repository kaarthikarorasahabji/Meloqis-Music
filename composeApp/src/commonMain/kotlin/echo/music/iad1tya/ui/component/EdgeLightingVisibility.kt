package echo.music.iad1tya.ui.component

/** Decorative lighting only belongs to the unlocked app while it is running on battery. */
fun edgeLightingVisible(
    enabled: Boolean,
    resumed: Boolean,
    interactive: Boolean,
    locked: Boolean,
    focused: Boolean,
    pluggedIn: Boolean,
): Boolean = enabled && resumed && interactive && !locked && focused && !pluggedIn
