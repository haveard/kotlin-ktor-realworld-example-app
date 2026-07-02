package io.realworld.app.web.controllers

import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.web.rules.AppRule
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ArticleFavoritesControllerTest {
    @Rule
    @JvmField
    val appRule = AppRule()

    @Test
    fun `create article with blank title returns 422`() {
        appRule.http.createUser()
        val article = Article(
            title = "",
            description = "Some description",
            body = "Some body"
        )
        val response = appRule.http.post<String>("/articles", ArticleDTO(article))
        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.status)
    }
}
