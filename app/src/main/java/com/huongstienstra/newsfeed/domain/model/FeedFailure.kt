package com.huongstienstra.newsfeed.domain.model

sealed interface FeedFailure {
    data object MissingApiKey : FeedFailure
    data object RateLimited : FeedFailure
    data object InvalidApiKey : FeedFailure
    data object Network : FeedFailure
    data class Api(val message: String?) : FeedFailure
    data object Unknown : FeedFailure
}
