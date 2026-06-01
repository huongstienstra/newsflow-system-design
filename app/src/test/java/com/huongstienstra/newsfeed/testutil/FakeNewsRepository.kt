package com.huongstienstra.newsfeed.testutil

import com.huongstienstra.newsfeed.domain.model.Article
import com.huongstienstra.newsfeed.domain.model.FeedKey
import com.huongstienstra.newsfeed.domain.model.FeedPage
import com.huongstienstra.newsfeed.domain.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeNewsRepository : NewsRepository {
    val loadCalls = mutableListOf<LoadCall>()
    val favoriteCalls = mutableListOf<FavoriteCall>()

    var nextLoadResult: Result<FeedPage> = Result.success(
        FeedPage(
            articles = emptyList(),
            page = 1,
            totalResults = 0,
            fromCache = false,
        ),
    )
    var nextFavoriteResult: Result<Unit> = Result.success(Unit)

    private val articles = MutableStateFlow<Map<String, List<Article>>>(emptyMap())

    fun emit(feedKey: FeedKey = FeedKey(), articlesForFeed: List<Article>) {
        articles.value = articles.value + (feedKey.value to articlesForFeed)
    }

    override fun observeFeed(feedKey: FeedKey): Flow<List<Article>> {
        return articles.map { it[feedKey.value].orEmpty() }
    }

    override fun observeArticle(articleId: String): Flow<Article?> {
        return articles.map { feeds ->
            feeds.values.flatten().firstOrNull { it.id == articleId }
        }
    }

    override suspend fun loadTopHeadlines(
        feedKey: FeedKey,
        page: Int,
        pageSize: Int,
        forceRefresh: Boolean,
    ): Result<FeedPage> {
        loadCalls.add(LoadCall(feedKey, page, pageSize, forceRefresh))
        nextLoadResult.onSuccess { pageResult ->
            emit(feedKey, pageResult.articles)
        }
        return nextLoadResult
    }

    override suspend fun setFavorite(articleId: String, isFavorite: Boolean): Result<Unit> {
        favoriteCalls.add(FavoriteCall(articleId, isFavorite))
        return nextFavoriteResult
    }
}

data class LoadCall(
    val feedKey: FeedKey,
    val page: Int,
    val pageSize: Int,
    val forceRefresh: Boolean,
)

data class FavoriteCall(
    val articleId: String,
    val isFavorite: Boolean,
)
