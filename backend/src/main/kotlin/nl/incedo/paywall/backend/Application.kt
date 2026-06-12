package nl.incedo.paywall.backend

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.serialization.json.Json
import nl.incedo.paywall.access.Article
import nl.incedo.paywall.access.ContentTier
import nl.incedo.paywall.access.StrategyConfig
import nl.incedo.paywall.access.Subject
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.ExperimentId
import nl.incedo.paywall.core.UserId
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.experiments.ExperimentDefinition
import nl.incedo.paywall.experiments.Variant

/**
 * Default experiment (EX-02): the four strategies of Doc 1 at equal weight.
 * Configuration, not code (PW-06) — replaced by the config store later.
 */
val defaultExperiment = ExperimentDefinition(
    id = ExperimentId("wall-strategy-2026Q3"),
    name = "Wall strategy comparison",
    variants = listOf(
        Variant("hard", StrategyConfig.Hard, weight = 25),
        Variant("metered", StrategyConfig.Metered(limit = 5), weight = 25),
        Variant("freemium", StrategyConfig.Freemium, weight = 25),
        Variant("dynamic", StrategyConfig.Dynamic(threshold = 0.5, floorLimit = 10), weight = 25),
    ),
)

/** PW-24: meter period is the calendar month in Europe/Amsterdam. */
fun currentPeriod() = nl.incedo.paywall.metering.MeterPeriod(
    YearMonth.now(ZoneId.of("Europe/Amsterdam")).toString(),
)

fun main() {
    // DATABASE_URL selects the PostgreSQL event store (Q-1); without it the
    // server runs on the in-memory store for local experimentation.
    val eventStore = System.getenv("DATABASE_URL")?.let { url ->
        nl.incedo.paywall.backend.persistence.PostgresEventStore.connect(
            jdbcUrl = url,
            username = System.getenv("DATABASE_USER") ?: "",
            password = System.getenv("DATABASE_PASSWORD") ?: "",
        )
    } ?: InMemoryEventStore()

    val service = AccessService(
        eventStore = eventStore,
        experiment = defaultExperiment,
        clock = { System.currentTimeMillis() },
        currentPeriod = ::currentPeriod,
    )
    embeddedServer(CIO, port = 8080) { module(service) }.start(wait = true)
}

fun Application.module(service: AccessService) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
        // API-05: the access decision endpoint — must respond < 50 ms p95
        post("/api/v1/decide") {
            val request = call.receive<DecideRequest>()
            val tier = when (request.tier.lowercase()) {
                "free" -> ContentTier.FREE
                "premium" -> ContentTier.PREMIUM
                else -> {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "tier must be free or premium"))
                    return@post
                }
            }
            val outcome = service.decide(
                subject = Subject(VisitorId(request.visitorId), request.userId?.let(::UserId)),
                article = Article(ArticleId(request.articleId), tier),
                propensityScore = request.propensityScore,
            )
            call.respond(DecideResponse.from(outcome))
        }
    }
}
