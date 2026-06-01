package com.huongstienstra.newsfeed.data.remote

import com.google.gson.Gson
import com.huongstienstra.newsfeed.data.remote.dto.ArticleDto
import com.huongstienstra.newsfeed.data.remote.dto.SourceDto
import com.huongstienstra.newsfeed.data.remote.dto.TopHeadlinesResponseDto
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class NewsRemoteDataSourceTest {
    private val gson = Gson()

    @Test
    fun `missing api key throws missing key error`() = runTest {
        // Given
        val dataSource = NewsRemoteDataSource(
            newsApiService = FakeNewsApiService(),
            gson = gson,
            apiKey = "",
        )

        // When
        val result = runCatching {
            dataSource.getTopHeadlines(country = "us", query = null, page = 1, pageSize = 20)
        }

        // Then
        assertEquals(NewsApiException.MissingApiKey, result.exceptionOrNull())
    }

    @Test
    fun `successful response returns total and articles`() = runTest {
        // Given
        val service = FakeNewsApiService()
        service.response = Response.success(
            TopHeadlinesResponseDto(
                status = "ok",
                totalResults = 1,
                articles = listOf(articleDto()),
                code = null,
                message = null,
            ),
        )
        val dataSource = NewsRemoteDataSource(service, gson = gson, apiKey = "test-key")

        // When
        val page = dataSource.getTopHeadlines(country = "us", query = "android", page = 2, pageSize = 10)

        // Then
        assertEquals(1, page.totalResults)
        assertEquals("Title", page.articles.single().title)
        assertEquals(ApiCall("us", "android", 2, 10, "test-key"), service.calls.single())
    }

    @Test
    fun `blank query is sent as null`() = runTest {
        // Given
        val service = FakeNewsApiService()
        service.response = Response.success(
            TopHeadlinesResponseDto(
                status = "ok",
                totalResults = 0,
                articles = emptyList(),
                code = null,
                message = null,
            ),
        )
        val dataSource = NewsRemoteDataSource(service, gson = gson, apiKey = "test-key")

        // When
        dataSource.getTopHeadlines(country = "us", query = "   ", page = 1, pageSize = 20)

        // Then
        assertEquals(null, service.calls.single().query)
    }

    @Test
    fun `api error body throws http error`() = runTest {
        // Given
        val service = FakeNewsApiService()
        service.response = Response.success(
            TopHeadlinesResponseDto(
                status = "error",
                totalResults = null,
                articles = null,
                code = "rateLimited",
                message = "rate limited",
            ),
        )
        val dataSource = NewsRemoteDataSource(service, gson = gson, apiKey = "test-key")

        // When
        val result = runCatching {
            dataSource.getTopHeadlines(country = "us", query = null, page = 1, pageSize = 20)
        }

        // Then
        val error = result.exceptionOrNull()
        assertTrue(error is NewsApiException.Http)
        assertEquals("rateLimited", (error as NewsApiException.Http).apiCode)
    }

    @Test
    fun `http error parses error body message`() = runTest {
        // Given
        val service = FakeNewsApiService()
        service.response = Response.error(
            500,
            """{"status":"error","message":"Internal server error"}"""
                .toResponseBody("application/json".toMediaType()),
        )
        val dataSource = NewsRemoteDataSource(service, gson = gson, apiKey = "test-key")

        // When
        val result = runCatching {
            dataSource.getTopHeadlines(country = "us", query = null, page = 1, pageSize = 20)
        }

        // Then
        val error = result.exceptionOrNull()
        assertTrue(error is NewsApiException.Http)
        assertEquals("Internal server error", (error as NewsApiException.Http).apiMessage)
    }

    private fun articleDto(): ArticleDto {
        return ArticleDto(
            source = SourceDto(id = "source", name = "Source"),
            author = "Author",
            title = "Title",
            description = "Description",
            url = "https://example.com/story",
            urlToImage = null,
            publishedAt = "2026-04-26T12:00:00Z",
            content = null,
        )
    }
}

private class FakeNewsApiService : NewsApiService {
    val calls = mutableListOf<ApiCall>()
    lateinit var response: Response<TopHeadlinesResponseDto>

    override suspend fun getTopHeadlines(
        country: String,
        query: String?,
        page: Int,
        pageSize: Int,
        apiKey: String,
    ): Response<TopHeadlinesResponseDto> {
        calls.add(ApiCall(country, query, page, pageSize, apiKey))
        return response
    }
}

private data class ApiCall(
    val country: String,
    val query: String?,
    val page: Int,
    val pageSize: Int,
    val apiKey: String,
)
