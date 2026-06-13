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
import nl.incedo.paywall.analytics.VariantStatsProjection
import nl.incedo.paywall.analytics.wallEventShardTags
import nl.incedo.paywall.analytics.WallEventRecorded
import nl.incedo.paywall.analytics.WallEventType
import nl.incedo.paywall.backend.auth.CiamJwtValidator
import nl.incedo.paywall.backend.auth.OriginTrust
import nl.incedo.paywall.backend.auth.StaffRole
import nl.incedo.paywall.backend.auth.requireStaff
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
import nl.incedo.paywall.grants.ShareTokenIssued
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
)

private suspend fun io.ktor.server.application.ApplicationCall.respondSaveResult(result: WallService.SaveResult) {
    when (result) {
        is WallService.SaveResult.Saved -> respond(result.view.toResponse())
        is WallService.SaveResult.NotFound -> respond(HttpStatusCode.NotFound, mapOf("error" to "unknown wall"))
        is WallService.SaveResult.VersionConflict ->
            respond(HttpStatusCode.Conflict, mapOf("error" to "version conflict", "currentVersion" to result.current.toString()))
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

    embeddedServer(CIO, port = 8080) {
        module(service, eventStore, jwtValidator, originSecret = originSecret, configStore = configStore, shareTokenService = shareTokenService)
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
) {
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
    // INF-02: every request except the health probe must arrive through the
    // edge (shared secret). No-op in dev where no secret is configured.
    intercept(ApplicationCallPipeline.Plugins) {
        if (call.request.path() != "/health" && !originTrust.verify(call)) finish()
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
            val subject = Subject(VisitorId(request.visitorId), userId)
            val clientIp = call.request.headers["X-Forwarded-For"]?.substringBefore(",")?.trim()
                ?: call.request.local.remoteAddress
            // MT-05: trust the verified-bot signal only when the origin secret is
            // configured — proves the request came through the edge (INF-02/BP-02).
            val isVerifiedCrawler = originSecret != null &&
                call.request.headers["X-Verified-Bot"] == "true"
            val outcome = service.decide(
                subject = subject,
                article = Article(ArticleId(request.articleId), tier),
                channel = request.channel,
                isBot = isBotUserAgent(call.request.headers[HttpHeaders.UserAgent]),
                isSuspicious = service.recordIpAndCheckSuspicious(subject, clientIp),
                isVerifiedCrawler = isVerifiedCrawler,
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
            if (stored.tier == ContentTier.PREMIUM) {
                call.response.headers.append("X-Robots-Tag", "noarchive")
            }
            val response = when (val decision = outcome.decision) {
                is nl.incedo.paywall.access.AccessDecision.Full -> {
                    // BP-07: full premium responses must not be cached by shared caches
                    // (CDN/proxy); private is sufficient since the body is personalized.
                    if (stored.tier == ContentTier.PREMIUM) {
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
                        structuredData = if (isCrawlerAccess && stored.tier == ContentTier.PREMIUM) {
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
            // AC-03: invalidate the per-session cache so the change takes effect
            // immediately rather than waiting for the 5-minute TTL to expire.
            service.invalidateEntitlementCache(change.subjectId)
            call.respond(HttpStatusCode.Accepted, mapOf("recorded" to event::class.simpleName))
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
                    // BP-04: premium content exposes teaser only — never the full body.
                    val description = when (article.tier) {
                        ContentTier.PREMIUM -> ArticleRepository.teaserOf(article)
                        ContentTier.FREE -> article.body
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
            val request = call.receive<AccountDeletionRequest>()
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
    }
}
