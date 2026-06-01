package com.huongstienstra.newsfeed.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class ArticleWithFavorite(
    @Embedded val article: ArticleEntity,
    @ColumnInfo(name = "isFavorite") val isFavorite: Boolean,
)
