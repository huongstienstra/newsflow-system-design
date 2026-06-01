package com.huongstienstra.newsfeed.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.huongstienstra.newsfeed.ui.detail.ArticleDetailScreen
import com.huongstienstra.newsfeed.ui.detail.ArticleDetailViewModel
import com.huongstienstra.newsfeed.ui.feed.FeedScreen

@Composable
fun FeedApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = FeedRoute.route,
    ) {
        composable(FeedRoute.route) {
            FeedScreen(
                onArticleClick = { articleId ->
                    navController.navigate(DetailRoute.create(articleId))
                },
            )
        }
        composable(
            route = DetailRoute.route,
            arguments = listOf(
                navArgument(ArticleDetailViewModel.ARTICLE_ID_ARGUMENT) {
                    type = NavType.StringType
                },
            ),
        ) {
            ArticleDetailScreen(
                onBackClick = navController::navigateUp,
            )
        }
    }
}

private object FeedRoute {
    const val route = "feed"
}

private object DetailRoute {
    const val route = "detail?articleId={articleId}"

    fun create(articleId: String): String {
        return "detail?articleId=${Uri.encode(articleId)}"
    }
}
