package nl.incedo.paywall.backend

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.SigningKeyNotFoundException
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import nl.incedo.paywall.backend.auth.CiamJwtValidator
import nl.incedo.paywall.backend.auth.TokenFailureTracker

/**
 * NFR-24: token validation failures are classified by reason and tracked per
 * source IP with a sliding-window abuse threshold.
 */
class TokenFailureMonitorTest {

    // ── CiamJwtValidator.verifyWithReason ────────────────────────────────────

    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val rogueKeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val kid = "nfr24-key"
    private val issuer = "http://ciam.nfr24.test/"

    private val validator: CiamJwtValidator = run {
        val pub = keyPair.public as RSAPublicKey
        val b64 = Base64.getUrlEncoder().withoutPadding()
        val jwkValues = mapOf(
            "kty" to "RSA", "kid" to kid, "use" to "sig", "alg" to "RS256",
            "n" to b64.encodeToString(pub.modulus.toByteArray().let { if (it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it }),
            "e" to b64.encodeToString(pub.publicExponent.toByteArray()),
        )
        val provider = JwkProvider { keyId ->
            if (keyId == kid) Jwk.fromValues(jwkValues) else throw SigningKeyNotFoundException(keyId, null)
        }
        CiamJwtValidator(provider, issuer)
    }

    private fun token(
        sub: String = "nfr24-user",
        signWith: RSAPrivateKey = keyPair.private as RSAPrivateKey,
        tokenIssuer: String = issuer,
        expiresAt: Date = Date(System.currentTimeMillis() + 60_000),
    ) = JWT.create()
        .withKeyId(kid)
        .withIssuer(tokenIssuer)
        .withSubject(sub)
        .withExpiresAt(expiresAt)
        .sign(Algorithm.RSA256(null, signWith))

    @Test
    fun validTokenReturnsNullReason() {
        val (jwt, reason) = validator.verifyWithReason("Bearer ${token()}")
        assertNull(reason, "NFR-24: valid token must have no failure reason")
        assertTrue(jwt != null, "NFR-24: valid token must return the decoded JWT")
    }

    @Test
    fun missingHeaderReturnsMissingReason() {
        val (jwt, reason) = validator.verifyWithReason(null)
        assertEquals("missing", reason, "NFR-24: null header must yield reason=missing")
        assertNull(jwt)
    }

    @Test
    fun noBearerPrefixReturnsMissingReason() {
        val (jwt, reason) = validator.verifyWithReason("Basic dXNlcjpwYXNz")
        assertEquals("missing", reason)
        assertNull(jwt)
    }

    @Test
    fun expiredTokenReturnsExpiredReason() {
        val expired = token(expiresAt = Date(System.currentTimeMillis() - 1_000))
        val (_, reason) = validator.verifyWithReason("Bearer $expired")
        assertEquals("expired", reason, "NFR-24: expired token must yield reason=expired")
    }

    @Test
    fun wrongSignatureReturnsBadSignatureReason() {
        val forged = token(signWith = rogueKeyPair.private as RSAPrivateKey)
        val (_, reason) = validator.verifyWithReason("Bearer $forged")
        assertEquals("bad_signature", reason, "NFR-24: wrong-key token must yield reason=bad_signature")
    }

    @Test
    fun wrongIssuerReturnsWrongClaimReason() {
        val foreign = token(tokenIssuer = "http://evil.test/")
        val (_, reason) = validator.verifyWithReason("Bearer $foreign")
        assertEquals("wrong_claim", reason, "NFR-24: wrong-issuer token must yield reason=wrong_claim")
    }

    @Test
    fun garbageTokenReturnsMalformedReason() {
        val (_, reason) = validator.verifyWithReason("Bearer not-a-jwt-at-all")
        assertEquals("malformed", reason, "NFR-24: unparseable token must yield reason=malformed")
    }

    // ── TokenFailureTracker ───────────────────────────────────────────────────

    @Test
    fun trackerCountsBelowThreshold() {
        val tracker = TokenFailureTracker(threshold = 5, windowMs = 60_000)
        repeat(4) { assertFalse(tracker.record("10.0.0.1"), "NFR-24: below threshold must return false") }
        assertEquals(4, tracker.countFor("10.0.0.1"))
    }

    @Test
    fun trackerReturnsTrueAtThreshold() {
        val tracker = TokenFailureTracker(threshold = 3, windowMs = 60_000)
        repeat(2) { tracker.record("10.0.0.2") }
        assertTrue(tracker.record("10.0.0.2"), "NFR-24: threshold hit must return true (abuse alert)")
    }

    @Test
    fun trackerResetsAfterWindow() {
        var fakeNow = 1_000L
        val tracker = TokenFailureTracker(clock = { fakeNow }, threshold = 5, windowMs = 1_000)
        repeat(4) { tracker.record("10.0.0.3") }
        assertEquals(4, tracker.countFor("10.0.0.3"))
        // advance past the window
        fakeNow = 3_000L
        assertEquals(0, tracker.countFor("10.0.0.3"), "NFR-24: count must reset after window expires")
        // new record starts a fresh window
        assertFalse(tracker.record("10.0.0.3"))
        assertEquals(1, tracker.countFor("10.0.0.3"))
    }

    @Test
    fun trackerTracksIpsIndependently() {
        val tracker = TokenFailureTracker(threshold = 2, windowMs = 60_000)
        tracker.record("10.0.0.10")
        assertTrue(tracker.record("10.0.0.10"), "NFR-24: first IP hits threshold")
        assertFalse(tracker.record("10.0.0.11"), "NFR-24: second IP starts its own window")
    }

    @Test
    fun unknownIpCountIsZero() {
        val tracker = TokenFailureTracker()
        assertEquals(0, tracker.countFor("192.168.0.1"), "NFR-24: unseen IP must have count=0")
    }
}
