package com.huongstienstra.newsfeed.ui.feed

import app.cash.turbine.test
import com.huongstienstra.newsfeed.analytics.AnalyticsEvent
import com.huongstienstra.newsfeed.domain.model.FeedKey
import com.huongstienstra.newsfeed.domain.model.FeedPage
import com.huongstienstra.newsfeed.testutil.FakeAnalyticsTracker
import com.huongstienstra.newsfeed.testutil.FakeNewsRepository
import com.huongstienstra.newsfeed.testutil.MainDispatcherRule
import com.huongstienstra.newsfeed.testutil.testArticle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeNewsRepository
    private lateinit var analyticsTracker: FakeAnalyticsTracker

    @Before
    fun setUp() {
        repository = FakeNewsRepository()
        analyticsTracker = FakeAnalyticsTracker()
    }

    @Test
    fun `initial load emits articles and stops loading`() = runTest {
        // Given
        val article = testArticle(id = "article-1")
        repository.nextLoadResult = Result.success(
            FeedPage(
                articles = listOf(article),
                page = 1,
                totalResults = 1,
                fromCache = false,
            ),
        )

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(listOf(article), state.articles)
            assertFalse(state.isInitialLoading)
            assertTrue(state.endReached)
            assertEquals(FeedKey(), repository.loadCalls.single().feedKey)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search waits for debounce and logs privacy safe analytics`() = runTest {
        // Given
        val searchKey = FeedKey(query = "android")
        repository.nextLoadResult = Result.success(
            FeedPage(
                articles = listOf(testArticle(id = "search-result")),
                page = 1,
                totalResults = 12,
                fromCache = false,
            ),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        repository.loadCalls.clear()

        // When
        viewModel.onSearchQueryChanged(" android ")
        advanceTimeBy(599)

        // Then
        assertTrue(repository.loadCalls.isEmpty())

        // When
        advanceTimeBy(1)
        advanceUntilIdle()

        // Then
        assertEquals(searchKey, repository.loadCalls.single().feedKey)
        assertEquals(
            AnalyticsEvent.SearchPerformed(queryLength = 7),
            analyticsTracker.events.single(),
        )
        assertFalse(analyticsTracker.events.single().params.containsKey("query"))
    }

    @Test
    fun `submitting existing search does not duplicate search analytics`() = runTest {
        // Given
        repository.nextLoadResult = Result.success(
            FeedPage(
                articles = listOf(testArticle(id = "search-result")),
                page = 1,
                totalResults = 12,
                fromCache = false,
            ),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSearchQueryChanged("trump")
        advanceTimeBy(600)
        advanceUntilIdle()
        analyticsTracker.events.clear()

        // When
        viewModel.submitSearch()
        advanceUntilIdle()

        // Then
        assertTrue(analyticsTracker.events.isEmpty())
    }

    @Test
    fun `clearing search allows same query to be tracked again`() = runTest {
        // Given
        repository.nextLoadResult = Result.success(
            FeedPage(
                articles = listOf(testArticle(id = "search-result")),
                page = 1,
                totalResults = 12,
                fromCache = false,
            ),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSearchQueryChanged("trump")
        advanceTimeBy(600)
        advanceUntilIdle()
        analyticsTracker.events.clear()

        // When
        viewModel.onSearchQueryChanged("")
        advanceTimeBy(600)
        advanceUntilIdle()
        viewModel.onSearchQueryChanged("trump")
        advanceTimeBy(600)
        advanceUntilIdle()

        // Then
        assertEquals(
            AnalyticsEvent.SearchPerformed(queryLength = 5),
            analyticsTracker.events.single(),
        )
    }

    @Test
    fun `manual refresh forces page one reload and logs refresh analytics`() = runTest {
        // Given
        repository.nextLoadResult = Result.success(
            FeedPage(
                articles = listOf(testArticle()),
                page = 1,
                totalResults = 3,
                fromCache = false,
            ),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        repository.loadCalls.clear()

        // When
        viewModel.refresh()
        advanceUntilIdle()

        // Then
        val call = repository.loadCalls.single()
        assertEquals(1, call.page)
        assertTrue(call.forceRefresh)
        assertEquals(
            AnalyticsEvent.FeedRefreshed(
                trigger = "manual",
                queryLength = null,
                resultsCount = 3,
            ),
            analyticsTracker.events.single(),
        )
    }

    @Test
    fun `initial load failure stops loading and shows error`() = runTest {
        // Given
        repository.nextLoadResult = Result.failure(IllegalStateException("boom"))

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isInitialLoading)
        assertFalse(state.isRefreshing)
        assertEquals("Something went wrong. Please try again.", state.errorMessage)
    }

    @Test
    fun `manual refresh failure keeps articles and clears refreshing`() = runTest {
        // Given
        val article = testArticle(id = "article-1")
        repository.nextLoadResult = Result.success(
            FeedPage(
                articles = listOf(article),
                page = 1,
                totalResults = 3,
                fromCache = false,
            ),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        repository.nextLoadResult = Result.failure(IllegalStateException("boom"))
        viewModel.refresh()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(listOf(article), state.articles)
        assertFalse(state.isRefreshing)
        assertFalse(state.isInitialLoading)
        assertEquals("Something went wrong. Please try again.", state.errorMessage)
    }

    @Test
    fun `load more requests next page`() = runTest {
        // Given
        repository.nextLoadResult = Result.success(
            FeedPage(
                articles = listOf(testArticle(id = "article-1")),
                page = 1,
                totalResults = 3,
                fromCache = false,
            ),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        repository.loadCalls.clear()

        // When
        repository.nextLoadResult = Result.success(
            FeedPage(
                articles = listOf(
                    testArticle(id = "article-1"),
                    testArticle(id = "article-2"),
                ),
                page = 2,
                totalResults = 3,
                fromCache = false,
            ),
        )
        viewModel.loadMore()
        advanceUntilIdle()

        // Then
        val call = repository.loadCalls.single()
        assertEquals(2, call.page)
        assertFalse(call.forceRefresh)
        assertFalse(viewModel.uiState.value.isLoadingMore)
        assertEquals(
            AnalyticsEvent.LoadMoreTriggered(
                page = 2,
                queryLength = null,
                currentItemCount = 1,
            ),
            analyticsTracker.events.single(),
        )
    }

    @Test
    fun `load more failure sets load more error`() = runTest {
        // Given
        repository.nextLoadResult = Result.success(
            FeedPage(
                articles = listOf(testArticle(id = "article-1")),
                page = 1,
                totalResults = 3,
                fromCache = false,
            ),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        repository.loadCalls.clear()

        // When
        repository.nextLoadResult = Result.failure(IllegalStateException("boom"))
        viewModel.loadMore()
        advanceUntilIdle()

        // Then
        assertEquals("Something went wrong. Please try again.", viewModel.uiState.value.loadMoreErrorMessage)
    }

    @Test
    fun `load more is ignored when end is reached`() = runTest {
        // Given
        repository.nextLoadResult = Result.success(
            FeedPage(
                articles = listOf(testArticle(id = "article-1")),
                page = 1,
                totalResults = 1,
                fromCache = false,
            ),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        repository.loadCalls.clear()

        // When
        viewModel.loadMore()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.endReached)
        assertTrue(repository.loadCalls.isEmpty())
        assertTrue(analyticsTracker.events.isEmpty())
    }

    @Test
    fun `load more cancellation keeps current state without error`() = runTest {
        // Given
        repository.nextLoadResult = Result.success(
            FeedPage(
                articles = listOf(testArticle(id = "article-1")),
                page = 1,
                totalResults = 3,
                fromCache = false,
            ),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        repository.nextLoadResult = Result.failure(CancellationException("cancelled"))
        viewModel.loadMore()
        advanceUntilIdle()

        // Then
        assertEquals(null, viewModel.uiState.value.loadMoreErrorMessage)
    }

    @Test
    fun `clearing search ignores cancelled reload failure`() = runTest {
        // Given
        repository.nextLoadResult = Result.success(
            FeedPage(
                articles = listOf(testArticle(id = "default-article")),
                page = 1,
                totalResults = 1,
                fromCache = false,
            ),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.nextLoadResult = Result.success(
            FeedPage(
                articles = listOf(testArticle(id = "search-article")),
                page = 1,
                totalResults = 1,
                fromCache = false,
            ),
        )
        viewModel.onSearchQueryChanged("trump")
        advanceTimeBy(600)
        advanceUntilIdle()

        // When
        repository.nextLoadResult = Result.failure(CancellationException("search cleared"))
        viewModel.onSearchQueryChanged("")
        advanceTimeBy(600)
        advanceUntilIdle()

        // Then
        assertEquals(null, viewModel.uiState.value.errorMessage)
        assertEquals(null, viewModel.uiState.value.loadMoreErrorMessage)
    }

    @Test
    fun `clear error removes top level and load more errors`() = runTest {
        // Given
        repository.nextLoadResult = Result.failure(IllegalStateException("boom"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.nextLoadResult = Result.success(
            FeedPage(
                articles = listOf(testArticle(id = "article-1")),
                page = 1,
                totalResults = 3,
                fromCache = false,
            ),
        )
        viewModel.refresh()
        advanceUntilIdle()
        repository.nextLoadResult = Result.failure(IllegalStateException("load more failed"))
        viewModel.loadMore()
        advanceUntilIdle()

        // When
        viewModel.clearError()

        // Then
        assertEquals(null, viewModel.uiState.value.errorMessage)
        assertEquals(null, viewModel.uiState.value.loadMoreErrorMessage)
    }

    @Test
    fun `favorite toggle saves next favorite state and logs event`() = runTest {
        // Given
        val article = testArticle(id = "article-1", sourceName = "Source", isFavorite = false)
        repository.nextLoadResult = Result.success(
            FeedPage(
                articles = listOf(article),
                page = 1,
                totalResults = 1,
                fromCache = false,
            ),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleFavorite(article)
        advanceUntilIdle()

        // Then
        assertEquals("article-1", repository.favoriteCalls.single().articleId)
        assertTrue(repository.favoriteCalls.single().isFavorite)
        assertEquals(
            AnalyticsEvent.ArticleFavorited(articleId = "article-1", sourceName = "Source"),
            analyticsTracker.events.single(),
        )
    }

    @Test
    fun `favorite toggle logs unfavorite when already favorite`() = runTest {
        // Given
        val article = testArticle(id = "article-1", sourceName = "Source", isFavorite = true)
        repository.nextLoadResult = Result.success(
            FeedPage(
                articles = listOf(article),
                page = 1,
                totalResults = 1,
                fromCache = false,
            ),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleFavorite(article)
        advanceUntilIdle()

        // Then
        assertEquals("article-1", repository.favoriteCalls.single().articleId)
        assertFalse(repository.favoriteCalls.single().isFavorite)
        assertEquals(
            AnalyticsEvent.ArticleUnfavorited(articleId = "article-1", sourceName = "Source"),
            analyticsTracker.events.single(),
        )
    }

    @Test
    fun `favorite failure shows error`() = runTest {
        // Given
        val article = testArticle(id = "article-1", isFavorite = false)
        repository.nextLoadResult = Result.success(
            FeedPage(
                articles = listOf(article),
                page = 1,
                totalResults = 1,
                fromCache = false,
            ),
        )
        repository.nextFavoriteResult = Result.failure(IllegalStateException("favorite failed"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleFavorite(article)
        advanceUntilIdle()

        // Then
        assertEquals("Something went wrong. Please try again.", viewModel.uiState.value.errorMessage)
        assertTrue(analyticsTracker.events.isEmpty())
    }

    private fun createViewModel(): FeedViewModel {
        return FeedViewModel(
            newsRepository = repository,
            analyticsTracker = analyticsTracker,
        )
    }
}
