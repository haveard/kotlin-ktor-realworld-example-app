package io.realworld.app.domain.repository

import io.realworld.app.domain.Article
import org.jetbrains.exposed.dao.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

internal object Articles : LongIdTable() {
    val slug: Column<String> = varchar("slug", 200).uniqueIndex()
    val title: Column<String> = varchar("title", 255)
    val description: Column<String?> = varchar("description", 1000).nullable()
    val body: Column<String> = text("body")
    val authorId: Column<Long> = long("author_id")
}

internal object ArticleFavorites : Table() {
    val articleId: Column<Long> = long("article_id").primaryKey()
    val userId: Column<Long> = long("user_id").primaryKey()
}

class ArticleRepository {
    init {
        transaction {
            SchemaUtils.create(Articles, ArticleFavorites)
        }
    }

    fun create(title: String, description: String?, body: String, authorId: Long): Article {
        val slug = title.toLowerCase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .trim()
            .replace(Regex("\\s+"), "-")
        transaction {
            Articles.insertAndGetId {
                it[Articles.slug] = slug
                it[Articles.title] = title
                it[Articles.description] = description
                it[Articles.body] = body
                it[Articles.authorId] = authorId
            }
        }
        return Article(slug = slug, title = title, description = description, body = body)
    }

    fun favorite(slug: String, userId: Long): Article? {
        return transaction {
            val articleRow = Articles.select { Articles.slug eq slug }.firstOrNull()
                ?: return@transaction null
            val articleId = articleRow[Articles.id].value
            val alreadyFavorited = ArticleFavorites
                .select { (ArticleFavorites.articleId eq articleId) and (ArticleFavorites.userId eq userId) }
                .count() > 0
            if (!alreadyFavorited) {
                ArticleFavorites.insert {
                    it[ArticleFavorites.articleId] = articleId
                    it[ArticleFavorites.userId] = userId
                }
            }
            val favCount = ArticleFavorites
                .select { ArticleFavorites.articleId eq articleId }
                .count()
            Article(
                slug = articleRow[Articles.slug],
                title = articleRow[Articles.title],
                description = articleRow[Articles.description],
                body = articleRow[Articles.body],
                favoritesCount = favCount.toLong(),
                favorited = true
            )
        }
    }

    fun getPopular(limit: Int, offset: Int): List<Article> {
        return transaction {
            Articles.selectAll()
                .map { row ->
                    val articleId = row[Articles.id].value
                    val favCount = ArticleFavorites
                        .select { ArticleFavorites.articleId eq articleId }
                        .count()
                    val author = Users.select { Users.id eq row[Articles.authorId] }
                        .map { Users.toDomain(it) }
                        .firstOrNull()
                    Article(
                        slug = row[Articles.slug],
                        title = row[Articles.title],
                        description = row[Articles.description],
                        body = row[Articles.body],
                        favoritesCount = favCount.toLong(),
                        author = author
                    )
                }
                .sortedByDescending { it.favoritesCount }
                .drop(offset)
                .take(limit)
        }
    }
}
