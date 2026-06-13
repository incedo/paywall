package nl.incedo.paywall.backend

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.GrantId
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.core.port.EventStore
import nl.incedo.paywall.grants.GrantIssued
import nl.incedo.paywall.grants.ShareTokenIssued
import nl.incedo.paywall.grants.shareMonthTag


/** BP-05: default monthly token cap per subscriber. */
const val SHARE_TOKEN_MONTHLY_CAP = 5

/**
 * BP-05: subscriber-generated, article-scoped, expiring share tokens that
 * redeem into FGA grants (BP-05 / FGA-03, grantedBy=share_token).
 *
 * Token format: `{base64url(payload)}.{base64url(hmac-sha256)}` where
 * payload = `{tokenId}:{subscriberSubjectId}:{articleId}:{expiresAtMs}`.
 * The server secret prevents forgery; expiry and article scope prevent re-use.
 */
class ShareTokenService(
    private val eventStore: EventStore,
    private val secret: String,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val monthlyCapCount: Int = SHARE_TOKEN_MONTHLY_CAP,
    /** Default token TTL: 7 days (BP-05). */
    private val tokenTtlMs: Long = 7 * 24 * 60 * 60 * 1000L,
) {

    data class IssuedToken(val token: String, val expiresAtEpochMs: Long)

    sealed class IssueResult {
        data class Success(val issued: IssuedToken) : IssueResult()
        data object CapExceeded : IssueResult()
        data object NotEntitled : IssueResult()
    }

    sealed class RedeemResult {
        data class Success(val grantId: String) : RedeemResult()
        data object InvalidToken : RedeemResult()
        data object Expired : RedeemResult()
    }

    /**
     * Issue a signed share token for an authenticated subscriber (BP-05).
     * Enforces the monthly cap before appending the event.
     */
    suspend fun issue(
        subscriberSubjectId: SubjectId,
        articleId: ArticleId,
        monthPeriod: String,
    ): IssueResult {
        val now = clock()
        val capTag = shareMonthTag(subscriberSubjectId, monthPeriod)
        val existing = eventStore.query(EventQuery(setOf(capTag))).events
            .filterIsInstance<ShareTokenIssued>()
        if (existing.size >= monthlyCapCount) return IssueResult.CapExceeded

        val tokenId = java.util.UUID.randomUUID().toString()
        val expiresAt = now + tokenTtlMs
        val token = sign(tokenId, subscriberSubjectId.value, articleId.value, expiresAt)

        val event = ShareTokenIssued(
            tokenId = tokenId,
            subscriberSubjectId = subscriberSubjectId,
            articleId = articleId,
            expiresAtEpochMs = expiresAt,
            issuedAtEpochMs = now,
            monthPeriod = monthPeriod,
        )
        eventStore.append(listOf(event), condition = null)
        return IssueResult.Success(IssuedToken(token, expiresAt))
    }

    /**
     * Redeem a share token: validate HMAC and expiry, then issue a GrantIssued
     * event for the visiting subject so the next decide() grants full access (BP-05).
     */
    suspend fun redeem(
        token: String,
        visitorSubjectId: SubjectId,
    ): RedeemResult {
        val now = clock()
        val payload = verify(token) ?: return RedeemResult.InvalidToken
        if (now > payload.expiresAtEpochMs) return RedeemResult.Expired

        val grantId = GrantId("share-${payload.tokenId}")
        val grant = GrantIssued(
            grantId = grantId,
            subjectId = visitorSubjectId,
            articleId = ArticleId(payload.articleId),
            grantedBy = "share_token",
            reason = "share token ${payload.tokenId}", // FGA-01
            expiresAtEpochMs = payload.expiresAtEpochMs,
        )
        // Idempotent: same grantId for same token — a second redemption by a
        // different visitor gets its own grant; same visitor re-redeeming is a no-op.
        eventStore.append(listOf(grant), condition = null)
        return RedeemResult.Success(grantId.value)
    }

    // ── token format ──────────────────────────────────────────────────────────

    private data class TokenPayload(
        val tokenId: String,
        val subscriberSubjectId: String,
        val articleId: String,
        val expiresAtEpochMs: Long,
    )

    private fun sign(tokenId: String, subscriberId: String, articleId: String, expiresAt: Long): String {
        // Use '|' as field separator — none of the fields (UUID, subject ID, article ID, epoch ms) contain '|'.
        val payloadStr = "$tokenId|$subscriberId|$articleId|$expiresAt"
        val payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadStr.toByteArray())
        val hmac = hmac(payloadStr)
        val sigB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(hmac)
        return "$payloadB64.$sigB64"
    }

    private fun verify(token: String): TokenPayload? {
        val dot = token.lastIndexOf('.')
        if (dot < 0) return null
        val payloadB64 = token.substring(0, dot)
        val sigB64 = token.substring(dot + 1)
        val payloadStr = runCatching {
            String(Base64.getUrlDecoder().decode(payloadB64))
        }.getOrNull() ?: return null
        val expectedSig = hmac(payloadStr)
        val actualSig = runCatching { Base64.getUrlDecoder().decode(sigB64) }.getOrNull() ?: return null
        if (!expectedSig.contentEquals(actualSig)) return null

        val parts = payloadStr.split('|')
        if (parts.size != 4) return null
        val expiresAt = parts[3].toLongOrNull() ?: return null
        return TokenPayload(parts[0], parts[1], parts[2], expiresAt)
    }

    private fun hmac(data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return mac.doFinal(data.toByteArray())
    }
}
