package nl.incedo.paywall.backend

import io.ktor.http.HttpHeaders
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
import nl.incedo.paywall.accounts.IdentityLinked
import nl.incedo.paywall.accounts.IdentityUnlinked
import nl.incedo.paywall.analytics.VariantStatsProjection
import nl.incedo.paywall.analytics.wallEventShardTags
import nl.incedo.paywall.analytics.WallEventRecorded
import nl.incedo.paywall.analytics.WallEventType
import nl.incedo.paywall.backend.auth.CiamJwtValidator
import nl.incedo.paywall.backend.content.ArticleRepository
import nl.incedo.paywall.cep.CepGateAdviceWithdrawn
import nl.incedo.paywall.cep.CepGateAdvised
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.ExperimentId
import nl.incedo.paywall.core.PlanId
import nl.incedo.paywall.core.SubscriptionId
import nl.incedo.paywall.core.GrantId
import nl.incedo.paywall.entitlements.EntitlementGranted
import nl.incedo.paywall.entitlements.EntitlementRevoked
import nl.incedo.paywall.grants.GrantIssued
import nl.incedo.paywall.grants.GrantRevoked
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.UserId
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.core.port.EventStore
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.experiments.ExperimentDefinition
import nl.incedo.paywall.experiments.Variant
import nl.incedo.paywall.experiments.VariantAssigner

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
        Variant("dynamic", StrategyConfig.Dynamic(floorLimit = 10), weight = 25),
    ),
)

/** AN-02 types a client may report; server-observed types are excluded. */
val clientEventTypes: Map<String, WallEventType> = mapOf(
    "page_view" to WallEventType.PAGE_VIEW,
    "wall_dismissed" to WallEventType.WALL_DISMISSED,
    "gate_cta_click" to WallEventType.GATE_CTA_CLICK,
    "register_start" to WallEventType.REGISTER_START,
    "register_complete" to WallEventType.REGISTER_COMPLETE,
    "checkout_start" to WallEventType.CHECKOUT_START,
    "checkout_complete" to WallEventType.CHECKOUT_COMPLETE,
    "login" to WallEventType.LOGIN,
    "cancel" to WallEventType.CANCEL,
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

    // TS-04: Ory Hydra issues the tokens; JWKS via its public endpoint, e.g.
    // CIAM_JWKS_URL=http://localhost:4444/.well-known/jwks.json
    // CIAM_ISSUER=http://localhost:4444/
    val jwtValidator = System.getenv("CIAM_JWKS_URL")?.let { jwks ->
        CiamJwtValidator.fromJwksUrl(
            jwksUrl = jwks,
            issuer = System.getenv("CIAM_ISSUER") ?: jwks.substringBefore(".well-known").trimEnd('/') + "/",
        )
    }

    embeddedServer(CIO, port = 8080) { module(service, eventStore, jwtValidator) }.start(wait = true)
}

fun Application.module(
    service: AccessService,
    eventStore: EventStore,
    /** Null = no CIAM configured: every request is anonymous (AC-07 fallback). */
    jwtValidator: CiamJwtValidator? = null,
    articles: ArticleRepository = ArticleRepository(),
) {
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
            // AC-07: identity exclusively from the validated CIAM JWT;
            // anything not provably authentic degrades to anonymous
            val userId = jwtValidator?.userIdFrom(call.request.headers[HttpHeaders.Authorization])
            val outcome = service.decide(
                subject = Subject(VisitorId(request.visitorId), userId),
                article = Article(ArticleId(request.articleId), tier),
                channel = request.channel,
            )
            call.respond(DecideResponse.from(outcome))
        }
        // The consumer endpoint (AC-04: same rules as any page). The premium
        // body is never present in a gated response (AC-01/BP-01); the teaser
        // is generated server-side (AC-05/PW-02).
        get("/api/v1/articles/{id}") {
            val articleId = call.parameters["id"]?.takeIf { it.isNotBlank() }
            val visitorId = call.request.headers["X-Visitor-Id"] ?: call.request.queryParameters["visitorId"]
            if (articleId == null || visitorId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "article id and visitor id are required"))
                return@get
            }
            val stored = articles.find(ArticleId(articleId))
            if (stored == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "unknown article"))
                return@get
            }
            val userId = jwtValidator?.userIdFrom(call.request.headers[HttpHeaders.Authorization])
            val outcome = service.decide(
                subject = Subject(VisitorId(visitorId), userId),
                article = Article(stored.id, stored.tier),
            )
            val response = when (val decision = outcome.decision) {
                is nl.incedo.paywall.access.AccessDecision.Full -> ArticleResponse(
                    id = stored.id.value,
                    title = stored.title,
                    tier = stored.tier.name.lowercase(),
                    access = "full",
                    body = stored.body,
                    meterUsed = outcome.meterUsedAfter,
                )
                is nl.incedo.paywall.access.AccessDecision.Gated -> ArticleResponse(
                    id = stored.id.value,
                    title = stored.title,
                    tier = stored.tier.name.lowercase(),
                    access = "gate",
                    teaser = ArticleRepository.teaserOf(stored),
                    gate = GateInfo(
                        wallType = outcome.variant.name,
                        variant = outcome.variant.name,
                        meterUsed = decision.meterUsed,
                        meterLimit = decision.meterLimit,
                    ),
                )
            }
            call.respond(response)
        }
        // Integration inbound: the CEP publishes gate advice as events; the
        // access layer acts on them at decide time (no synchronous CEP call).
        post("/api/v1/integration/cep-advice") {
            val advice = call.receive<CepAdviceEvent>()
            if (advice.subjectId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "subjectId must not be blank"))
                return@post
            }
            val subjectId = SubjectId(advice.subjectId)
            val event = if (advice.gate) {
                CepGateAdvised(subjectId, advice.validUntilEpochMs)
            } else {
                CepGateAdviceWithdrawn(subjectId)
            }
            eventStore.append(listOf(event), condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to event::class.simpleName))
        }
        // Client-originated funnel events (AN-02): only interaction types the
        // client legitimately observes; server-observed types are rejected.
        post("/api/v1/events") {
            val request = call.receive<ClientEventRequest>()
            val type = clientEventTypes[request.type.lowercase()]
            if (type == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "type must be one of ${clientEventTypes.keys.sorted()}"),
                )
                return@post
            }
            val userId = jwtValidator?.userIdFrom(call.request.headers[HttpHeaders.Authorization])
            val subject = Subject(VisitorId(request.visitorId), userId)
            val variant = VariantAssigner.assign(subject.visitorId, defaultExperiment)
            eventStore.append(
                listOf(
                    WallEventRecorded(
                        eventType = type,
                        subjectId = subject.subjectId,
                        variant = variant.name,
                        channel = request.channel,
                        occurredAtEpochMs = System.currentTimeMillis(),
                        articleId = request.articleId?.let(::ArticleId),
                        context = request.context,
                    ),
                ),
                condition = null,
            )
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to type.name))
        }
        // Experiment dashboard numbers (AN-10): per-variant funnel stats,
        // rebuilt from the wall-event stream (projection — DM-04/DM-08).
        get("/api/v1/stats") {
            val events = eventStore.query(EventQuery(wallEventShardTags())).events
            val projection = VariantStatsProjection().also { it.applyAll(events) }
            val response = projection.stats().map { (variant, s) ->
                VariantStatsResponse(
                    variant = variant,
                    visitors = s.visitors,
                    articleReads = s.articleReads,
                    wallsShown = s.wallsShown,
                    gateCtaClicks = s.gateCtaClicks,
                    gateCtr = s.gateCtr,
                    registrations = s.registrations,
                    checkoutStarts = s.checkoutStarts,
                    conversions = s.conversions,
                    conversionRate = s.conversionRate,
                )
            }.sortedBy { it.variant }
            call.respond(response)
        }
        // Integration inbound (AC-02, EA-*): entitlement changes from the
        // external subscription administration. The paywall enforces; it
        // never manages subscriptions (scope boundary).
        post("/api/v1/integration/entitlements") {
            val change = call.receive<EntitlementChangeRequest>()
            if (change.subjectId.isBlank() || change.subscriptionRef.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "subjectId and subscriptionRef are required"))
                return@post
            }
            val event = if (change.active) {
                EntitlementGranted(
                    subjectId = SubjectId(change.subjectId),
                    planId = PlanId(change.planId ?: "unknown"),
                    subscriptionRef = SubscriptionId(change.subscriptionRef),
                    validUntilEpochMs = change.validUntilEpochMs,
                )
            } else {
                EntitlementRevoked(
                    subjectId = SubjectId(change.subjectId),
                    subscriptionRef = SubscriptionId(change.subscriptionRef),
                )
            }
            eventStore.append(listOf(event), condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to event::class.simpleName))
        }
        // FGA grant management (FGA-03): issue/revoke article-scoped grants —
        // day/week passes carry TTL = pass duration (PW-08). All writes are
        // events, hence audited by construction (ADM-03).
        post("/api/v1/grants") {
            val change = call.receive<GrantChangeRequest>()
            if (change.grantId.isBlank() || change.subjectId.isBlank() || change.articleId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "grantId, subjectId and articleId are required"))
                return@post
            }
            val event = if (change.active) {
                GrantIssued(
                    grantId = GrantId(change.grantId),
                    subjectId = SubjectId(change.subjectId),
                    articleId = ArticleId(change.articleId),
                    grantedBy = change.grantedBy,
                    expiresAtEpochMs = change.expiresAtEpochMs,
                )
            } else {
                GrantRevoked(
                    grantId = GrantId(change.grantId),
                    subjectId = SubjectId(change.subjectId),
                    articleId = ArticleId(change.articleId),
                )
            }
            eventStore.append(listOf(event), condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to event::class.simpleName))
        }
        // Integration inbound (MT-13): consent-based identity link signals —
        // login (US-04), newsletter tokens, share tokens (BP-05), extra devices.
        post("/api/v1/integration/identity-link") {
            val request = call.receive<IdentityLinkRequest>()
            if (request.subjectA.isBlank() || request.subjectB.isBlank() || request.cause.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "subjectA, subjectB and cause are required"))
                return@post
            }
            val event = if (request.link) {
                IdentityLinked(SubjectId(request.subjectA), SubjectId(request.subjectB), request.cause)
            } else {
                IdentityUnlinked(SubjectId(request.subjectA), SubjectId(request.subjectB), request.cause)
            }
            eventStore.append(listOf(event), condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to event::class.simpleName))
        }
    }
}
