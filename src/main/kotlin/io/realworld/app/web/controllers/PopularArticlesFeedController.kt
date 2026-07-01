package io.realworld.app.web.controllers

import io.ktor.application.ApplicationCall
import io.ktor.auth.principal
import io.ktor.response.respond
import io.realworld.app.domain.User
import io.realworld.app.domain.service.ArticleService

class PopularArticlesFeedController(private val articleService: ArticleService) {
    suspend fun getPopular(ctx: ApplicationCall) {
        val limitRaw = ctx.parameters["limit"]
        val limit = limitRaw?.toIntOrNull()?.takeIf { it > 0 } ?: 20
        val offset = ctx.parameters["offset"]?.toIntOrNull()?.takeIf { it >= 0 } ?: 0
        ctx.respond(articleService.getPopular(limit, offset))
    }
}
