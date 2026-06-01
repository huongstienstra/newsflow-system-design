package com.huongstienstra.newsfeed.ui.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.huongstienstra.newsfeed.analytics.AnalyticsEvent
import com.huongstienstra.newsfeed.domain.model.FeedKey
import com.huongstienstra.newsfeed.testutil.FakeAnalyticsTracker
import com.huongstienstra.newsfeed.testutil.FakeNewsRepository
import com.huongstienstra.newsfeed.testutil.MainDispatcherRule
import com.huongstienstra.newsfeed.testutil.testArticle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleDetailViewModelTest {
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
    fun `observing article emits article and logs viewed once`() = runTest {
        // Given
        val article = testArticle(id = "article-1", sourceName = "Source")
        repository.emit(FeedKey(), listOf(article))

        // When
        val viewModel = createViewModel("article-1")
        advanceUntilIdle()
        repository.emit(FeedKey(), listOf(article.copy(title = "Updated title")))
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Updated title", state.article?.title)
            assertEquals(false, state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(
            listOf(
                AnalyticsEvent.ArticleViewed(
                    articleId = "article-1",
                    sourceName = "Source",
                    publishedAt = article.publishedAt?.toString(),
                ),
            ),
            analyticsTracker.events,
        )
    }

    @Test
    fun `missing article emits unavailable error`() = runTest {
        // When
        val viewModel = createViewModel("missing-id")
        advanceUntilIdle()

        // Then
        assertEquals(null, viewModel.uiState.value.article)
        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals("Article is no longer available.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `favorite toggle logs unfavorite when article is already favorite`() = runTest {
        // Given
        val article = testArticle(id = "article-1", sourceName = "Source", isFavorite = true)
        repository.emit(FeedKey(), listOf(article))
        val viewModel = createViewModel("article-1")
        advanceUntilIdle()
        analyticsTracker.events.clear()

        // When
        viewModel.toggleFavorite(article)
        advanceUntilIdle()

        // Then
        assertEquals("article-1", repository.favoriteCalls.single().articleId)
        assertEquals(false, repository.favoriteCalls.single().isFavorite)
        assertEquals(
            AnalyticsEvent.ArticleUnfavorited(articleId = "article-1", sourceName = "Source"),
            analyticsTracker.events.single(),
        )
    }

    @Test
    fun `favorite toggle logs favorite when article is not favorite`() = runTest {
        // Given
        val article = testArticle(id = "article-1", sourceName = "Source", isFavorite = false)
        repository.emit(FeedKey(), listOf(article))
        val viewModel = createViewModel("article-1")
        advanceUntilIdle()
        analyticsTracker.events.clear()

        // When
        viewModel.toggleFavorite(article)
        advanceUntilIdle()

        // Then
        assertEquals("article-1", repository.favoriteCalls.single().articleId)
        assertEquals(true, repository.favoriteCalls.single().isFavorite)
        assertEquals(
            AnalyticsEvent.ArticleFavorited(articleId = "article-1", sourceName = "Source"),
            analyticsTracker.events.single(),
        )
    }

    @Test
    fun `favorite failure emits error`() = runTest {
        // Given
        val article = testArticle(id = "article-1")
        repository.nextFavoriteResult = Result.failure(IllegalStateException("boom"))
        repository.emit(FeedKey(), listOf(article))
        val viewModel = createViewModel("article-1")
        advanceUntilIdle()

        // When
        viewModel.toggleFavorite(article)
        advanceUntilIdle()

        // Then
        assertEquals("Something went wrong. Please try again.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `clear error removes detail error`() = runTest {
        // Given
        val article = testArticle(id = "article-1")
        repository.nextFavoriteResult = Result.failure(IllegalStateException("boom"))
        repository.emit(FeedKey(), listOf(article))
        val viewModel = createViewModel("article-1")
        advanceUntilIdle()
        viewModel.toggleFavorite(article)
        advanceUntilIdle()

        // When
        viewModel.clearError()

        // Then
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `open original article logs link event`() = runTest {
        // Given
        val article = testArticle(id = "article-1")
        val viewModel = createViewModel("article-1")

        // When
        viewModel.trackOpenOriginalArticle(article)

        // Then
        assertTrue(
            analyticsTracker.events.contains(
                AnalyticsEvent.ArticleLinkOpened(
                    articleId = "article-1",
                    url = "https://example.com/article-1",
                ),
            ),
        )
    }

    @Test
    fun `encoded article id is decoded before observing`() = runTest {
        // Given
        val article = testArticle(id = "https://example.com/article 1")
        repository.emit(FeedKey(), listOf(article))

        // When
        val viewModel = createViewModel("https%3A%2F%2Fexample.com%2Farticle+1")
        advanceUntilIdle()

        // Then
        assertEquals(article, viewModel.uiState.value.article)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    private fun createViewModel(articleId: String): ArticleDetailViewModel {
        return ArticleDetailViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(ArticleDetailViewModel.ARTICLE_ID_ARGUMENT to articleId),
            ),
            newsRepository = repository,
            analyticsTracker = analyticsTracker,
        )
    }
}
