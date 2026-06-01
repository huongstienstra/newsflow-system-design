package com.huongstienstra.newsfeed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.huongstienstra.newsfeed.ui.navigation.FeedApp
import com.huongstienstra.newsfeed.ui.theme.NewsFeedSystemDesignTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NewsFeedSystemDesignTheme {
                FeedApp()
            }
        }
    }
}
