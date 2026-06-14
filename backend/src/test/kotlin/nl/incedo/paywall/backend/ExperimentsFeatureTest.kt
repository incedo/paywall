package nl.incedo.paywall.backend

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import nl.incedo.paywall.access.StrategyConfig
import nl.incedo.paywall.core.ExperimentId
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.experiments.ExperimentDefinition
import nl.incedo.paywall.experiments.Variant
import nl.incedo.paywall.experiments.VariantAssigner
import nl.incedo.paywall.metering.MeterPeriod

/**
 * BDD scenarios for `src/test/resources/features/experiments.feature` —
 * mirrors each Gherkin scenario 1:1 (EX-01..05).
 */
class ExperimentsFeatureTest {

    private val now = 1_750_000_000_000L

    private val twoVariantExp = ExperimentDefinition(
        id = ExperimentId("exp-bdd"),
        name = "BDD two-variant experiment",
        variants = listOf(
            Variant("metered", StrategyConfig.Metered(limit = 5), weight = 1),
            Variant("hard", StrategyConfig.Hard, weight = 1),
        ),
    )

    private val controlExp = ExperimentDefinition(
        id = ExperimentId("exp-ctrl"),
        name = "BDD experiment with control",
        variants = listOf(
            Variant("control", StrategyConfig.Metered(limit = Int.MAX_VALUE), weight = 1, isControl = true),
            Variant("hard", StrategyConfig.Hard, weight = 1),
        ),
    )

    private fun scenario(
        exp: ExperimentDefinition = twoVariantExp,
        block: suspend (io.ktor.client.HttpClient) -> Unit,
    ) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = exp,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    private suspend fun decide(
        client: io.ktor.client.HttpClient,
        visitorId: String,
        article: String = "art-1",
        tier: String = "premium",
        forceVariant: String? = null,
    ): DecideResponse {
        val url = if (forceVariant != null) "/api/v1/decide?forceVariant=$forceVariant" else "/api/v1/decide"
        return client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = visitorId, articleId = article, tier = tier))
        }.body()
    }

    @Test
    fun `Scenario - Variant assignment is deterministic`() = scenario { client ->
        // Given a visitor "visitor-A"
        // When the visitor requests access for the first time
        val first = decide(client, "visitor-A")
        // Then a variant is assigned and recorded
        val assignedVariant = first.variant
        // And the same variant is returned on the next request
        val second = decide(client, "visitor-A", article = "art-2")
        assertEquals(assignedVariant, second.variant, "EX-01: variant must be sticky across requests")
    }

    @Test
    fun `Scenario - Variant assignment is sticky across articles`() = scenario { client ->
        // Given a visitor "visitor-B"
        val first = decide(client, "visitor-B", article = "art-1")
        val expectedVariant = first.variant
        // When the visitor accesses three different premium articles
        val second = decide(client, "visitor-B", article = "art-2")
        val third = decide(client, "visitor-B", article = "art-3")
        // Then every response carries the same variant name
        assertEquals(expectedVariant, second.variant, "EX-01: sticky across articles")
        assertEquals(expectedVariant, third.variant, "EX-01: sticky across articles")
    }

    @Test
    fun `Scenario - Force-variant overrides assignment (EX-05 debug)`() = scenario { client ->
        // Given a visitor "visitor-C" that naturally gets one variant
        val natural = decide(client, "visitor-C")
        // Force to the other variant regardless of natural assignment
        val forcedVariant = if (natural.variant == "metered") "hard" else "metered"
        // When the visitor requests access with ?forceVariant query parameter
        val forced = decide(client, "visitor-C", forceVariant = forcedVariant)
        // Then the response variant is the forced variant
        assertEquals(forcedVariant, forced.variant, "EX-05: forced variant must override natural assignment")
        // And the natural assignment is unchanged (force is per-request only)
        val natural2 = decide(client, "visitor-C", article = "art-after-force")
        assertEquals(natural.variant, natural2.variant, "EX-05: force must not affect future non-forced requests")
    }

    @Test
    fun `Scenario - Control variant gives unlimited access (EX-04)`() = scenario(controlExp) { client ->
        // Given the experiment contains a control variant (isControl=true) with unlimited meter
        // Find a visitor assigned to the control variant
        val controlVisitor = (1..10_000).asSequence()
            .map { "bdd-ctrl-$it" }
            .first { VariantAssigner.assign(VisitorId(it), controlExp).name == "control" }
        // When a visitor is deterministically assigned to the control variant
        // Then premium articles are always served in full
        repeat(20) { i ->
            val resp = decide(client, controlVisitor, article = "art-ctrl-$i")
            assertEquals("full", resp.access, "EX-04: control variant must always serve full access")
        }
        // And the variant name appears as "control" in the response
        val resp = decide(client, controlVisitor, article = "art-ctrl-final")
        assertEquals("control", resp.variant, "EX-04: variant name must be 'control'")
    }
}
