package com.huongstienstra.newsfeed.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AnalyticsEventTest {
    @Test
    fun articleViewedContainsExpectedNameAndParams() {
        val articleId = "article-1"
        val event = AnalyticsEvent.ArticleViewed(
            articleId = articleId,
            sourceName = "BBC",
            publishedAt = "2026-04-26T12:00:00Z",
        )

        assertEquals("article_viewed", event.eventName)
        assertEquals(64, (event.params["article_id"] as String).length)
        assertFalse(event.params["article_id"].toString().contains(articleId))
        assertEquals("BBC", event.params["source_name"])
        assertEquals("2026-04-26T12:00:00Z", event.params["published_at"])
    }

    @Test
    fun favoriteEventsContainArticleAndSource() {
        val favorited = AnalyticsEvent.ArticleFavorited("article-1", "CNN")
        val unfavorited = AnalyticsEvent.ArticleUnfavorited("article-1", "CNN")

        assertEquals("article_favorited", favorited.eventName)
        assertEquals("article_unfavorited", unfavorited.eventName)
        assertEquals(favorited.params["article_id"], unfavorited.params["article_id"])
        assertEquals(64, (favorited.params["article_id"] as String).length)
        assertEquals("CNN", favorited.params["source_name"])
    }

    @Test
    fun linkOpenedContainsSafeArticleAndUrlParams() {
        val event = AnalyticsEvent.ArticleLinkOpened(
            articleId = "article-1",
            url = "https://example.com/story",
        )

        assertEquals("article_link_opened", event.eventName)
        assertEquals(64, (event.params["article_id"] as String).length)
        assertEquals("example.com", event.params["url_host"])
        assertEquals(25, event.params["url_length"])
        assertFalse(event.params.containsKey("url"))
    }

    @Test
    fun articleIdParamStaysUnderFirebaseStringLimitForLongUrlIds() {
        val longArticleId = "https://www.startribune.com/report-wolves-anthony-edwards-likely-out-multiple-weeks-with-hyperextended-left-knee-bone-bruise/601801771"
        val event = AnalyticsEvent.ArticleFavorited(longArticleId, "Minneapolis Star Tribune")

        assertEquals(134, longArticleId.length)
        assertEquals(64, (event.params["article_id"] as String).length)
        assertFalse(event.params["article_id"].toString().contains("startribune"))
    }

    @Test
    fun searchPerformedDoesNotContainRawQuery() {
        val event = AnalyticsEvent.SearchPerformed(
            queryLength = 7,
        )

        assertEquals("search_performed", event.eventName)
        assertEquals(7, event.params["query_length"])
        assertEquals(null, event.params["results_count"])
        assertFalse(event.params.containsKey("query"))
    }

    @Test
    fun feedRefreshedContainsTriggerAndCounts() {
        val event = AnalyticsEvent.FeedRefreshed(
            trigger = "manual",
            queryLength = null,
            resultsCount = 35,
        )

        assertEquals("feed_refreshed", event.eventName)
        assertEquals("manual", event.params["trigger"])
        assertEquals(null, event.params["query_length"])
        assertEquals(35, event.params["results_count"])
    }

    @Test
    fun loadMoreTriggeredContainsPageQueryLengthAndCurrentItemCount() {
        val event = AnalyticsEvent.LoadMoreTriggered(
            page = 2,
            queryLength = 7,
            currentItemCount = 20,
        )

        assertEquals("load_more_triggered", event.eventName)
        assertEquals(2, event.params["page"])
        assertEquals(7, event.params["query_length"])
        assertEquals(20, event.params["current_item_count"])
        assertFalse(event.params.containsKey("query"))
    }
}
