package echo.music.iad1tya.kotlinytmusicscraper.models.subscriptionButton

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubscribeButtonRenderer(
    @SerialName("longSubscriberCountText")
    val longSubscriberCountText: LongSubscriberCountText,
)