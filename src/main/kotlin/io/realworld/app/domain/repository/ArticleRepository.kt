package io.realworld.app.domain.repository

import io.realworld.app.domain.Article
import io.realworld.app.domain.exceptions.NotFoundException
import org.jetbrains.exposed.dao.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.Date

internal object Articles : LongIdTable() {
    val slug: Column<String> = varchar("slug", 255).uniqueIndex()
    val title: Column<String> = varchar("title", 255)
    val description: Column<String?> = varchar("description", 1000).nullable()
    val body: Column<String> = varchar("body", 4000)
    val createdAt: Column<Long> = long("created_at")
    val updatedAt: Column<Long> = long("updated_at")
    val author: Column<Long> = long("author")

    fun toDomain(
        row: ResultRow,
        tagList: List<String>,
        favorited: Boolean,
        favoritesCount: Long,
        articleAuthor: io.realworld.app.domain.User?
    ): Article = Article(
        slug = row[slug],
        title = row[title],
        description = row[description],
        body = row[body],
        tagList = tagList,
        createdAt = Date(row[createdAt]),
        updatedAt = Date(row[updatedAt]),
        favorited = favorited,
        favoritesCount = favoritesCount,
        author = articleAuthor
    )
}

internal object ArticleTags : Table() {
    val article: Column<Long> = long("article")
    val tag: Column<Long> = long("tag")
}

internal object ArticleFavorites : Table() {
    val article: Column<Long> = long("article")
    val user: Column<Long> = long("user")
}

class ArticleRepository {
    init {
        transaction {
            SchemaUtils.create(Tags)
            SchemaUtils.create(Articles)
            SchemaUtils.create(ArticleTags)
            SchemaUtils.create(ArticleFavorites)
        }
    }

    fun create(article: Article, authorId: Long): Article {
        val now = System.currentTimeMillis()
        val slug = slugify(article.title ?: "")
        val id = transaction {
            Articles.insertAndGetId { row ->
                row[Articles.slug] = slug
                row[Articles.title] = article.title ?: ""
                row[Articles.description] = article.description
                row[Articles.body] = article.body
                row[Articles.createdAt] = now
                row[Articles.updatedAt] = now
                row[Articles.author] = authorId
            }.value
        }
        transaction {
            article.tagList.forEach { tagName ->
                val tagId = Tags.select { Tags.name eq tagName }.firstOrNull()?.get(Tags.id)?.value
                    ?: Tags.insertAndGetId { it[Tags.name] = tagName }.value
                ArticleTags.insert { row ->
                    row[ArticleTags.article] = id
                    row[ArticleTags.tag] = tagId
                }
            }
        }
        return findBySlug(slug, authorId) ?: throw NotFoundException("Article not found after creation")
    }

    fun findBySlug(slug: String, requestingUserId: Long? = null): Article? {
        return transaction {
            Articles.select { Articles.slug eq slug }.firstOrNull()?.let { row ->
                buildArticle(row, requestingUserId)
            }
        }
    }

    fun findAll(
        tag: String? = null,
        authorId: Long? = null,
        favoritedByUserId: Long? = null,
        requestingUserId: Long? = null,
        limit: Int = 20,
        offset: Int = 0
    ): List<Article> {
        return transaction {
            val tagArticleIds: Set<Long>? = if (tag != null) {
                val tagId = Tags.select { Tags.name eq tag }.firstOrNull()?.get(Tags.id)?.value
                    ?: return@transaction emptyList()
                ArticleTags.select { ArticleTags.tag eq tagId }
                    .map { it[ArticleTags.article] }.toSet()
            } else null

            val favArticleIds: Set<Long>? = if (favoritedByUserId != null) {
                ArticleFavorites.select { ArticleFavorites.user eq favoritedByUserId }
                    .map { it[ArticleFavorites.article] }.toSet()
            } else null

            val combinedIds: Set<Long>? = when {
                tagArticleIds != null && favArticleIds != null -> tagArticleIds.intersect(favArticleIds)
                tagArticleIds != null -> tagArticleIds
                favArticleIds != null -> favArticleIds
                else -> null
            }

            val query = when {
                combinedIds != null && authorId != null ->
                    Articles.select {
                        (Articles.id inList combinedIds.toList()) and (Articles.author eq authorId)
                    }
                combinedIds != null ->
                    Articles.select { Articles.id inList combinedIds.toList() }
                authorId != null ->
                    Articles.select { Articles.author eq authorId }
                else ->
                    Articles.selectAll()
            }

            query.orderBy(Articles.createdAt to false)
                .limit(limit, offset)
                .map { row -> buildArticle(row, requestingUserId) }
                .filterNotNull()
        }
    }

    fun findFeed(
        followedAuthorIds: List<Long>,
        requestingUserId: Long,
        limit: Int = 20,
        offset: Int = 0
    ): List<Article> {
        if (followedAuthorIds.isEmpty()) return emptyList()
        return transaction {
            Articles.select { Articles.author inList followedAuthorIds }
                .orderBy(Articles.createdAt to false)
                .limit(limit, offset)
                .map { row -> buildArticle(row, requestingUserId) }
                .filterNotNull()
        }
    }

    fun update(slug: String, article: Article): Article? {
        val now = System.currentTimeMillis()
        transaction {
            Articles.update({ Articles.slug eq slug }) { row ->
                if (article.title != null) {
                    row[Articles.title] = article.title
                    row[Articles.slug] = slugify(article.title)
                }
                if (article.description != null) {
                    row[Articles.description] = article.description
                }
                row[Articles.body] = article.body
                row[Articles.updatedAt] = now
            }
        }
        val newSlug = if (article.title != null) slugify(article.title) else slug
        return findBySlug(newSlug)
    }

    fun delete(slug: String) {
        transaction {
            val articleId = Articles.select { Articles.slug eq slug }
                .firstOrNull()?.get(Articles.id)?.value ?: return@transaction
            ArticleTags.deleteWhere { ArticleTags.article eq articleId }
            ArticleFavorites.deleteWhere { ArticleFavorites.article eq articleId }
            Articles.deleteWhere { Articles.id eq articleId }
        }
    }

    fun favorite(slug: String, userId: Long): Article {
        val articleId = transaction {
            Articles.select { Articles.slug eq slug }.firstOrNull()?.get(Articles.id)?.value
        } ?: throw NotFoundException("Article not found: $slug")

        val alreadyFavorited = transaction {
            ArticleFavorites.select {
                (ArticleFavorites.article eq articleId) and (ArticleFavorites.user eq userId)
            }.count() > 0
        }
        if (!alreadyFavorited) {
            transaction {
                ArticleFavorites.insert { row ->
                    row[ArticleFavorites.article] = articleId
                    row[ArticleFavorites.user] = userId
                }
            }
        }
        return findBySlug(slug, userId) ?: throw NotFoundException("Article not found: $slug")
    }

    fun unfavorite(slug: String, userId: Long): Article {
        val articleId = transaction {
            Articles.select { Articles.slug eq slug }.firstOrNull()?.get(Articles.id)?.value
        } ?: throw NotFoundException("Article not found: $slug")

        transaction {
            ArticleFavorites.deleteWhere {
                (ArticleFavorites.article eq articleId) and (ArticleFavorites.user eq userId)
            }
        }
        return findBySlug(slug, userId) ?: throw NotFoundException("Article not found: $slug")
    }

    private fun buildArticle(row: ResultRow, requestingUserId: Long?): Article? {
        val articleId = row[Articles.id].value
        val tagList = ArticleTags.select { ArticleTags.article eq articleId }
            .mapNotNull { tagRow ->
                Tags.select { Tags.id eq tagRow[ArticleTags.tag] }
                    .firstOrNull()?.get(Tags.name)
            }
        val favoritesCount = ArticleFavorites.select { ArticleFavorites.article eq articleId }
            .count().toLong()
        val favorited = if (requestingUserId != null) {
            ArticleFavorites.select {
                (ArticleFavorites.article eq articleId) and (ArticleFavorites.user eq requestingUserId)
            }.count() > 0
        } else false
        val articleAuthor = Users.select { Users.id eq row[Articles.author] }
            .firstOrNull()?.let { Users.toDomain(it) }
        return Articles.toDomain(row, tagList, favorited, favoritesCount, articleAuthor)
    }

    companion object {
        fun slugify(title: String): String =
            title.lowercase().trim()
                .replace(Regex("[^a-z0-9\\s-]"), "")
                .replace(Regex("\\s+"), "-")
                .replace(Regex("-+"), "-")
    }
}
