package echo.music.iad1tya.viewModel

internal fun isWeeklySupportPromptDue(lastShown: Long?, now: Long): Boolean =
    lastShown == null || (now >= lastShown && now - lastShown >= 7L * 24 * 60 * 60 * 1000)
