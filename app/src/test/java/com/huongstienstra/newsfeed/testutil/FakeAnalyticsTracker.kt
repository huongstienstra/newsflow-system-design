package com.huongstienstra.newsfeed.testutil

import com.huongstienstra.newsfeed.analytics.AnalyticsEvent
import com.huongstienstra.newsfeed.analytics.AnalyticsTracker

class FakeAnalyticsTracker : AnalyticsTracker {
    val events = mutableListOf<AnalyticsEvent>()

    override fun logEvent(event: AnalyticsEvent) {
        events.add(event)
    }
}
