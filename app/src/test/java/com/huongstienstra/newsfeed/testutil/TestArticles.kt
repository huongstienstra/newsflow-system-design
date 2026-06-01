package com.huongstienstra.newsfeed.testutil

import com.huongstienstra.newsfeed.domain.model.Article
import com.huongstienstra.newsfeed.domain.model.Source
import java.time.Instant

fun testArticle(
    id: String = "article-1",
    title: String = "Article title",
    sourceName: String? = "Source",
    isFavorite: Boolean = false,
): Article {
    return Article(
        id = id,
        source = Source(id = "source-$id", name = sourceName),
        author = "Author",
        title = title,
        description = "Description",
        originalUrl = "https://example.com/$id",
        imageUrl = "https://example.com/$id.jpg",
        publishedAt = Instant.parse("2026-04-26T12:00:00Z"),
        isFavorite = isFavorite,
    )
}
