package echo.music.iad1tya.kotlinytmusicscraper.models.response

enum class LikeStatus {
    LIKE,
    DISLIKE,
    INDIFFERENT,
}

fun String?.toLikeStatus(): LikeStatus =
    when (this) {
        "LIKE" -> LikeStatus.LIKE
        "DISLIKE" -> LikeStatus.DISLIKE
        else -> LikeStatus.INDIFFERENT
    }