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
    fun userIdFrom(authorizationHeader: String?): UserId? {
        val token = authorizationHeader
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return try {
            val keyId = JWT.decode(token).keyId ?: return null
            val publicKey = jwkProvider.get(keyId).publicKey as? RSAPublicKey ?: return null
            val verified = JWT.require(Algorithm.RSA256(publicKey, null))
                .withIssuer(issuer)
                .build()
                .verify(token)
            verified.subject?.takeIf { it.isNotBlank() }?.let(::UserId)
        } catch (_: Exception) {
            null // AC-07: degrade to anonymous — never an error page
        }
    }
}
