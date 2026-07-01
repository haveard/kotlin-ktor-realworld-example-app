package io.realworld.app.web.controllers

import io.ktor.application.ApplicationCall
import io.ktor.auth.authentication
import io.ktor.request.receive
import io.ktor.response.respond
import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.domain.User
import io.realworld.app.domain.service.ArticleService

class ArticleController(private val articleService: ArticleService) {

    suspend fun findBy(ctx: ApplicationCall) {
        val tag = ctx.request.queryParameters["tag"]
        val author = ctx.request.queryParameters["author"]
        val favorited = ctx.request.queryParameters["favorited"]
        val limit = ctx.request.queryParameters["limit"]?.toIntOrNull() ?: 20
        val offset = ctx.request.queryParameters["offset"]?.toIntOrNull() ?: 0
        val email = ctx.authentication.principal<User>()?.email
        val articles = articleService.findBy(tag, author, favorited, limit, offset, email)
        ctx.respond(ArticlesDTO(articles, articles.size))
    }

    suspend fun feed(ctx: ApplicationCall) {
        val limit = ctx.request.queryParameters["limit"]?.toIntOrNull() ?: 20
        val offset = ctx.request.queryParameters["offset"]?.toIntOrNull() ?: 0
        val email = ctx.authentication.principal<User>()?.email ?: return
        val articles = articleService.findFeed(email, limit, offset)
        ctx.respond(ArticlesDTO(articles, articles.size))
    }

    suspend fun get(ctx: ApplicationCall) {
        val slug = ctx.parameters["slug"] ?: return
        val email = ctx.authentication.principal<User>()?.email
        val article = articleService.findBySlug(slug, email)
        ctx.respond(ArticleDTO(article))
    }

    suspend fun create(ctx: ApplicationCall) {
        val email = ctx.authentication.principal<User>()?.email ?: return
        val articleDTO = ctx.receive<ArticleDTO>()
        val article = articleDTO.article ?: return
        val created = articleService.create(email, article)
        ctx.respond(ArticleDTO(created))
    }

    suspend fun update(ctx: ApplicationCall) {
        val slug = ctx.parameters["slug"] ?: return
        val email = ctx.authentication.principal<User>()?.email ?: return
        val articleDTO = ctx.receive<ArticleDTO>()
        val article = articleDTO.article ?: return
        val updated = articleService.update(email, slug, article)
        ctx.respond(ArticleDTO(updated))
    }

    suspend fun delete(ctx: ApplicationCall) {
        val slug = ctx.parameters["slug"] ?: return
        val email = ctx.authentication.principal<User>()?.email ?: return
        articleService.delete(email, slug)
        ctx.respond(mapOf<String, String>())
    }

    suspend fun favorite(ctx: ApplicationCall) {
        val slug = ctx.parameters["slug"] ?: return
        val email = ctx.authentication.principal<User>()?.email ?: return
        val article = articleService.favorite(email, slug)
        ctx.respond(ArticleDTO(article))
    }

    suspend fun unfavorite(ctx: ApplicationCall) {
        val slug = ctx.parameters["slug"] ?: return
        val email = ctx.authentication.principal<User>()?.email ?: return
        val article = articleService.unfavorite(email, slug)
        ctx.respond(ArticleDTO(article))
    }
}

