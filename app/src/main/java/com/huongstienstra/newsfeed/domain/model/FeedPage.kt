package com.huongstienstra.newsfeed.domain.model

data class FeedPage(
    val articles: List<Article>,
    val page: Int,
    val totalResults: Int,
    val fromCache: Boolean,
) {
    val hasMore: Boolean
        get() = articles.size < totalResults
}
