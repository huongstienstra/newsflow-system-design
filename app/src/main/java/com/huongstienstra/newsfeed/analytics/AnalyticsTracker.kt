package com.huongstienstra.newsfeed.analytics

interface AnalyticsTracker {
    fun logEvent(event: AnalyticsEvent)
}
