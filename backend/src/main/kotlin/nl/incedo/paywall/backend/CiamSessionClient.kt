package nl.incedo.paywall.backend

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant

/**
 * ADM-04: read-only CIAM session lookup for the subject inspector.
 * Returns the active Ory Kratos sessions for a given user ID (UUID part of "user:{id}").
 * In the experiment the mock returns an empty list; swap for [KratosCiamSessionClient]
 * in production without touching callers.
 */
interface CiamSessionClient {
    suspend fun activeSessions(userId: String): List<CiamSession>
}

@Serializable
data class CiamSession(
    val id: String,
    val device: String? = null,
    val ipAddress: String? = null,
    val lastActiveAtEpochMs: Long? = null,
)

/** ADM-04: no CIAM configured — always returns empty session list. */
class MockCiamSessionClient : CiamSessionClient {
    override suspend fun activeSessions(userId: String): List<CiamSession> = emptyList()
}

/**
 * ADM-04: reads active sessions from the Ory Kratos admin API.
 * [adminUrl] is the Kratos admin base URL, e.g. `http://kratos:4434`.
 * Sessions endpoint: GET /admin/identities/{id}/sessions
 */
class KratosCiamSessionClient(private val adminUrl: String) : CiamSessionClient {

    private val httpClient = HttpClient.newHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun activeSessions(userId: String): List<CiamSession> = runCatching {
        val url = "${adminUrl.trimEnd('/')}/admin/identities/$userId/sessions"
        val request = HttpRequest.newBuilder(URI.create(url)).GET().build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) return emptyList()
        parseKratosSessions(response.body())
    }.getOrDefault(emptyList())

    private fun parseKratosSessions(body: String): List<CiamSession> {
        val array = json.parseToJsonElement(body) as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element.jsonObject
            val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val devices = obj["devices"]?.let { it as? JsonArray }
            val firstDevice = devices?.firstOrNull()?.jsonObject
            val userAgent = firstDevice?.get("user_agent")?.jsonPrimitive?.content
            val ipAddress = firstDevice?.get("ip_address")?.jsonPrimitive?.content
            val lastActive = obj["authenticated_at"]?.jsonPrimitive?.content
                ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
                ?: obj["expires_at"]?.jsonPrimitive?.longOrNull
            CiamSession(id = id, device = userAgent, ipAddress = ipAddress, lastActiveAtEpochMs = lastActive)
        }
    }
}
