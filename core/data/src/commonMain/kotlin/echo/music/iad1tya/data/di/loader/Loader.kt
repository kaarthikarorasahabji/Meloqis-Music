package echo.music.iad1tya.data.di.loader

import echo.music.iad1tya.data.di.databaseModule
import echo.music.iad1tya.data.di.mediaHandlerModule
import echo.music.iad1tya.data.di.repositoryModule
import org.koin.core.context.loadKoinModules

fun loadAllModules() {
    loadKoinModules(
        listOf(
            databaseModule,
            repositoryModule,
        ),
    )
    loadKoinModules(mediaHandlerModule)
    loadMediaService()
}

expect fun loadMediaService()