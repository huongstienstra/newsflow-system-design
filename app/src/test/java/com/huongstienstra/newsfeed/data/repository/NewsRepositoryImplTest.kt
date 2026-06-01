package com.huongstienstra.newsfeed.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.huongstienstra.newsfeed.data.local.FeedDatabase
import com.huongstienstra.newsfeed.data.remote.NewsApiService
import com.huongstienstra.newsfeed.data.remote.NewsRemoteDataSource
import com.huongstienstra.newsfeed.data.remote.dto.ArticleDto
import com.huongstienstra.newsfeed.data.remote.dto.SourceDto
import com.huongstienstra.newsfeed.data.remote.dto.TopHeadlinesResponseDto
import com.huongstienstra.newsfeed.domain.model.FeedKey
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
class NewsRepositoryImplTest {
    private lateinit var database: FeedDatabase
    private lateinit var service: FakeNewsApiService
    private lateinit var repository: NewsRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FeedDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        service = FakeNewsApiService()
        repository = NewsRepositoryImpl(
            remoteDataSource = NewsRemoteDataSource(service, gson = Gson(), apiKey = "test-api-key"),
            articleDao = database.articleDao(),
            clock = Clock.fixed(Instant.parse("2026-04-26T12:00:00Z"), ZoneOffset.UTC),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `first load fetches remote and stores articles`() = runTest {
        // Given
        service.enqueueSuccess(totalResults = 2, articles = listOf(article("a"), article("b")))

        // When
        val result = repository.loadTopHeadlines(FeedKey(), page = 1, pageSize = 20)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow().fromCache)
        assertEquals(listOf("Title a", "Title b"), result.getOrThrow().articles.map { it.title })
        assertEquals(1, service.calls.size)
        assertEquals(listOf("Title a", "Title b"), repository.observeFeed(FeedKey()).first().map { it.title })
    }

    @Test
    fun `fresh first page load uses cache`() = runTest {
        // Given
        service.enqueueSuccess(totalResults = 2, articles = listOf(article("a"), article("b")))
        repository.loadTopHeadlines(FeedKey(), page = 1, pageSize = 20).getOrThrow()

        // When
        val result = repository.loadTopHeadlines(FeedKey(), page = 1, pageSize = 20)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().fromCache)
        assertEquals(1, service.calls.size)
    }

    @Test
    fun `force refresh bypasses fresh cache`() = runTest {
        // Given
        service.enqueueSuccess(totalResults = 1, articles = listOf(article("a", title = "Old title")))
        repository.loadTopHeadlines(FeedKey(), page = 1, pageSize = 20).getOrThrow()
        service.enqueueSuccess(totalResults = 1, articles = listOf(article("a", title = "New title")))

        // When
        val result = repository.loadTopHeadlines(
            feedKey = FeedKey(),
            page = 1,
            pageSize = 20,
            forceRefresh = true,
        )

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow().fromCache)
        assertEquals(2, service.calls.size)
        assertEquals(listOf("New title"), result.getOrThrow().articles.map { it.title })
    }

    @Test
    fun `page two appends to existing feed order`() = runTest {
        // Given
        service.enqueueSuccess(totalResults = 4, articles = listOf(article("a"), article("b")))
        repository.loadTopHeadlines(FeedKey(), page = 1, pageSize = 2).getOrThrow()
        service.enqueueSuccess(totalResults = 4, articles = listOf(article("c"), article("d")))

        // When
        val result = repository.loadTopHeadlines(FeedKey(), page = 2, pageSize = 2)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            listOf("Title a", "Title b", "Title c", "Title d"),
            result.getOrThrow().articles.map { it.title },
        )
    }

    @Test
    fun `duplicate remote articles are deduped by id`() = runTest {
        // Given
        service.enqueueSuccess(
            totalResults = 2,
            articles = listOf(
                article("a", title = "First title"),
                article("a", title = "Duplicate title"),
            ),
        )

        // When
        val result = repository.loadTopHeadlines(FeedKey(), page = 1, pageSize = 20)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("First title"), result.getOrThrow().articles.map { it.title })
    }

    @Test
    fun `favorite survives refresh that removes article from current feed`() = runTest {
        // Given
        service.enqueueSuccess(totalResults = 2, articles = listOf(article("a"), article("b")))
        repository.loadTopHeadlines(FeedKey(), page = 1, pageSize = 20).getOrThrow()
        repository.setFavorite("https://example.com/b", isFavorite = true).getOrThrow()
        service.enqueueSuccess(totalResults = 2, articles = listOf(article("a"), article("c")))

        // When
        val refreshed = repository.loadTopHeadlines(
            feedKey = FeedKey(),
            page = 1,
            pageSize = 20,
            forceRefresh = true,
        ).getOrThrow()

        // Then
        assertEquals(listOf("Title a", "Title c"), refreshed.articles.map { it.title })
        assertEquals(true, repository.observeArticle("https://example.com/b").first()?.isFavorite)
    }

    @Test
    fun `favorite and unfavorite update observed article`() = runTest {
        // Given
        service.enqueueSuccess(totalResults = 1, articles = listOf(article("a")))
        repository.loadTopHeadlines(FeedKey(), page = 1, pageSize = 20).getOrThrow()

        // When / Then
        repository.setFavorite("https://example.com/a", isFavorite = true).getOrThrow()
        assertEquals(true, repository.observeArticle("https://example.com/a").first()?.isFavorite)

        repository.setFavorite("https://example.com/a", isFavorite = false).getOrThrow()
        assertEquals(false, repository.observeArticle("https://example.com/a").first()?.isFavorite)
    }

    @Test
    fun `api error returns failure`() = runTest {
        // Given
        service.enqueueApiError(message = "rate limited")

        // When
        val result = repository.loadTopHeadlines(FeedKey(), page = 1, pageSize = 20)

        // Then
        assertTrue(result.isFailure)
        assertEquals(1, service.calls.size)
    }

    private fun article(idSuffix: String, title: String = "Title $idSuffix"): ArticleDto {
        return ArticleDto(
            source = SourceDto(id = "source-$idSuffix", name = "Source $idSuffix"),
            author = "Author $idSuffix",
            title = title,
            description = "Description $idSuffix",
            url = "https://example.com/$idSuffix",
            urlToImage = "https://example.com/$idSuffix.jpg",
            publishedAt = "2026-04-26T12:00:00Z",
            content = "Content",
        )
    }
}

private class FakeNewsApiService : NewsApiService {
    val calls = mutableListOf<ApiCall>()
    private val responses = ArrayDeque<Response<TopHeadlinesResponseDto>>()

    fun enqueueSuccess(totalResults: Int, articles: List<ArticleDto>) {
        responses.add(
            Response.success(
                TopHeadlinesResponseDto(
                    status = "ok",
                    totalResults = totalResults,
                    articles = articles,
                    code = null,
                    message = null,
                ),
            ),
        )
    }

    fun enqueueApiError(message: String) {
        responses.add(
            Response.success(
                TopHeadlinesResponseDto(
                    status = "error",
                    totalResults = null,
                    articles = null,
                    code = "rateLimited",
                    message = message,
                ),
            ),
        )
    }

    override suspend fun getTopHeadlines(
        country: String,
        query: String?,
        page: Int,
        pageSize: Int,
        apiKey: String,
    ): Response<TopHeadlinesResponseDto> {
        calls.add(ApiCall(country, query, page, pageSize, apiKey))
        return responses.removeFirst()
    }
}

private data class ApiCall(
    val country: String,
    val query: String?,
    val page: Int,
    val pageSize: Int,
    val apiKey: String,
)
