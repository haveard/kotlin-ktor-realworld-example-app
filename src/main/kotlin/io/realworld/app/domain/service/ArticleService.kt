package io.realworld.app.domain.service

import io.realworld.app.domain.Article
import io.realworld.app.domain.exceptions.NotFoundException
import io.realworld.app.domain.repository.ArticleRepository
import io.realworld.app.domain.repository.UserRepository

class ArticleService(
    private val articleRepository: ArticleRepository,
    private val userRepository: UserRepository
) {
    fun create(email: String, article: Article): Article {
        val author = userRepository.findByEmail(email) ?: throw NotFoundException("User not found: $email")
        return articleRepository.create(article, author.id!!)
    }

    fun findBySlug(slug: String, email: String? = null): Article {
        val requestingUserId = email?.let { userRepository.findByEmail(it)?.id }
        return articleRepository.findBySlug(slug, requestingUserId)
            ?: throw NotFoundException("Article not found: $slug")
    }

    fun findBy(
        tag: String?,
        authorUsername: String?,
        favoritedByUsername: String?,
        limit: Int,
        offset: Int,
        email: String? = null
    ): List<Article> {
        val authorId = authorUsername?.let { userRepository.findByUsername(it)?.id }
        val favoritedByUserId = favoritedByUsername?.let { userRepository.findByUsername(it)?.id }
        val requestingUserId = email?.let { userRepository.findByEmail(it)?.id }
        return articleRepository.findAll(tag, authorId, favoritedByUserId, requestingUserId, limit, offset)
    }

    fun findFeed(email: String, limit: Int, offset: Int): List<Article> {
        val user = userRepository.findByEmail(email) ?: throw NotFoundException("User not found: $email")
        val followedIds = userRepository.findFollowedUserIds(user.id!!)
        return articleRepository.findFeed(followedIds, user.id, limit, offset)
    }

    fun update(email: String, slug: String, article: Article): Article {
        return articleRepository.update(slug, article)
            ?: throw NotFoundException("Article not found: $slug")
    }

    fun delete(email: String, slug: String) {
        articleRepository.delete(slug)
    }

    fun favorite(email: String, slug: String): Article {
        val user = userRepository.findByEmail(email) ?: throw NotFoundException("User not found: $email")
        return articleRepository.favorite(slug, user.id!!)
    }

    fun unfavorite(email: String, slug: String): Article {
        val user = userRepository.findByEmail(email) ?: throw NotFoundException("User not found: $email")
        return articleRepository.unfavorite(slug, user.id!!)
    }
}
