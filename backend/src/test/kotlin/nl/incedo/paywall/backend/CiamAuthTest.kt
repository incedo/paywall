package nl.incedo.paywall.backend

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.SigningKeyNotFoundException
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import nl.incedo.paywall.accounts.IdentityLinked
import nl.incedo.paywall.backend.auth.CiamJwtValidator
import nl.incedo.paywall.core.PlanId
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.SubscriptionId
import nl.incedo.paywall.core.UserId
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.entitlements.EntitlementGranted
import nl.incedo.paywall.experiments.VariantAssigner
import nl.incedo.paywall.metering.MeterPeriod

/**
 * CIAM JWT integration (TS-04, AC-07/08, NFR-20..24): a local JWKS endpoint
 * stands in for Ory Hydra — same RS256/JWKS mechanics, real signatures.
 */
class CiamAuthTest {

    private val now = 1_750_000_000_000L
    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val rogueKeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val kid = "test-key-1"
    private val issuer = "http://ciam.test/"

    /** In-memory JWKS standing in for Hydra's endpoint: same key material and lookup contract. */
    private val validator: CiamJwtValidator = run {
        val pub = keyPair.public as RSAPublicKey
        val b64 = Base64.getUrlEncoder().withoutPadding()
        val jwkValues = mapOf(
            "kty" to "RSA",
            "kid" to kid,
            "use" to "sig",
            "alg" to "RS256",
            "n" to b64.encodeToString(pub.modulus.toByteArray().let { if (it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it }),
            "e" to b64.encodeToString(pub.publicExponent.toByteArray()),
        )
        val provider = JwkProvider { keyId ->
            if (keyId == kid) Jwk.fromValues(jwkValues) else throw SigningKeyNotFoundException(keyId, null)
        }
        CiamJwtValidator(provider, issuer)
    }

    private fun token(
        sub: String = "ciam-user-1",
        signWith: RSAPrivateKey = keyPair.private as RSAPrivateKey,
        tokenIssuer: String = issuer,
        expiresAt: Date = Date(System.currentTimeMillis() + 60_000),
    ): String = JWT.create()
        .withKeyId(kid)
        .withIssuer(tokenIssuer)
        .withSubject(sub)
        .withExpiresAt(expiresAt)
        .sign(Algorithm.RSA256(null, signWith))

    private fun apiTest(block: suspend (io.ktor.client.HttpClient, InMemoryEventStore) -> Unit) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store, validator) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client, store)
    }

    private fun visitorIn(variant: String): String =
        (0 until 10_000).asSequence()
            .map { "ciam-visitor-$it" }
            .first { VariantAssigner.assign(VisitorId(it), defaultExperiment).name == variant }

    /** EX-03: find a user ID whose userId-keyed assignment is the given variant. */
    private fun userIn(variant: String): String =
        (0 until 10_000).asSequence()
            .map { "ciam-user-$it" }
            .first { VariantAssigner.assign(VisitorId(it), defaultExperiment).name == variant }

    private suspend fun decide(
        client: io.ktor.client.HttpClient,
        visitor: String,
        article: String = "a-1",
        bearer: String? = null,
    ): DecideResponse {
        val response = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            bearer?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            setBody(DecideRequest(visitorId = visitor, articleId = article, tier = "premium"))
        }
        assertEquals(HttpStatusCode.OK, response.status, "AC-07: token problems must never become errors")
        return response.body()
    }

    @Test
    fun validTokenResolvesEntitlementsOfTheUser() = apiTest { client, store ->
        // AC-08: entitlements come from the local store, keyed by the token sub
        store.append(
            listOf(
                EntitlementGranted(
                    subjectId = SubjectId.of(UserId("ciam-user-1")),
                    planId = PlanId("pro"),
                    subscriptionRef = SubscriptionId("sub-1"),
                    validUntilEpochMs = now + 86_400_000,
                ),
            ),
            condition = null,
        )
        val body = decide(client, visitorIn("hard"), bearer = token(sub = "ciam-user-1"))
        assertEquals("full", body.access)
        assertEquals("entitled", body.reason)
    }

    @Test
    fun missingTokenIsAnonymous() = apiTest { client, _ ->
        val body = decide(client, visitorIn("hard"))
        assertEquals("gate", body.access)
    }

    @Test
    fun entitlementLifecycleViaIntegrationEndpoint() = apiTest { client, _ ->
        val visitor = visitorIn("hard")
        val bearer = token(sub = "ciam-user-2")

        // The external subscription administration publishes a grant (AC-02)
        val grant = client.post("/api/v1/integration/entitlements") {
            contentType(ContentType.Application.Json)
            setBody(
                EntitlementChangeRequest(
                    subjectId = "user:ciam-user-2",
                    subscriptionRef = "sub-77",
                    planId = "pro",
                    validUntilEpochMs = now + 86_400_000,
                ),
            )
        }
        assertEquals(HttpStatusCode.Accepted, grant.status)
        assertEquals("full", decide(client, visitor, bearer = bearer).access)

        // Cancellation arrives: access ends while the token is still valid
        // (AC-08: entitlement claims never outlive the local store)
        client.post("/api/v1/integration/entitlements") {
            contentType(ContentType.Application.Json)
            setBody(EntitlementChangeRequest(subjectId = "user:ciam-user-2", subscriptionRef = "sub-77", active = false))
        }
        assertEquals("gate", decide(client, visitor, article = "a-2", bearer = bearer).access)
    }

    @Test
    fun forgedSignatureDegradesToAnonymous() = apiTest { client, store ->
        store.append(
            listOf(
                EntitlementGranted(
                    SubjectId.of(UserId("ciam-user-1")), PlanId("pro"),
                    SubscriptionId("sub-1"), validUntilEpochMs = now + 86_400_000,
                ),
            ),
            condition = null,
        )
        // Signed with a rogue key: must not unlock the entitlement (NFR-20)
        val forged = token(sub = "ciam-user-1", signWith = rogueKeyPair.private as RSAPrivateKey)
        val body = decide(client, visitorIn("hard"), bearer = forged)
        assertEquals("gate", body.access)
    }

    @Test
    fun expiredTokenDegradesToAnonymous() = apiTest { client, _ ->
        // NFR-20: 60 s leeway tolerated, so expire by > 60 s to guarantee rejection.
        val expired = token(expiresAt = Date(System.currentTimeMillis() - 65_000))
        val body = decide(client, visitorIn("hard"), bearer = expired)
        assertEquals("gate", body.access)
    }

    @Test
    fun wrongIssuerDegradesToAnonymous() = apiTest { client, _ ->
        val foreign = token(tokenIssuer = "http://evil.test/")
        val body = decide(client, visitorIn("hard"), bearer = foreign)
        assertEquals("gate", body.access)
    }

    @Test
    fun garbageTokenDegradesToAnonymous() = apiTest { client, _ ->
        val body = decide(client, visitorIn("hard"), bearer = "not-a-jwt-at-all")
        assertEquals("gate", body.access)
    }

    // ── NFR-20: audience claim validation ────────────────────────────────────

    @Test
    fun tokenWithCorrectAudienceAccepted() {
        // NFR-20: when a CIAM_AUDIENCE is configured, tokens with that audience are valid.
        val pub = keyPair.public as RSAPublicKey
        val b64 = Base64.getUrlEncoder().withoutPadding()
        val jwkValues = mapOf(
            "kty" to "RSA", "kid" to kid, "use" to "sig", "alg" to "RS256",
            "n" to b64.encodeToString(pub.modulus.toByteArray().let { if (it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it }),
            "e" to b64.encodeToString(pub.publicExponent.toByteArray()),
        )
        val provider = com.auth0.jwk.JwkProvider { keyId ->
            if (keyId == kid) com.auth0.jwk.Jwk.fromValues(jwkValues) else throw com.auth0.jwk.SigningKeyNotFoundException(keyId, null)
        }
        val audValidator = nl.incedo.paywall.backend.auth.CiamJwtValidator(provider, issuer, audience = "paywall")
        val tokenWithAud = JWT.create()
            .withKeyId(kid).withIssuer(issuer).withSubject("u1")
            .withAudience("paywall")
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 60_000))
            .sign(Algorithm.RSA256(null, keyPair.private as java.security.interfaces.RSAPrivateKey))
        val userId = audValidator.userIdFrom("Bearer $tokenWithAud")
        assertEquals(nl.incedo.paywall.core.UserId("u1"), userId,
            "NFR-20: token with matching audience must be accepted")
    }

    @Test
    fun tokenWithWrongAudienceRejected() {
        // NFR-20: token claiming wrong audience must be rejected even if signature is valid.
        val pub = keyPair.public as RSAPublicKey
        val b64 = Base64.getUrlEncoder().withoutPadding()
        val jwkValues = mapOf(
            "kty" to "RSA", "kid" to kid, "use" to "sig", "alg" to "RS256",
            "n" to b64.encodeToString(pub.modulus.toByteArray().let { if (it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it }),
            "e" to b64.encodeToString(pub.publicExponent.toByteArray()),
        )
        val provider = com.auth0.jwk.JwkProvider { keyId ->
            if (keyId == kid) com.auth0.jwk.Jwk.fromValues(jwkValues) else throw com.auth0.jwk.SigningKeyNotFoundException(keyId, null)
        }
        val audValidator = nl.incedo.paywall.backend.auth.CiamJwtValidator(provider, issuer, audience = "paywall")
        val tokenWrongAud = JWT.create()
            .withKeyId(kid).withIssuer(issuer).withSubject("u1")
            .withAudience("other-app") // wrong audience
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 60_000))
            .sign(Algorithm.RSA256(null, keyPair.private as java.security.interfaces.RSAPrivateKey))
        val userId = audValidator.userIdFrom("Bearer $tokenWrongAud")
        assertEquals(null, userId, "NFR-20: token with wrong audience must be rejected")
    }

    @Test
    fun tokenWithNoAudienceRejectedWhenAudienceConfigured() {
        // NFR-20: when CIAM_AUDIENCE is set, tokens without aud claim are rejected.
        val pub = keyPair.public as RSAPublicKey
        val b64 = Base64.getUrlEncoder().withoutPadding()
        val jwkValues = mapOf(
            "kty" to "RSA", "kid" to kid, "use" to "sig", "alg" to "RS256",
            "n" to b64.encodeToString(pub.modulus.toByteArray().let { if (it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it }),
            "e" to b64.encodeToString(pub.publicExponent.toByteArray()),
        )
        val provider = com.auth0.jwk.JwkProvider { keyId ->
            if (keyId == kid) com.auth0.jwk.Jwk.fromValues(jwkValues) else throw com.auth0.jwk.SigningKeyNotFoundException(keyId, null)
        }
        val audValidator = nl.incedo.paywall.backend.auth.CiamJwtValidator(provider, issuer, audience = "paywall")
        val tokenNoAud = JWT.create()
            .withKeyId(kid).withIssuer(issuer).withSubject("u1")
            // no withAudience() call
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 60_000))
            .sign(Algorithm.RSA256(null, keyPair.private as java.security.interfaces.RSAPrivateKey))
        val userId = audValidator.userIdFrom("Bearer $tokenNoAud")
        assertEquals(null, userId, "NFR-20: token without aud must be rejected when audience is configured")
    }

    @Test
    fun loginLinksVisitorToUserOnceAndMergesTheMeter() = apiTest { client, store ->
        // EX-03: both the visitor and the user must be in the metered variant —
        // the authenticated request uses the userId as assignment key.
        val visitor = visitorIn("metered")
        val userId = userIn("metered")
        // Anonymous: three counted reads on this device
        for (i in 1..3) decide(client, visitor, article = "a-$i")

        // First authenticated request: US-04 auto-link, meter continues (MT-03)
        val authed = decide(client, visitor, article = "a-4", bearer = token(sub = userId))
        assertEquals("full", authed.access)
        assertEquals(4, authed.meterUsed, "anonymous meter state merged into the user")

        // Second authenticated request: no duplicate link event
        decide(client, visitor, article = "a-5", bearer = token(sub = userId))
        val links = store.query(EventQuery(setOf("subject:user:$userId"))).events
            .filterIsInstance<IdentityLinked>()
        assertEquals(1, links.size, "US-04 link is appended exactly once")
        assertEquals("login", links.single().cause)
    }
}
