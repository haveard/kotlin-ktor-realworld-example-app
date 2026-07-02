package io.realworld.app.web.controllers

import com.mashape.unirest.http.Unirest
import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.web.rules.AppRule
import io.realworld.app.web.util.HttpUtil
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PopularArticlesFeedControllerTest {
    @Rule
    @JvmField
    val appRule = AppRule()

    @Test
    fun `popular feed returns articles sorted by favoritesCount descending`() {
        // Create author and two articles
        appRule.http.createArticleAs(
            Article(title = "Less Popular Article", description = "desc", body = "body"),
            "author@popular_feed.com", "author_popular"
        )
        val morePopularSlug = appRule.http.createArticleAs(
            Article(title = "More Popular Article", description = "desc", body = "body"),
            "author2@popular_feed.com", "author_popular2"
        ).body.article?.slug!!

        // Favorite "More Popular Article" as two different users
        appRule.http.registerAndLogin("fan1@popular_feed.com", "fan1_popular")
        appRule.http.favoriteArticle(morePopularSlug)

        appRule.http.registerAndLogin("fan2@popular_feed.com", "fan2_popular")
        appRule.http.favoriteArticle(morePopularSlug)

        // GET /articles/feed/popular as authenticated user
        val response = appRule.http.get<ArticlesDTO>("/articles/feed/popular")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertTrue(response.body.articlesCount > 0)
        assertEquals(response.body.articles.size, response.body.articlesCount)

        // Most favorited article should come first
        val first = response.body.articles.first()
        assertEquals(morePopularSlug, first.slug)
        assertTrue(first.favoritesCount >= 2)

        // Verify descending order
        val counts = response.body.articles.map { it.favoritesCount }
        assertEquals(counts, counts.sortedDescending())
    }

    @Test
    fun `popular feed returns 401 when no auth token provided`() {
        val unauthHttp = HttpUtil(appRule.port)
        // Use asString() to avoid deserializing the 401 error body as ArticlesDTO
        val response = Unirest.get(unauthHttp.origin + "/articles/feed/popular")
            .headers(unauthHttp.headers)
            .asString()

        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.status)
    }

    @Test
    fun `popular feed returns empty list when no articles exist`() {
        appRule.http.createUser("empty_feed@popular_feed.com", "empty_feed_user")

        val response = appRule.http.get<ArticlesDTO>("/articles/feed/popular")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertTrue(response.body.articles.isEmpty())
        assertEquals(0, response.body.articlesCount)
    }
}
