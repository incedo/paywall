package nl.incedo.paywall.backend.auth

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.response.respond

/**
 * Origin trust (INF-02) and staff authorization (ADM-05/AA-01) for the
 * backend routes.
 *
 * - [OriginTrust] enforces the shared secret the Cloudflare Worker presents.
 *   In production the edge strips client identity/channel headers and re-sets
 *   them, so the origin can trust its enriched context. With no secret
 *   configured (local dev / tests) the gate is open.
 * - [requireStaff] enforces a minimum role and optional AAL2 step-up. With no
 *   CIAM configured (dev), it yields a synthetic admin so local work and the
 *   existing API tests keep running; once a validator is present the JWT is
 *   authoritative.
 */
class OriginTrust(private val sharedSecret: String?) {
    /** @return true when the request may proceed; otherwise responds 401 and returns false. */
    suspend fun verify(call: ApplicationCall): Boolean {
        val expected = sharedSecret ?: return true // dev: no edge in front of the origin
        if (call.request.header(ORIGIN_SECRET_HEADER) == expected) return true
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "origin requests must come through the edge (INF-02)"))
        return false
    }

    companion object {
        const val ORIGIN_SECRET_HEADER = "X-Origin-Secret"
    }
}

/**
 * Enforce staff access. Returns the [StaffPrincipal] when allowed; otherwise
 * responds (401/403) and returns null — callers `return` on null.
 */
suspend fun ApplicationCall.requireStaff(
    validator: CiamJwtValidator?,
    minimum: StaffRole,
    needAal2: Boolean = false,
): StaffPrincipal? {
    if (validator == null) {
        // Dev mode: no CIAM wired. Treat as a fully-authenticated admin so
        // local runs and existing tests work; production always wires a validator.
        return StaffPrincipal(nl.incedo.paywall.core.UserId("dev-admin"), StaffRole.ADMIN, aal2 = true)
    }
    val principal = validator.staffFrom(request.header(HttpHeaders.Authorization))
    if (principal == null) {
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "staff authentication required (ADM-05)"))
        return null
    }
    if (!principal.role.satisfies(minimum)) {
        respond(HttpStatusCode.Forbidden, mapOf("error" to "requires ${minimum.name.lowercase()} role or higher"))
        return null
    }
    if (needAal2 && !principal.aal2) {
        respond(HttpStatusCode.Forbidden, mapOf("error" to "step-up authentication required (AA-01)"))
        return null
    }
    return principal
}
