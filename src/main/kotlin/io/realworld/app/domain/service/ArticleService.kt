package io.realworld.app.domain.service

import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.domain.repository.ArticleRepository

class ArticleService(private val articleRepository: ArticleRepository) {
    fun create(title: String, description: String?, body: String, authorId: Long): Article {
        return articleRepository.create(title, description, body, authorId)
    }

    fun favorite(slug: String, userId: Long): Article? {
        return articleRepository.favorite(slug, userId)
    }

    fun getPopular(limit: Int, offset: Int): ArticlesDTO {
        val articles = articleRepository.getPopular(limit, offset)
        return ArticlesDTO(articles, articles.size)
    }
}
