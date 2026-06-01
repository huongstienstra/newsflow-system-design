package com.huongstienstra.newsfeed.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huongstienstra.newsfeed.analytics.AnalyticsEvent
import com.huongstienstra.newsfeed.analytics.AnalyticsTracker
import com.huongstienstra.newsfeed.domain.model.Article
import com.huongstienstra.newsfeed.domain.model.FeedKey
import com.huongstienstra.newsfeed.domain.model.toFeedFailure
import com.huongstienstra.newsfeed.domain.model.userMessage
import com.huongstienstra.newsfeed.domain.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    // default feed:    headlines?country=us
    // feed by keyword: headlines?country=us?q="trump"
    private val activeFeedKey = MutableStateFlow(FeedKey())
    private val queryChanges = MutableStateFlow("")

    private val _uiState = MutableStateFlow(FeedUiState(isInitialLoading = true))
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private var currentPage = FIRST_PAGE
    private var totalResults = 0
    private var loadJob: Job? = null
    private var lastTrackedSearchQuery: String? = null

    init {
        observeArticles()
        observeSearch()
        loadFirstPage(forceRefresh = false)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                errorMessage = null,
                loadMoreErrorMessage = null,
            )
        }
        queryChanges.value = query
    }

    fun submitSearch() {
        val normalizedQuery = _uiState.value.searchQuery.trim().takeIf { it.isNotEmpty() }
        val nextFeedKey = FeedKey(query = normalizedQuery)
        if (normalizedQuery == null) {
            lastTrackedSearchQuery = null
        } else {
            trackSearchPerformed(normalizedQuery)
        }
        if (activeFeedKey.value != nextFeedKey) {
            activeFeedKey.value = nextFeedKey
            loadFirstPage(forceRefresh = true)
        }
    }

    fun refresh() {
        loadFirstPage(forceRefresh = true, isManualRefresh = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isInitialLoading || state.isRefreshing || state.isLoadingMore || state.endReached) {
            return
        }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, loadMoreErrorMessage = null) }
            val nextPage = currentPage + 1
            analyticsTracker.logEvent(
                AnalyticsEvent.LoadMoreTriggered(
                    page = nextPage,
                    queryLength = activeFeedKey.value.query?.length,
                    currentItemCount = state.articles.size,
                ),
            )
            val result = newsRepository.loadTopHeadlines(
                feedKey = activeFeedKey.value,
                page = nextPage,
                pageSize = PAGE_SIZE,
                forceRefresh = false,
            )

            result.fold(
                onSuccess = { page ->
                    currentPage = nextPage
                    totalResults = page.totalResults
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            fromCache = page.fromCache,
                            endReached = isEndReached(page.articles, page.totalResults),
                        )
                    }
                },
                onFailure = { throwable ->
                    if (throwable is CancellationException) return@fold
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            loadMoreErrorMessage = throwable.toFeedFailure().userMessage(),
                        )
                    }
                },
            )
        }
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

    fun clearError() {
        _uiState.update {
            it.copy(errorMessage = null, loadMoreErrorMessage = null)
        }
    }

    private fun observeArticles() {
        viewModelScope.launch {
            activeFeedKey
                .flatMapLatest(newsRepository::observeFeed)
                .collect { articles ->
                    _uiState.update {
                        it.copy(
                            articles = articles,
                            endReached = isEndReached(articles, totalResults),
                        )
                    }
                }
        }
    }

    private fun observeSearch() {
        viewModelScope.launch {
            queryChanges
                .debounce(SEARCH_DEBOUNCE_MILLIS)
                .distinctUntilChanged()
                .collect { submitSearch() }
        }
    }

    private fun loadFirstPage(
        forceRefresh: Boolean,
        isManualRefresh: Boolean = false,
    ) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            currentPage = FIRST_PAGE
            totalResults = 0
            _uiState.update {
                it.copy(
                    isInitialLoading = it.articles.isEmpty() && !isManualRefresh,
                    isRefreshing = isManualRefresh,
                    isLoadingMore = false,
                    endReached = false,
                    errorMessage = null,
                    loadMoreErrorMessage = null,
                )
            }

            val feedKey = activeFeedKey.value
            val result = newsRepository.loadTopHeadlines(
                feedKey = feedKey,
                page = FIRST_PAGE,
                pageSize = PAGE_SIZE,
                forceRefresh = forceRefresh,
            )

            result.fold(
                onSuccess = { page ->
                    totalResults = page.totalResults
                    _uiState.update {
                        it.copy(
                            isInitialLoading = false,
                            isRefreshing = false,
                            fromCache = page.fromCache,
                            endReached = isEndReached(page.articles, page.totalResults),
                        )
                    }
                    trackSuccessfulFirstPageLoad(
                        feedKey = feedKey,
                        resultsCount = page.totalResults,
                        isManualRefresh = isManualRefresh,
                    )
                },
                onFailure = { throwable ->
                    if (throwable is CancellationException) return@fold
                    _uiState.update {
                        it.copy(
                            isInitialLoading = false,
                            isRefreshing = false,
                            errorMessage = throwable.toFeedFailure().userMessage(),
                        )
                    }
                },
            )
        }
    }

    private fun trackSuccessfulFirstPageLoad(
        feedKey: FeedKey,
        resultsCount: Int,
        isManualRefresh: Boolean,
    ) {
        if (isManualRefresh) {
            analyticsTracker.logEvent(
                AnalyticsEvent.FeedRefreshed(
                    trigger = "manual",
                    queryLength = feedKey.query?.length,
                    resultsCount = resultsCount,
                ),
            )
            return
        }

        feedKey.query ?: return
    }

    private fun trackSearchPerformed(query: String) {
        if (lastTrackedSearchQuery == query) return
        lastTrackedSearchQuery = query
        analyticsTracker.logEvent(
            AnalyticsEvent.SearchPerformed(
                queryLength = query.length,
            ),
        )
    }

    private fun isEndReached(
        articles: List<Article>,
        totalResults: Int,
    ): Boolean {
        return totalResults > 0 && articles.size >= totalResults
    }

    private companion object {
        const val FIRST_PAGE = 1
        const val PAGE_SIZE = 20
        const val SEARCH_DEBOUNCE_MILLIS = 600L
    }
}
