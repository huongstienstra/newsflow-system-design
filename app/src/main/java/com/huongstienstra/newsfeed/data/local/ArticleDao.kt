package com.huongstienstra.newsfeed.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.huongstienstra.newsfeed.data.local.entity.ArticleEntity
import com.huongstienstra.newsfeed.data.local.entity.ArticleWithFavorite
import com.huongstienstra.newsfeed.data.local.entity.FavoriteEntity
import com.huongstienstra.newsfeed.data.local.entity.FeedArticleEntity
import com.huongstienstra.newsfeed.data.local.entity.FeedCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ArticleDao {
    @Transaction
    @Query(
        """
        SELECT a.*, CASE WHEN f.articleId IS NULL THEN 0 ELSE 1 END AS isFavorite
        FROM feed_articles AS fa
        INNER JOIN articles AS a ON a.id = fa.articleId
        LEFT JOIN favorites AS f ON f.articleId = a.id
        WHERE fa.feedKey = :feedKey
        ORDER BY fa.page ASC, fa.position ASC
        """,
    )
    abstract fun observeArticles(feedKey: String): Flow<List<ArticleWithFavorite>>

    @Transaction
    @Query(
        """
        SELECT a.*, CASE WHEN f.articleId IS NULL THEN 0 ELSE 1 END AS isFavorite
        FROM feed_articles AS fa
        INNER JOIN articles AS a ON a.id = fa.articleId
        LEFT JOIN favorites AS f ON f.articleId = a.id
        WHERE fa.feedKey = :feedKey
        ORDER BY fa.page ASC, fa.position ASC
        """,
    )
    abstract suspend fun getArticles(feedKey: String): List<ArticleWithFavorite>

    @Query(
        """
        SELECT a.*, CASE WHEN f.articleId IS NULL THEN 0 ELSE 1 END AS isFavorite
        FROM articles AS a
        LEFT JOIN favorites AS f ON f.articleId = a.id
        WHERE a.id = :articleId
        LIMIT 1
        """,
    )
    abstract fun observeArticle(articleId: String): Flow<ArticleWithFavorite?>

    @Query("SELECT * FROM feed_cache WHERE feedKey = :feedKey LIMIT 1")
    abstract suspend fun getFeedCache(feedKey: String): FeedCacheEntity?

    @Upsert
    protected abstract suspend fun upsertArticles(articles: List<ArticleEntity>)

    @Upsert
    protected abstract suspend fun upsertFeedCache(feedCache: FeedCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertFeedArticles(feedArticles: List<FeedArticleEntity>)

    @Query("DELETE FROM feed_articles WHERE feedKey = :feedKey")
    protected abstract suspend fun clearFeed(feedKey: String)

    @Query("DELETE FROM feed_articles WHERE feedKey = :feedKey AND page = :page")
    protected abstract suspend fun clearPage(feedKey: String, page: Int)

    @Transaction
    open suspend fun upsertFeedPage(
        feedKey: String,
        page: Int,
        articles: List<ArticleEntity>,
        feedArticles: List<FeedArticleEntity>,
        feedCache: FeedCacheEntity,
        resetFeed: Boolean,
    ) {
        if (resetFeed) {
            clearFeed(feedKey)
        } else {
            clearPage(feedKey, page)
        }
        upsertArticles(articles)
        upsertFeedCache(feedCache)
        insertFeedArticles(feedArticles)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertFavorite(favorite: FavoriteEntity)

    @Delete
    abstract suspend fun deleteFavorite(favorite: FavoriteEntity)
}
