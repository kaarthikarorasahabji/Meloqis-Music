package echo.music.iad1tya.data.di

import echo.music.iad1tya.common.Config.SERVICE_SCOPE
import echo.music.iad1tya.data.io.fileDir
import echo.music.iad1tya.data.repository.AccountRepositoryImpl
import echo.music.iad1tya.data.repository.AlbumRepositoryImpl
import echo.music.iad1tya.data.repository.AnalyticsRepositoryImpl
import echo.music.iad1tya.data.repository.ArtistRepositoryImpl
import echo.music.iad1tya.data.repository.AutoEqRepositoryImpl
import echo.music.iad1tya.data.repository.CommonRepositoryImpl
import echo.music.iad1tya.data.repository.HomeRepositoryImpl
import echo.music.iad1tya.data.repository.ImportRepositoryImpl
import echo.music.iad1tya.data.repository.LocalPlaylistRepositoryImpl
import echo.music.iad1tya.data.repository.LyricsCanvasRepositoryImpl
import echo.music.iad1tya.data.repository.PlaylistRepositoryImpl
import echo.music.iad1tya.data.repository.PodcastRepositoryImpl
import echo.music.iad1tya.data.repository.SearchRepositoryImpl
import echo.music.iad1tya.data.repository.SongRepositoryImpl
import echo.music.iad1tya.data.repository.StreamRepositoryImpl
import echo.music.iad1tya.data.repository.UpdateRepositoryImpl
import echo.music.iad1tya.domain.repository.AccountRepository
import echo.music.iad1tya.domain.repository.AlbumRepository
import echo.music.iad1tya.domain.repository.AnalyticsRepository
import echo.music.iad1tya.domain.repository.ArtistRepository
import echo.music.iad1tya.domain.repository.AutoEqRepository
import echo.music.iad1tya.domain.repository.CommonRepository
import echo.music.iad1tya.domain.repository.HomeRepository
import echo.music.iad1tya.domain.repository.ImportRepository
import echo.music.iad1tya.domain.repository.LocalPlaylistRepository
import echo.music.iad1tya.domain.repository.LyricsCanvasRepository
import echo.music.iad1tya.domain.repository.PlaylistRepository
import echo.music.iad1tya.domain.repository.PodcastRepository
import echo.music.iad1tya.domain.repository.SearchRepository
import echo.music.iad1tya.domain.repository.SongRepository
import echo.music.iad1tya.domain.repository.StreamRepository
import echo.music.iad1tya.domain.repository.UpdateRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

val repositoryModule =
    module {
        single<AccountRepository>(createdAtStart = true) {
            AccountRepositoryImpl(get(), get())
        }

        single<AlbumRepository>(createdAtStart = true) {
            AlbumRepositoryImpl(get(), get())
        }

        single<ArtistRepository>(createdAtStart = true) {
            ArtistRepositoryImpl(get(), get(), get())
        }

        single<CommonRepository>(createdAtStart = true) {
            CommonRepositoryImpl(get(named(SERVICE_SCOPE)), get(), get(), get(), get(), get()).apply {
                this.init("${fileDir()}/ytdlp-cookie.txt", get())
            }
        }

        // Lazy for the same reason its client is: the picker is the only thing that wants it.
        single<AutoEqRepository> {
            AutoEqRepositoryImpl(get(), get())
        }

        single<HomeRepository>(createdAtStart = true) {
            HomeRepositoryImpl(get(), get())
        }

        single<ImportRepository>(createdAtStart = true) {
            ImportRepositoryImpl(get())
        }

        single<LocalPlaylistRepository>(createdAtStart = true) {
            LocalPlaylistRepositoryImpl(get(), get())
        }

        single<LyricsCanvasRepository>(createdAtStart = true) {
            LyricsCanvasRepositoryImpl(get(), get(), get(), get(), get())
        }

        single<PlaylistRepository>(createdAtStart = true) {
            PlaylistRepositoryImpl(get(), get(), get())
        }

        single<PodcastRepository>(createdAtStart = true) {
            PodcastRepositoryImpl(get(), get())
        }

        single<SearchRepository>(createdAtStart = true) {
            SearchRepositoryImpl(get(), get())
        }

        single<SongRepository>(createdAtStart = true) {
            SongRepositoryImpl(get(), get(), get())
        }

        single<StreamRepository>(createdAtStart = true) {
            StreamRepositoryImpl(get(), get())
        }

        single<UpdateRepository>(createdAtStart = true) {
            UpdateRepositoryImpl(get())
        }

        single<AnalyticsRepository>(createdAtStart = true) {
            AnalyticsRepositoryImpl(get())
        }
    }