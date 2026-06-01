package com.huongstienstra.newsfeed.analytics

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.huongstienstra.newsfeed.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAnalyticsTracker @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
) : AnalyticsTracker {
    override fun logEvent(event: AnalyticsEvent) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "logEvent ${event.eventName} ${event.params}")
        }
        firebaseAnalytics.logEvent(event.eventName, event.params.toBundle())
    }

    private companion object {
        const val TAG = "NewsFeedAnalytics"
    }
}

private fun Map<String, Any?>.toBundle(): Bundle {
    return Bundle().apply {
        this@toBundle.forEach { (key, value) ->
            when (value) {
                null -> Unit
                is String -> putString(key, value)
                is Int -> putLong(key, value.toLong())
                is Long -> putLong(key, value)
                is Float -> putDouble(key, value.toDouble())
                is Double -> putDouble(key, value)
                is Boolean -> putString(key, value.toString())
                else -> putString(key, value.toString())
            }
        }
    }
}
