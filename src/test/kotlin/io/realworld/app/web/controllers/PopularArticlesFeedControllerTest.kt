package io.realworld.app.web.controllers

import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.web.rules.AppRule
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PopularArticlesFeedControllerTest {
    @Rule
    @JvmField
    val appRule = AppRule()

    @Test
    fun `pagination - limit=1 offset=1 returns the second-ranked article and order is stable`() {
        // Set up a user and two articles with different popularity rankings
        appRule.http.createUser("popular_pagination@test.com", "popular_pagination_user")

        val article1 = Article(
            title = "Popular Article First",
            description = "First article",
            body = "This is the first article body."
        )
        val article2 = Article(
            title = "Popular Article Second",
            description = "Second article",
            body = "This is the second article body."
        )

        val created1 = appRule.http.createPopularArticle(article1)
        appRule.http.createPopularArticle(article2)

        // Give article1 a favorite so it becomes the most popular
        val slug1 = created1.body.article?.slug
        assertNotNull(slug1)
        appRule.http.favoriteArticle(slug1!!)

        // First page: should return the most popular article (article1, favoritesCount=1)
        val firstPage = appRule.http.get<ArticlesDTO>("/articles/popular?limit=1&offset=0")
        assertEquals(HttpStatus.SC_OK, firstPage.status)
        assertEquals(1, firstPage.body.articles.size)
        val firstRanked = firstPage.body.articles.first()

        // Second page: offset=1 should return the second-ranked article (article2, favoritesCount=0)
        val secondPage = appRule.http.get<ArticlesDTO>("/articles/popular?limit=1&offset=1")
        assertEquals(HttpStatus.SC_OK, secondPage.status)
        assertEquals(1, secondPage.body.articles.size)
        val secondRanked = secondPage.body.articles.first()

        // The two pages must return different articles
        assertNotEquals(firstRanked.slug, secondRanked.slug)

        // Order stability: calling offset=1 again must return the same article
        val secondPageAgain = appRule.http.get<ArticlesDTO>("/articles/popular?limit=1&offset=1")
        assertEquals(HttpStatus.SC_OK, secondPageAgain.status)
        assertEquals(1, secondPageAgain.body.articles.size)
        assertEquals(secondRanked.slug, secondPageAgain.body.articles.first().slug)
    }

    @Test
    fun `invalid limit falls back to default window of 20`() {
        // Create 25 articles to exceed the default page size
        appRule.http.createUser("popular_invalid_limit@test.com", "popular_invalid_limit_user")
        for (i in 1..25) {
            appRule.http.createPopularArticle(
                Article(
                    title = "Invalid Limit Article $i",
                    description = "Article $i description",
                    body = "Article $i body."
                )
            )
        }

        // ?limit=abc is not a valid integer; the endpoint must fall back to the default limit of 20
        val response = appRule.http.get<ArticlesDTO>("/articles/popular?limit=abc")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body.articles)
        assertTrue(
            "Expected at most 20 articles (default limit), but got ${response.body.articles.size}",
            response.body.articles.size <= 20
        )
    }
}
