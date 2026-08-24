package echo.music.iad1tya.kotlinytmusicscraper.models.response

import echo.music.iad1tya.kotlinytmusicscraper.models.Button
import echo.music.iad1tya.kotlinytmusicscraper.models.Continuation
import echo.music.iad1tya.kotlinytmusicscraper.models.Menu
import echo.music.iad1tya.kotlinytmusicscraper.models.MusicResponsiveListItemRenderer
import echo.music.iad1tya.kotlinytmusicscraper.models.MusicShelfRenderer
import echo.music.iad1tya.kotlinytmusicscraper.models.MusicTwoRowItemRenderer
import echo.music.iad1tya.kotlinytmusicscraper.models.ResponseContext
import echo.music.iad1tya.kotlinytmusicscraper.models.Runs
import echo.music.iad1tya.kotlinytmusicscraper.models.SectionListRenderer
import echo.music.iad1tya.kotlinytmusicscraper.models.SubscriptionButton
import echo.music.iad1tya.kotlinytmusicscraper.models.Tabs
import echo.music.iad1tya.kotlinytmusicscraper.models.ThumbnailRenderer
import echo.music.iad1tya.kotlinytmusicscraper.models.youtube.data.YouTubeDataPage
import kotlinx.serialization.Serializable

@Serializable
data class BrowseResponse(
    val contents: Contents?,
    val continuationContents: ContinuationContents?,
    val header: Header?,
    val microformat: Microformat?,
    val responseContext: ResponseContext,
    val background: Background?,
    val onResponseReceivedActions: List<OnResponseReceivedActions>?,
) {
    @Serializable
    data class OnResponseReceivedActions(
        val appendContinuationItemsAction: AppendContinuationItemsAction?,
    ) {
        @Serializable
        data class AppendContinuationItemsAction(
            val continuationItems: List<ContinuationItem>,
            val targetId: String,
        ) {
            @Serializable
            data class ContinuationItem(
                val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer?,
                val continuationItemRenderer:
                    YouTubeDataPage.Contents.TwoColumnWatchNextResults.Results.Results.Content.ItemSectionRenderer.Content.ContinuationItemRenderer?,
            )
        }
    }

    @Serializable
    data class Background(
        val musicThumbnailRenderer: ThumbnailRenderer.MusicThumbnailRenderer?,
    )

    @Serializable
    data class Contents(
        val singleColumnBrowseResultsRenderer: Tabs?,
        val twoColumnBrowseResultsRenderer: TwoColumnBrowseResultsRenderer?,
        val sectionListRenderer: SectionListRenderer?,
    ) {
        @Serializable
        data class TwoColumnBrowseResultsRenderer(
            val secondaryContents: SecondaryContents?,
            val tabs: List<Tabs.Tab>?,
        ) {
            @Serializable
            data class SecondaryContents(
                val sectionListRenderer: SectionListRenderer?,
            )
        }
    }

    @Serializable
    data class ContinuationContents(
        val sectionListContinuation: SectionListContinuation?,
        val musicPlaylistShelfContinuation: MusicPlaylistShelfContinuation?,
        val musicShelfContinuation: SearchResponse.ContinuationContents.MusicShelfContinuation?,
        val gridContinuation: GridContinuation?,
    ) {
        @Serializable
        data class GridContinuation(
            val itemSize: String?,
            val items: List<GridContinuation.Item>,
            val continuations: List<Continuation>?,
        ) {
            @Serializable
            data class Item(
                val musicTwoRowItemRenderer: MusicTwoRowItemRenderer?,
            )
        }

        @Serializable
        data class SectionListContinuation(
            val contents: List<SectionListRenderer.Content>,
            val continuations: List<Continuation>?,
        )

        @Serializable
        data class MusicPlaylistShelfContinuation(
            val contents: List<MusicShelfRenderer.Content>,
            val continuations: List<Continuation>?,
        )
    }

    @Serializable
    data class Header(
        val musicImmersiveHeaderRenderer: MusicImmersiveHeaderRenderer?,
        val musicDetailHeaderRenderer: MusicDetailHeaderRenderer?,
        val musicEditablePlaylistDetailHeaderRenderer: MusicEditablePlaylistDetailHeaderRenderer?,
        val musicVisualHeaderRenderer: MusicVisualHeaderRenderer?,
        val musicHeaderRenderer: MusicHeaderRenderer?,
    ) {
        @Serializable
        data class MusicImmersiveHeaderRenderer(
            val title: Runs,
            val description: Runs?,
            val thumbnail: ThumbnailRenderer?,
            val playButton: Button?,
            val startRadioButton: Button?,
            val subscriptionButton: SubscriptionButton?,
            val menu: Menu,
        )

        @Serializable
        data class MusicDetailHeaderRenderer(
            val title: Runs,
            val subtitle: Runs,
            val secondSubtitle: Runs,
            val description: Runs?,
            val thumbnail: ThumbnailRenderer,
            val menu: Menu,
        )

        @Serializable
        data class MusicEditablePlaylistDetailHeaderRenderer(
            val header: Header,
        ) {
            @Serializable
            data class Header(
                val musicDetailHeaderRenderer: MusicDetailHeaderRenderer?,
                val musicResponsiveHeaderRenderer: SectionListRenderer.Content.MusicResponsiveHeaderRenderer?,
            )
        }

        @Serializable
        data class MusicVisualHeaderRenderer(
            val title: Runs,
            val foregroundThumbnail: ThumbnailRenderer,
            val thumbnail: ThumbnailRenderer?,
        )

        @Serializable
        data class MusicHeaderRenderer(
            val title: Runs,
        )
    }

    @Serializable
    data class Microformat(
        val microformatDataRenderer: MicroformatDataRenderer?,
    ) {
        @Serializable
        data class MicroformatDataRenderer(
            val urlCanonical: String?,
        )
    }
}