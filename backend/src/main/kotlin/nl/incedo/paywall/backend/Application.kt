package nl.incedo.paywall.backend

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.serialization.json.Json
import nl.incedo.paywall.api.SaveWallRequest
import nl.incedo.paywall.api.VariantStatsResponse
import nl.incedo.paywall.api.WallResponse
import nl.incedo.paywall.access.Article
import nl.incedo.paywall.access.ContentTier
import nl.incedo.paywall.access.StrategyConfig
import nl.incedo.paywall.access.Subject
import nl.incedo.paywall.accounts.IdentityLinked
import nl.incedo.paywall.accounts.IdentityUnlinked
import nl.incedo.paywall.accounts.UserDeleted
import nl.incedo.paywall.analytics.SoftGateDismissed
import nl.incedo.paywall.analytics.VariantStatsProjection
import nl.incedo.paywall.analytics.wallEventShardTags
import nl.incedo.paywall.analytics.WallEventRecorded
import nl.incedo.paywall.analytics.WallEventType
import nl.incedo.paywall.backend.auth.CiamJwtValidator
import nl.incedo.paywall.backend.auth.OriginTrust
import nl.incedo.paywall.backend.auth.RequestRateLimiter
import nl.incedo.paywall.backend.auth.StaffRole
import nl.incedo.paywall.backend.auth.TokenFailureMonitorPlugin
import nl.incedo.paywall.backend.auth.TokenFailureTracker
import nl.incedo.paywall.backend.auth.requireStaff
import nl.incedo.paywall.backend.content.ArticleRepository
import nl.incedo.paywall.brands.BrandCreated
import nl.incedo.paywall.plans.DefaultPlans
import nl.incedo.paywall.brands.BrandDecision
import nl.incedo.paywall.brands.BrandThemeUpdated
import nl.incedo.paywall.brands.brandTag
import nl.incedo.paywall.grants.DataGateConsentGiven
import nl.incedo.paywall.offers.OFFER_EVENT_TAG
import nl.incedo.paywall.offers.OfferAccepted
import nl.incedo.paywall.offers.OfferDeclined
import nl.incedo.paywall.offers.OfferStatsProjection
import nl.incedo.paywall.offers.offerTag
import nl.incedo.paywall.cep.CepClient
import nl.incedo.paywall.cep.CepGateAdviceWithdrawn
import nl.incedo.paywall.cep.CepGateAdvised
import nl.incedo.paywall.analytics.PartnerUsageProjection
import nl.incedo.paywall.offers.OfferTriggered
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.BrandId
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.ExperimentId
import nl.incedo.paywall.core.PartnerId
import nl.incedo.paywall.core.PlanId
import nl.incedo.paywall.core.SubscriptionId
import nl.incedo.paywall.core.GrantId
import nl.incedo.paywall.entitlements.EntitlementGranted
import nl.incedo.paywall.entitlements.EntitlementRevoked
import nl.incedo.paywall.entitlements.SubscriptionPaused
import nl.incedo.paywall.entitlements.SubscriptionResumed
import nl.incedo.paywall.grants.GrantIssued
import nl.incedo.paywall.grants.GrantRevoked
import nl.incedo.paywall.grants.ShareTokenIssued
import nl.incedo.paywall.partners.PartnerCreated
import nl.incedo.paywall.partners.PartnerDecision
import nl.incedo.paywall.partners.PartnerIpRangeConfigured
import nl.incedo.paywall.partners.PartnerMemberAdded
import nl.incedo.paywall.partners.PartnerMemberRemoved
import nl.incedo.paywall.partners.PartnerOffboarded
import nl.incedo.paywall.partners.partnerTag
import nl.incedo.paywall.walls.WallConfig
import nl.incedo.paywall.walls.WallView
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.UserId
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.core.WallId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.core.port.EventStore
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.experiments.ExperimentConfigPublished
import nl.incedo.paywall.experiments.ExperimentConfigProjection
import nl.incedo.paywall.experiments.ExperimentDefinition
import nl.incedo.paywall.experiments.Variant
import nl.incedo.paywall.experiments.VariantAssigner
import nl.incedo.paywall.metering.MeterDecision
import nl.incedo.paywall.metering.meterTag

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

private fun WallView.toResponse() = WallResponse(
    id = id.value,
    name = config.name,
    wallType = config.wallType,
    title = config.title,
    body = config.body,
    primaryCta = config.primaryCta,
    secondaryCta = config.secondaryCta,
    channels = config.channels,
    status = status,
    version = version,
    lastEditedBy = lastEditedBy,
    brandId = config.brandId,
    requireConsentStep = config.requireConsentStep,
    translations = config.translations, // ADM-15
)

private suspend fun io.ktor.server.application.ApplicationCall.respondSaveResult(result: WallService.SaveResult) {
    when (result) {
        is WallService.SaveResult.Saved -> respond(result.view.toResponse())
        is WallService.SaveResult.NotFound -> respond(HttpStatusCode.NotFound, mapOf("error" to "unknown wall"))
        is WallService.SaveResult.VersionConflict ->
            respond(HttpStatusCode.Conflict, mapOf("error" to "version conflict", "currentVersion" to result.current.toString()))
        is WallService.SaveResult.VersionNotFound ->
            respond(HttpStatusCode.NotFound, mapOf("error" to "version not found in history"))
    }
}

/** XML entity escaping for RSS feed values (BP-04). */
private fun String.xmlEscape() = replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")

/**
 * SEO-01: JSON-LD paywalled-content structured data for verified crawler responses.
 * Tells Google this is not cloaking — the full body is provided transparently with
 * the schema markup identifying the gated section (SEO-03 server-rendered path).
 */
private fun buildSeoStructuredData(articleId: String, title: String): String =
    """{"@context":"https://schema.org","@type":"NewsArticle","headline":${kotlinx.serialization.json.Json.encodeToString(title)},"isAccessibleForFree":false,"hasPart":[{"@type":"WebPageElement","isAccessibleForFree":false,"cssSelector":".premium-content"}]}"""

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

    // API-03: hot-reloadable experiment config backed by the event store.
    val configStore = ConfigStore(eventStore, fallback = defaultExperiment)
    val service = AccessService(
        eventStore = eventStore,
        experiment = defaultExperiment,
        clock = { System.currentTimeMillis() },
        currentPeriod = ::currentPeriod,
        experimentLoader = configStore::experiment,
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

    // INF-02: the shared secret the edge presents; absent in local dev.
    val originSecret = System.getenv("ORIGIN_SHARED_SECRET")

    // BP-05: SHARE_TOKEN_SECRET must be set in production. Falls back to a
    // random per-process key in dev (tokens valid only while the server runs).
    val shareSecret = System.getenv("SHARE_TOKEN_SECRET")
        ?: java.util.UUID.randomUUID().toString()
    val shareTokenService = ShareTokenService(eventStore, shareSecret)

    // NFR-03: webhook secret for provider signature verification; absent in dev.
    val webhookSecret = System.getenv("WEBHOOK_SECRET")
    val webhookVerifier = WebhookVerifier(webhookSecret)

    // API-07: mock CEP client for the experiment; replace with real HTTP client in production.
    val cepClient = nl.incedo.paywall.cep.MockCepClient()
    // UP-01: offer decision service — wraps the CEP client with guardrails and logging.
    val offerService = OfferService(eventStore, cepClient)

    embeddedServer(CIO, port = 8080) {
        module(
            service, eventStore, jwtValidator,
            originSecret = originSecret,
            configStore = configStore,
            shareTokenService = shareTokenService,
            webhookVerifier = webhookVerifier,
            cepClient = cepClient,
            offerService = offerService,
        )
    }.start(wait = true)
}

fun Application.module(
    service: AccessService,
    eventStore: EventStore,
    /** Null = no CIAM configured: every request is anonymous (AC-07 fallback). */
    jwtValidator: CiamJwtValidator? = null,
    articles: ArticleRepository = ArticleRepository(),
    /** INF-02: shared secret the edge presents; null = no edge in front (dev). */
    originSecret: String? = null,
    /** API-03: hot-reloadable config; null = use defaultExperiment (tests/dev). */
    configStore: ConfigStore? = null,
    /** BP-05: subscriber-generated signed share tokens; null = feature disabled (tests). */
    shareTokenService: ShareTokenService? = null,
    /** NFR-03: webhook signature verifier; null secret = accept all (dev). */
    webhookVerifier: WebhookVerifier = WebhookVerifier(null),
    /** API-07: CEP outbound client; null = no offer engine in tests. */
    cepClient: nl.incedo.paywall.cep.CepClient? = null,
    /** UP-01: offer decision service; auto-built from cepClient when null. */
    offerService: OfferService? = null,
    /** NFR-04: request rate limiter; injectable for tests. */
    rateLimiter: RequestRateLimiter? = null,
) {
    // UP-01: auto-construct the offer service from cepClient if not provided explicitly.
    // This keeps all test helpers that pass cepClient working without change.
    @Suppress("NAME_SHADOWING")
    val offerService = offerService ?: cepClient?.let { OfferService(eventStore, it) }
    val originTrust = OriginTrust(originSecret)
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    // Dev CORS for the Wasm console; in production every request enters
    // through the Worker on the same origin (INF-01), so this disappears.
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(OriginTrust.ORIGIN_SECRET_HEADER)
    }
    // NFR-24: log token validation failures with reason; rate-track per source IP.
    if (jwtValidator != null) {
        install(TokenFailureMonitorPlugin) {
            validator = jwtValidator
            tracker = TokenFailureTracker()
        }
    }
    // NFR-04: per-IP rate limiting on auth and checkout endpoints, plus
    // INF-02 origin trust enforcement. CSRF is prevented by the combination of
    // Content-Type: application/json (ContentNegotiation) and X-Origin-Secret
    // edge verification (OriginTrust), so no additional CSRF token is required.
    val effectiveRateLimiter = rateLimiter ?: RequestRateLimiter(windowMs = 60_000L, maxRequests = 30)
    val rateLimitedPrefixes = listOf("/api/v1/offers", "/api/v1/integration", "/api/v1/admin", "/api/v1/grants")
    val rateLimitedMethods = setOf("POST", "PUT", "PATCH", "DELETE")
    intercept(ApplicationCallPipeline.Plugins) {
        val path = call.request.path()
        if (path != "/health" && !originTrust.verify(call)) { finish(); return@intercept }
        if (call.request.httpMethod.value in rateLimitedMethods &&
            rateLimitedPrefixes.any { path.startsWith(it) }
        ) {
            val sourceIp = call.request.headers["X-Forwarded-For"]
                ?.substringBefore(",")?.trim()
                ?: call.request.local.remoteAddress
            if (effectiveRateLimiter.isExceeded("$sourceIp:$path")) {
                val retryAfter = effectiveRateLimiter.windowMs / 1_000
                call.response.headers.append("Retry-After", retryAfter.toString())
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "rate limit exceeded — retry after ${retryAfter}s (NFR-04)"))
                finish()
            }
        }
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
                "complete" -> ContentTier.COMPLETE // UP-12: complete-tier content
                else -> {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "tier must be free, premium, or complete"))
                    return@post
                }
            }
            // AC-07: identity exclusively from the validated CIAM JWT;
            // anything not provably authentic degrades to anonymous
            val userId = jwtValidator?.userIdFrom(call.request.headers[HttpHeaders.Authorization])
            val subject = Subject(VisitorId(request.visitorId), userId)
            val clientIp = call.request.headers["X-Forwarded-For"]?.substringBefore(",")?.trim()
                ?: call.request.local.remoteAddress
            // MT-05: trust the verified-bot signal only when the origin secret is
            // configured — proves the request came through the edge (INF-02/BP-02).
            val isVerifiedCrawler = originSecret != null &&
                call.request.headers["X-Verified-Bot"] == "true"
            // NFR-14: correlation ID for structured log tracing. Echoed in the response
            // header so clients can correlate their requests with the event store.
            val correlationId = call.request.headers["X-Request-ID"]
                ?: java.util.UUID.randomUUID().toString()
            call.response.headers.append("X-Request-ID", correlationId)
            // IPW-02: partner_id is edge-injected after CIDR match (INF-01/02).
            // Only trust it when origin secret is configured.
            val partnerIdHeader = if (originSecret != null) call.request.headers["X-Partner-Id"] else null
            if (partnerIdHeader != null) {
                val pEvents = eventStore.query(EventQuery(setOf(partnerTag(PartnerId(partnerIdHeader))))).events
                val partner = PartnerDecision().also { it.applyAll(pEvents) }
                if (partner.name.isNotEmpty() && partner.isActive) { // PA-03: isActive false after offboarding
                    // PA-04: log partner access as a wall event so reads per partner are
                    // reportable from the event store for contract management (AN-04 export).
                    val partnerEvent = nl.incedo.paywall.analytics.WallEventRecorded(
                        eventType = nl.incedo.paywall.analytics.WallEventType.ARTICLE_READ,
                        subjectId = subject.subjectId,
                        variant = "partner",
                        channel = request.channel,
                        occurredAtEpochMs = System.currentTimeMillis(),
                        articleId = ArticleId(request.articleId),
                        context = buildMap {
                            put("reason", "partner_entitled")
                            put("partner_id", partnerIdHeader)
                            put("partner_name", partner.name)
                            put("request_id", correlationId) // NFR-14
                        },
                    )
                    eventStore.append(listOf(partnerEvent), condition = null)
                    call.respond(DecideResponse(access = "full", reason = "partner_entitled", variant = "partner", meterUsed = null))
                    return@post
                }
            }
            // EX-05: staff debug override — forces a specific variant for QA.
            // Applied only when the caller carries a valid staff JWT; silently
            // ignored for regular readers (no 4xx). Analytics suppressed for debug runs.
            val forceVariant = call.request.queryParameters["forceVariant"]
                ?.takeIf { jwtValidator == null || jwtValidator.staffFrom(call.request.headers[HttpHeaders.Authorization]) != null }
            val outcome = service.decide(
                subject = subject,
                article = Article(ArticleId(request.articleId), tier),
                channel = request.channel,
                isBot = isBotUserAgent(call.request.headers[HttpHeaders.UserAgent]),
                isSuspicious = service.recordIpAndCheckSuspicious(subject, clientIp),
                isVerifiedCrawler = isVerifiedCrawler,
                forceVariant = forceVariant,
                correlationId = correlationId,
                externalScore = request.externalScore, // DY-06
            )
            call.respond(DecideResponse.from(outcome))
        }
        // AC-13: soft-gate dismissal — visitor dismisses the soft overlay in the Dynamic
        // variant. Logged as a SoftGateDismissed event (subject-tagged) so the next
        // decide() call within the 30-minute session window bypasses the soft gate.
        // Hard gates (score ≥ T_hard or floor reached) are not dismissible.
        post("/api/v1/decide/dismiss-soft-gate") {
            val visitorId = call.request.queryParameters["visitorId"]
                ?: call.request.headers["X-Visitor-Id"]
            if (visitorId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "visitorId is required"))
                return@post
            }
            val userId = jwtValidator?.userIdFrom(call.request.headers[HttpHeaders.Authorization])
            val subjectId = userId?.let { nl.incedo.paywall.core.SubjectId.of(it) }
                ?: nl.incedo.paywall.core.SubjectId.of(VisitorId(visitorId))
            val event = SoftGateDismissed(
                subjectId = subjectId,
                dismissedAtEpochMs = System.currentTimeMillis(),
            )
            eventStore.append(listOf(event), condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to "SoftGateDismissed"))
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
            val subject = Subject(VisitorId(visitorId), userId)
            val clientIp = call.request.headers["X-Forwarded-For"]?.substringBefore(",")?.trim()
                ?: call.request.local.remoteAddress
            // MT-05: trust the verified-bot signal only when the origin secret is
            // configured — proves the request came through the edge (INF-02/BP-02).
            val isVerifiedCrawler = originSecret != null &&
                call.request.headers["X-Verified-Bot"] == "true"
            val outcome = service.decide(
                subject = subject,
                article = Article(stored.id, stored.tier),
                isBot = isBotUserAgent(call.request.headers[HttpHeaders.UserAgent]),
                isSuspicious = service.recordIpAndCheckSuspicious(subject, clientIp),
                isVerifiedCrawler = isVerifiedCrawler,
            )
            // BP-03: premium pages carry noarchive so archive crawlers only
            // snapshot the teaser and not the full body (also applies to verified
            // search crawlers — noarchive suppresses Google Cache, not indexing).
            if (stored.tier != ContentTier.FREE) {
                call.response.headers.append("X-Robots-Tag", "noarchive")
            }
            val response = when (val decision = outcome.decision) {
                is nl.incedo.paywall.access.AccessDecision.Full -> {
                    // BP-07: full premium responses must not be cached by shared caches
                    // (CDN/proxy); private is sufficient since the body is personalized.
                    if (stored.tier != ContentTier.FREE) {
                        call.response.headers.append(HttpHeaders.CacheControl, "private, no-store")
                    }
                    val isCrawlerAccess =
                        decision.reason == nl.incedo.paywall.access.AccessReason.VERIFIED_CRAWLER
                    ArticleResponse(
                        id = stored.id.value,
                        title = stored.title,
                        tier = stored.tier.name.lowercase(),
                        access = "full",
                        body = stored.body,
                        meterUsed = if (isCrawlerAccess) null else outcome.meterUsedAfter,
                        // SEO-01: JSON-LD paywalled-content schema for verified crawlers
                        // so Googlebot doesn't treat the full-body serving as cloaking.
                        structuredData = if (isCrawlerAccess && stored.tier != ContentTier.FREE) {
                            buildSeoStructuredData(stored.id.value, stored.title)
                        } else null,
                    )
                }
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
        // API-08: webhook signature required — same shared secret as entitlements.
        post("/api/v1/integration/cep-advice") {
            val rawBody = call.receive<ByteArray>()
            if (!webhookVerifier.verify(rawBody, call.request.headers[WebhookVerifier.SIGNATURE_HEADER])) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid webhook signature (API-08)"))
                return@post
            }
            val advice = kotlinx.serialization.json.Json.decodeFromString<CepAdviceEvent>(rawBody.decodeToString())
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
            val variant = VariantAssigner.assign(subject, configStore?.experiment() ?: defaultExperiment) // EX-03
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
        // Wall designer API (ADM-01/02/11): wall definitions as structured
        // content, versioned via events, optimistic concurrency per ADM-06.
        val wallService = WallService(eventStore)
        get("/api/v1/walls") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            call.respond(wallService.list().map { it.toResponse() })
        }
        get("/api/v1/walls/{id}") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            val id = call.parameters["id"]?.takeIf { it.isNotBlank() }
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "wall id required"))
            val view = wallService.get(WallId(id))
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "unknown wall"))
            call.respond(view.toResponse())
        }
        post("/api/v1/walls/{id}") {
            val staff = call.requireStaff(jwtValidator, StaffRole.OPERATOR) ?: return@post
            val id = call.parameters["id"]?.takeIf { it.isNotBlank() }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "wall id required"))
            val request = call.receive<SaveWallRequest>()
            if (request.name.isBlank() || request.title.isBlank()) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "name and title are required"))
            }
            if (request.wallType !in setOf("hard", "metered", "freemium", "dynamic")) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "wallType must be hard|metered|freemium|dynamic"))
            }
            val config = WallConfig(
                name = request.name, wallType = request.wallType, title = request.title,
                body = request.body, primaryCta = request.primaryCta,
                secondaryCta = request.secondaryCta, channels = request.channels,
                brandId = request.brandId, // ADM-10: optional brand association
                requireConsentStep = request.requireConsentStep, // AC-14: GDPR consent step
                translations = request.translations, // ADM-15: per-locale copy overrides
            )
            val wallId = WallId(id)
            // The actor is the authenticated staff subject, not client-supplied (ADM-03 audit).
            val actor = staff.userId.value
            val result = if (wallService.get(wallId) == null) {
                wallService.create(wallId, config, actor)
            } else {
                wallService.update(wallId, config, actor, request.expectedVersion)
            }
            call.respondSaveResult(result)
        }
        // MT-10/ADM-02/API-03: read and update experiment + meter configuration.
        // Configuration is stored as versioned events (auditable) and hot-reloadable.
        get("/api/v1/admin/config") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            val events = eventStore.query(EventQuery(setOf("config:experiment"))).events
            val projection = ExperimentConfigProjection().also { it.applyAll(events) }
            val published = projection.current
            call.respond(
                ExperimentConfigResponse(
                    experiment = published?.experiment ?: defaultExperiment,
                    publishedBy = published?.actor,
                    publishedAtEpochMs = published?.publishedAtEpochMs,
                    isDefault = published == null,
                ),
            )
        }
        post("/api/v1/admin/config") {
            val staff = call.requireStaff(jwtValidator, StaffRole.ADMIN) ?: return@post
            val request = call.receive<PublishExperimentConfigRequest>()
            // Validate: weights must sum to a positive value (relative weights, not required to sum to 100)
            val experiment = request.experiment
            if (experiment.variants.isEmpty()) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "at least one variant is required"))
            }
            if (experiment.variants.any { it.weight <= 0 }) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "all variant weights must be positive"))
            }
            val now = System.currentTimeMillis()
            val event = ExperimentConfigPublished(
                experiment = experiment,
                actor = staff.userId.value,
                publishedAtEpochMs = now,
            )
            eventStore.append(listOf(event), condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to "ExperimentConfigPublished", "actor" to staff.userId.value))
        }
        post("/api/v1/walls/{id}/publish") {
            val staff = call.requireStaff(jwtValidator, StaffRole.ADMIN) ?: return@post
            val id = call.parameters["id"]?.takeIf { it.isNotBlank() }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "wall id required"))
            call.respondSaveResult(wallService.publish(WallId(id), actor = staff.userId.value))
        }
        // ADM-13: rollback a wall to a previous version. In event-sourcing terms this
        // re-applies the historical config as a new draft — history is never deleted.
        post("/api/v1/walls/{id}/rollback") {
            val staff = call.requireStaff(jwtValidator, StaffRole.ADMIN) ?: return@post
            val id = call.parameters["id"]?.takeIf { it.isNotBlank() }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "wall id required"))
            val targetVersion = call.request.queryParameters["version"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "version query parameter required (integer)"))
            val result = wallService.rollback(WallId(id), targetVersion, actor = staff.userId.value)
            if (result is WallService.SaveResult.VersionNotFound) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "version $targetVersion not found in wall history (ADM-13)"))
            } else {
                call.respondSaveResult(result)
            }
        }
        // ADM-04: subject inspector — meter state, entitlements, identity
        // links and recent wall events for one person, plus the audited
        // meter-reset support action.
        get("/api/v1/admin/subjects/{subjectId}") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            val subjectParam = call.parameters["subjectId"]?.takeIf { it.isNotBlank() }
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "subject id required"))
            val subjectId = SubjectId(subjectParam)
            val period = currentPeriod()

            val baseEvents = eventStore.query(
                EventQuery(setOf("subject:${subjectId.value}", meterTag(subjectId, period))),
            ).events
            val links = nl.incedo.paywall.accounts.IdentityLinkDecision().also { it.applyAll(baseEvents) }
            val allSubjects = links.linkedSubjects(setOf(subjectId))
            val events = if (allSubjects == setOf(subjectId)) {
                baseEvents
            } else {
                eventStore.query(
                    EventQuery(
                        allSubjects.flatMap { listOf("subject:${it.value}", meterTag(it, period)) }.toSet(),
                    ),
                ).events
            }

            val now = System.currentTimeMillis()
            val meter = MeterDecision(period).also { it.applyAll(events) }
            val entitlement = nl.incedo.paywall.entitlements.EntitlementDecision().also { it.applyAll(events) }
            // Live grant ids across the linked subjects (FGA-08 browse)
            val grantExpiry = mutableMapOf<String, Long?>()
            events.forEach { event ->
                when (event) {
                    is GrantIssued -> grantExpiry[event.grantId.value] = event.expiresAtEpochMs
                    is GrantRevoked -> grantExpiry.remove(event.grantId.value)
                    else -> {}
                }
            }
            val liveGrants = grantExpiry.filterValues { it == null || it > now }.keys.sorted()
            val recent = events.filterIsInstance<WallEventRecorded>().takeLast(20).map {
                InspectorWallEvent(
                    type = it.eventType.name.lowercase(),
                    articleId = it.articleId?.value,
                    variant = it.variant,
                    channel = it.channel,
                    occurredAtEpochMs = it.occurredAtEpochMs,
                )
            }
            val variant = subjectId.value.removePrefix("visitor:")
                .takeIf { it != subjectId.value } // only visitor subjects carry an assignment
                ?.let { VariantAssigner.assign(VisitorId(it), configStore?.experiment() ?: defaultExperiment).name }
            call.respond(
                SubjectInspectorResponse(
                    subjectId = subjectId.value,
                    variant = variant,
                    meterPeriod = period.value,
                    meterUsed = meter.used,
                    meteredArticles = meter.countedArticleIds().map { it.value }.sorted(), // MT-11
                    entitled = entitlement.hasValidEntitlement(now),
                    liveGrants = liveGrants,
                    linkedSubjects = allSubjects.map { it.value }.sorted(),
                    recentWallEvents = recent,
                ),
            )
        }
        // FGA-08: full grant audit trail for a subject — all grants ever issued
        // (active and revoked) with grantedBy, expiry, and live status.
        get("/api/v1/admin/subjects/{subjectId}/grants") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            val subjectParam = call.parameters["subjectId"]?.takeIf { it.isNotBlank() } ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "subjectId required"))
                return@get
            }
            val now = System.currentTimeMillis()
            val events = eventStore.query(EventQuery(setOf("subject:$subjectParam"))).events
            val issued = mutableMapOf<String, GrantIssued>()
            val revoked = mutableSetOf<String>()
            events.forEach { event ->
                when (event) {
                    is GrantIssued -> issued[event.grantId.value] = event
                    is GrantRevoked -> revoked.add(event.grantId.value)
                    else -> {}
                }
            }
            val entries = issued.values
                .sortedBy { it.grantId.value }
                .map { g ->
                    GrantAuditEntry(
                        grantId = g.grantId.value,
                        articleId = g.articleId.value,
                        grantedBy = g.grantedBy,
                        expiresAtEpochMs = g.expiresAtEpochMs,
                        isLive = g.grantId.value !in revoked &&
                            (g.expiresAtEpochMs?.let { it > now } ?: true),
                    )
                }
            call.respond(entries)
        }
        post("/api/v1/admin/subjects/{subjectId}/meter-reset") {
            // AA-01: a user-scoped support action — operator role + step-up.
            val staff = call.requireStaff(jwtValidator, StaffRole.OPERATOR, needAal2 = true) ?: return@post
            val subjectParam = call.parameters["subjectId"]?.takeIf { it.isNotBlank() }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "subject id required"))
            val request = call.receive<MeterResetRequest>()
            if (request.reason.isBlank()) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "reason is required (audited action)"))
            }
            eventStore.append(
                listOf(
                    nl.incedo.paywall.metering.MeterReset(
                        subjectId = SubjectId(subjectParam),
                        period = currentPeriod(),
                        actor = staff.userId.value,
                        reason = request.reason,
                    ),
                ),
                condition = null,
            )
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to "MeterReset"))
        }
        // Experiment dashboard numbers (AN-10): per-variant funnel stats,
        // rebuilt from the wall-event stream (projection — DM-04/DM-08).
        get("/api/v1/stats") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            val events = eventStore.query(EventQuery(wallEventShardTags())).events
            val projection = VariantStatsProjection().also { it.applyAll(events) }
            val response = projection.stats().map { (variant, s) ->
                VariantStatsResponse(
                    variant = variant,
                    visitors = s.visitors,
                    pageViews = s.pageViews,
                    articleReads = s.articleReads,
                    wallsShown = s.wallsShown,
                    gateCtaClicks = s.gateCtaClicks,
                    gateCtr = s.gateCtr,
                    registrations = s.registrations,
                    checkoutStarts = s.checkoutStarts,
                    conversions = s.conversions,
                    conversionRate = s.conversionRate,
                    conversionRateLow = s.conversionRateLow,
                    conversionRateHigh = s.conversionRateHigh,
                    sampleSizeTooSmall = s.sampleSizeTooSmall,
                )
            }.sortedBy { it.variant }
            call.respond(response)
        }
        // AN-14: offer performance view — per offer_id funnel counts, acceptance rate,
        // and channel breakdown. Rebuilt from the offer-event stream (DM-04).
        get("/api/v1/stats/offers") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            val events = eventStore.query(EventQuery(setOf(OFFER_EVENT_TAG))).events
            val projection = OfferStatsProjection().also { it.applyAll(events) }
            call.respond(projection.stats().map { s ->
                OfferStatsResponse(
                    offerId = s.offerId,
                    triggered = s.triggered,
                    accepted = s.accepted,
                    declined = s.declined,
                    suppressed = s.suppressed,
                    acceptanceRate = s.acceptanceRate,
                    channels = s.channels.mapValues { (_, ch) ->
                        OfferChannelStatsResponse(
                            triggered = ch.triggered,
                            accepted = ch.accepted,
                            declined = ch.declined,
                            suppressed = ch.suppressed,
                        )
                    },
                )
            })
        }
        // PA-04: partner usage report — reads per partner and unique users,
        // derived from the wall-event stream. Staff VIEWER access for contract management.
        get("/api/v1/stats/partners") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            val events = eventStore.query(EventQuery(wallEventShardTags())).events
            val projection = PartnerUsageProjection().also { it.applyAll(events) }
            call.respond(projection.usage().map { u ->
                PartnerUsageResponse(
                    partnerId = u.partnerId,
                    totalReads = u.totalReads,
                    uniqueUsers = u.uniqueUsers,
                )
            })
        }
        // Integration inbound (AC-02, EA-*): entitlement changes from the
        // external subscription administration. The paywall enforces; it
        // never manages subscriptions (scope boundary).
        post("/api/v1/integration/entitlements") {
            // NFR-03: verify provider webhook signature before deserializing (prevents replay/forgery).
            val rawBody = call.receive<ByteArray>()
            if (!webhookVerifier.verify(rawBody, call.request.headers[WebhookVerifier.SIGNATURE_HEADER])) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid webhook signature (NFR-03)"))
                return@post
            }
            val change = kotlinx.serialization.json.Json.decodeFromString<EntitlementChangeRequest>(rawBody.decodeToString())
            if (change.subjectId.isBlank() || change.subscriptionRef.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "subjectId and subscriptionRef are required"))
                return@post
            }
            val subjectId = SubjectId(change.subjectId)
            val subscriptionRef = SubscriptionId(change.subscriptionRef)
            val planId = PlanId(change.planId ?: "unknown")
            // SUB-07: status field takes precedence over legacy active boolean.
            val gracePeriodMs = 7 * 24 * 60 * 60 * 1000L
            val nowMs = System.currentTimeMillis()
            val events: List<DomainEvent> = when (change.status) {
                "active" -> listOf(
                    EntitlementGranted(subjectId, planId, subscriptionRef, change.validUntilEpochMs),
                    SubscriptionResumed(subjectId, subscriptionRef, nowMs), // clears paused state if any
                )
                "canceled" -> listOf( // SUB-03: retain access until current_period_end
                    EntitlementGranted(subjectId, planId, subscriptionRef, change.validUntilEpochMs),
                )
                "past_due" -> listOf( // SUB-05: 7-day grace period with access retained
                    EntitlementGranted(subjectId, planId, subscriptionRef, nowMs + gracePeriodMs),
                )
                "paused" -> listOf( // SUB-07: billing suspended → access off
                    SubscriptionPaused(subjectId, subscriptionRef, planId, nowMs),
                )
                "expired" -> listOf(
                    EntitlementRevoked(subjectId, subscriptionRef),
                )
                else -> if (change.active) { // legacy boolean path
                    listOf(EntitlementGranted(subjectId, planId, subscriptionRef, change.validUntilEpochMs))
                } else {
                    listOf(EntitlementRevoked(subjectId, subscriptionRef))
                }
            }
            eventStore.append(events, condition = null)
            // AC-03: invalidate the per-session cache so the change takes effect
            // immediately rather than waiting for the 5-minute TTL to expire.
            service.invalidateEntitlementCache(change.subjectId)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to events.map { it::class.simpleName }))
        }
        // FGA grant management (FGA-03): issue/revoke article-scoped grants —
        // day/week passes carry TTL = pass duration (PW-08). All writes are
        // events, hence audited by construction (ADM-03).
        post("/api/v1/grants") {
            // AA-01: user-scoped grant administration — operator role + step-up.
            call.requireStaff(jwtValidator, StaffRole.OPERATOR, needAal2 = true) ?: return@post
            val change = call.receive<GrantChangeRequest>()
            if (change.grantId.isBlank() || change.subjectId.isBlank() || change.articleId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "grantId, subjectId and articleId are required"))
                return@post
            }
            // FGA-02: every grant is time-bound (expires_at required, max 90 days).
            // If not supplied, default to 30 days from now.
            val now = System.currentTimeMillis()
            val maxGrantTtlMs = 90L * 24 * 3600 * 1000
            val defaultGrantTtlMs = 30L * 24 * 3600 * 1000
            val expiresAt = if (change.active) {
                val requested = change.expiresAtEpochMs ?: (now + defaultGrantTtlMs)
                if (requested > now + maxGrantTtlMs) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "grant TTL exceeds maximum of 90 days (FGA-02)"),
                    )
                    return@post
                }
                requested
            } else null
            // FGA-03: max 10 active grants per subject per source (grantedBy) — prevent
            // privilege escalation through a compromised CEP/AI integration (FGA-06).
            if (change.active) {
                val maxGrantsPerSource = 10
                val subjectEvents = eventStore.query(EventQuery(setOf("subject:${change.subjectId}"))).events
                val activeGrantsFromSource = subjectEvents
                    .filterIsInstance<GrantIssued>()
                    .count { it.grantedBy == change.grantedBy && (it.expiresAtEpochMs ?: Long.MAX_VALUE) > now }
                if (activeGrantsFromSource >= maxGrantsPerSource) {
                    call.respond(HttpStatusCode.TooManyRequests,
                        mapOf("error" to "max active grants per source ($maxGrantsPerSource) reached (FGA-03)"))
                    return@post
                }
            }
            val event = if (change.active) {
                GrantIssued(
                    grantId = GrantId(change.grantId),
                    subjectId = SubjectId(change.subjectId),
                    articleId = ArticleId(change.articleId),
                    grantedBy = change.grantedBy,
                    expiresAtEpochMs = expiresAt,
                )
            } else {
                GrantRevoked(
                    grantId = GrantId(change.grantId),
                    subjectId = SubjectId(change.subjectId),
                    articleId = ArticleId(change.articleId),
                )
            }
            eventStore.append(listOf(event), condition = null)
            // FGA-05: invalidate the 60s grant result cache so the change takes effect immediately.
            service.invalidateGrantCache(change.subjectId, change.articleId)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to event::class.simpleName))
        }
        // AN-04: the wall-event stream is exportable for offline analysis.
        // CSV here; Parquet via the warehouse pipeline later. `since` pages
        // by store position so exports can run incrementally.
        get("/api/v1/export/wall-events.csv") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
            val result = eventStore.query(EventQuery(wallEventShardTags(), since = since))
            val rows = result.events.filterIsInstance<WallEventRecorded>()
            val csv = buildString {
                appendLine("occurred_at_epoch_ms,type,subject_id,variant,channel,article_id,context")
                rows.forEach { event ->
                    val context = event.context.entries.joinToString(";") { "${it.key}=${it.value}" }
                    appendLine(
                        listOf(
                            event.occurredAtEpochMs.toString(),
                            event.eventType.name.lowercase(),
                            event.subjectId.value,
                            event.variant,
                            event.channel,
                            event.articleId?.value ?: "",
                            context,
                        ).joinToString(",") { field ->
                            if ("," in field || "\"" in field) "\"${field.replace("\"", "\"\"")}\"" else field
                        },
                    )
                }
            }
            call.response.headers.append("X-Export-Position", result.position.toString())
            call.respondText(csv, io.ktor.http.ContentType.Text.CSV)
        }
        // BP-04: RSS 2.0 feed — premium articles carry teaser-only in the
        // description so full bodies are never exposed via syndication.
        // No authentication required: the feed is public (titles and teasers only).
        get("/api/v1/feed.rss") {
            val feedArticles = articles.findAll()
            val rss = buildString {
                appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
                appendLine("""<rss version="2.0" xmlns:content="http://purl.org/rss/1.0/modules/content/">""")
                appendLine("  <channel>")
                appendLine("    <title>Articles</title>")
                appendLine("    <link>/</link>")
                appendLine("    <description>Latest articles</description>")
                feedArticles.forEach { article ->
                    // BP-04: premium/complete content exposes teaser only — never the full body.
                    val description = when (article.tier) {
                        ContentTier.FREE -> article.body
                        ContentTier.PREMIUM, ContentTier.COMPLETE -> ArticleRepository.teaserOf(article)
                    }
                    appendLine("    <item>")
                    appendLine("      <title>${article.title.xmlEscape()}</title>")
                    appendLine("      <guid>${article.id.value.xmlEscape()}</guid>")
                    appendLine("      <description><![CDATA[${description}]]></description>")
                    appendLine("    </item>")
                }
                appendLine("  </channel>")
                append("</rss>")
            }
            call.respondText(rss, io.ktor.http.ContentType.Application.Xml)
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
        // AN-21/US-07: GDPR account deletion. Severs all identity links involving
        // the deleted user so event data becomes pseudonymous (visitor_id only).
        // The user's wall-event history remains but is no longer linkable to a real
        // identity. Caller is the CIAM webhook after account deletion in Kratos.
        post("/api/v1/integration/account-deletion") {
            val rawBody = call.receive<ByteArray>()
            if (!webhookVerifier.verify(rawBody, call.request.headers[WebhookVerifier.SIGNATURE_HEADER])) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid webhook signature (NFR-03)"))
                return@post
            }
            val request = kotlinx.serialization.json.Json.decodeFromString<AccountDeletionRequest>(rawBody.decodeToString())
            if (request.userId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId is required"))
                return@post
            }
            val userSubject = SubjectId("user:${request.userId}")
            val now = System.currentTimeMillis()

            // Find all subjects linked to this user.
            val events = eventStore.query(EventQuery(setOf("subject:${userSubject.value}"))).events
            val links = nl.incedo.paywall.accounts.IdentityLinkDecision().also { it.applyAll(events) }
            val linked = links.linkedSubjects(setOf(userSubject)) - userSubject

            val deletionEvents = mutableListOf<nl.incedo.paywall.core.DomainEvent>()
            // Audit record — written first so the intent is visible even if the rest fails.
            deletionEvents.add(UserDeleted(userSubject, now))
            // Sever each link: visitor events remain but are no longer attributed to the user.
            linked.forEach { other ->
                deletionEvents.add(IdentityUnlinked(userSubject, other, reason = "account_deletion"))
            }
            eventStore.append(deletionEvents, condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to deletionEvents.size, "linksRevoked" to linked.size))
        }
        // AG-02: verified ad-completion webhook from the third-party ad player.
        // Issues a 24-hour grant for the triggering article, daily cap = 2 per subject.
        post("/api/v1/integration/ad-completion") {
            val rawBody = call.receive<ByteArray>()
            if (!webhookVerifier.verify(rawBody, call.request.headers[WebhookVerifier.SIGNATURE_HEADER])) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid webhook signature (NFR-03)"))
                return@post
            }
            val req = kotlinx.serialization.json.Json.decodeFromString<AdCompletionRequest>(rawBody.decodeToString())
            if (req.subjectId.isBlank() || req.articleId.isBlank() || req.adPlayId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "subjectId, articleId and adPlayId are required"))
                return@post
            }
            val subjectId = SubjectId(req.subjectId)
            val now = System.currentTimeMillis()
            // AG-02: daily cap = 2 ad-gated unlocks per subject per calendar day (Amsterdam).
            val todayStart = java.time.LocalDate.now(ZoneId.of("Europe/Amsterdam"))
                .atStartOfDay(ZoneId.of("Europe/Amsterdam")).toInstant().toEpochMilli()
            val subjectEvents = eventStore.query(EventQuery(setOf("subject:${subjectId.value}"))).events
            val adGrantsToday = subjectEvents
                .filterIsInstance<nl.incedo.paywall.grants.GrantIssued>()
                .count { it.grantedBy == "ad_gated" && (it.expiresAtEpochMs ?: 0) >= todayStart }
            val dailyCap = 2
            if (adGrantsToday >= dailyCap) {
                call.respond(HttpStatusCode.TooManyRequests,
                    mapOf("error" to "daily ad-gated unlock cap reached ($dailyCap/day) (AG-02)"))
                return@post
            }
            // Idempotency: use adPlayId as the grantId so replays are no-ops.
            val grantId = GrantId("ad-${req.adPlayId}")
            val grantTtlMs = 24L * 60 * 60 * 1000
            val event = nl.incedo.paywall.grants.GrantIssued(
                grantId = grantId,
                subjectId = subjectId,
                articleId = ArticleId(req.articleId),
                grantedBy = "ad_gated",
                expiresAtEpochMs = now + grantTtlMs,
            )
            eventStore.append(listOf(event), condition = null)
            service.invalidateGrantCache(req.subjectId, req.articleId)
            call.respond(HttpStatusCode.Created,
                mapOf("grantId" to grantId.value, "expiresAtEpochMs" to (now + grantTtlMs).toString()))
        }
        // DG-02/03: verified data-gate completion webhook from the CIAM or survey platform.
        // On verified completion: record explicit consent (DG-03), issue a 7-day grant
        // for the triggering article (default), granted_by=data_gate (DG-02).
        post("/api/v1/integration/data-gate-completion") {
            val rawBody = call.receive<ByteArray>()
            if (!webhookVerifier.verify(rawBody, call.request.headers[WebhookVerifier.SIGNATURE_HEADER])) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid webhook signature (NFR-03)"))
                return@post
            }
            val req = try {
                kotlinx.serialization.json.Json.decodeFromString<DataGateCompletionRequest>(rawBody.decodeToString())
            } catch (_: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid request body (DG-02)"))
                return@post
            }
            if (req.subjectId.isBlank() || req.purposeId.isBlank() || req.completionId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "subjectId, purposeId and completionId are required (DG-02)"))
                return@post
            }
            val subjectId = SubjectId(req.subjectId)
            val now = System.currentTimeMillis()
            // Idempotency: use completionId as the grantId so replays don't double-grant.
            val grantId = GrantId("dg-${req.completionId}")
            val grantTtlMs = 7L * 24 * 60 * 60 * 1000 // DG-02: 7-day TTL
            val grant = nl.incedo.paywall.grants.GrantIssued(
                grantId = grantId,
                subjectId = subjectId,
                articleId = req.articleId?.let { ArticleId(it) } ?: ArticleId("*"),
                grantedBy = "data_gate",
                expiresAtEpochMs = now + grantTtlMs,
            )
            // DG-03: record explicit consent alongside the grant
            val consent = DataGateConsentGiven(
                subjectId = subjectId,
                purposeId = req.purposeId,
                grantId = grantId,
                consentAtEpochMs = now,
            )
            eventStore.append(listOf(consent, grant), condition = null)
            if (req.articleId != null) service.invalidateGrantCache(req.subjectId, req.articleId)
            call.respond(HttpStatusCode.Created,
                mapOf("grantId" to grantId.value, "expiresAtEpochMs" to (now + grantTtlMs).toString()))
        }
        // UP-01/01a: outbound CEP offer request via OfferService (guardrails + logging).
        // The mock CEP returns a fixed configurable offer; replace with a real HTTP client in prod.
        post("/api/v1/offers/request") {
            val request = call.receive<DecideRequest>()
            val userId = jwtValidator?.userIdFrom(call.request.headers[HttpHeaders.Authorization])
            val subject = Subject(VisitorId(request.visitorId), userId)
            val trigger = call.request.queryParameters["trigger"] ?: "gate_shown"
            val channel = request.channel
            // UP-10/11: the checkout flow passes currentPlanId so the CEP can return
            // an annual-upsell (UP-10) or tier-upsell (UP-11) offer at checkout.
            val currentPlanId = call.request.queryParameters["currentPlanId"]
            if (offerService == null) {
                call.respond(OfferResponse(offerId = null, kind = null))
                return@post
            }
            // UP-06: pass the variant so the CEP can tailor offer strategies per A/B arm.
            val ctx = OfferService.TriggerContext(
                trigger = trigger,
                channel = channel,
                currentPlanId = currentPlanId,
                variant = service.variantFor(subject),
            )
            val decision = offerService.decideOffer(subject, ctx)
            val offer = (decision as? OfferService.OfferDecision.Triggered)?.offer
            call.respond(OfferResponse(
                offerId = offer?.offerId,
                kind = offer?.kind,
                discountPercent = offer?.discountPercent,
                validForSeconds = offer?.validForSeconds,
                cta = offer?.cta,
            ))
        }
        // UP-05: record an explicit offer decline so frequency capping suppresses
        // the same offer_id for 30 days across all channels (cross-channel cap).
        post("/api/v1/offers/decline") {
            val userId = jwtValidator?.userIdFrom(call.request.headers[HttpHeaders.Authorization])
            val visitorId = call.request.queryParameters["visitorId"]
                ?: call.request.headers["X-Visitor-Id"]
            if (visitorId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "visitorId is required"))
                return@post
            }
            val offerId = call.request.queryParameters["offerId"]
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "offerId is required"))
                    return@post
                }
            val channel = call.request.queryParameters["channel"] ?: "web"
            val subject = Subject(VisitorId(visitorId), userId)
            val subjectId = userId?.let { nl.incedo.paywall.core.SubjectId.of(it) }
                ?: nl.incedo.paywall.core.SubjectId.of(VisitorId(visitorId))
            val event = OfferDeclined(
                subjectId = subjectId,
                offerId = offerId,
                channel = channel,
                declinedAtEpochMs = System.currentTimeMillis(),
            )
            eventStore.append(listOf(event), condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to "OfferDeclined"))
        }
        // DN-05/DN-06: record an accepted offer for retention cap tracking and
        // conversion analytics. Retention kinds (downsell/discount/pause) count against
        // the rolling 12-month cap enforced in OfferService.
        post("/api/v1/offers/accept") {
            val userId = jwtValidator?.userIdFrom(call.request.headers[HttpHeaders.Authorization])
            val visitorId = call.request.queryParameters["visitorId"]
                ?: call.request.headers["X-Visitor-Id"]
            if (visitorId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "visitorId is required"))
                return@post
            }
            val offerId = call.request.queryParameters["offerId"]
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "offerId is required"))
                    return@post
                }
            val kind = call.request.queryParameters["kind"] ?: "unknown"
            val channel = call.request.queryParameters["channel"] ?: "web"
            val subjectId = userId?.let { nl.incedo.paywall.core.SubjectId.of(it) }
                ?: nl.incedo.paywall.core.SubjectId.of(VisitorId(visitorId))
            val acceptedAt = System.currentTimeMillis()
            val events = mutableListOf<nl.incedo.paywall.core.DomainEvent>(
                OfferAccepted(
                    subjectId = subjectId,
                    offerId = offerId,
                    kind = kind,
                    channel = channel,
                    acceptedAtEpochMs = acceptedAt,
                ),
            )
            // FGA-07: access_grant kind automatically issues a grant on accept.
            // articleId and grantTtlSeconds must be supplied; grantedBy defaults to "cep_offer".
            if (kind == "access_grant") {
                val articleId = call.request.queryParameters["articleId"]
                val grantTtlSeconds = call.request.queryParameters["grantTtlSeconds"]?.toLongOrNull()
                    ?: 72L * 3600 // default 72 h
                val grantedBy = call.request.queryParameters["grantedBy"] ?: "cep_offer"
                if (articleId != null) {
                    events += GrantIssued(
                        grantId = GrantId("cep-$offerId"),
                        subjectId = subjectId,
                        articleId = ArticleId(articleId),
                        grantedBy = grantedBy,
                        expiresAtEpochMs = acceptedAt + grantTtlSeconds * 1_000,
                    )
                }
            }
            eventStore.append(events, condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to "OfferAccepted"))
        }
        // UP-08: CEP-initiated offer push for async channels (email, chat).
        // The CEP pushes offers here instead of being called synchronously;
        // the paywall validates (UP-09), frequency-caps (UP-05), and logs
        // OfferTriggered with trigger="cep_push" so the async channel can
        // later fetch it as a pending offer.
        post("/api/v1/integration/cep-offers") {
            val rawBody = call.receive<ByteArray>()
            if (!webhookVerifier.verify(rawBody, call.request.headers[WebhookVerifier.SIGNATURE_HEADER])) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid webhook signature (API-08)"))
                return@post
            }
            val req = kotlinx.serialization.json.Json.decodeFromString<CepOfferPushRequest>(rawBody.decodeToString())
            if (req.subjectId.isBlank() || req.offerId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "subjectId and offerId are required"))
                return@post
            }
            val subjectId = SubjectId(req.subjectId)
            val offer = nl.incedo.paywall.cep.Offer(
                offerId = req.offerId,
                kind = req.kind,
                fromPlanId = req.fromPlanId,
                toPlanId = req.toPlanId,
                discountPercent = req.discountPercent,
                validForSeconds = req.validForSeconds,
                pauseMonths = req.pauseMonths,
                channels = req.channels,
                source = req.source,
                cta = req.cta,
            )
            when (val result = offerService?.receiveAsyncOffer(subjectId, offer, req.channel)) {
                is OfferService.OfferDecision.Triggered ->
                    call.respond(HttpStatusCode.Accepted, mapOf("offerId" to result.offer.offerId))
                is OfferService.OfferDecision.Suppressed ->
                    call.respond(HttpStatusCode.UnprocessableEntity, mapOf("suppressed" to result.reason))
                null -> call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "offer service not configured"))
            }
        }
        // UP-08: query pending CEP-pushed offers for a subject, optionally filtered
        // by channel. An offer is pending when it has been triggered (cep_push) but
        // not yet accepted or declined by the subject.
        get("/api/v1/subjects/{subjectId}/offers/pending") {
            val subjectId = call.parameters["subjectId"]?.takeIf { it.isNotBlank() } ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "subjectId is required"))
                return@get
            }
            val channel = call.request.queryParameters["channel"]
            val events = eventStore.query(EventQuery(setOf("subject:$subjectId"))).events
            val triggered = events.filterIsInstance<nl.incedo.paywall.offers.OfferTriggered>()
                .filter { it.trigger == "cep_push" }
                .filter { channel == null || it.channel == channel }
            val settled = (
                events.filterIsInstance<OfferAccepted>().map { it.offerId } +
                    events.filterIsInstance<OfferDeclined>().map { it.offerId }
            ).toSet()
            val pending = triggered
                .filter { it.offerId !in settled }
                .sortedByDescending { it.triggeredAtEpochMs }
                .map { PendingOfferResponse(it.offerId, it.kind, it.channel, it.triggeredAtEpochMs) }
            call.respond(pending)
        }
        // BP-05: subscriber-generated signed share tokens that redeem into FGA grants.
        // POST /api/v1/articles/{id}/share — authenticated subscriber issues a token (≤5/month).
        post("/api/v1/articles/{id}/share") {
            if (shareTokenService == null) {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "share tokens not configured"))
                return@post
            }
            val userId = jwtValidator?.userIdFrom(call.request.headers[HttpHeaders.Authorization]) ?: run {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "authentication required (BP-05)"))
                return@post
            }
            val articleId = call.parameters["id"]?.let { ArticleId(it) } ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "articleId is required"))
                return@post
            }
            val subscriberSubjectId = SubjectId("user:$userId")
            val monthPeriod = YearMonth.now(ZoneId.of("Europe/Amsterdam")).toString()
            when (val result = shareTokenService.issue(subscriberSubjectId, articleId, monthPeriod)) {
                is ShareTokenService.IssueResult.Success -> {
                    val capTag = nl.incedo.paywall.grants.shareMonthTag(subscriberSubjectId, monthPeriod)
                    val used = eventStore.query(EventQuery(setOf(capTag))).events
                        .filterIsInstance<nl.incedo.paywall.grants.ShareTokenIssued>().size
                    call.respond(ShareTokenResponse(
                        token = result.issued.token,
                        expiresAtEpochMs = result.issued.expiresAtEpochMs,
                        remainingThisMonth = maxOf(0, SHARE_TOKEN_MONTHLY_CAP - used),
                    ))
                }
                ShareTokenService.IssueResult.CapExceeded ->
                    call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "monthly share token cap reached (BP-05)"))
                ShareTokenService.IssueResult.NotEntitled ->
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "active subscription required to share"))
            }
        }
        // BP-05: redeem a share token — anyone (including anonymous visitors) may redeem.
        post("/api/v1/shares/redeem") {
            if (shareTokenService == null) {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "share tokens not configured"))
                return@post
            }
            val request = call.receive<RedeemShareTokenRequest>()
            if (request.visitorId.isBlank() || request.token.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "visitorId and token are required"))
                return@post
            }
            val visitorSubjectId = SubjectId("visitor:${request.visitorId}")
            when (val result = shareTokenService.redeem(request.token, visitorSubjectId)) {
                is ShareTokenService.RedeemResult.Success ->
                    call.respond(HttpStatusCode.OK, mapOf("grantId" to result.grantId))
                ShareTokenService.RedeemResult.InvalidToken ->
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid share token"))
                ShareTokenService.RedeemResult.Expired ->
                    call.respond(HttpStatusCode.Gone, mapOf("error" to "share token has expired"))
            }
        }
        // ── Brand management (ADM-10) ────────────────────────────────────────
        // ADM-10: create a brand (theme tokens, domain, locale).
        post("/api/v1/admin/brands") {
            call.requireStaff(jwtValidator, StaffRole.ADMIN) ?: return@post
            val req = call.receive<CreateBrandRequest>()
            if (req.brandId.isBlank() || req.name.isBlank() || req.domain.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "brandId, name and domain are required"))
                return@post
            }
            val event = BrandCreated(
                brandId = BrandId(req.brandId),
                name = req.name,
                domain = req.domain,
                locale = req.locale,
                themeJson = req.themeJson,
                createdAtEpochMs = System.currentTimeMillis(),
            )
            eventStore.append(listOf(event), condition = null)
            call.respond(HttpStatusCode.Created, mapOf("brandId" to req.brandId))
        }
        // ADM-10: update theme tokens for a brand.
        post("/api/v1/admin/brands/{brandId}/theme") {
            call.requireStaff(jwtValidator, StaffRole.ADMIN) ?: return@post
            val brandId = call.parameters["brandId"]?.takeIf { it.isNotBlank() } ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "brandId required"))
                return@post
            }
            val req = call.receive<UpdateBrandThemeRequest>()
            val bId = BrandId(brandId)
            val bEvents = eventStore.query(EventQuery(setOf(brandTag(bId)))).events
            val brand = BrandDecision().also { it.applyAll(bEvents) }
            if (!brand.exists) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "brand not found"))
                return@post
            }
            val event = BrandThemeUpdated(bId, req.themeJson, req.actor, System.currentTimeMillis())
            eventStore.append(listOf(event), condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to "BrandThemeUpdated"))
        }
        // ADM-10: get brand state.
        get("/api/v1/admin/brands/{brandId}") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            val brandId = call.parameters["brandId"]?.takeIf { it.isNotBlank() } ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "brandId required"))
                return@get
            }
            val bId = BrandId(brandId)
            val bEvents = eventStore.query(EventQuery(setOf(brandTag(bId)))).events
            val brand = BrandDecision().also { it.applyAll(bEvents) }
            if (!brand.exists) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "brand not found"))
                return@get
            }
            call.respond(BrandResponse(brandId, brand.name, brand.domain, brand.locale, brand.themeJson))
        }
        // ADM-10: list all brands (for the admin console brand picker).
        get("/api/v1/admin/brands") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            val events = eventStore.query(EventQuery(setOf("brands"))).events
            val byBrand = mutableMapOf<String, BrandDecision>()
            events.forEach { ev ->
                val id = when (ev) {
                    is BrandCreated -> ev.brandId.value
                    is BrandThemeUpdated -> ev.brandId.value
                    else -> return@forEach
                }
                byBrand.getOrPut(id) { BrandDecision() }.apply(ev)
            }
            call.respond(byBrand.entries
                .filter { it.value.exists }
                .map { (id, b) -> BrandResponse(id, b.name, b.domain, b.locale, b.themeJson) }
                .sortedBy { it.brandId })
        }
        // PAY-01/01a: plan catalogue — two tiers × two billing periods, with ranks
        // for upsell/downsell logic. Configuration-driven; no auth required (public).
        get("/api/v1/plans") {
            call.respond(DefaultPlans.all.map { p ->
                PlanResponse(p.planId.value, p.tier, p.billingPeriod, p.rank, p.displayName, p.priceMinorUnits, p.currency)
            })
        }
        // ── Partner management (PA-01/02/03/05 / IPW-01) ─────────────────────
        // PA-01: create partner with optional seat cap and plan tier.
        post("/api/v1/admin/partners") {
            call.requireStaff(jwtValidator, StaffRole.ADMIN) ?: return@post
            val req = call.receive<CreatePartnerRequest>()
            if (req.partnerId.isBlank() || req.name.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "partnerId and name are required"))
                return@post
            }
            val event = PartnerCreated(
                partnerId = PartnerId(req.partnerId),
                name = req.name,
                maxSeats = req.maxSeats,
                planId = req.planId,
                createdAtEpochMs = System.currentTimeMillis(),
            )
            eventStore.append(listOf(event), condition = null)
            call.respond(HttpStatusCode.Created, mapOf("partnerId" to req.partnerId))
        }
        // PA-05: add a member to a partner; enforces seat limit.
        post("/api/v1/admin/partners/{partnerId}/members") {
            call.requireStaff(jwtValidator, StaffRole.OPERATOR) ?: return@post
            val partnerId = call.parameters["partnerId"]?.takeIf { it.isNotBlank() } ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "partnerId required"))
                return@post
            }
            val req = call.receive<AddPartnerMemberRequest>()
            if (req.subjectId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "subjectId is required"))
                return@post
            }
            val pId = PartnerId(partnerId)
            val pEvents = eventStore.query(EventQuery(setOf(partnerTag(pId)))).events
            val partner = PartnerDecision().also { it.applyAll(pEvents) }
            if (partner.name.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "partner not found"))
                return@post
            }
            if (!partner.hasSeatAvailable()) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "partner seat limit reached (PA-05)"))
                return@post
            }
            val event = PartnerMemberAdded(
                partnerId = pId,
                subjectId = SubjectId(req.subjectId),
                addedBy = req.addedBy,
                addedAtEpochMs = System.currentTimeMillis(),
            )
            eventStore.append(listOf(event), condition = null)
            call.respond(HttpStatusCode.Created, mapOf("recorded" to "PartnerMemberAdded"))
        }
        // PA-03: remove a member from a partner (freeing the seat).
        post("/api/v1/admin/partners/{partnerId}/members/remove") {
            call.requireStaff(jwtValidator, StaffRole.OPERATOR) ?: return@post
            val partnerId = call.parameters["partnerId"]?.takeIf { it.isNotBlank() } ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "partnerId required"))
                return@post
            }
            val req = call.receive<AddPartnerMemberRequest>()
            val event = PartnerMemberRemoved(
                partnerId = PartnerId(partnerId),
                subjectId = SubjectId(req.subjectId),
                removedBy = req.addedBy,
                removedAtEpochMs = System.currentTimeMillis(),
            )
            eventStore.append(listOf(event), condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to "PartnerMemberRemoved"))
        }
        // IPW-01: configure an IP CIDR range for a partner.
        post("/api/v1/admin/partners/{partnerId}/ip-ranges") {
            call.requireStaff(jwtValidator, StaffRole.ADMIN) ?: return@post
            val partnerId = call.parameters["partnerId"]?.takeIf { it.isNotBlank() } ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "partnerId required"))
                return@post
            }
            val req = call.receive<PartnerIpRangeRequest>()
            if (req.cidr.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "cidr is required"))
                return@post
            }
            // IPW-03: validate CIDR format before storing.
            CidrValidator.validate(req.cidr)?.let { err ->
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid CIDR: $err"))
                return@post
            }
            // IPW-03: reject overlapping CIDRs with existing active ranges for this partner.
            if (req.active) {
                val existingEvents = eventStore.query(EventQuery(setOf("partner:ip-ranges"))).events
                val activeCidrs = mutableListOf<String>()
                existingEvents.filterIsInstance<PartnerIpRangeConfigured>()
                    .filter { it.partnerId.value == partnerId }
                    .forEach { ev -> if (ev.active) activeCidrs.add(ev.cidr) else activeCidrs.remove(ev.cidr) }
                CidrValidator.overlaps(req.cidr, activeCidrs)?.let { err ->
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "IPW-03: $err"))
                    return@post
                }
            }
            val event = PartnerIpRangeConfigured(
                partnerId = PartnerId(partnerId),
                cidr = req.cidr,
                active = req.active,
                configuredAtEpochMs = System.currentTimeMillis(),
            )
            eventStore.append(listOf(event), condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to "PartnerIpRangeConfigured"))
        }
        // PA-03: partner offboarding — transitively revokes all member access immediately.
        // PartnerDecision.isActive becomes false; the decide path stops granting partner access.
        post("/api/v1/admin/partners/{partnerId}/offboard") {
            val staff = call.requireStaff(jwtValidator, StaffRole.ADMIN) ?: return@post
            val partnerId = call.parameters["partnerId"]?.let { PartnerId(it) } ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "partnerId is required"))
                return@post
            }
            val pEvents = eventStore.query(EventQuery(setOf(partnerTag(partnerId)))).events
            val partner = PartnerDecision().also { it.applyAll(pEvents) }
            if (partner.name.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "partner not found"))
                return@post
            }
            if (!partner.isActive) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "partner already offboarded"))
                return@post
            }
            val event = PartnerOffboarded(
                partnerId = partnerId,
                offboardedBy = staff.userId.value,
                offboardedAtEpochMs = System.currentTimeMillis(),
            )
            eventStore.append(listOf(event), condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to "PartnerOffboarded", "partnerId" to partnerId.value))
        }
        // IPW-01: the edge polls this endpoint to build its CIDR → partner_id map.
        // No auth required from the edge (protected by origin secret at the intercept level).
        get("/api/v1/admin/ip-allowlist") {
            val events = eventStore.query(EventQuery(setOf("partner:ip-ranges"))).events
            val byPartner = mutableMapOf<String, MutableList<String>>()
            events.filterIsInstance<PartnerIpRangeConfigured>().forEach { ev ->
                val list = byPartner.getOrPut(ev.partnerId.value) { mutableListOf() }
                if (ev.active) list.add(ev.cidr) else list.remove(ev.cidr)
            }
            val response = byPartner.map { (partnerId, cidrs) -> IpAllowlistEntry(partnerId, cidrs.sorted()) }
            call.respond(response)
        }
        // PA-01: get partner summary (for admin console).
        get("/api/v1/admin/partners/{partnerId}") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            val partnerId = call.parameters["partnerId"]?.takeIf { it.isNotBlank() } ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "partnerId required"))
                return@get
            }
            val pId = PartnerId(partnerId)
            val pEvents = eventStore.query(EventQuery(setOf(partnerTag(pId)))).events
            val partner = PartnerDecision().also { it.applyAll(pEvents) }
            if (partner.name.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "partner not found"))
                return@get
            }
            call.respond(PartnerResponse(
                partnerId = partnerId,
                name = partner.name,
                maxSeats = partner.maxSeats,
                activeSeats = partner.activeSeatCount(),
                planId = partner.planId,
                activeCidrs = partner.activeCidrs(),
            ))
        }
    }
}
