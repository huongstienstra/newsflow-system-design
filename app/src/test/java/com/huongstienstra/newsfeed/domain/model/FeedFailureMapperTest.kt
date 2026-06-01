package com.huongstienstra.newsfeed.domain.model

import com.huongstienstra.newsfeed.data.remote.NewsApiException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedFailureMapperTest {
    @Test
    fun mapsMissingApiKey() {
        assertEquals(
            FeedFailure.MissingApiKey,
            NewsApiException.MissingApiKey.toFeedFailure(),
        )
    }

    @Test
    fun mapsInvalidApiKeyByApiCode() {
        val error = NewsApiException.Http(
            statusCode = 400,
            apiCode = "apiKeyInvalid",
            apiMessage = "bad key",
        )

        assertEquals(FeedFailure.InvalidApiKey, error.toFeedFailure())
    }

    @Test
    fun mapsRateLimitedByStatusCode() {
        val error = NewsApiException.Http(
            statusCode = 429,
            apiCode = null,
            apiMessage = "slow down",
        )

        assertEquals(FeedFailure.RateLimited, error.toFeedFailure())
    }

    @Test
    fun mapsNetworkError() {
        assertEquals(FeedFailure.Network, IOException("offline").toFeedFailure())
    }

    @Test
    fun mapsGenericApiErrorAndMessage() {
        val error = NewsApiException.Http(
            statusCode = 500,
            apiCode = "serverError",
            apiMessage = "server failed",
        )

        val failure = error.toFeedFailure()

        assertEquals(FeedFailure.Api("server failed"), failure)
        assertEquals("server failed", failure.userMessage())
    }

    @Test
    fun mapsUnknownError() {
        assertEquals(FeedFailure.Unknown, IllegalStateException("boom").toFeedFailure())
    }
}
