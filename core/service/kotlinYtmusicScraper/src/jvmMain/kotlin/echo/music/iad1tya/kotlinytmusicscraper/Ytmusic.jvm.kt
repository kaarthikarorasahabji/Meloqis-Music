package echo.music.iad1tya.kotlinytmusicscraper

import java.util.*

actual fun getCountry(): String = Locale.getDefault().country
actual fun getLanguage(): String = Locale.getDefault().language