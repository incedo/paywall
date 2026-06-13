package nl.incedo.paywall.backend.content

import nl.incedo.paywall.access.ContentTier
import nl.incedo.paywall.core.ArticleId

/**
 * Content adapter: articles are CMS-owned; the paywall only needs id, tier
 * (PW-01) and body to serve full content or a server-generated teaser
 * (AC-05). Replaced by the CMS integration in production.
 */
data class StoredArticle(
    val id: ArticleId,
    val title: String,
    val tier: ContentTier,
    val body: String,
)

class ArticleRepository(articles: List<StoredArticle> = emptyList()) {

    private val byId = articles.associateBy { it.id }

    fun find(id: ArticleId): StoredArticle? = byId[id]

    companion object {
        /**
         * PW-02/AC-05: the teaser is cut server-side (~150 words, configurable)
         * so the cut-off point cannot be manipulated by the client.
         */
        fun teaserOf(article: StoredArticle, teaserWords: Int = 150): String {
            val words = article.body.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size <= teaserWords) return article.body
            return words.take(teaserWords).joinToString(" ") + " …"
        }
    }
}
