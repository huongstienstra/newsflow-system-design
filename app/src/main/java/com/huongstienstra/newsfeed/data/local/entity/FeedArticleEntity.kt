package com.huongstienstra.newsfeed.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "feed_articles",
    primaryKeys = ["feedKey", "articleId"],
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("articleId"),
        Index(value = ["feedKey", "page", "position"]),
    ],
)
data class FeedArticleEntity(
    val feedKey: String,
    val articleId: String,
    val page: Int,
    val position: Int,
)
