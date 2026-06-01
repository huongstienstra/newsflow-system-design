package com.huongstienstra.newsfeed.domain.model

import java.time.Instant

data class Article(
    val id: String,
    val source: Source,
    val author: String?,
    val title: String,
    val description: String?,
    val originalUrl: String?,
    val imageUrl: String?,
    val publishedAt: Instant?,
    val isFavorite: Boolean,
)

data class Source(
    val id: String?,
    val name: String?,
)
