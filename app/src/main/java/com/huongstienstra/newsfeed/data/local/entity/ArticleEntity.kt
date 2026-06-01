package com.huongstienstra.newsfeed.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: String,
    val sourceId: String?,
    val sourceName: String?,
    val author: String?,
    val title: String,
    val description: String?,
    val originalUrl: String?,
    val imageUrl: String?,
    val publishedAtEpochMillis: Long?,
    val insertedAtEpochMillis: Long,
)
