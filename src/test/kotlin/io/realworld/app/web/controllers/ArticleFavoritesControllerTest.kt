package io.realworld.app.web.controllers

import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.web.rules.AppRule
import io.realworld.app.web.util.HttpUtil
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ArticleFavoritesControllerTest {
    @Rule
    @JvmField
    val appRule = AppRule()

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun createArticleAsNewUser(
        title: String = "How to train your dragon",
        email: String = "author@test.com",
        username: String = "author_test"
    ): ArticleDTO {
        val http = HttpUtil(appRule.port)
        http.createUser(email, username)
        val article = Article(
            title = title,
            description = "A description",
            body = "The body.",
            tagList = listOf("test")
        )
        return http.post<ArticleDTO>("/articles", ArticleDTO(article)).body
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    fun `create article persists and appears in global feed`() {
        val created = createArticleAsNewUser(
            title = "Feed Visibility Article",
            email = "feed_vis@test.com",
            username = "feed_vis_user"
        )

        assertNotNull(created.article)
        assertNotNull(created.article?.slug)
        assertEquals("Feed Visibility Article", created.article?.title)

        // A separate unauthenticated client can retrieve it from the global list
        val reader = HttpUtil(appRule.port)
        val listResp = reader.get<ArticlesDTO>("/articles")
        assertEquals(HttpStatus.SC_OK, listResp.status)
        val slugs = listResp.body.articles.map { it.slug }
        assertTrue(slugs.contains(created.article?.slug))
    }

    // ── favorite ──────────────────────────────────────────────────────────────

    @Test
    fun `favorite increments favoritesCount and sets favorited true`() {
        val slug = createArticleAsNewUser(
            title = "Fav Count Article",
            email = "fav_count_author@test.com",
            username = "fav_count_author"
        ).article?.slug!!

        // A different user favorites the article
        val fan = HttpUtil(appRule.port)
        fan.createUser("fav_count_fan@test.com", "fav_count_fan")

        val favResp = fan.post<ArticleDTO>("/articles/$slug/favorite")
        assertEquals(HttpStatus.SC_OK, favResp.status)
        assertNotNull(favResp.body.article)
        assertTrue(favResp.body.article!!.favorited)
        assertEquals(1L, favResp.body.article!!.favoritesCount)
    }

    // ── unfavorite ────────────────────────────────────────────────────────────

    @Test
    fun `unfavorite decrements favoritesCount and clears favorited flag`() {
        val slug = createArticleAsNewUser(
            title = "Unfav Article",
            email = "unfav_author@test.com",
            username = "unfav_author"
        ).article?.slug!!

        val fan = HttpUtil(appRule.port)
        fan.createUser("unfav_fan@test.com", "unfav_fan")

        // Favorite first
        fan.post<ArticleDTO>("/articles/$slug/favorite")

        // Then unfavorite
        val unfavResp = fan.deleteWithResponseBody<ArticleDTO>("/articles/$slug/favorite")
        assertEquals(HttpStatus.SC_OK, unfavResp.status)
        assertNotNull(unfavResp.body.article)
        assertFalse(unfavResp.body.article!!.favorited)
        assertEquals(0L, unfavResp.body.article!!.favoritesCount)
    }

    // ── no double-count ───────────────────────────────────────────────────────

    @Test
    fun `repeated favorite by same user does not double-count`() {
        val slug = createArticleAsNewUser(
            title = "No Double Count Article",
            email = "no_double_author@test.com",
            username = "no_double_author"
        ).article?.slug!!

        val fan = HttpUtil(appRule.port)
        fan.createUser("no_double_fan@test.com", "no_double_fan")

        // Favorite twice
        fan.post<ArticleDTO>("/articles/$slug/favorite")
        val secondFavResp = fan.post<ArticleDTO>("/articles/$slug/favorite")

        assertEquals(HttpStatus.SC_OK, secondFavResp.status)
        assertEquals(1L, secondFavResp.body.article!!.favoritesCount)
    }

    // ── 404 on unknown slug ───────────────────────────────────────────────────

    @Test
    fun `favorite on non-existent slug returns 404`() {
        appRule.http.createUser("notfound_fav@test.com", "notfound_fav_user")
        val resp = appRule.http.post<ArticleDTO>("/articles/this-slug-does-not-exist/favorite")
        assertEquals(HttpStatus.SC_NOT_FOUND, resp.status)
    }

    @Test
    fun `unfavorite on non-existent slug returns 404`() {
        appRule.http.createUser("notfound_unfav@test.com", "notfound_unfav_user")
        val resp = appRule.http.deleteWithResponseBody<ArticleDTO>("/articles/this-slug-does-not-exist/favorite")
        assertEquals(HttpStatus.SC_NOT_FOUND, resp.status)
    }
}
