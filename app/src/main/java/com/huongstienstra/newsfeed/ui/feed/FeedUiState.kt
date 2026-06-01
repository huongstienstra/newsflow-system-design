package com.huongstienstra.newsfeed.ui.feed

import com.huongstienstra.newsfeed.domain.model.Article

data class FeedUiState(
    val articles: List<Article> = emptyList(),
    val searchQuery: String = "",
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val fromCache: Boolean = false,
    val errorMessage: String? = null,
    val loadMoreErrorMessage: String? = null,
)
