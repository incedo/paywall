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
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import nl.incedo.paywall.api.AddPartnerMemberRequest
import nl.incedo.paywall.api.BrandResponse
import nl.incedo.paywall.api.BypassRateResponse
import nl.incedo.paywall.api.CiamSession
import nl.incedo.paywall.api.CohortStatsResponse
import nl.incedo.paywall.api.ExperimentConfigVersionSummary
import nl.incedo.paywall.api.OfferChannelStatsResponse
import nl.incedo.paywall.api.OfferStatsResponse
import nl.incedo.paywall.api.PartnerUsageResponse
import nl.incedo.paywall.api.CreateBrandRequest
import nl.incedo.paywall.api.CreatePartnerRequest
import nl.incedo.paywall.api.ExperimentConfigResponse
import nl.incedo.paywall.api.PartnerIpRangeRequest
import nl.incedo.paywall.api.PartnerResponse
import nl.incedo.paywall.api.GrantAuditEntry
import nl.incedo.paywall.api.GrantChangeRequest
import nl.incedo.paywall.api.InspectorWallEvent
import nl.incedo.paywall.api.MeterResetRequest
import nl.incedo.paywall.api.PublishExperimentConfigRequest
import nl.incedo.paywall.api.SaveWallRequest
import nl.incedo.paywall.api.SubjectInspectorResponse
import nl.incedo.paywall.api.UpdateBrandThemeRequest
import nl.incedo.paywall.api.UpdateScenarioRequest
import nl.incedo.paywall.api.UpdateStoryRequest
import nl.incedo.paywall.api.VariantStatsResponse
import nl.incedo.paywall.api.WallResponse
import nl.incedo.paywall.api.WallTemplateRequest
import nl.incedo.paywall.api.WallTemplateResponse
import nl.incedo.paywall.api.WallVersionSummary
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
import nl.incedo.paywall.entitlements.CancellationSurveySubmitted
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
import nl.incedo.paywall.walls.WallConfigChanged
import nl.incedo.paywall.walls.WallCreated
import nl.incedo.paywall.walls.WallPublished
import nl.incedo.paywall.walls.WallTemplateCreated
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
import nl.incedo.paywall.experiments.KILL_SWITCH_TAG
import nl.incedo.paywall.experiments.VariantKilled
import nl.incedo.paywall.experiments.VariantRestored
import nl.incedo.paywall.notifications.MailSent
import nl.incedo.paywall.api.AddControlRequest
import nl.incedo.paywall.api.ChangeControlDefaultRequest
import nl.incedo.paywall.api.ControlSchemaResponse
import nl.incedo.paywall.api.DecoratorResponse
import nl.incedo.paywall.api.RegisterControlSchemaRequest
import nl.incedo.paywall.api.RegisterDecoratorRequest
import nl.incedo.paywall.api.RegisterScenarioRequest
import nl.incedo.paywall.api.RegisterStoryRequest
import nl.incedo.paywall.api.ScenarioResponse
import nl.incedo.paywall.api.StoryResponse
import nl.incedo.paywall.api.toResponse
import nl.incedo.paywall.api.UpdateDecoratorPriorityRequest
import nl.incedo.paywall.storybook.ControlAdded
import nl.incedo.paywall.storybook.ControlDefaultChanged
import nl.incedo.paywall.storybook.ControlId
import nl.incedo.paywall.storybook.ControlKey
import nl.incedo.paywall.storybook.ControlRemoved
import nl.incedo.paywall.storybook.ControlSchemaDecisionModel
import nl.incedo.paywall.storybook.ControlSchemaId
import nl.incedo.paywall.storybook.ControlSchemaRegistered
import nl.incedo.paywall.storybook.ControlType
import nl.incedo.paywall.storybook.ScenarioArchived
import nl.incedo.paywall.storybook.ScenarioDecision
import nl.incedo.paywall.storybook.ScenarioMetadataUpdated
import nl.incedo.paywall.storybook.ScenarioId
import nl.incedo.paywall.storybook.ScenarioKey
import nl.incedo.paywall.storybook.ScenarioLifecycle
import nl.incedo.paywall.storybook.ScenarioRegistered
import nl.incedo.paywall.storybook.ScenarioType
import nl.incedo.paywall.storybook.StoryArchived
import nl.incedo.paywall.storybook.StoryDecision
import nl.incedo.paywall.storybook.StoryMetadataUpdated
import nl.incedo.paywall.storybook.StoryId
import nl.incedo.paywall.storybook.StoryKey
import nl.incedo.paywall.storybook.StoryKeyUniquenessDecision
import nl.incedo.paywall.storybook.StoryLifecycle
import nl.incedo.paywall.storybook.StoryRegistered
import nl.incedo.paywall.storybook.StoryType
import nl.incedo.paywall.storybook.controlSchemaTag
import nl.incedo.paywall.storybook.controlTag
import nl.incedo.paywall.storybook.scenarioKeyTag
import nl.incedo.paywall.storybook.scenarioTag
import nl.incedo.paywall.storybook.storyKeyTag
import nl.incedo.paywall.storybook.storyTag
import nl.incedo.paywall.storybook.DecoratorArchived
import nl.incedo.paywall.storybook.DecoratorDecision
import nl.incedo.paywall.storybook.DecoratorId
import nl.incedo.paywall.storybook.DecoratorKey
import nl.incedo.paywall.storybook.DecoratorKeyUniquenessDecision
import nl.incedo.paywall.storybook.DecoratorLifecycle
import nl.incedo.paywall.storybook.DecoratorLinkedToScenario
import nl.incedo.paywall.storybook.DecoratorLinkedToStory
import nl.incedo.paywall.storybook.DecoratorMetadataUpdated
import nl.incedo.paywall.storybook.DecoratorPriorityChanged
import nl.incedo.paywall.storybook.DecoratorRegistered
import nl.incedo.paywall.storybook.DecoratorScope
import nl.incedo.paywall.storybook.DecoratorType
import nl.incedo.paywall.storybook.decoratorKeyTag
import nl.incedo.paywall.storybook.decoratorTag

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
    imageUrl = config.imageUrl, // ADM-11
    imageAlt = config.imageAlt, // ADM-17
    legalText = config.legalText, // ADM-11
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

/** MT-06/UP-13: calendar period one month before [current]. */
fun previousPeriod(current: nl.incedo.paywall.metering.MeterPeriod): nl.incedo.paywall.metering.MeterPeriod {
    val (year, month) = current.value.split("-").map { it.toInt() }
    return if (month == 1) nl.incedo.paywall.metering.MeterPeriod("${year - 1}-12")
    else nl.incedo.paywall.metering.MeterPeriod("$year-${(month - 1).toString().padStart(2, '0')}")
}

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
    // CIAM_AUDIENCE=paywall (NFR-20: aud claim must contain this value when set)
    val jwtValidator = System.getenv("CIAM_JWKS_URL")?.let { jwks ->
        CiamJwtValidator.fromJwksUrl(
            jwksUrl = jwks,
            issuer = System.getenv("CIAM_ISSUER") ?: jwks.substringBefore(".well-known").trimEnd('/') + "/",
            audience = System.getenv("CIAM_AUDIENCE"), // NFR-20: null = skip aud check (dev/test)
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

    // ADM-04: Ory Kratos admin URL for session lookup; absent in dev (mock returns empty list).
    val ciamSessionClient: CiamSessionClient = System.getenv("KRATOS_ADMIN_URL")
        ?.let { KratosCiamSessionClient(it) } ?: MockCiamSessionClient()

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
            ciamSessionClient = ciamSessionClient,
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
    /** PAY-05: payment provider for checkout; null = mock (tests/dev). */
    paymentProvider: PaymentProvider = MockPaymentProvider(),
    /** ADM-04: CIAM session lookup for subject inspector; mock = empty list (tests/dev). */
    ciamSessionClient: CiamSessionClient = MockCiamSessionClient(),
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
    // NFR-15: in-process cache of killed variant names. Reloaded from the event
    // store on every admin kill/restore operation; staleness after a crash
    // is bounded by the next request that repopulates it via loadKilledVariants().
    val killedVariantsCache = java.util.concurrent.atomic.AtomicReference(emptySet<String>())

    suspend fun loadKilledVariants(): Set<String> {
        val events = eventStore.query(EventQuery(setOf(KILL_SWITCH_TAG))).events
        return events.fold(mutableSetOf<String>()) { acc, e ->
            when (e) {
                is VariantKilled -> acc.also { it.add(e.variantName) }
                is VariantRestored -> acc.also { it.remove(e.variantName) }
                else -> acc
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
            // INF-09: Cloudflare bot management score forwarded by the Worker as a
            // trusted header. Only read when origin secret is configured (INF-02).
            val cfBotScore = if (originSecret != null)
                parseCfBotScore(call.request.headers["X-CF-Bot-Score"]) else null
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
                // INF-09: CF bot score supplements UA-based detection (either signal flags as bot).
                isBot = isBotUserAgent(call.request.headers[HttpHeaders.UserAgent]) || isBotByCfScore(cfBotScore),
                isSuspicious = service.recordIpAndCheckSuspicious(subject, clientIp),
                isVerifiedCrawler = isVerifiedCrawler,
                forceVariant = forceVariant,
                correlationId = correlationId,
                externalScore = request.externalScore, // DY-06
                killedVariants = killedVariantsCache.get(), // NFR-15
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
            // INF-09: Cloudflare bot management score (trusted when origin secret present).
            val cfBotScoreArticle = if (originSecret != null)
                parseCfBotScore(call.request.headers["X-CF-Bot-Score"]) else null
            val outcome = service.decide(
                subject = subject,
                article = Article(stored.id, stored.tier),
                isBot = isBotUserAgent(call.request.headers[HttpHeaders.UserAgent]) || isBotByCfScore(cfBotScoreArticle),
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
            val variant = VariantAssigner.assign(subject, service.currentExperiment()) // EX-03
            val nowMs = System.currentTimeMillis()
            val eventsToAppend = mutableListOf<DomainEvent>(
                WallEventRecorded(
                    eventType = type,
                    subjectId = subject.subjectId,
                    variant = variant.name,
                    channel = request.channel,
                    occurredAtEpochMs = nowMs,
                    articleId = request.articleId?.let(::ArticleId),
                    context = request.context,
                ),
            )
            // US-10: purchase confirmation email logged on checkout_complete.
            if (type == WallEventType.CHECKOUT_COMPLETE) {
                eventsToAppend += MailSent(
                    subjectId = subject.subjectId,
                    kind = "purchase_confirmation",
                    sentAtEpochMs = nowMs,
                )
            }
            eventStore.append(eventsToAppend, condition = null)
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
                imageUrl = request.imageUrl, // ADM-11: optional image block
                imageAlt = request.imageAlt, // ADM-17: alt text for WCAG 2.1 AA
                legalText = request.legalText, // ADM-11: optional legal text block
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
        // ADM-06: config version history — each ExperimentConfigPublished event is one entry.
        // Returned newest-first; version numbers are 1-indexed from oldest (same semantics
        // as wall history so the console rollback picker works identically).
        get("/api/v1/admin/config/history") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            val events = eventStore.query(EventQuery(setOf("config:experiment"))).events
                .filterIsInstance<ExperimentConfigPublished>()
            val history = events.mapIndexed { index, ev ->
                ExperimentConfigVersionSummary(
                    version = index + 1,
                    actor = ev.actor,
                    publishedAtEpochMs = ev.publishedAtEpochMs,
                    variantCount = ev.experiment.variants.size,
                    variantNames = ev.experiment.variants.joinToString(", ") { it.name },
                )
            }
            call.respond(history.asReversed())
        }
        // ADM-06: rollback to a previous config version — re-publishes the config at
        // position `version` as a new event (history is never mutated, event-sourcing style).
        post("/api/v1/admin/config/rollback") {
            val staff = call.requireStaff(jwtValidator, StaffRole.ADMIN) ?: return@post
            val targetVersion = call.request.queryParameters["version"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "version query param required"))
            val events = eventStore.query(EventQuery(setOf("config:experiment"))).events
                .filterIsInstance<ExperimentConfigPublished>()
            val target = events.getOrNull(targetVersion - 1)
                ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "version $targetVersion not found in config history (ADM-06)"))
            val now = System.currentTimeMillis()
            eventStore.append(
                listOf(ExperimentConfigPublished(
                    experiment = target.experiment,
                    actor = staff.userId.value,
                    publishedAtEpochMs = now,
                )),
                condition = null,
            )
            call.respond(HttpStatusCode.Accepted, mapOf("rolledBackTo" to targetVersion.toString(), "actor" to staff.userId.value))
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
        // ADM-13: version history for the rollback picker in the wall editor console.
        // Returns one entry per config-change and publish event, newest first.
        get("/api/v1/walls/{id}/history") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            val id = call.parameters["id"]?.takeIf { it.isNotBlank() }
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "wall id required"))
            val events = eventStore.query(EventQuery(setOf("wall:$id"))).events
            val history = mutableListOf<WallVersionSummary>()
            var version = 0
            var status = "draft"
            events.forEach { ev ->
                when (ev) {
                    is WallCreated -> {
                        version = 1; status = "draft"
                        history.add(WallVersionSummary(version, status, ev.actor))
                    }
                    is WallConfigChanged -> {
                        version++; status = "draft"
                        history.add(WallVersionSummary(version, status, ev.actor))
                    }
                    is WallPublished -> {
                        status = "published"
                        history.removeLastOrNull()
                        history.add(WallVersionSummary(version, status, ev.actor))
                    }
                }
            }
            call.respond(history.asReversed())
        }
        // ADM-16: wall design templates — brand-neutral layout/copy that can be
        // instantiated for any brand. Theme tokens come from the target brand at
        // render time; they are NOT stored in the template.
        get("/api/v1/admin/wall-templates") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            val events = eventStore.query(EventQuery(setOf("wall-templates"))).events
            val templates = events
                .filterIsInstance<WallTemplateCreated>()
                .groupBy { it.templateId }
                .mapValues { (_, evts) -> evts.last() }
                .values
                .sortedBy { it.templateId }
            call.respond(templates.map { e ->
                WallTemplateResponse(
                    id = e.templateId, name = e.name,
                    wallType = e.config.wallType, title = e.config.title,
                    body = e.config.body, primaryCta = e.config.primaryCta,
                    secondaryCta = e.config.secondaryCta, channels = e.config.channels,
                    translations = e.config.translations, createdBy = e.actor,
                )
            })
        }
        get("/api/v1/admin/wall-templates/{id}") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            val id = call.parameters["id"]?.takeIf { it.isNotBlank() }
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "template id required"))
            val events = eventStore.query(EventQuery(setOf("wall-template:$id"))).events
            val event = events.filterIsInstance<WallTemplateCreated>().lastOrNull()
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "unknown template"))
            call.respond(
                WallTemplateResponse(
                    id = event.templateId, name = event.name,
                    wallType = event.config.wallType, title = event.config.title,
                    body = event.config.body, primaryCta = event.config.primaryCta,
                    secondaryCta = event.config.secondaryCta, channels = event.config.channels,
                    translations = event.config.translations, createdBy = event.actor,
                ),
            )
        }
        post("/api/v1/admin/wall-templates/{id}") {
            val staff = call.requireStaff(jwtValidator, StaffRole.OPERATOR) ?: return@post
            val id = call.parameters["id"]?.takeIf { it.isNotBlank() }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "template id required"))
            val request = call.receive<WallTemplateRequest>()
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
                brandId = null, // templates are always brand-neutral (ADM-16)
                imageUrl = request.imageUrl, // ADM-11
                imageAlt = request.imageAlt, // ADM-17
                legalText = request.legalText, // ADM-11
                translations = request.translations,
            )
            val event = WallTemplateCreated(
                templateId = id, name = request.name, config = config, actor = staff.userId.value,
            )
            eventStore.append(listOf(event), condition = null)
            call.respond(
                WallTemplateResponse(
                    id = id, name = request.name,
                    wallType = config.wallType, title = config.title,
                    body = config.body, primaryCta = config.primaryCta,
                    secondaryCta = config.secondaryCta, channels = config.channels,
                    imageUrl = config.imageUrl, imageAlt = config.imageAlt,
                    legalText = config.legalText,
                    translations = config.translations, createdBy = staff.userId.value,
                ),
            )
        }
        // ADM-16: instantiate a template for a brand — copies template config,
        // sets brandId to the target brand, creates a new wall via WallService.
        post("/api/v1/walls/{wallId}/from-template/{templateId}") {
            val staff = call.requireStaff(jwtValidator, StaffRole.OPERATOR) ?: return@post
            val wallId = call.parameters["wallId"]?.takeIf { it.isNotBlank() }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "wall id required"))
            val templateId = call.parameters["templateId"]?.takeIf { it.isNotBlank() }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "template id required"))
            val brandId = call.request.queryParameters["brandId"]?.takeIf { it.isNotBlank() }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "brandId query parameter required"))
            val templateEvents = eventStore.query(EventQuery(setOf("wall-template:$templateId"))).events
            val templateEvent = templateEvents.filterIsInstance<WallTemplateCreated>().lastOrNull()
                ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "unknown template"))
            val config = templateEvent.config.copy(brandId = brandId)
            val result = wallService.create(WallId(wallId), config, actor = staff.userId.value)
            call.respondSaveResult(result)
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
            // ADM-04: fetch active CIAM sessions for user subjects (read-only from Ory Kratos).
            val sessions = if (subjectId.value.startsWith("user:")) {
                ciamSessionClient.activeSessions(subjectId.value.removePrefix("user:"))
            } else emptyList()
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
                    sessions = sessions, // ADM-04
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
                        reason = g.reason, // FGA-01
                        expiresAtEpochMs = g.expiresAtEpochMs,
                        isLive = g.grantId.value !in revoked &&
                            (g.expiresAtEpochMs?.let { it > now } ?: true),
                    )
                }
            call.respond(entries)
        }
        // PR-03: profile completeness — consent records per subject (AN-20).
        // Returns the set of purposeIds the subject has consented to via data-gate
        // interactions, with timestamps. Used by the CEP for segmentation.
        get("/api/v1/subjects/{subjectId}/profile") {
            val subjectParam = call.parameters["subjectId"]?.takeIf { it.isNotBlank() } ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "subjectId required"))
                return@get
            }
            val events = eventStore.query(EventQuery(setOf("subject:$subjectParam"))).events
            // Collect all consented purposes; last consent for the same purposeId wins.
            val consentedPurposes = mutableMapOf<String, Long>()
            events.filterIsInstance<DataGateConsentGiven>().forEach { consent ->
                val existing = consentedPurposes[consent.purposeId]
                if (existing == null || consent.consentAtEpochMs > existing) {
                    consentedPurposes[consent.purposeId] = consent.consentAtEpochMs
                }
            }
            // Completeness score: how many distinct purposes this subject has given consent for.
            // Total = 3 known purposes in experiment phase (newsletter, survey, ad_profile).
            val knownPurposes = 3
            val completenessScore = if (knownPurposes > 0)
                consentedPurposes.size.toDouble() / knownPurposes else 0.0
            call.respond(ProfileCompletenessResponse(
                subjectId = subjectParam,
                consentedPurposes = consentedPurposes,
                completenessScore = completenessScore.coerceIn(0.0, 1.0),
            ))
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
        // SUB-06: cancel-flow survey — one optional question logged for churn analysis.
        // Both reason and freeText may be null (skip path). Authenticated subjects are
        // resolved from the JWT; unauthenticated calls use subjectId from the request body.
        post("/api/v1/subjects/cancellation-survey") {
            val request = call.receive<CancellationSurveyRequest>()
            if (request.subjectId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "subjectId is required"))
                return@post
            }
            val subjectId = SubjectId(request.subjectId)
            val freeTextTrimmed = request.freeText?.take(500)
            eventStore.append(
                listOf(
                    CancellationSurveySubmitted(
                        subjectId = subjectId,
                        reason = request.reason,
                        freeText = freeTextTrimmed,
                        submittedAtEpochMs = System.currentTimeMillis(),
                    ),
                ),
                condition = null,
            )
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to "CancellationSurveySubmitted"))
        }
        // SUB-04: self-service cancellation — at most 2 clicks from account page (NL/EU legal requirement).
        // DN-01: decide_offer(cancel_intent) is called first so the CEP can return a retention offer;
        // the offer is included in the response but never blocks the cancellation (DN-02).
        // Access is retained until current period end (SUB-03); validUntilEpochMs must be supplied
        // by the client (obtained from the payment provider or the existing grant's validUntilEpochMs).
        post("/api/v1/subscriptions/{ref}/cancel") {
            val ref = call.parameters["ref"]?.takeIf { it.isNotBlank() } ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "subscription ref is required"))
                return@post
            }
            val req = call.receive<CancelSubscriptionRequest>()
            if (req.subjectId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "subjectId is required"))
                return@post
            }
            val subjectId = SubjectId(req.subjectId)
            val subscriptionRef = SubscriptionId(ref)
            val planId = PlanId(req.planId ?: "unknown")
            val nowMs = System.currentTimeMillis()
            // DN-01: call decide_offer(cancel_intent) — CEP may return a retention offer to present.
            val retentionOfferId: String? = if (offerService != null) {
                val cancelSubject = if (req.subjectId.startsWith("user:")) {
                    Subject(VisitorId("_"), UserId(req.subjectId.removePrefix("user:")))
                } else {
                    Subject(VisitorId(req.subjectId.removePrefix("visitor:")), null)
                }
                val ctx = OfferService.TriggerContext(
                    trigger = "cancel_intent",
                    channel = req.channel,
                    currentPlanId = req.planId,
                    variant = service.variantFor(cancelSubject),
                )
                val decision = offerService.decideOffer(cancelSubject, ctx)
                (decision as? OfferService.OfferDecision.Triggered)?.offer?.offerId
            } else null
            // SUB-04/SUB-03: record cancellation — access retained until current period end.
            val canceledUntilMs = req.validUntilEpochMs ?: nowMs
            eventStore.append(
                listOf(
                    EntitlementGranted(subjectId, planId, subscriptionRef, canceledUntilMs),
                    MailSent(subjectId, "cancellation_confirmation", nowMs, planId.value, ref), // US-10
                ),
                condition = null,
            )
            service.invalidateEntitlementCache(req.subjectId)
            val responseBody = buildMap<String, String> {
                put("recorded", "canceled")
                put("canceledUntilEpochMs", canceledUntilMs.toString())
                if (retentionOfferId != null) put("retentionOfferId", retentionOfferId) // DN-01: informational
            }
            call.respond(HttpStatusCode.Accepted, responseBody)
        }
        // PAY-03/PAY-05: checkout initiation — creates a payment session via the
        // PaymentProvider interface (MockPaymentProvider in experiment; swap for
        // Stripe/Mollie/Adyen in production without touching this handler).
        // UP-10/11: checkout trigger is forwarded to the CEP so it can return an
        // upsell offer (annual / tier upgrade) if the offerId wasn't already chosen.
        post("/api/v1/checkout") {
            val req = call.receive<CheckoutRequest>()
            if (req.subjectId.isBlank() || req.planId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "subjectId and planId are required"))
                return@post
            }
            val subjectId = SubjectId(req.subjectId)
            val checkoutSubject = if (req.subjectId.startsWith("user:")) {
                Subject(VisitorId("_"), UserId(req.subjectId.removePrefix("user:")))
            } else {
                Subject(VisitorId(req.subjectId.removePrefix("visitor:")), null)
            }
            val nowMs = System.currentTimeMillis()
            // UP-10/11: call decide_offer("checkout") so CEP can upsell at checkout.
            if (offerService != null && req.offerId == null) {
                val ctx = OfferService.TriggerContext(
                    trigger = "checkout",
                    channel = req.channel,
                    currentPlanId = req.planId,
                    variant = service.variantFor(checkoutSubject),
                )
                offerService.decideOffer(checkoutSubject, ctx) // result ignored; logged via OfferTriggered
            }
            // AN-02: log CHECKOUT_START for funnel analytics.
            eventStore.append(
                listOf(WallEventRecorded(
                    eventType = WallEventType.CHECKOUT_START,
                    subjectId = subjectId,
                    variant = VariantAssigner.assign(checkoutSubject, service.currentExperiment()).name,
                    channel = req.channel,
                    occurredAtEpochMs = nowMs,
                    context = buildMap {
                        put("planId", req.planId)
                        if (req.offerId != null) put("offerId", req.offerId)
                    },
                )),
                condition = null,
            )
            val session = paymentProvider.createCheckoutSession(
                req.planId, req.subjectId, req.returnUrl, req.paymentMethod,
            )
            call.respond(HttpStatusCode.Created, buildJsonObject {
                put("sessionId", session.sessionId)
                put("checkoutUrl", session.checkoutUrl)
                if (req.returnUrl != null) put("returnUrl", req.returnUrl) // PAY-04/AC-12
                if (req.paymentMethod != null) put("paymentMethod", req.paymentMethod) // PAY-06
                // PAY-06: advertise available methods so the checkout page can render the picker.
                put("availablePaymentMethods", kotlinx.serialization.json.buildJsonArray {
                    paymentProvider.availablePaymentMethods.forEach { add(it) }
                })
            })
        }
        // PAY-05 (mock): confirm a mock checkout session — used in the experiment to
        // complete checkout without a real payment provider. A real provider would
        // instead fire the entitlement webhook (POST /api/v1/integration/entitlements).
        // PAY-07: pass ?simulate_failure=true to receive a 402 with retry information
        // (the session stays alive so the same sessionId retries without data loss).
        post("/api/v1/checkout/{sessionId}/confirm") {
            val sessionId = call.parameters["sessionId"]?.takeIf { it.isNotBlank() } ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "sessionId is required"))
                return@post
            }
            val mock = paymentProvider as? MockPaymentProvider ?: run {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "confirm endpoint only available with mock provider"))
                return@post
            }
            // PAY-07: simulate a declined payment; session is NOT consumed so the client can retry.
            val simulateFailure = call.request.queryParameters["simulate_failure"] == "true"
            if (simulateFailure) {
                val peek = mock.consumeSession(sessionId, consume = false)
                if (peek == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "session not found or already confirmed"))
                    return@post
                }
                call.respond(HttpStatusCode.PaymentRequired, buildJsonObject {
                    put("error", "payment_declined")
                    put("retryAllowed", true)
                    put("sessionId", sessionId)
                    // PAY-07: client retries with the same sessionId; no data re-entry needed.
                    put("retryUrl", "/api/v1/checkout/$sessionId/confirm")
                })
                return@post
            }
            val session = mock.consumeSession(sessionId) ?: run {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "session not found or already confirmed"))
                return@post
            }
            val subjectId = SubjectId(session.subjectId)
            val planId = PlanId(session.planId)
            val nowMs = System.currentTimeMillis()
            val defaultPeriodMs = 30L * 24 * 60 * 60 * 1000L
            // Record the successful purchase: grant entitlement + log mail + checkout_complete event.
            eventStore.append(
                listOf(
                    EntitlementGranted(subjectId, planId, SubscriptionId("checkout-$sessionId"),
                        nowMs + defaultPeriodMs),
                    MailSent(subjectId, "purchase_confirmation", nowMs, planId.value, null), // US-10
                    WallEventRecorded(
                        eventType = WallEventType.CHECKOUT_COMPLETE,
                        subjectId = subjectId,
                        variant = "unknown",
                        channel = "web",
                        occurredAtEpochMs = nowMs,
                        context = mapOf("planId" to session.planId, "sessionId" to sessionId),
                    ),
                ),
                condition = null,
            )
            service.invalidateEntitlementCache(session.subjectId)
            call.respond(HttpStatusCode.Accepted, buildJsonObject {
                put("recorded", "purchase_confirmed")
                put("sessionId", sessionId)
                if (session.returnUrl != null) put("returnUrl", session.returnUrl) // AC-12
                if (session.paymentMethod != null) put("paymentMethod", session.paymentMethod) // PAY-06
            })
        }
        // Experiment dashboard numbers (AN-10/AN-11/AN-12): per-variant funnel stats,
        // rebuilt from the wall-event stream (projection — DM-04/DM-08).
        // AN-11: reach cost = delta in page views and article reads vs. the EX-04
        // control variant; null when no control variant is configured.
        get("/api/v1/stats") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            val events = eventStore.query(EventQuery(wallEventShardTags())).events
            val projection = VariantStatsProjection().also { it.applyAll(events) }
            val allStats = projection.stats()
            // AN-11: find the control variant name from the active experiment (EX-04).
            val controlName = service.currentExperiment().variants.firstOrNull { it.isControl }?.name
            val controlStats = controlName?.let { allStats[it] }
            val response = allStats.map { (variant, s) ->
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
                    pageViewsDeltaVsControl = controlStats?.let { s.pageViews - it.pageViews },
                    articleReadsDeltaVsControl = controlStats?.let { s.articleReads - it.articleReads },
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
        // AN-13: cohort view — conversion and 30-day retention by ISO week of
        // first visit. Rebuilt on demand from the full wall-event stream.
        get("/api/v1/stats/cohorts") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            val events = eventStore.query(EventQuery(wallEventShardTags())).events
            val projection = CohortProjection().also { it.applyAll(events) }
            call.respond(projection.cohorts().map { c ->
                CohortStatsResponse(
                    cohortWeek = c.cohortWeek,
                    visitors = c.visitors,
                    conversions = c.conversions,
                    conversionRate = c.conversionRate,
                    retainedAt30Days = c.retainedAt30Days,
                    retentionRate = c.retentionRate,
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
            // NFR-03/SUB-02: idempotency — payment providers retry on timeout; skip duplicates.
            val webhookTag: Set<String> = if (change.webhookEventId != null) {
                val dup = eventStore.query(EventQuery(setOf("webhook:${change.webhookEventId}"))).events
                if (dup.isNotEmpty()) {
                    call.respond(HttpStatusCode.Accepted, mapOf("recorded" to "already_processed"))
                    return@post
                }
                setOf("webhook:${change.webhookEventId}")
            } else emptySet()
            // SUB-07: status field takes precedence over legacy active boolean.
            val gracePeriodMs = 7 * 24 * 60 * 60 * 1000L
            val nowMs = System.currentTimeMillis()
            val events: List<DomainEvent> = when (change.status) {
                "active" -> listOf(
                    EntitlementGranted(subjectId, planId, subscriptionRef, change.validUntilEpochMs,
                        tags = setOf("subject:${subjectId.value}") + webhookTag),
                    SubscriptionResumed(subjectId, subscriptionRef, nowMs), // clears paused state if any
                )
                "canceled" -> listOf( // SUB-03: retain access until current_period_end
                    EntitlementGranted(subjectId, planId, subscriptionRef, change.validUntilEpochMs,
                        tags = setOf("subject:${subjectId.value}") + webhookTag),
                    MailSent(subjectId, "cancellation_confirmation", nowMs, planId.value, subscriptionRef.value), // US-10
                )
                "past_due" -> listOf( // SUB-05: 7-day grace period with access retained
                    EntitlementGranted(subjectId, planId, subscriptionRef, nowMs + gracePeriodMs,
                        tags = setOf("subject:${subjectId.value}") + webhookTag),
                    MailSent(subjectId, "payment_failure", nowMs, planId.value, subscriptionRef.value), // US-10
                )
                "paused" -> listOf( // SUB-07: billing suspended → access off
                    SubscriptionPaused(subjectId, subscriptionRef, planId, nowMs,
                        tags = setOf("subject:${subjectId.value}") + webhookTag),
                )
                "expired" -> listOf(
                    EntitlementRevoked(subjectId, subscriptionRef,
                        tags = setOf("subject:${subjectId.value}") + webhookTag),
                )
                else -> if (change.active) { // legacy boolean path
                    listOf(EntitlementGranted(subjectId, planId, subscriptionRef, change.validUntilEpochMs,
                        tags = setOf("subject:${subjectId.value}") + webhookTag))
                } else {
                    listOf(EntitlementRevoked(subjectId, subscriptionRef,
                        tags = setOf("subject:${subjectId.value}") + webhookTag))
                }
            }
            eventStore.append(events, condition = null)
            // AC-03: invalidate the per-session cache so the change takes effect
            // immediately rather than waiting for the 5-minute TTL to expire.
            service.invalidateEntitlementCache(change.subjectId)
            // DN-03: on payment failure, fire the "payment_failure" trigger so the CEP can
            // offer a cheaper plan before the subscriber churns at grace-period expiry.
            val paymentFailureOfferId: String? = if (change.status == "past_due" && offerService != null) {
                val failedSubject = if (change.subjectId.startsWith("user:")) {
                    Subject(VisitorId("_"), UserId(change.subjectId.removePrefix("user:")))
                } else {
                    Subject(VisitorId(change.subjectId.removePrefix("visitor:")), null)
                }
                val ctx = OfferService.TriggerContext(
                    trigger = "payment_failure",
                    channel = "web",
                    currentPlanId = change.planId,
                    variant = service.variantFor(failedSubject),
                )
                (offerService.decideOffer(failedSubject, ctx) as? OfferService.OfferDecision.Triggered)?.offer?.offerId
            } else null
            val responseBody = buildJsonObject {
                putJsonArray("recorded") { events.forEach { add(it::class.simpleName) } }
                if (paymentFailureOfferId != null) put("retentionOfferId", paymentFailureOfferId)
            }
            call.respond(HttpStatusCode.Accepted, responseBody)
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
            // FGA-02: every grant is time-bound (expires_at required, max TTL configurable,
            // default 30 days). If not supplied, default to 30 days from now.
            val now = System.currentTimeMillis()
            val maxGrantTtlDays = service.currentExperiment().maxGrantTtlDays
            val maxGrantTtlMs = maxGrantTtlDays * 24L * 3600 * 1000
            val defaultGrantTtlMs = 30L * 24 * 3600 * 1000
            val expiresAt = if (change.active) {
                val requested = change.expiresAtEpochMs ?: (now + defaultGrantTtlMs)
                if (requested > now + maxGrantTtlMs) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "grant TTL exceeds maximum of $maxGrantTtlDays days (FGA-02)"),
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
                    reason = change.reason, // FGA-01
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
        // BP-06: bypass-rate estimation — ratio of gated-rendering requests that
        // carry bypass markers (bot UA flag, suspicious-IP flag) relative to total
        // wall-shown events. Reported, never blocked (DL-03). Optional `since`
        // paginates by store position for incremental/periodic reporting.
        get("/api/v1/admin/bypass-rate") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
            val result = eventStore.query(EventQuery(wallEventShardTags(), since = since))
            val wallEvents = result.events.filterIsInstance<WallEventRecorded>()
            // Denominator: every gate render (wall shown to a visitor)
            val gatedRenders = wallEvents.count {
                it.eventType == nl.incedo.paywall.analytics.WallEventType.WALL_SHOWN
            }
            // Numerator: gate renders that arrived with a bypass marker
            val markedGatedRenders = wallEvents.count {
                it.eventType == nl.incedo.paywall.analytics.WallEventType.WALL_SHOWN &&
                    (it.context["bot"] == "true" || it.context["suspicious_ip"] == "true")
            }
            // Secondary: flagged article reads that may represent successful bypasses
            val flaggedReads = wallEvents.count {
                it.eventType == nl.incedo.paywall.analytics.WallEventType.ARTICLE_READ &&
                    (it.context["bot"] == "true" || it.context["suspicious_ip"] == "true")
            }
            val bypassRate = if (gatedRenders > 0) markedGatedRenders.toDouble() / gatedRenders else 0.0
            call.respond(
                BypassRateResponse(
                    gatedRenders = gatedRenders,
                    markedGatedRenders = markedGatedRenders,
                    flaggedReads = flaggedReads,
                    bypassRate = bypassRate,
                    storePosition = result.position,
                ),
            )
        }
        // NFR-15: variant kill-switch admin endpoints (OPERATOR only).
        // Kill: instantly opens access for all visitors in the named variant.
        // Restore: reverts to the normal paywall strategy.
        // The in-process cache is updated immediately; the event store provides
        // the audit trail and rehydration source after a restart.
        get("/api/v1/admin/variant-kill-switches") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            call.respond(mapOf("killed" to killedVariantsCache.get().toList().sorted()))
        }
        post("/api/v1/admin/variants/{name}/kill") {
            val staff = call.requireStaff(jwtValidator, StaffRole.OPERATOR) ?: return@post
            val name = call.parameters["name"]?.takeIf { it.isNotBlank() }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "variant name required"))
            eventStore.append(listOf(VariantKilled(name, staff.userId.value, System.currentTimeMillis())), condition = null)
            killedVariantsCache.set(loadKilledVariants())
            call.respond(HttpStatusCode.Accepted, mapOf("killed" to name))
        }
        post("/api/v1/admin/variants/{name}/restore") {
            val staff = call.requireStaff(jwtValidator, StaffRole.OPERATOR) ?: return@post
            val name = call.parameters["name"]?.takeIf { it.isNotBlank() }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "variant name required"))
            eventStore.append(listOf(VariantRestored(name, staff.userId.value, System.currentTimeMillis())), condition = null)
            killedVariantsCache.set(loadKilledVariants())
            call.respond(HttpStatusCode.Accepted, mapOf("restored" to name))
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
        // AG-01/AG-02: ad-gated grant endpoint. AG-01: completion verified server-side via
        // signed callback (webhookVerifier) before any grant is issued. AG-02: 24 h grant
        // for the triggering article, daily cap = 2 per subject.
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
                reason = "ad completion ${req.adPlayId}", // FGA-01 / AG-02
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
                reason = "data exchange ${req.purposeId} completion ${req.completionId}", // FGA-01 / DG-02
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
            call.respond(offer.toOfferResponse())
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
            // DN-06: optional offer object fields for retention value measurement (AN-10).
            val fromPlanId = call.request.queryParameters["fromPlanId"]
            val toPlanId = call.request.queryParameters["toPlanId"]
            val discountPercent = call.request.queryParameters["discountPercent"]?.toIntOrNull()
            val discountFixed = call.request.queryParameters["discountFixed"]?.toIntOrNull() // UP-02
            val discountDurationPeriods = call.request.queryParameters["discountDurationPeriods"]?.toIntOrNull() // UP-02
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
                    fromPlanId = fromPlanId, // DN-06
                    toPlanId = toPlanId, // DN-06
                    discountPercent = discountPercent, // DN-06
                    discountFixed = discountFixed, // UP-02
                    discountDurationPeriods = discountDurationPeriods, // UP-02
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
                        reason = "offer accepted $offerId", // FGA-01
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
        // UP-13: engagement-based upsell — a basic monthly subscriber with high engagement
        // (meter count >= threshold) in 2 consecutive months receives an annual/complete offer.
        // Trigger context is "account_view" so CEP can return an upgrade-tailored offer.
        // The threshold is configurable via the `threshold` query param (default 5).
        post("/api/v1/offers/account-view") {
            val userId = jwtValidator?.userIdFrom(call.request.headers[HttpHeaders.Authorization])
            val visitorId = call.request.queryParameters["visitorId"]
                ?: call.request.headers["X-Visitor-Id"]
            if (visitorId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "visitorId is required"))
                return@post
            }
            if (offerService == null) {
                call.respond(OfferResponse(offerId = null, kind = null))
                return@post
            }
            val threshold = call.request.queryParameters["threshold"]?.toIntOrNull() ?: 5
            val subject = Subject(VisitorId(visitorId), userId)
            val subjectId = userId?.let { SubjectId.of(it) } ?: SubjectId.of(VisitorId(visitorId))
            val currentPer = currentPeriod()
            val previousPer = previousPeriod(currentPer)
            // Load events for both the current and previous meter period in one query.
            val tags = buildSet {
                add("subject:${subjectId.value}")
                add(meterTag(subjectId, currentPer))
                add(meterTag(subjectId, previousPer))
            }
            val events = eventStore.query(EventQuery(tags)).events
            val now = System.currentTimeMillis()
            val entitlement = nl.incedo.paywall.entitlements.EntitlementDecision().also { it.applyAll(events) }
            // UP-13: only trigger for basic subscribers (rank-1) who are NOT already on complete (rank-2).
            val isBasicSubscriber = entitlement.hasValidEntitlement(now) &&
                !entitlement.hasValidEntitlementForTier(now, minRank = 2)
            if (!isBasicSubscriber) {
                call.respond(OfferResponse(offerId = null, kind = null))
                return@post
            }
            // Count premium reads in each period as the engagement proxy (DY-01 input data).
            val currentMeter = nl.incedo.paywall.metering.MeterDecision(currentPer).also { it.applyAll(events) }
            val previousMeter = nl.incedo.paywall.metering.MeterDecision(previousPer).also { it.applyAll(events) }
            val highEngagement = currentMeter.used >= threshold && previousMeter.used >= threshold
            if (!highEngagement) {
                call.respond(OfferResponse(offerId = null, kind = null))
                return@post
            }
            val ctx = OfferService.TriggerContext(
                trigger = "account_view",
                channel = "account",
                currentPlanId = "basic-monthly", // UP-13: basic monthly → annual or complete offer
                variant = service.variantFor(subject),
            )
            val decision = offerService.decideOffer(subject, ctx)
            val offer = (decision as? OfferService.OfferDecision.Triggered)?.offer
            call.respond(offer.toOfferResponse())
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
        // PAY-01/01a/02: plan catalogue — two tiers × two billing periods, with ranks
        // for upsell/downsell logic. Configuration-driven; no auth required (public).
        // PAY-02: introPriceMinorUnits/introPeriodsCount exposed when an intro offer exists.
        get("/api/v1/plans") {
            call.respond(DefaultPlans.all.map { p ->
                PlanResponse(p.planId.value, p.tier, p.billingPeriod, p.rank, p.displayName, p.priceMinorUnits, p.currency,
                    introPriceMinorUnits = p.introPriceMinorUnits,
                    introPeriodsCount = p.introPeriodsCount,
                )
            })
        }
        // ── Partner management (PA-01/02/03/05 / IPW-01) ─────────────────────
        // PA-01: list all partners — enabled by the "partners" catalog tag on PartnerCreated.
        get("/api/v1/admin/partners") {
            call.requireStaff(jwtValidator, StaffRole.VIEWER) ?: return@get
            val createdEvents = eventStore.query(EventQuery(setOf("partners"))).events
                .filterIsInstance<PartnerCreated>()
            val partners = createdEvents.map { created ->
                val pEvents = eventStore.query(EventQuery(setOf(partnerTag(created.partnerId)))).events
                val partner = PartnerDecision().also { it.applyAll(pEvents) }
                PartnerResponse(
                    partnerId = created.partnerId.value,
                    name = partner.name,
                    maxSeats = partner.maxSeats,
                    activeSeats = partner.activeSeatCount(),
                    planId = partner.planId,
                    activeCidrs = partner.activeCidrs(),
                )
            }.filter { it.name.isNotEmpty() }
            call.respond(partners)
        }
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
        // ── Storybook API (SB-01 – SB-15) ─────────────────────────────────────────
        // Story → Scenario → ControlSchema — the three BCs of the interactive docs layer.

        post("/api/v1/storybook/stories") {
            val req = call.receive<RegisterStoryRequest>()
            if (req.storyId.isBlank() || req.storyKey.isBlank() || req.title.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "storyId, storyKey, and title are required"))
                return@post
            }
            val storyType = runCatching { StoryType.valueOf(req.type.uppercase()) }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "type must be one of ${StoryType.entries.joinToString()}"))
                return@post
            }
            val lifecycle = runCatching { StoryLifecycle.valueOf(req.lifecycle.uppercase()) }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "lifecycle must be one of ${StoryLifecycle.entries.joinToString()}"))
                return@post
            }
            // BR-3: storyKey must be globally unique
            val keyEvents = eventStore.query(EventQuery(setOf(storyKeyTag(StoryKey(req.storyKey))))).events
            if (StoryKeyUniquenessDecision().also { it.applyAll(keyEvents) }.isTaken(req.storyKey)) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "storyKey '${req.storyKey}' is already in use (BR-3)"))
                return@post
            }
            val storyId = StoryId(req.storyId)
            if (StoryDecision().also { it.applyAll(eventStore.query(EventQuery(setOf(storyTag(storyId)))).events) }.exists) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "story '${req.storyId}' already exists"))
                return@post
            }
            eventStore.append(listOf(StoryRegistered(
                storyId = storyId, storyKey = StoryKey(req.storyKey), title = req.title,
                type = storyType, groupId = req.groupId, owner = req.owner,
                renderContractRef = req.renderContractRef, lifecycle = lifecycle,
                coverageScope = req.coverageScope, registeredAtEpochMs = System.currentTimeMillis(),
            )), condition = null)
            call.respond(HttpStatusCode.Created, StoryResponse(req.storyId, req.storyKey, req.title, storyType.name, req.groupId, lifecycle.name))
        }

        get("/api/v1/storybook/stories") {
            val events = eventStore.query(EventQuery(setOf("stories"))).events
            val byId = mutableMapOf<String, StoryRegistered>()
            val archived = mutableSetOf<String>()
            events.forEach { when (it) {
                is StoryRegistered -> byId[it.storyId.value] = it
                is StoryArchived -> archived += it.storyId.value
                else -> Unit
            } }
            call.respond(byId.values.filter { it.storyId.value !in archived }.sortedBy { it.storyId.value }
                .map { StoryResponse(it.storyId.value, it.storyKey.value, it.title, it.type.name, it.groupId, it.lifecycle.name) })
        }

        get("/api/v1/storybook/stories/{storyId}") {
            val sid = call.parameters["storyId"]?.takeIf { it.isNotBlank() }
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "storyId required"))
            val decision = StoryDecision().also { it.applyAll(eventStore.query(EventQuery(setOf(storyTag(StoryId(sid))))).events) }
            if (!decision.exists || decision.archived) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "story not found")); return@get
            }
            call.respond(StoryResponse(sid, decision.storyKey, decision.title, decision.storyType, decision.groupId, decision.lifecycle.name))
        }

        post("/api/v1/storybook/stories/{storyId}/scenarios") {
            val sid = call.parameters["storyId"]?.takeIf { it.isNotBlank() }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "storyId required"))
            val storyId = StoryId(sid)
            val story = StoryDecision().also { it.applyAll(eventStore.query(EventQuery(setOf(storyTag(storyId)))).events) }
            if (!story.exists || story.archived) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "story not found")); return@post
            }
            val req = call.receive<RegisterScenarioRequest>()
            if (req.scenarioId.isBlank() || req.scenarioKey.isBlank() || req.title.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "scenarioId, scenarioKey, and title are required"))
                return@post
            }
            val scenarioType = runCatching { ScenarioType.valueOf(req.type.uppercase()) }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "type must be one of ${ScenarioType.entries.joinToString()}"))
                return@post
            }
            val scenarioLifecycle = runCatching { ScenarioLifecycle.valueOf(req.lifecycle.uppercase()) }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "lifecycle must be one of ${ScenarioLifecycle.entries.joinToString()}"))
                return@post
            }
            // BR-4: scenarioKey unique within story
            val keyTag = scenarioKeyTag(storyId, ScenarioKey(req.scenarioKey))
            if (eventStore.query(EventQuery(setOf(keyTag))).events.filterIsInstance<ScenarioRegistered>().isNotEmpty()) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "scenarioKey '${req.scenarioKey}' already in use for this story (BR-4)"))
                return@post
            }
            val scenarioId = ScenarioId(req.scenarioId)
            if (eventStore.query(EventQuery(setOf(scenarioTag(scenarioId)))).events.filterIsInstance<ScenarioRegistered>().isNotEmpty()) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "scenario '${req.scenarioId}' already exists"))
                return@post
            }
            eventStore.append(listOf(ScenarioRegistered(
                scenarioId = scenarioId, storyId = storyId, scenarioKey = ScenarioKey(req.scenarioKey),
                title = req.title, type = scenarioType, lifecycle = scenarioLifecycle,
                registeredAtEpochMs = System.currentTimeMillis(),
            )), condition = null)
            call.respond(HttpStatusCode.Created, ScenarioResponse(req.scenarioId, sid, req.scenarioKey, req.title, scenarioType.name, scenarioLifecycle.name))
        }

        get("/api/v1/storybook/stories/{storyId}/scenarios") {
            val sid = call.parameters["storyId"]?.takeIf { it.isNotBlank() }
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "storyId required"))
            val storyId = StoryId(sid)
            val events = eventStore.query(EventQuery(setOf(storyTag(storyId)))).events
            val byId = mutableMapOf<String, ScenarioRegistered>()
            val archived = mutableSetOf<String>()
            events.forEach { when (it) {
                is ScenarioRegistered -> byId[it.scenarioId.value] = it
                is ScenarioArchived -> archived += it.scenarioId.value
                else -> Unit
            } }
            call.respond(byId.values.filter { it.scenarioId.value !in archived }.sortedBy { it.scenarioId.value }
                .map { ScenarioResponse(it.scenarioId.value, sid, it.scenarioKey.value, it.title, it.type.name, it.lifecycle.name) })
        }

        post("/api/v1/storybook/scenarios/{scenarioId}/control-schema") {
            val scenId = call.parameters["scenarioId"]?.takeIf { it.isNotBlank() }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "scenarioId required"))
            val scenarioId = ScenarioId(scenId)
            // BR-2: scenario must exist and not be archived
            val scenario = ScenarioDecision().also { it.applyAll(eventStore.query(EventQuery(setOf(scenarioTag(scenarioId)))).events) }
            if (!scenario.exists || scenario.archived) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "scenario not found or archived (BR-2)")); return@post
            }
            val req = call.receive<RegisterControlSchemaRequest>()
            if (req.schemaId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "schemaId is required")); return@post
            }
            val schemaId = ControlSchemaId(req.schemaId)
            if (eventStore.query(EventQuery(setOf(controlSchemaTag(schemaId)))).events.filterIsInstance<ControlSchemaRegistered>().isNotEmpty()) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "control schema '${req.schemaId}' already exists")); return@post
            }
            eventStore.append(listOf(ControlSchemaRegistered(
                schemaId = schemaId, scenarioId = scenarioId, lifecycle = req.lifecycle,
                registeredAtEpochMs = System.currentTimeMillis(),
            )), condition = null)
            call.respond(HttpStatusCode.Created, ControlSchemaResponse(req.schemaId, scenId, req.lifecycle, emptyList()))
        }

        get("/api/v1/storybook/scenarios/{scenarioId}/controls") {
            val scenId = call.parameters["scenarioId"]?.takeIf { it.isNotBlank() }
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "scenarioId required"))
            val scenarioId = ScenarioId(scenId)
            val scenEvents = eventStore.query(EventQuery(setOf(scenarioTag(scenarioId)))).events
            val schema = scenEvents.filterIsInstance<ControlSchemaRegistered>().firstOrNull()
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "no control schema for this scenario"))
            val controlEvents = eventStore.query(EventQuery(setOf(controlSchemaTag(schema.schemaId)))).events
            val decision = ControlSchemaDecisionModel().also { it.applyAll(controlEvents) }
            call.respond(ControlSchemaResponse(
                schemaId = schema.schemaId.value, scenarioId = scenId,
                lifecycle = if (decision.archived) "ARCHIVED" else "ACTIVE",
                controls = decision.activeControls().map { it.toResponse() },
            ))
        }

        post("/api/v1/storybook/control-schemas/{schemaId}/controls") {
            val schId = call.parameters["schemaId"]?.takeIf { it.isNotBlank() }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "schemaId required"))
            val schemaId = ControlSchemaId(schId)
            val schemaEvents = eventStore.query(EventQuery(setOf(controlSchemaTag(schemaId)))).events
            val schema = ControlSchemaDecisionModel().also { it.applyAll(schemaEvents) }
            if (!schema.exists) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "control schema not found (BR-1)")); return@post
            }
            if (schema.archived) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "control schema is archived (BR-11)")); return@post
            }
            val req = call.receive<AddControlRequest>()
            if (req.controlId.isBlank() || req.key.isBlank() || req.label.isBlank() || req.bindingRef.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "controlId, key, label, and bindingRef are required"))
                return@post
            }
            val controlType = runCatching { ControlType.valueOf(req.type.uppercase()) }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "type must be one of ${ControlType.entries.joinToString()}"))
                return@post
            }
            if (schema.isKeyTaken(req.key)) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "control key '${req.key}' already in use (BR-4)")); return@post
            }
            val controlId = ControlId(req.controlId)
            if (schema.isActive(req.controlId)) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "control '${req.controlId}' already exists")); return@post
            }
            eventStore.append(listOf(ControlAdded(
                schemaId = schemaId, controlId = controlId, key = ControlKey(req.key),
                label = req.label, type = controlType, defaultValue = req.defaultValue,
                bindingRef = req.bindingRef, optionsRef = req.optionsRef,
                validationRules = req.validationRules, addedAtEpochMs = System.currentTimeMillis(),
                tags = setOf(controlSchemaTag(schemaId), controlTag(controlId), scenarioTag(ScenarioId(schema.scenarioId))),
            )), condition = null)
            call.respond(HttpStatusCode.Created, mapOf("controlId" to req.controlId, "key" to req.key))
        }

        put("/api/v1/storybook/control-schemas/{schemaId}/controls/{controlId}/default") {
            val schId = call.parameters["schemaId"]?.takeIf { it.isNotBlank() }
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "schemaId required"))
            val ctrlId = call.parameters["controlId"]?.takeIf { it.isNotBlank() }
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "controlId required"))
            val schemaId = ControlSchemaId(schId)
            val schemaEvents = eventStore.query(EventQuery(setOf(controlSchemaTag(schemaId)))).events
            val schema = ControlSchemaDecisionModel().also { it.applyAll(schemaEvents) }
            if (!schema.exists) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "control schema not found")); return@put
            }
            if (schema.archived) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "control schema is archived (BR-11)")); return@put
            }
            if (!schema.isActive(ctrlId)) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "control not found or removed")); return@put
            }
            val req = call.receive<ChangeControlDefaultRequest>()
            eventStore.append(listOf(ControlDefaultChanged(
                schemaId = schemaId, controlId = ControlId(ctrlId),
                newDefaultValue = req.defaultValue, changedAtEpochMs = System.currentTimeMillis(),
            )), condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to "ControlDefaultChanged"))
        }

        // Story metadata update and archive
        put("/api/v1/storybook/stories/{storyId}") {
            val sid = call.parameters["storyId"]?.takeIf { it.isNotBlank() }
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "storyId required"))
            val storyId = StoryId(sid)
            val decision = StoryDecision().also { it.applyAll(eventStore.query(EventQuery(setOf(storyTag(storyId)))).events) }
            if (!decision.exists) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "story not found")); return@put
            }
            if (decision.archived) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "story is archived")); return@put
            }
            val req = call.receive<UpdateStoryRequest>()
            eventStore.append(listOf(StoryMetadataUpdated(
                storyId = storyId, title = req.title, description = req.description,
                owner = req.owner, updatedAtEpochMs = System.currentTimeMillis(),
            )), condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to "StoryMetadataUpdated"))
        }

        delete("/api/v1/storybook/stories/{storyId}") {
            val sid = call.parameters["storyId"]?.takeIf { it.isNotBlank() }
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "storyId required"))
            val storyId = StoryId(sid)
            val decision = StoryDecision().also { it.applyAll(eventStore.query(EventQuery(setOf(storyTag(storyId)))).events) }
            if (!decision.exists) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "story not found")); return@delete
            }
            if (decision.archived) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "story already archived")); return@delete
            }
            eventStore.append(listOf(StoryArchived(storyId, archivedAtEpochMs = System.currentTimeMillis())), condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to "StoryArchived"))
        }

        // Single-scenario GET, metadata update, and archive
        get("/api/v1/storybook/scenarios/{scenarioId}") {
            val scenId = call.parameters["scenarioId"]?.takeIf { it.isNotBlank() }
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "scenarioId required"))
            val scenarioId = ScenarioId(scenId)
            val decision = ScenarioDecision().also { it.applyAll(eventStore.query(EventQuery(setOf(scenarioTag(scenarioId)))).events) }
            if (!decision.exists || decision.archived) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "scenario not found")); return@get
            }
            call.respond(ScenarioResponse(scenId, decision.storyId, "", decision.title, decision.scenarioType, "ACTIVE"))
        }

        put("/api/v1/storybook/scenarios/{scenarioId}") {
            val scenId = call.parameters["scenarioId"]?.takeIf { it.isNotBlank() }
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "scenarioId required"))
            val scenarioId = ScenarioId(scenId)
            val decision = ScenarioDecision().also { it.applyAll(eventStore.query(EventQuery(setOf(scenarioTag(scenarioId)))).events) }
            if (!decision.exists) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "scenario not found")); return@put
            }
            if (decision.archived) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "scenario is archived")); return@put
            }
            val req = call.receive<UpdateScenarioRequest>()
            val newType = req.type?.let { t ->
                runCatching { ScenarioType.valueOf(t.uppercase()) }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "type must be one of ${ScenarioType.entries.joinToString()}"))
                    return@put
                }
            }
            eventStore.append(listOf(ScenarioMetadataUpdated(
                scenarioId = scenarioId, title = req.title, description = req.description,
                type = newType, updatedAtEpochMs = System.currentTimeMillis(),
            )), condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to "ScenarioMetadataUpdated"))
        }

        delete("/api/v1/storybook/scenarios/{scenarioId}") {
            val scenId = call.parameters["scenarioId"]?.takeIf { it.isNotBlank() }
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "scenarioId required"))
            val scenarioId = ScenarioId(scenId)
            val decision = ScenarioDecision().also { it.applyAll(eventStore.query(EventQuery(setOf(scenarioTag(scenarioId)))).events) }
            if (!decision.exists) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "scenario not found")); return@delete
            }
            if (decision.archived) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "scenario already archived")); return@delete
            }
            eventStore.append(listOf(ScenarioArchived(
                scenarioId = scenarioId,
                archivedAtEpochMs = System.currentTimeMillis(),
                tags = setOf(scenarioTag(scenarioId), storyTag(StoryId(decision.storyId)), "scenarios"),
            )), condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to "ScenarioArchived"))
        }

        delete("/api/v1/storybook/control-schemas/{schemaId}/controls/{controlId}") {
            val schId = call.parameters["schemaId"]?.takeIf { it.isNotBlank() }
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "schemaId required"))
            val ctrlId = call.parameters["controlId"]?.takeIf { it.isNotBlank() }
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "controlId required"))
            val schemaId = ControlSchemaId(schId)
            val schemaEvents = eventStore.query(EventQuery(setOf(controlSchemaTag(schemaId)))).events
            val schema = ControlSchemaDecisionModel().also { it.applyAll(schemaEvents) }
            if (!schema.exists) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "control schema not found")); return@delete
            }
            if (schema.archived) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "control schema is archived (BR-11)")); return@delete
            }
            if (!schema.isActive(ctrlId)) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "control not found or already removed")); return@delete
            }
            eventStore.append(listOf(ControlRemoved(
                schemaId = schemaId, controlId = ControlId(ctrlId),
                removedAtEpochMs = System.currentTimeMillis(),
            )), condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to "ControlRemoved"))
        }

        // ── Decorator API ──────────────────────────────────────────────────────────

        post("/api/v1/storybook/decorators") {
            val req = call.receive<RegisterDecoratorRequest>()
            if (req.decoratorId.isBlank() || req.decoratorKey.isBlank() || req.title.isBlank() || req.renderRef.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "decoratorId, decoratorKey, title, and renderRef are required"))
                return@post
            }
            if (req.priority <= 0) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "priority must be positive (BR-5)"))
                return@post
            }
            val decType = runCatching { DecoratorType.valueOf(req.type.uppercase()) }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "type must be one of ${DecoratorType.entries.joinToString()}"))
                return@post
            }
            val scope = runCatching { DecoratorScope.valueOf(req.scope.uppercase()) }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "scope must be one of ${DecoratorScope.entries.joinToString()}"))
                return@post
            }
            val lifecycle = runCatching { DecoratorLifecycle.valueOf(req.lifecycle.uppercase()) }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "lifecycle must be one of ${DecoratorLifecycle.entries.joinToString()}"))
                return@post
            }
            val decoratorId = DecoratorId(req.decoratorId)
            val decoratorKey = DecoratorKey(req.decoratorKey)
            // BR-3: key must be globally unique
            val keyEvents = eventStore.query(EventQuery(setOf(decoratorKeyTag(decoratorKey)))).events
            if (DecoratorKeyUniquenessDecision().also { it.applyAll(keyEvents) }.isTaken(req.decoratorKey)) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "decoratorKey '${req.decoratorKey}' already in use (BR-3)"))
                return@post
            }
            if (DecoratorDecision().also { it.applyAll(eventStore.query(EventQuery(setOf(decoratorTag(decoratorId)))).events) }.exists) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "decorator '${req.decoratorId}' already exists"))
                return@post
            }
            eventStore.append(listOf(DecoratorRegistered(
                decoratorId = decoratorId, decoratorKey = decoratorKey, title = req.title,
                type = decType, renderRef = req.renderRef, priority = req.priority,
                scope = scope, lifecycle = lifecycle, configurationRef = req.configurationRef,
                registeredAtEpochMs = System.currentTimeMillis(),
            )), condition = null)
            call.respond(HttpStatusCode.Created, DecoratorResponse(
                decoratorId = req.decoratorId, decoratorKey = req.decoratorKey, title = req.title,
                type = decType.name, scope = scope.name, priority = req.priority,
                lifecycle = lifecycle.name, configurationRef = req.configurationRef,
            ))
        }

        get("/api/v1/storybook/decorators") {
            val events = eventStore.query(EventQuery(setOf("decorators"))).events
            val byId = mutableMapOf<String, DecoratorDecision>()
            events.forEach { ev ->
                when (ev) {
                    is DecoratorRegistered -> byId.getOrPut(ev.decoratorId.value) { DecoratorDecision() }.apply(ev)
                    is DecoratorMetadataUpdated -> byId[ev.decoratorId.value]?.apply(ev)
                    is DecoratorPriorityChanged -> byId[ev.decoratorId.value]?.apply(ev)
                    is DecoratorArchived -> byId[ev.decoratorId.value]?.apply(ev)
                    else -> Unit
                }
            }
            call.respond(byId.values.filter { !it.archived }.sortedWith(compareBy({ it.priority }, { it.decoratorKey }))
                .map { DecoratorResponse(it.decoratorKey, it.decoratorKey, it.title, it.type.name, it.scope.name, it.priority, it.lifecycle.name) })
        }

        get("/api/v1/storybook/decorators/{decoratorId}") {
            val did = call.parameters["decoratorId"]?.takeIf { it.isNotBlank() }
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "decoratorId required"))
            val decision = DecoratorDecision().also { it.applyAll(eventStore.query(EventQuery(setOf(decoratorTag(DecoratorId(did))))).events) }
            if (!decision.exists || decision.archived) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "decorator not found")); return@get
            }
            call.respond(DecoratorResponse(did, decision.decoratorKey, decision.title, decision.type.name, decision.scope.name, decision.priority, decision.lifecycle.name))
        }

        put("/api/v1/storybook/decorators/{decoratorId}/priority") {
            val did = call.parameters["decoratorId"]?.takeIf { it.isNotBlank() }
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "decoratorId required"))
            val decoratorId = DecoratorId(did)
            val decision = DecoratorDecision().also { it.applyAll(eventStore.query(EventQuery(setOf(decoratorTag(decoratorId)))).events) }
            if (!decision.exists) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "decorator not found")); return@put
            }
            if (decision.archived) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "decorator is archived")); return@put
            }
            val req = call.receive<UpdateDecoratorPriorityRequest>()
            if (req.priority <= 0) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "priority must be positive (BR-5)")); return@put
            }
            eventStore.append(listOf(DecoratorPriorityChanged(decoratorId, req.priority, System.currentTimeMillis())), condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to "DecoratorPriorityChanged", "newPriority" to req.priority.toString()))
        }

        post("/api/v1/storybook/decorators/{decoratorId}/stories/{storyId}") {
            val did = call.parameters["decoratorId"]?.takeIf { it.isNotBlank() }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "decoratorId required"))
            val sid = call.parameters["storyId"]?.takeIf { it.isNotBlank() }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "storyId required"))
            val decoratorId = DecoratorId(did)
            val storyId = StoryId(sid)
            val decision = DecoratorDecision().also { it.applyAll(eventStore.query(EventQuery(setOf(decoratorTag(decoratorId)))).events) }
            if (!decision.exists) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "decorator not found")); return@post
            }
            if (decision.archived) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "decorator is archived")); return@post
            }
            // BR-7: GLOBAL scope decorator may not be bound per-story
            if (decision.scope == DecoratorScope.GLOBAL) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "GLOBAL-scope decorators cannot be story-linked (BR-7)")); return@post
            }
            val story = StoryDecision().also { it.applyAll(eventStore.query(EventQuery(setOf(storyTag(storyId)))).events) }
            if (!story.exists || story.archived) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "story not found")); return@post
            }
            eventStore.append(listOf(DecoratorLinkedToStory(decoratorId, storyId, System.currentTimeMillis())), condition = null)
            call.respond(HttpStatusCode.Created, mapOf("recorded" to "DecoratorLinkedToStory"))
        }

        post("/api/v1/storybook/decorators/{decoratorId}/scenarios/{scenarioId}") {
            val did = call.parameters["decoratorId"]?.takeIf { it.isNotBlank() }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "decoratorId required"))
            val scenId = call.parameters["scenarioId"]?.takeIf { it.isNotBlank() }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "scenarioId required"))
            val decoratorId = DecoratorId(did)
            val scenarioId = ScenarioId(scenId)
            val decision = DecoratorDecision().also { it.applyAll(eventStore.query(EventQuery(setOf(decoratorTag(decoratorId)))).events) }
            if (!decision.exists) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "decorator not found")); return@post
            }
            if (decision.archived) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "decorator is archived")); return@post
            }
            // BR-8: scenario must exist
            val scenario = ScenarioDecision().also { it.applyAll(eventStore.query(EventQuery(setOf(scenarioTag(scenarioId)))).events) }
            if (!scenario.exists || scenario.archived) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "scenario not found (BR-8)")); return@post
            }
            eventStore.append(listOf(DecoratorLinkedToScenario(decoratorId, scenarioId, System.currentTimeMillis())), condition = null)
            call.respond(HttpStatusCode.Created, mapOf("recorded" to "DecoratorLinkedToScenario"))
        }

        delete("/api/v1/storybook/decorators/{decoratorId}") {
            val did = call.parameters["decoratorId"]?.takeIf { it.isNotBlank() }
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "decoratorId required"))
            val decoratorId = DecoratorId(did)
            val decision = DecoratorDecision().also { it.applyAll(eventStore.query(EventQuery(setOf(decoratorTag(decoratorId)))).events) }
            if (!decision.exists) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "decorator not found")); return@delete
            }
            if (decision.archived) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "decorator already archived")); return@delete
            }
            eventStore.append(listOf(DecoratorArchived(decoratorId, System.currentTimeMillis())), condition = null)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to "DecoratorArchived"))
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
