package com.huongstienstra.newsfeed.analytics

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpAnalyticsTracker @Inject constructor() : AnalyticsTracker {
    override fun logEvent(event: AnalyticsEvent) = Unit
}
