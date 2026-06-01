package com.huongstienstra.newsfeed.data.remote

import com.google.gson.Gson
import com.huongstienstra.newsfeed.data.remote.dto.ArticleDto
import com.huongstienstra.newsfeed.data.remote.dto.TopHeadlinesResponseDto
import javax.inject.Inject

class NewsRemoteDataSource @Inject constructor(
    private val newsApiService: NewsApiService,
    private val gson: Gson,
    @NewsApiKey private val apiKey: String,
) {
    suspend fun getTopHeadlines(
        country: String,
        query: String?,
        page: Int,
        pageSize: Int,
    ): RemoteNewsPage {
        if (apiKey.isBlank()) {
            throw NewsApiException.MissingApiKey
        }

        val response = newsApiService.getTopHeadlines(
            country = country,
            query = query?.takeIf { it.isNotBlank() },
            page = page,
            pageSize = pageSize,
            apiKey = apiKey,
        )
        val body = response.body()
        val errorBody = if (response.isSuccessful) {
            null
        } else {
            response.errorBody()?.string()?.toNewsApiErrorOrNull()
        }

        if (!response.isSuccessful || body == null || body.status != "ok") {
            throw NewsApiException.Http(
                statusCode = response.code(),
                apiCode = body?.code ?: errorBody?.code,
                apiMessage = body?.message
                    ?: errorBody?.message
                    ?: response.message().takeIf { it.isNotBlank() && it != "Response.error()" },
            )
        }

        return RemoteNewsPage(
            totalResults = body.totalResults ?: 0,
            articles = body.articles.orEmpty(),
        )
    }

    private fun String.toNewsApiErrorOrNull(): TopHeadlinesResponseDto? {
        return runCatching {
            gson.fromJson(this, TopHeadlinesResponseDto::class.java)
        }.getOrNull()
    }
}

data class RemoteNewsPage(
    val totalResults: Int,
    val articles: List<ArticleDto>,
)

sealed class NewsApiException(message: String) : Exception(message) {
    data object MissingApiKey : NewsApiException("NewsAPI key is missing")

    data class Http(
        val statusCode: Int,
        val apiCode: String?,
        val apiMessage: String?,
    ) : NewsApiException(apiMessage ?: "NewsAPI request failed")
}
