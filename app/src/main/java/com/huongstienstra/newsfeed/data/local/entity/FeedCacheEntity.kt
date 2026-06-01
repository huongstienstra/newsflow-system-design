package com.huongstienstra.newsfeed.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed_cache")
data class FeedCacheEntity(
    @PrimaryKey val feedKey: String,
    val cachedAtEpochMillis: Long,
    val totalResults: Int,
)
