package io.realworld.app.web.controllers

import io.ktor.application.ApplicationCall
import io.ktor.auth.authentication
import io.ktor.auth.principal
import io.ktor.request.receive
import io.ktor.response.respond
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.domain.User
import io.realworld.app.domain.repository.ArticleRepository

class ArticleController(private val articleRepository: ArticleRepository) {

    fun findBy(ctx: ApplicationCall): ArticlesDTO {
        val tag = ctx.parameters["tag"]
        val author = ctx.parameters["author"]
        val favorited = ctx.parameters["favorited"]
        val limit = ctx.parameters["limit"] ?: "20"
        val offset = ctx.parameters["offset"] ?: "0"
        return ArticlesDTO(listOf(), 0)
    }

    fun feed(ctx: ApplicationCall): ArticlesDTO {
        val limit = ctx.parameters["limit"] ?: "20"
        val offset = ctx.parameters["offset"] ?: "0"
        return ArticlesDTO(listOf(), 0)
    }

    suspend fun popularFeed(ctx: ApplicationCall) {
        val limit = ctx.parameters["limit"]?.toIntOrNull() ?: 20
        val offset = ctx.parameters["offset"]?.toIntOrNull() ?: 0
        val articles = articleRepository.findPopular(limit, offset)
        ctx.respond(ArticlesDTO(articles, articles.size))
    }

    fun get(ctx: ApplicationCall): ArticleDTO {
        ctx.parameters["slug"]
        return ArticleDTO(null)
    }

    suspend fun create(ctx: ApplicationCall) {
        val email = ctx.authentication.principal<User>()?.email ?: return
        val article = ctx.receive<ArticleDTO>().article ?: return
        val created = articleRepository.create(email, article)
        ctx.respond(ArticleDTO(created))
    }

    suspend fun update(ctx: ApplicationCall): ArticleDTO {
        val slug = ctx.parameters["slug"]
        ctx.receive<ArticleDTO>()
        return ArticleDTO(null)
    }

    fun delete(ctx: ApplicationCall) {
        ctx.parameters["slug"]
    }

    suspend fun favorite(ctx: ApplicationCall) {
        val email = ctx.authentication.principal<User>()?.email ?: return
        val slug = ctx.parameters["slug"] ?: return
        val article = articleRepository.favorite(email, slug)
        ctx.respond(ArticleDTO(article))
    }

    suspend fun unfavorite(ctx: ApplicationCall) {
        val email = ctx.authentication.principal<User>()?.email ?: return
        val slug = ctx.parameters["slug"] ?: return
        val article = articleRepository.unfavorite(email, slug)
        ctx.respond(ArticleDTO(article))
    }
}

