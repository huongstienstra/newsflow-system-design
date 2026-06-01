package com.huongstienstra.newsfeed.domain.repository

import com.huongstienstra.newsfeed.domain.model.Article
import com.huongstienstra.newsfeed.domain.model.FeedKey
import com.huongstienstra.newsfeed.domain.model.FeedPage
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    fun observeFeed(feedKey: FeedKey): Flow<List<Article>>

    fun observeArticle(articleId: String): Flow<Article?>

    suspend fun loadTopHeadlines(
        feedKey: FeedKey,
        page: Int,
        pageSize: Int,
        forceRefresh: Boolean = false,
    ): Result<FeedPage>

    suspend fun setFavorite(articleId: String, isFavorite: Boolean): Result<Unit>
}
