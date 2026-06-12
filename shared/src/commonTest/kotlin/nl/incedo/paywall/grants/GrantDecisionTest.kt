package nl.incedo.paywall.grants

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.GrantId
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.VisitorId

class GrantDecisionTest {

    private val subject = SubjectId.of(VisitorId("v-1"))
    private val article = ArticleId("a-1")
    private val now = 1_750_000_000_000L

    private fun issued(id: String, articleId: ArticleId = article, expiresAt: Long? = null) = GrantIssued(
        grantId = GrantId(id),
        subjectId = subject,
        articleId = articleId,
        grantedBy = "day_pass",
        expiresAtEpochMs = expiresAt,
    )

    @Test
    fun liveGrantGivesAccess() {
        val decision = GrantDecision(article)
        decision.apply(issued("g-1"))
        assertTrue(decision.hasLiveGrant(now))
    }

    @Test
    fun expiredPassGivesNoAccess() {
        // PW-08: day/week passes carry TTL = pass duration
        val decision = GrantDecision(article)
        decision.apply(issued("g-1", expiresAt = now))
        assertFalse(decision.hasLiveGrant(now))
    }

    @Test
    fun revokedGrantGivesNoAccess() {
        val decision = GrantDecision(article)
        decision.apply(issued("g-1"))
        decision.apply(GrantRevoked(GrantId("g-1"), subject, article))
        assertFalse(decision.hasLiveGrant(now))
    }

    @Test
    fun grantForAnotherArticleIsIgnored() {
        // FGA grants are article-scoped
        val decision = GrantDecision(article)
        decision.apply(issued("g-1", articleId = ArticleId("a-other")))
        assertFalse(decision.hasLiveGrant(now))
    }
}
