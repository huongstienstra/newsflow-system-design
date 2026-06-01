package com.huongstienstra.newsfeed.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.huongstienstra.newsfeed.BuildConfig
import com.huongstienstra.newsfeed.analytics.AnalyticsTracker
import com.huongstienstra.newsfeed.analytics.FirebaseAnalyticsTracker
import com.huongstienstra.newsfeed.analytics.NoOpAnalyticsTracker
import com.huongstienstra.newsfeed.data.repository.NewsRepositoryImpl
import com.huongstienstra.newsfeed.domain.repository.NewsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindNewsRepository(
        implementation: NewsRepositoryImpl,
    ): NewsRepository

    companion object {
        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.systemUTC()

        @Provides
        @Singleton
        fun provideAnalyticsTracker(
            @ApplicationContext context: Context,
        ): AnalyticsTracker {
            return if (BuildConfig.HAS_FIREBASE_CONFIG) {
                FirebaseAnalyticsTracker(FirebaseAnalytics.getInstance(context))
            } else {
                NoOpAnalyticsTracker()
            }
        }
    }
}
