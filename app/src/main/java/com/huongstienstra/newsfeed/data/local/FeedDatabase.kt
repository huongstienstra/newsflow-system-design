package com.huongstienstra.newsfeed.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.huongstienstra.newsfeed.data.local.entity.ArticleEntity
import com.huongstienstra.newsfeed.data.local.entity.FavoriteEntity
import com.huongstienstra.newsfeed.data.local.entity.FeedArticleEntity
import com.huongstienstra.newsfeed.data.local.entity.FeedCacheEntity

@Database(
    entities = [
        ArticleEntity::class,
        FeedArticleEntity::class,
        FeedCacheEntity::class,
        FavoriteEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class FeedDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
}
