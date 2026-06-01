package com.huongstienstra.newsfeed.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huongstienstra.newsfeed.analytics.AnalyticsEvent
import com.huongstienstra.newsfeed.analytics.AnalyticsTracker
import com.huongstienstra.newsfeed.domain.model.Article
import com.huongstienstra.newsfeed.domain.model.toFeedFailure
import com.huongstienstra.newsfeed.domain.model.userMessage
import com.huongstienstra.newsfeed.domain.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newsRepository: NewsRepository,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val articleId: String = URLDecoder.decode(
        checkNotNull(savedStateHandle[ARTICLE_ID_ARGUMENT]),
        StandardCharsets.UTF_8.name(),
    )
    private var hasTrackedView = false

    private val _uiState = MutableStateFlow(ArticleDetailUiState())
    val uiState: StateFlow<ArticleDetailUiState> = _uiState.asStateFlow()

    init {
        observeArticle()
    }

    fun toggleFavorite(article: Article) {
        val nextFavoriteState = !article.isFavorite
        viewModelScope.launch {
            val result = newsRepository.setFavorite(article.id, nextFavoriteState)
            result.onSuccess {
                analyticsTracker.logEvent(
                    if (nextFavoriteState) {
                        AnalyticsEvent.ArticleFavorited(
                            articleId = article.id,
                            sourceName = article.source.name,
                        )
                    } else {
                        AnalyticsEvent.ArticleUnfavorited(
                            articleId = article.id,
                            sourceName = article.source.name,
                        )
                    },
                )
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(errorMessage = throwable.toFeedFailure().userMessage())
                }
            }
        }
    }

    fun trackOpenOriginalArticle(article: Article) {
        analyticsTracker.logEvent(
            AnalyticsEvent.ArticleLinkOpened(
                articleId = article.id,
                url = article.originalUrl,
            ),
        )
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun observeArticle() {
        viewModelScope.launch {
            newsRepository.observeArticle(articleId).collect { article ->
                _uiState.update {
                    it.copy(
                        article = article,
                        isLoading = false,
                        errorMessage = if (article == null) {
                            "Article is no longer available."
                        } else {
                            it.errorMessage
                        },
                    )
                }

                if (article != null && !hasTrackedView) {
                    hasTrackedView = true
                    analyticsTracker.logEvent(
                        AnalyticsEvent.ArticleViewed(
                            articleId = article.id,
                            sourceName = article.source.name,
                            publishedAt = article.publishedAt?.toString(),
                        ),
                    )
                }
            }
        }
    }

    companion object {
        const val ARTICLE_ID_ARGUMENT = "articleId"
    }
}
