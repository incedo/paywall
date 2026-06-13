package nl.incedo.paywall.backend.auth

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.net.URL
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit
import nl.incedo.paywall.core.UserId

/**
 * Validates CIAM-issued JWTs against Hydra's JWKS endpoint (TS-04, NFR-20..24).
 *
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
) {
    companion object {
        /** Production wiring: JWKS via Hydra's public endpoint, keys cached (NFR-23). */
        fun fromJwksUrl(jwksUrl: String, issuer: String) = CiamJwtValidator(
            jwkProvider = JwkProviderBuilder(URL(jwksUrl))
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build(),
            issuer = issuer,
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

    private fun verify(authorizationHeader: String?): com.auth0.jwt.interfaces.DecodedJWT? {
        val token = authorizationHeader
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return try {
            val keyId = JWT.decode(token).keyId ?: return null
            val publicKey = jwkProvider.get(keyId).publicKey as? RSAPublicKey ?: return null
            JWT.require(Algorithm.RSA256(publicKey, null))
                .withIssuer(issuer)
                .build()
                .verify(token)
        } catch (_: Exception) {
            null // AC-07: degrade to anonymous — never an error page
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
