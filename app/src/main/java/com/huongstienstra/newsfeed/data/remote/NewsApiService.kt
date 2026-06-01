package com.huongstienstra.newsfeed.data.remote

import com.huongstienstra.newsfeed.data.remote.dto.TopHeadlinesResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("v2/top-headlines")
    suspend fun getTopHeadlines(
        @Query("country") country: String,
        @Query("q") query: String?,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("apiKey") apiKey: String,
    ): Response<TopHeadlinesResponseDto>
}
