package nl.incedo.paywall.backend

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.SigningKeyNotFoundException
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
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
import nl.incedo.paywall.api.GrantChangeRequest
import nl.incedo.paywall.api.MeterResetRequest
import nl.incedo.paywall.backend.auth.CiamJwtValidator
import nl.incedo.paywall.backend.auth.OriginTrust
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/** Origin trust (INF-02) and staff RBAC + step-up (ADM-05/AA-01). */
class SecurityApiTest {

    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val kid = "staff-key"
    private val issuer = "http://ciam.test/"

    private val validator: CiamJwtValidator = run {
        val pub = keyPair.public as RSAPublicKey
        val b64 = Base64.getUrlEncoder().withoutPadding()
        val values = mapOf(
            "kty" to "RSA", "kid" to kid, "use" to "sig", "alg" to "RS256",
            "n" to b64.encodeToString(pub.modulus.toByteArray().let { if (it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it }),
            "e" to b64.encodeToString(pub.publicExponent.toByteArray()),
        )
        val provider = JwkProvider { keyId ->
            if (keyId == kid) Jwk.fromValues(values) else throw SigningKeyNotFoundException(keyId, null)
        }
        CiamJwtValidator(provider, issuer)
    }

    private fun token(role: String?, aal2: Boolean = false): String {
        val builder = JWT.create().withKeyId(kid).withIssuer(issuer).withSubject("staff-1")
            .withExpiresAt(Date(System.currentTimeMillis() + 60_000))
        if (role != null) builder.withClaim("role", role)
        if (aal2) builder.withClaim("acr", "aal2")
        return builder.sign(Algorithm.RSA256(null, keyPair.private as RSAPrivateKey))
    }

    private fun secured(
        originSecret: String? = null,
        block: suspend (io.ktor.client.HttpClient) -> Unit,
    ) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(store, defaultExperiment, { 1_750_000_000_000L }, { MeterPeriod("2026-06") })
        application { module(service, store, validator, originSecret = originSecret) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    // ── INF-02 — origin trust ──────────────────────────────────────────────

    @Test
    fun originSecretIsRequiredWhenConfigured() = secured(originSecret = "edge-secret") { client ->
        // No secret header: rejected before any handler runs
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.post("/api/v1/decide") {
                contentType(ContentType.Application.Json)
                setBody(DecideRequest(visitorId = "v-1", articleId = "a-1", tier = "premium"))
            }.status,
        )
    }

    @Test
    fun correctOriginSecretPassesTheGate() = secured(originSecret = "edge-secret") { client ->
        val status = client.post("/api/v1/decide") {
            header(OriginTrust.ORIGIN_SECRET_HEADER, "edge-secret")
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "v-1", articleId = "a-1", tier = "free"))
        }.status
        assertEquals(HttpStatusCode.OK, status)
    }

    @Test
    fun healthProbeBypassesOriginTrust() = secured(originSecret = "edge-secret") { client ->
        assertEquals(HttpStatusCode.OK, client.get("/health").status)
    }

    // ── ADM-05 — staff RBAC ────────────────────────────────────────────────

    @Test
    fun statsRequireAuthentication() = secured { client ->
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/stats").status)
    }

    @Test
    fun viewerCanReadStatsButNotPublish() = secured { client ->
        val viewer = token("viewer")
        assertEquals(
            HttpStatusCode.OK,
            client.get("/api/v1/stats") { header("Authorization", "Bearer $viewer") }.status,
        )
        // Publishing needs admin (ADM-06)
        assertEquals(
            HttpStatusCode.Forbidden,
            client.post("/api/v1/walls/w-1/publish") { header("Authorization", "Bearer $viewer") }.status,
        )
    }

    @Test
    fun operatorWithoutStepUpCannotResetMeter() = secured { client ->
        val operator = token("operator", aal2 = false)
        val response = client.post("/api/v1/admin/subjects/visitor:v-1/meter-reset") {
            header("Authorization", "Bearer $operator")
            contentType(ContentType.Application.Json)
            setBody(MeterResetRequest(actor = "ignored", reason = "support"))
        }
        assertEquals(HttpStatusCode.Forbidden, response.status) // AA-01 step-up missing
    }

    @Test
    fun operatorWithStepUpCanResetMeter() = secured { client ->
        val operator = token("operator", aal2 = true)
        val response = client.post("/api/v1/admin/subjects/visitor:v-1/meter-reset") {
            header("Authorization", "Bearer $operator")
            contentType(ContentType.Application.Json)
            setBody(MeterResetRequest(actor = "ignored", reason = "support ticket 4711"))
        }
        assertEquals(HttpStatusCode.Accepted, response.status)
    }

    @Test
    fun grantAdministrationRequiresOperatorAndStepUp() = secured { client ->
        // A viewer (no step-up) is refused FGA writes
        val viewer = token("viewer")
        assertEquals(
            HttpStatusCode.Forbidden,
            client.post("/api/v1/grants") {
                header("Authorization", "Bearer $viewer")
                contentType(ContentType.Application.Json)
                setBody(GrantChangeRequest(grantId = "g-1", subjectId = "visitor:v-1", articleId = "a-1"))
            }.status,
        )
        // Operator with step-up succeeds
        val operator = token("operator", aal2 = true)
        assertEquals(
            HttpStatusCode.Accepted,
            client.post("/api/v1/grants") {
                header("Authorization", "Bearer $operator")
                contentType(ContentType.Application.Json)
                setBody(GrantChangeRequest(grantId = "g-1", subjectId = "visitor:v-1", articleId = "a-1"))
            }.status,
        )
    }

    @Test
    fun tokenWithoutRoleClaimIsNotStaff() = secured { client ->
        // A valid reader token (no role) cannot reach the console API
        val noRole = token(role = null)
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.get("/api/v1/walls") { header("Authorization", "Bearer $noRole") }.status,
        )
    }
}
