package nl.incedo.paywall.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import nl.incedo.paywall.experiments.ExperimentDefinition

/**
 * Console → backend adapter (ADM-01: the console owns no logic; everything
 * it does goes through the API). Same client on every target — web (Wasm),
 * desktop (JVM), mobile later (multiplatform rule).
 */
class ConsoleApi(private val baseUrl: String = "http://localhost:8080") {

    sealed interface SaveOutcome {
        data class Saved(val wall: WallResponse) : SaveOutcome
        data class Conflict(val message: String) : SaveOutcome
        data class Failed(val message: String) : SaveOutcome
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun walls(): List<WallResponse> = client.get("$baseUrl/api/v1/walls").body()

    suspend fun saveWall(id: String, request: SaveWallRequest): SaveOutcome {
        val response = client.post("$baseUrl/api/v1/walls/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return when {
            response.status.isSuccess() -> SaveOutcome.Saved(response.body())
            response.status == HttpStatusCode.Conflict ->
                SaveOutcome.Conflict("Someone else edited this wall — reload before saving")
            else -> SaveOutcome.Failed("Save failed (${response.status.value})")
        }
    }

    suspend fun publish(id: String): SaveOutcome {
        val response = client.post("$baseUrl/api/v1/walls/$id/publish") {
            contentType(ContentType.Application.Json)
        }
        return if (response.status.isSuccess()) SaveOutcome.Saved(response.body())
        else SaveOutcome.Failed("Publish failed (${response.status.value})")
    }

    suspend fun stats(): List<VariantStatsResponse> = client.get("$baseUrl/api/v1/stats").body()

    // ── ADM-04: subject inspector ─────────────────────────────────────────────

    suspend fun inspectSubject(subjectId: String): SubjectInspectorResponse =
        client.get("$baseUrl/api/v1/admin/subjects/$subjectId").body()

    suspend fun subjectGrants(subjectId: String): List<GrantAuditEntry> =
        client.get("$baseUrl/api/v1/admin/subjects/$subjectId/grants").body()

    suspend fun resetMeter(subjectId: String, actor: String, reason: String): Boolean {
        val response = client.post("$baseUrl/api/v1/admin/subjects/$subjectId/meter-reset") {
            contentType(ContentType.Application.Json)
            setBody(MeterResetRequest(actor = actor, reason = reason))
        }
        return response.status.isSuccess()
    }

    // ── ADM-02: experiment config management ──────────────────────────────────

    suspend fun getConfig(): ExperimentConfigResponse =
        client.get("$baseUrl/api/v1/admin/config").body()

    suspend fun publishConfig(experiment: ExperimentDefinition): Boolean {
        val response = client.post("$baseUrl/api/v1/admin/config") {
            contentType(ContentType.Application.Json)
            setBody(PublishExperimentConfigRequest(experiment))
        }
        return response.status.isSuccess()
    }
}
