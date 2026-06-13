package nl.incedo.paywall.backend.auth

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.InvalidClaimException
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.SignatureVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import java.net.URL
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit
import nl.incedo.paywall.core.UserId

/**
 * Validates CIAM-issued JWTs against Hydra's JWKS endpoint (TS-04, NFR-20..24).
 *
 * - NFR-20: checks signature (RSA256), iss, aud (when configured), exp/nbf with
 *   60 s clock-skew leeway, and rejects none/HS* algorithms implicitly by requiring RSA.
 * - AC-07: logged-in identity is established exclusively from a valid token;
 *   a missing, malformed, expired or unverifiable token yields `null` —
 *   the request degrades to anonymous, never to an error.
 * - AC-08: the token answers WHO; entitlements are resolved from the local
 *   store. Entitlement claims inside the token are ignored here by design.
 * - NFR-23: JWKS keys are cached, so tokens validatable from cache keep
 *   working while the CIAM is unreachable.
 */
class CiamJwtValidator(
    private val jwkProvider: JwkProvider,
    private val issuer: String,
    /** NFR-20: expected audience value; null = audience check skipped (dev/test mode). */
    private val audience: String? = null,
) {
    companion object {
        /**
         * Production wiring: JWKS fetched from the CIAM endpoint, keys cached for 24 h
         * with automatic refresh on unknown kid so key rotation needs no deploy (NFR-21/NFR-23).
         */
        fun fromJwksUrl(jwksUrl: String, issuer: String, audience: String? = null) = CiamJwtValidator(
            jwkProvider = JwkProviderBuilder(URL(jwksUrl))
                .cached(10, 24, TimeUnit.HOURS)   // NFR-21: cache ≥ 1 h; unknown kid triggers fresh fetch
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build(),
            issuer = issuer,
            audience = audience,
        )
    }

    /** @return the CIAM subject as [UserId], or null for anything not provably authentic. */
    fun userIdFrom(authorizationHeader: String?): UserId? =
        verify(authorizationHeader)?.subject?.takeIf { it.isNotBlank() }?.let(::UserId)

    /**
     * Staff principal for console/admin access (ADM-05): subject, role and
     * step-up level. Role comes from the `role` claim (or first of `roles`);
     * AAL2 is asserted by `acr == "aal2"` or an `amr` factor (NFR-22/AA-01).
     * Returns null for any token that is not provably authentic (AC-07).
     */
    fun staffFrom(authorizationHeader: String?): StaffPrincipal? {
        val jwt = verify(authorizationHeader) ?: return null
        val sub = jwt.subject?.takeIf { it.isNotBlank() } ?: return null
        val roleClaim = jwt.getClaim("role").asString()
            ?: jwt.getClaim("roles").asList(String::class.java)?.firstOrNull()
        val role = StaffRole.fromClaim(roleClaim) ?: return null
        val acr = jwt.getClaim("acr").asString()
        val amr = jwt.getClaim("amr").asList(String::class.java).orEmpty()
        val aal2 = acr.equals("aal2", ignoreCase = true) ||
            amr.any { it.lowercase() in setOf("mfa", "otp", "totp", "webauthn") }
        return StaffPrincipal(userId = UserId(sub), role = role, aal2 = aal2)
    }

    private fun verify(authorizationHeader: String?) = verifyWithReason(authorizationHeader).first

    /**
     * NFR-24: validates the token and classifies the failure reason so callers
     * can log structured diagnostics without re-parsing.
     * Returns (jwt, null) on success; (null, reason) on failure.
     * Reason values: "missing", "missing_kid", "unknown_key", "expired",
     * "bad_signature", "wrong_claim", "malformed", "invalid".
     */
    fun verifyWithReason(authorizationHeader: String?): Pair<com.auth0.jwt.interfaces.DecodedJWT?, String?> {
        val token = authorizationHeader
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null to "missing"
        return try {
            val keyId = JWT.decode(token).keyId ?: return null to "missing_kid"
            val publicKey = jwkProvider.get(keyId).publicKey as? RSAPublicKey ?: return null to "unknown_key"
            // NFR-20: validate signature, iss, aud (when configured), exp/nbf with 60 s clock-skew leeway.
            val verifier = JWT.require(Algorithm.RSA256(publicKey, null))
                .withIssuer(issuer)
                .acceptLeeway(60) // NFR-20: ≤ 60 s clock skew tolerated
                .let { if (audience != null) it.withAudience(audience) else it }
                .build()
            verifier.verify(token) to null
        } catch (_: TokenExpiredException) {
            null to "expired"
        } catch (_: SignatureVerificationException) {
            null to "bad_signature"
        } catch (_: InvalidClaimException) {
            null to "wrong_claim"
        } catch (_: JWTDecodeException) {
            null to "malformed"
        } catch (_: Exception) {
            null to "invalid"
        }
    }
}

/** Console roles (ADM-05), ordered by privilege. */
enum class StaffRole {
    VIEWER, OPERATOR, ADMIN;

    fun satisfies(minimum: StaffRole): Boolean = ordinal >= minimum.ordinal

    companion object {
        fun fromClaim(value: String?): StaffRole? = when (value?.lowercase()) {
            "viewer" -> VIEWER
            "operator" -> OPERATOR
            "admin" -> ADMIN
            else -> null
        }
    }
}

data class StaffPrincipal(
    val userId: UserId,
    val role: StaffRole,
    /** AA-01: step-up authentication level reached (TOTP/WebAuthn/email code). */
    val aal2: Boolean,
)
