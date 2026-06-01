package com.huongstienstra.newsfeed.domain.model

import com.huongstienstra.newsfeed.data.remote.NewsApiException
import java.io.IOException

fun Throwable.toFeedFailure(): FeedFailure {
    return when (this) {
        NewsApiException.MissingApiKey -> FeedFailure.MissingApiKey
        is NewsApiException.Http -> when {
            apiCode == "apiKeyInvalid" || statusCode == 401 -> FeedFailure.InvalidApiKey
            apiCode == "rateLimited" || statusCode == 429 -> FeedFailure.RateLimited
            else -> FeedFailure.Api(apiMessage)
        }

        is IOException -> FeedFailure.Network
        else -> FeedFailure.Unknown
    }
}

fun FeedFailure.userMessage(): String {
    return when (this) {
        FeedFailure.MissingApiKey -> "News API key is missing."
        FeedFailure.InvalidApiKey -> "News API key is invalid."
        FeedFailure.RateLimited -> "News API limit reached. Please retry later."
        FeedFailure.Network -> "No internet connection. Showing saved articles if available."
        is FeedFailure.Api -> message ?: "Unable to load news right now."
        FeedFailure.Unknown -> "Something went wrong. Please try again."
    }
}
