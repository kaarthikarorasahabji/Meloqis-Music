package echo.music.iad1tya.domain.utils

sealed class FilterState {
    data object CustomOrder : FilterState()

    data object OlderFirst : FilterState()

    data object NewerFirst : FilterState()

    data object Title : FilterState()
}