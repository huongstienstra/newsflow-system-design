package com.huongstienstra.newsfeed.di

import android.content.Context
import androidx.room.Room
import com.huongstienstra.newsfeed.data.local.ArticleDao
import com.huongstienstra.newsfeed.data.local.FeedDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideFeedDatabase(
        @ApplicationContext context: Context,
    ): FeedDatabase {
        return Room.databaseBuilder(
            context,
            FeedDatabase::class.java,
            "feed.db",
        ).build()
    }

    @Provides
    fun provideArticleDao(database: FeedDatabase): ArticleDao {
        return database.articleDao()
    }
}
