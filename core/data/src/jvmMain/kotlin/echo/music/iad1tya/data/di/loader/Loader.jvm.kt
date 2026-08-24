package echo.music.iad1tya.data.di.loader

import echo.music.iad1tya.media_jvm.di.loadDesktopPlayerModule

actual fun loadMediaService() {
    loadDesktopPlayerModule()
}
