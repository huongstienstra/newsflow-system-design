package com.huongstienstra.newsfeed.data.mapper

import com.huongstienstra.newsfeed.data.local.entity.ArticleEntity
import com.huongstienstra.newsfeed.data.local.entity.ArticleWithFavorite
import com.huongstienstra.newsfeed.data.remote.dto.ArticleDto
import com.huongstienstra.newsfeed.data.remote.dto.SourceDto
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleMappersTest {
    @Test
    fun dtoUsesUrlAsStableArticleId() {
        val entity = articleDto(url = "https://example.com/story").toEntity(123L)

        assertNotNull(entity)
        assertEquals("https://example.com/story", entity?.id)
        assertEquals("Example", entity?.sourceName)
        assertEquals(Instant.parse("2026-04-26T12:00:00Z").toEpochMilli(), entity?.publishedAtEpochMillis)
    }

    @Test
    fun dtoGeneratesFallbackIdWhenUrlIsMissing() {
        val entity = articleDto(url = null).toEntity(123L)

        assertNotNull(entity)
        assertTrue(entity?.id?.startsWith("generated-") == true)
        assertEquals("Breaking news", entity?.title)
    }

    @Test
    fun dtoSkipsArticleWhenTitleIsMissing() {
        val entity = articleDto(title = "   ").toEntity(123L)

        assertNull(entity)
    }

    @Test
    fun dtoTreatsInvalidPublishedAtAsNull() {
        val entity = articleDto(publishedAt = "not-a-date").toEntity(123L)

        assertNotNull(entity)
        assertNull(entity?.publishedAtEpochMillis)
    }

    @Test
    fun dtoKeepsNullableApiFieldsSafe() {
        val entity = articleDto(
            source = SourceDto(id = null, name = null),
            author = " ",
            description = "",
            urlToImage = null,
        ).toEntity(123L)

        assertNotNull(entity)
        assertNull(entity?.sourceId)
        assertNull(entity?.sourceName)
        assertNull(entity?.author)
        assertNull(entity?.description)
        assertNull(entity?.imageUrl)
    }

    @Test
    fun entityWithFavoriteMapsToDomain() {
        val domain = ArticleWithFavorite(
            article = ArticleEntity(
                id = "article-1",
                sourceId = "source-1",
                sourceName = "Source",
                author = "Author",
                title = "Title",
                description = "Description",
                originalUrl = "https://example.com/story",
                imageUrl = "https://example.com/image.jpg",
                publishedAtEpochMillis = 1_775_000_000_000L,
                insertedAtEpochMillis = 1_775_000_001_000L,
            ),
            isFavorite = true,
        ).toDomain()

        assertEquals("article-1", domain.id)
        assertEquals("Source", domain.source.name)
        assertEquals("Author", domain.author)
        assertEquals(true, domain.isFavorite)
        assertEquals(Instant.ofEpochMilli(1_775_000_000_000L), domain.publishedAt)
    }

    private fun articleDto(
        title: String? = "Breaking news",
        source: SourceDto? = SourceDto(id = "example", name = "Example"),
        url: String? = "https://example.com/story",
        publishedAt: String? = "2026-04-26T12:00:00Z",
        author: String? = "Reporter",
        description: String? = "Description",
        urlToImage: String? = "https://example.com/image.jpg",
    ): ArticleDto {
        return ArticleDto(
            source = source,
            author = author,
            title = title,
            description = description,
            url = url,
            urlToImage = urlToImage,
            publishedAt = publishedAt,
            content = "Truncated content",
        )
    }
}
