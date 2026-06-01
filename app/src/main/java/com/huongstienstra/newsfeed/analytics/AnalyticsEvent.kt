package com.huongstienstra.newsfeed.analytics

import java.net.URI
import java.security.MessageDigest

sealed class AnalyticsEvent(
    val eventName: String,
    val params: Map<String, Any?>,
) {
    data class ArticleFavorited(
        val articleId: String,
        val sourceName: String?,
    ) : AnalyticsEvent(
        eventName = "article_favorited",
        params = mapOf(
            PARAM_ARTICLE_ID to articleId.toAnalyticsId(),
            PARAM_SOURCE_NAME to sourceName,
        ),
    )

    data class ArticleUnfavorited(
        val articleId: String,
        val sourceName: String?,
    ) : AnalyticsEvent(
        eventName = "article_unfavorited",
        params = mapOf(
            PARAM_ARTICLE_ID to articleId.toAnalyticsId(),
            PARAM_SOURCE_NAME to sourceName,
        ),
    )

    data class ArticleViewed(
        val articleId: String,
        val sourceName: String?,
        val publishedAt: String?,
    ) : AnalyticsEvent(
        eventName = "article_viewed",
        params = mapOf(
            PARAM_ARTICLE_ID to articleId.toAnalyticsId(),
            PARAM_SOURCE_NAME to sourceName,
            PARAM_PUBLISHED_AT to publishedAt,
        ),
    )

    data class ArticleLinkOpened(
        val articleId: String,
        val url: String?,
    ) : AnalyticsEvent(
        eventName = "article_link_opened",
        params = mapOf(
            PARAM_ARTICLE_ID to articleId.toAnalyticsId(),
            PARAM_URL_HOST to url.hostOrNull(),
            PARAM_URL_LENGTH to url?.length,
        ),
    )

    data class SearchPerformed(
        val queryLength: Int,
        val resultsCount: Int? = null,
    ) : AnalyticsEvent(
        eventName = "search_performed",
        params = mapOf(
            PARAM_QUERY_LENGTH to queryLength,
            PARAM_RESULTS_COUNT to resultsCount,
        ),
    )

    data class FeedRefreshed(
        val trigger: String,
        val queryLength: Int?,
        val resultsCount: Int,
    ) : AnalyticsEvent(
        eventName = "feed_refreshed",
        params = mapOf(
            PARAM_TRIGGER to trigger,
            PARAM_QUERY_LENGTH to queryLength,
            PARAM_RESULTS_COUNT to resultsCount,
        ),
    )

    data class LoadMoreTriggered(
        val page: Int,
        val queryLength: Int?,
        val currentItemCount: Int,
    ) : AnalyticsEvent(
        eventName = "load_more_triggered",
        params = mapOf(
            PARAM_PAGE to page,
            PARAM_QUERY_LENGTH to queryLength,
            PARAM_CURRENT_ITEM_COUNT to currentItemCount,
        ),
    )

    private companion object {
        const val PARAM_ARTICLE_ID = "article_id"
        const val PARAM_SOURCE_NAME = "source_name"
        const val PARAM_PUBLISHED_AT = "published_at"
        const val PARAM_URL_HOST = "url_host"
        const val PARAM_URL_LENGTH = "url_length"
        const val PARAM_PAGE = "page"
        const val PARAM_QUERY_LENGTH = "query_length"
        const val PARAM_RESULTS_COUNT = "results_count"
        const val PARAM_CURRENT_ITEM_COUNT = "current_item_count"
        const val PARAM_TRIGGER = "trigger"
    }
}

private fun String.toAnalyticsId(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
    return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private fun String?.hostOrNull(): String? {
    return this?.let {
        runCatching { URI(it).host }.getOrNull()
    }
}
