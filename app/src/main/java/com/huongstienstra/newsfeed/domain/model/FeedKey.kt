package com.huongstienstra.newsfeed.domain.model

data class FeedKey(
    val country: String = "us",
    val query: String? = null,
) {
    val value: String
        get() = buildString {
            append("top-headlines")
            append(":country=")
            append(country.lowercase())
            query?.trim()?.takeIf { it.isNotEmpty() }?.let {
                append(":q=")
                append(it.lowercase())
            }
        }
}
