package com.huongstienstra.newsfeed.ui.detail

import com.huongstienstra.newsfeed.domain.model.Article

data class ArticleDetailUiState(
    val article: Article? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
