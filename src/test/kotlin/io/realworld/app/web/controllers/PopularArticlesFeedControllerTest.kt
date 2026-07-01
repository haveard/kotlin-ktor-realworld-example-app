package io.realworld.app.web.controllers

import com.mashape.unirest.http.Unirest
import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.web.rules.AppRule
import io.realworld.app.web.util.HttpUtil
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PopularArticlesFeedControllerTest {
    @Rule
    @JvmField
    val appRule = AppRule()

    // (a) Ranking happy path: articles returned sorted by favoritesCount descending
    @Test
    fun `popular feed returns articles sorted by favorites count descending`() {
        // Arrange: create a user and two articles via the prefix-less paths
        appRule.http.createUser("ranker@example.com", "ranker_user")

        val articleLow = Article(
            title = "Low Favorites Article",
            description = "desc",
            body = "body"
        )
        val articleHigh = Article(
            title = "High Favorites Article",
            description = "desc",
            body = "body"
        )

        val respLow = appRule.http.post<ArticleDTO>("/articles", ArticleDTO(articleLow))
        val respHigh = appRule.http.post<ArticleDTO>("/articles", ArticleDTO(articleHigh))

        assertEquals(HttpStatus.SC_OK, respLow.status)
        assertEquals(HttpStatus.SC_OK, respHigh.status)

        val slugLow = respLow.body.article?.slug!!
        val slugHigh = respHigh.body.article?.slug!!

        // Favorite the "high" article twice (once by the creator, once by a second user)
        appRule.http.post<ArticleDTO>("/articles/$slugHigh/favorite")

        val http2 = HttpUtil(appRule.port)
        http2.createUser("ranker2@example.com", "ranker2_user")
        http2.post<ArticleDTO>("/articles/$slugHigh/favorite")

        // Act: call the popular feed with the first user's token
        val response = appRule.http.get<ArticlesDTO>("/articles/feed/popular")

        // Assert
        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body.articles)
        assertTrue("Expected at least 2 articles", response.body.articles.size >= 2)

        val articles = response.body.articles
        val highIdx = articles.indexOfFirst { it.slug == slugHigh }
        val lowIdx = articles.indexOfFirst { it.slug == slugLow }

        assertTrue("High-favorites article must appear before low-favorites article",
            highIdx < lowIdx)
        assertTrue("High-favorites article must have a higher favoritesCount",
            articles[highIdx].favoritesCount > articles[lowIdx].favoritesCount)
    }

    // (b) Missing / invalid token returns 401
    @Test
    fun `popular feed without token returns 401`() {
        // Use a fresh HttpUtil with no Authorization header set
        val unauthHttp = HttpUtil(appRule.port)
        val response = Unirest.get(unauthHttp.origin + "/articles/feed/popular")
            .headers(unauthHttp.headers)
            .asString()

        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.status)
    }

    // (c) Authenticated user with no articles gets 200 with empty list
    @Test
    fun `popular feed with no articles returns 200 with empty list`() {
        appRule.http.createUser("emptyuser@example.com", "empty_user")

        val response = appRule.http.get<ArticlesDTO>("/articles/feed/popular")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body.articles)
        assertTrue("Expected empty articles list", response.body.articles.isEmpty())
        assertEquals(0, response.body.articlesCount)
    }
}
