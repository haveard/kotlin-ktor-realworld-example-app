package io.realworld.app.domain.repository

import io.realworld.app.domain.Article
import io.realworld.app.domain.User
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

internal object Articles : LongIdTable("articles") {
    val slug = varchar("slug", 200).uniqueIndex()
    val title = varchar("title", 200)
    val description = varchar("description", 2000).nullable()
    val body = text("body")
    val authorId = long("author_id")
}

internal object ArticleFavorites : Table("article_favorites") {
    val articleId = long("article_id")
    val userId = long("user_id")
    override val primaryKey = PrimaryKey(articleId, userId)
}

class ArticleRepository(private val userRepository: UserRepository) {
    init {
        transaction {
            SchemaUtils.create(Articles, ArticleFavorites)
        }
    }

    private fun generateSlug(title: String): String =
        title.lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .trim('-')

    fun create(email: String, article: Article): Article {
        val user = userRepository.findByEmail(email)
            ?: throw RuntimeException("User not found: $email")
        val slug = generateSlug(article.title ?: "untitled")
        transaction {
            Articles.insert { row ->
                row[Articles.slug] = slug
                row[Articles.title] = article.title ?: ""
                row[Articles.description] = article.description
                row[Articles.body] = article.body
                row[Articles.authorId] = user.id!!
            }
        }
        return findBySlug(slug) ?: throw RuntimeException("Article not found after create: $slug")
    }

    fun findBySlug(slug: String): Article? {
        return transaction {
            Articles.select { Articles.slug eq slug }
                .map { row -> rowToArticle(row) }
                .firstOrNull()
        }
    }

    fun favorite(email: String, slug: String): Article {
        val user = userRepository.findByEmail(email)
            ?: throw RuntimeException("User not found")
        val articleId = transaction {
            Articles.select { Articles.slug eq slug }
                .map { it[Articles.id].value }
                .firstOrNull()
        } ?: throw RuntimeException("Article not found: $slug")
        transaction {
            val alreadyFavorited = ArticleFavorites
                .select { (ArticleFavorites.articleId eq articleId) and (ArticleFavorites.userId eq user.id!!) }
                .count() > 0
            if (!alreadyFavorited) {
                ArticleFavorites.insert { row ->
                    row[ArticleFavorites.articleId] = articleId
                    row[ArticleFavorites.userId] = user.id!!
                }
            }
        }
        return findBySlug(slug)!!
    }

    fun unfavorite(email: String, slug: String): Article {
        val user = userRepository.findByEmail(email)
            ?: throw RuntimeException("User not found")
        val articleId = transaction {
            Articles.select { Articles.slug eq slug }
                .map { it[Articles.id].value }
                .firstOrNull()
        } ?: throw RuntimeException("Article not found: $slug")
        transaction {
            ArticleFavorites.deleteWhere {
                (ArticleFavorites.articleId eq articleId) and (ArticleFavorites.userId eq user.id!!)
            }
        }
        return findBySlug(slug)!!
    }

    fun findPopular(limit: Int = 20, offset: Int = 0): List<Article> {
        return transaction {
            val favCountExpr = ArticleFavorites.articleId.count()
            Articles
                .join(ArticleFavorites, JoinType.LEFT, Articles.id, ArticleFavorites.articleId)
                .slice(
                    Articles.id, Articles.slug, Articles.title,
                    Articles.description, Articles.body, Articles.authorId, favCountExpr
                )
                .selectAll()
                .groupBy(
                    Articles.id, Articles.slug, Articles.title,
                    Articles.description, Articles.body, Articles.authorId
                )
                .orderBy(favCountExpr, SortOrder.DESC)
                .limit(limit, offset.toLong())
                .map { row ->
                    rowToArticle(row, row[favCountExpr])
                }
        }
    }

    private fun rowToArticle(row: org.jetbrains.exposed.sql.ResultRow, favCount: Long? = null): Article {
        val articleId = row[Articles.id].value
        val authorId = row[Articles.authorId]
        val resolvedFavCount = favCount ?: ArticleFavorites
            .select { ArticleFavorites.articleId eq articleId }
            .count()
        val author = Users.select { Users.id eq authorId }
            .map { Users.toDomain(it) }
            .firstOrNull()
        return Article(
            slug = row[Articles.slug],
            title = row[Articles.title],
            description = row[Articles.description],
            body = row[Articles.body],
            favoritesCount = resolvedFavCount,
            author = author
        )
    }
}
