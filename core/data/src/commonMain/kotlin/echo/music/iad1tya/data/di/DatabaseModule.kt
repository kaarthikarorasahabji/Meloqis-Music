package echo.music.iad1tya.data.di

import DatabaseDao
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import echo.music.iad1tya.data.dataStore.DataStoreManagerImpl
import echo.music.iad1tya.data.dataStore.createDataStoreInstance
import echo.music.iad1tya.data.db.Converters
import echo.music.iad1tya.data.db.MusicDatabase
import echo.music.iad1tya.data.db.datasource.AnalyticsDatasource
import echo.music.iad1tya.data.db.datasource.LocalDataSource
import echo.music.iad1tya.data.db.getDatabaseBuilder
import echo.music.iad1tya.domain.manager.DataStoreManager
import echo.music.iad1tya.kotlinytmusicscraper.YouTube
import echo.music.iad1tya.spotify.Spotify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module
import echo.music.iad1tya.aiservice.AiClient
import echo.music.iad1tya.lyrics.SimpMusicLyricsClient
import kotlin.time.ExperimentalTime
import echo.music.iad1tya.autoeq.AutoEq

@OptIn(ExperimentalTime::class)
val databaseModule =
    module {
        single(createdAtStart = true) {
            Converters()
        }
        // Database
        single(createdAtStart = true) {
            getDatabaseBuilder(
                get<Converters>()
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
        // DatabaseDao
        single(createdAtStart = true) {
            get<MusicDatabase>().getDatabaseDao()
        }
        // LocalDataSource
        single(createdAtStart = true) {
            LocalDataSource(get<DatabaseDao>(), get<MusicDatabase>())
        }
        // AnalyticsDatasource
        single(createdAtStart = true) {
            AnalyticsDatasource(get<DatabaseDao>())
        }
        // Datastore
        single(createdAtStart = true) {
            createDataStoreInstance()
        }
        // DatastoreManager
        single<DataStoreManager>(createdAtStart = true) {
            DataStoreManagerImpl(get<DataStore<Preferences>>())
        }

        // Move YouTube from Singleton to Koin DI
        single(createdAtStart = true) {
            YouTube()
        }

        single(createdAtStart = true) {
            Spotify()
        }

        single(createdAtStart = true) {
            AiClient()
        }

        single(createdAtStart = true) {
            SimpMusicLyricsClient()
        }

        // Not created at start, unlike the rest: nothing needs it until someone opens the AutoEq
        // picker, and it holds an HTTP client the vast majority of sessions never use.
        single {
            AutoEq()
        }
    }