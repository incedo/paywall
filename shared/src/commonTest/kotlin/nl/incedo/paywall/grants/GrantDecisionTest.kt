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

    // default: expires 1 h in the future so every plain issued() is a live grant
    private fun issued(id: String, articleId: ArticleId = article, expiresAt: Long = now + 3_600_000) = GrantIssued(
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
        // PW-08: day/week passes carry TTL = pass duration; FGA-02: fail closed on expired
        val decision = GrantDecision(article)
        decision.apply(issued("g-1", expiresAt = now - 1))
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

    @Test
    fun cachedGrantIsLive() {
        // FGA-05: ofCached uses Long.MAX_VALUE sentinel so cached grants are always live
        // within the 60s cache window; the cache layer enforces its own TTL.
        val d = GrantDecision.ofCached(article, hasGrant = true, grantedBy = "day_pass")
        assertTrue(d.hasLiveGrant(now))
        assertTrue(d.hasLiveGrant(Long.MAX_VALUE - 1))
    }

    @Test
    fun cachedGrantWithNoGrantIsFalse() {
        val d = GrantDecision.ofCached(article, hasGrant = false, grantedBy = null)
        assertFalse(d.hasLiveGrant(now))
    }
}
