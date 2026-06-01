package com.huongstienstra.newsfeed.data.repository

import com.huongstienstra.newsfeed.data.local.ArticleDao
import com.huongstienstra.newsfeed.data.local.entity.FavoriteEntity
import com.huongstienstra.newsfeed.data.local.entity.FeedArticleEntity
import com.huongstienstra.newsfeed.data.local.entity.FeedCacheEntity
import com.huongstienstra.newsfeed.data.mapper.toDomain
import com.huongstienstra.newsfeed.data.mapper.toEntity
import com.huongstienstra.newsfeed.data.remote.NewsRemoteDataSource
import com.huongstienstra.newsfeed.domain.model.Article
import com.huongstienstra.newsfeed.domain.model.FeedKey
import com.huongstienstra.newsfeed.domain.model.FeedPage
import com.huongstienstra.newsfeed.domain.repository.NewsRepository
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val remoteDataSource: NewsRemoteDataSource,
    private val articleDao: ArticleDao,
    private val clock: Clock,
) : NewsRepository {
    override fun observeFeed(feedKey: FeedKey): Flow<List<Article>> {
        return articleDao.observeArticles(feedKey.value)
            .map { articles -> articles.map { it.toDomain() } }
    }

    override fun observeArticle(articleId: String): Flow<Article?> {
        return articleDao.observeArticle(articleId)
            .map { it?.toDomain() }
    }

    override suspend fun loadTopHeadlines(
        feedKey: FeedKey,
        page: Int,
        pageSize: Int,
        forceRefresh: Boolean,
    ): Result<FeedPage> {
        return resultOf {
            val now = clock.millis()
            val cachedPage = maybeReadFreshCache(
                feedKey = feedKey,
                page = page,
                forceRefresh = forceRefresh,
                now = now,
            )
            if (cachedPage != null) {
                return@resultOf cachedPage
            }

            val remotePage = remoteDataSource.getTopHeadlines(
                country = feedKey.country,
                query = feedKey.query,
                page = page,
                pageSize = pageSize,
            )
            val articleEntities = remotePage.articles
                .mapNotNull { it.toEntity(insertedAtEpochMillis = now) }
                .distinctBy { it.id }

            val feedArticles = articleEntities.mapIndexed { index, article ->
                FeedArticleEntity(
                    feedKey = feedKey.value,
                    articleId = article.id,
                    page = page,
                    position = index,
                )
            }

            articleDao.upsertFeedPage(
                feedKey = feedKey.value,
                page = page,
                articles = articleEntities,
                feedArticles = feedArticles,
                feedCache = FeedCacheEntity(
                    feedKey = feedKey.value,
                    cachedAtEpochMillis = now,
                    totalResults = remotePage.totalResults,
                ),
                resetFeed = page == FIRST_PAGE,
            )

            val cachedArticles = articleDao.getArticles(feedKey.value).map { it.toDomain() }
            FeedPage(
                articles = cachedArticles,
                page = page,
                totalResults = remotePage.totalResults,
                fromCache = false,
            )
        }
    }

    override suspend fun setFavorite(articleId: String, isFavorite: Boolean): Result<Unit> {
        return resultOf {
            val favorite = FavoriteEntity(
                articleId = articleId,
                createdAtEpochMillis = clock.millis(),
            )
            if (isFavorite) {
                articleDao.upsertFavorite(favorite)
            } else {
                articleDao.deleteFavorite(favorite)
            }
        }
    }

    private suspend fun <T> resultOf(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
    }

    private suspend fun maybeReadFreshCache(
        feedKey: FeedKey,
        page: Int,
        forceRefresh: Boolean,
        now: Long,
    ): FeedPage? {
        if (forceRefresh || page != FIRST_PAGE) return null

        val cache = articleDao.getFeedCache(feedKey.value) ?: return null
        val isFresh = now - cache.cachedAtEpochMillis <= CACHE_TTL_MILLIS
        if (!isFresh) return null

        val cachedArticles = articleDao.getArticles(feedKey.value).map { it.toDomain() }
        if (cachedArticles.isEmpty()) return null

        return FeedPage(
            articles = cachedArticles,
            page = page,
            totalResults = cache.totalResults,
            fromCache = true,
        )
    }

    private companion object {
        const val FIRST_PAGE = 1
        const val CACHE_TTL_MILLIS = 30 * 60 * 1000L
    }
}
