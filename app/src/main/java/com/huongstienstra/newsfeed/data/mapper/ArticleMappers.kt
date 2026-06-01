package com.huongstienstra.newsfeed.data.mapper

import com.huongstienstra.newsfeed.data.local.entity.ArticleEntity
import com.huongstienstra.newsfeed.data.local.entity.ArticleWithFavorite
import com.huongstienstra.newsfeed.data.remote.dto.ArticleDto
import com.huongstienstra.newsfeed.domain.model.Article
import com.huongstienstra.newsfeed.domain.model.Source
import java.security.MessageDigest
import java.time.Instant

fun ArticleDto.toEntity(insertedAtEpochMillis: Long): ArticleEntity? {
    val normalizedTitle = title?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val normalizedUrl = url?.trim()?.takeIf { it.isNotEmpty() }
    val publishedAtEpochMillis = publishedAt.toEpochMillisOrNull()
    val sourceName = source?.name?.trim()?.takeIf { it.isNotEmpty() }

    return ArticleEntity(
        id = normalizedUrl ?: fallbackArticleId(
            title = normalizedTitle,
            publishedAt = publishedAt,
            sourceName = sourceName,
        ),
        sourceId = source?.id?.trim()?.takeIf { it.isNotEmpty() },
        sourceName = sourceName,
        author = author?.trim()?.takeIf { it.isNotEmpty() },
        title = normalizedTitle,
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        originalUrl = normalizedUrl,
        imageUrl = urlToImage?.trim()?.takeIf { it.isNotEmpty() },
        publishedAtEpochMillis = publishedAtEpochMillis,
        insertedAtEpochMillis = insertedAtEpochMillis,
    )
}

fun ArticleWithFavorite.toDomain(): Article {
    return Article(
        id = article.id,
        source = Source(
            id = article.sourceId,
            name = article.sourceName,
        ),
        author = article.author,
        title = article.title,
        description = article.description,
        originalUrl = article.originalUrl,
        imageUrl = article.imageUrl,
        publishedAt = article.publishedAtEpochMillis?.let(Instant::ofEpochMilli),
        isFavorite = isFavorite,
    )
}

private fun String?.toEpochMillisOrNull(): Long? {
    return try {
        this?.let(Instant::parse)?.toEpochMilli()
    } catch (_: Exception) {
        null
    }
}

private fun fallbackArticleId(
    title: String,
    publishedAt: String?,
    sourceName: String?,
): String {
    val rawKey = listOf(title, publishedAt.orEmpty(), sourceName.orEmpty()).joinToString("|")
    val digest = MessageDigest.getInstance("SHA-256").digest(rawKey.toByteArray())
    return digest.joinToString(prefix = "generated-", separator = "") { "%02x".format(it) }
}
