package com.huongstienstra.newsfeed.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FeedKeyTest {
    @Test
    fun defaultKeyRepresentsUsTopHeadlines() {
        val key = FeedKey()

        assertEquals("us", key.country)
        assertEquals(null, key.query)
        assertEquals("top-headlines:country=us", key.value)
    }

    @Test
    fun queryKeyIsTrimmedAndLowercased() {
        val key = FeedKey(country = "US", query = "  Android  ")

        assertEquals("top-headlines:country=us:q=android", key.value)
    }

    @Test
    fun blankQueryIsIgnored() {
        val key = FeedKey(query = "   ")

        assertEquals("top-headlines:country=us", key.value)
    }
}
