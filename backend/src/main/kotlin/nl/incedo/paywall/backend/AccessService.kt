package nl.incedo.paywall.backend

import java.util.concurrent.ConcurrentHashMap
import nl.incedo.paywall.access.AccessDecision
import nl.incedo.paywall.access.AccessDecisionEngine
import nl.incedo.paywall.access.AccessRequest
import nl.incedo.paywall.access.Article
import nl.incedo.paywall.access.StrategyConfig
import nl.incedo.paywall.access.Subject
import nl.incedo.paywall.accounts.IdentityLinkDecision
import nl.incedo.paywall.accounts.IdentityLinked
import nl.incedo.paywall.analytics.SoftGateDismissed
import nl.incedo.paywall.analytics.WallEventRecorded
import nl.incedo.paywall.analytics.WallEventType
import nl.incedo.paywall.cep.CepAdviceDecision
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.PlanId
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.SubscriptionId
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.core.port.EventStore
import nl.incedo.paywall.entitlements.EntitlementDecision
import nl.incedo.paywall.entitlements.EntitlementGranted
import nl.incedo.paywall.experiments.ExperimentDefinition
import nl.incedo.paywall.experiments.Variant
import nl.incedo.paywall.experiments.VariantAssigner
import nl.incedo.paywall.access.HeuristicPropensityScorer
import nl.incedo.paywall.access.PropensityScorer
import nl.incedo.paywall.grants.GrantDecision
import nl.incedo.paywall.metering.MeterDecision
import nl.incedo.paywall.metering.meterTag
import nl.incedo.paywall.metering.MeterPeriod
import nl.incedo.paywall.metering.RecordArticleRead
import nl.incedo.paywall.metering.RecordArticleReadHandler

// AC-03: entitlement results are cached per authenticated user for at most
// 5 minutes; cancellation/expiry propagate within that window.
private const val ENTITLEMENT_CACHE_TTL_MS = 5 * 60 * 1000L

private data class CachedEntitlement(val valid: Boolean, val expiresAtMs: Long)

// FGA-05: grant check results are cached per subject+article for ≤ 60 s.
// Invalidated immediately when a grant is issued or revoked (FGA-05 freshness).
private const val GRANT_CACHE_TTL_MS = 60_000L

private data class CachedGrantResult(val hasGrant: Boolean, val grantedBy: String?, val expiresAtMs: Long)

// AC-06: flag when the same authenticated identity is seen from more than
// this many distinct IPs — a signal for account-sharing analysis.
private const val SUSPICIOUS_IP_THRESHOLD = 5

/**
 * Application service behind `POST /api/v1/decide` (API-05): assigns the
 * variant (PW-05/EX-01), builds the decision models from one tagged event
 * query, runs the engine, and counts the read when the decision says so
 * (MT-04). Wall-event logging (AN-*) hooks in here later.
 */
class AccessService(
    private val eventStore: EventStore,
    private val experiment: ExperimentDefinition,
    private val clock: () -> Long,
    private val currentPeriod: () -> MeterPeriod,
    /** DY-04: pluggable scorer; defaults to the phase-1 heuristic (DY-01). */
    private val propensityScorer: PropensityScorer = HeuristicPropensityScorer(),
    /**
     * API-03: when set, this loader is called on every decide() to obtain the
     * current (hot-reloadable) experiment config. Takes precedence over
     * [experiment]. Used in production via [ConfigStore]; tests use the fixed
     * [experiment] to keep setup concise.
     */
    private val experimentLoader: (suspend () -> ExperimentDefinition)? = null,
) {

    private val entitlementCache = ConcurrentHashMap<String, CachedEntitlement>()
    private val grantCache = ConcurrentHashMap<String, CachedGrantResult>()

    /** AC-06: per-user set of distinct IPs seen in this process's lifetime. */
    private val ipsByUser = ConcurrentHashMap<String, MutableSet<String>>()

    /** AC-03: force a cache miss for the given subject on the next decide call.
     *  Called by the integration endpoint whenever an entitlement changes. */
    fun invalidateEntitlementCache(subjectId: String) {
        val userId = subjectId.removePrefix("user:")
        if (userId != subjectId) entitlementCache.remove(userId)
    }

    /** FGA-05: invalidate the grant result cache for a subject+article pair.
     *  Called from the grants endpoint after every GrantIssued/GrantRevoked append. */
    fun invalidateGrantCache(subjectId: String, articleId: String) {
        grantCache.remove("$subjectId#$articleId")
    }

    /**
     * UP-06: return the current variant name for a subject (EX-03 applies: userId key
     * wins over visitorId). Used by the offer endpoint to pass variant context to the
     * CEP so offer strategies can be A/B-tested alongside paywall variants.
     */
    suspend fun variantFor(subject: Subject): String {
        val exp = experimentLoader?.invoke() ?: experiment
        return VariantAssigner.assign(subject, exp).name
    }

    /** AN-11/EX-04: return the active experiment definition (hot-reloadable). */
    suspend fun currentExperiment(): ExperimentDefinition = experimentLoader?.invoke() ?: experiment

    /**
     * AC-06: record the client IP for an authenticated user and return true
     * when the number of distinct IPs exceeds the suspicious threshold.
     * Anonymous (no userId) requests are not tracked — no stable identity.
     */
    fun recordIpAndCheckSuspicious(subject: Subject, clientIp: String?): Boolean {
        val userId = subject.userId?.value ?: return false
        if (clientIp.isNullOrBlank()) return false
        val ips = ipsByUser.getOrPut(userId) {
            java.util.Collections.newSetFromMap(ConcurrentHashMap())
        }
        ips.add(clientIp)
        return ips.size > SUSPICIOUS_IP_THRESHOLD
    }

    data class Outcome(
        val decision: AccessDecision,
        val variant: Variant,
        val meterUsedAfter: Int,
        /** The resolved meter limit for metered variants (PW-22/23 indicator). */
        val meterLimit: Int? = null,
        /**
         * DY-05: what each other (non-killed) variant would have decided for the
         * same visitor and article, using the same decision models (meter, entitlement,
         * grant). Enables offline A/B counterfactual comparison without re-playing events.
         * Key = variant name, value = "gate" | "full".
         */
        val counterfactuals: Map<String, String> = emptyMap(),
    )

    suspend fun decide(
        subject: Subject,
        article: Article,
        channel: String = "web",
        isBot: Boolean = false,
        isSuspicious: Boolean = false,
        /**
         * MT-05: the edge (INF-01) verified this is a legitimate search crawler
         * via reverse DNS / Cloudflare verified-bot signal. Never trust UA alone.
         * When true, the decide call is exempt from metering and gating (SEO-02)
         * and logs a bot-flagged ARTICLE_READ for analytics (AN-05).
         */
        isVerifiedCrawler: Boolean = false,
        /**
         * MT-04: a prefetch read (Sec-Purpose: prefetch) must not burn a meter credit.
         * The edge sets this flag; the origin never trusts raw browser prefetch headers.
         */
        isPrefetch: Boolean = false,
        /**
         * EX-05: staff debug override — when set, bypasses VariantAssigner and
         * uses the named variant. Unknown names fall back to normal assignment.
         * Analytics events are suppressed so QA runs do not pollute funnel data.
         */
        forceVariant: String? = null,
        /**
         * NFR-14: correlation ID for structured log tracing. When provided, it is
         * included in the WallEventRecorded context so decisions are traceable
         * across the origin log, event store, and CEP.
         */
        correlationId: String? = null,
        /**
         * DY-06: caller-supplied propensity score (0–100) from an external model
         * (e.g. a CDP Likelihood-to-Churn score). When present and in range,
         * replaces the heuristic for the Dynamic strategy so the scoring interface
         * can accept externally trained models without changing callers.
         */
        externalScore: Int? = null,
        /**
         * NFR-15: variant names that have been killed by an admin; visitors assigned
         * to a killed variant receive full access so a misbehaving paywall can be
         * neutralised without a deployment. Empty in normal operation.
         */
        killedVariants: Set<String> = emptySet(),
    ): Outcome {
        if (isVerifiedCrawler) {
            val variant = VariantAssigner.assign(subject, experimentLoader?.invoke() ?: experiment)
            val now = clock()
            // AN-05: bot traffic is flagged in events, not silently dropped.
            val crawlerEvent = WallEventRecorded(
                eventType = WallEventType.ARTICLE_READ,
                subjectId = subject.subjectId,
                variant = variant.name,
                channel = channel,
                occurredAtEpochMs = now,
                articleId = article.id,
                context = mapOf("reason" to "verified_crawler", "bot" to "true"),
            )
            eventStore.append(listOf(crawlerEvent), condition = null)
            return Outcome(nl.incedo.paywall.access.AccessDecision.Full(nl.incedo.paywall.access.AccessReason.VERIFIED_CRAWLER), variant, meterUsedAfter = 0)
        }

        // API-03: use the hot-reloadable config if available; fall back to the
        // static experiment passed at construction time (tests).
        val currentExperiment = experimentLoader?.invoke() ?: experiment

        // EX-03: authenticated subjects use userId as the assignment key so the
        // variant is stable across devices after login.
        // EX-05: staff debug override uses the forced variant name when present.
        val variant = if (forceVariant != null) {
            currentExperiment.variants.firstOrNull { it.name == forceVariant }
                ?: VariantAssigner.assign(subject, currentExperiment)
        } else {
            VariantAssigner.assign(subject, currentExperiment)
        }
        // NFR-15: killed variant → full access immediately; no metering, no events.
        if (variant.name in killedVariants) {
            return Outcome(nl.incedo.paywall.access.AccessDecision.Full(nl.incedo.paywall.access.AccessReason.VARIANT_KILLED), variant, meterUsedAfter = 0)
        }
        // MT-06: period type is per variant config; resolved via periodFor().
        val period = periodFor(variant.strategy)
        val now = clock()

        // One union query covers the subject's events: composite meter tags
        // (DM-05 — only the current period) for visitor + linked user (MT-03),
        // subject tags for entitlements/CEP advice/identity links, and the
        // article tag for grants.
        val initialSubjects = buildSet {
            add(SubjectId.of(subject.visitorId))
            subject.userId?.let { add(SubjectId.of(it)) }
        }
        var events = eventStore.query(EventQuery(queryTags(initialSubjects, article, period))).events

        // MT-13: identity link events expand the subject set — one person, one
        // meter, across devices. Only when links exist does this cost a second
        // (equally bounded) query for the newly discovered subjects.
        val links = IdentityLinkDecision().also { it.applyAll(events) }

        // US-04: on an authenticated request the visitor is linked to the user
        // (once — idempotent), so the anonymous meter state merges (MT-03)
        val userId = subject.userId
        if (userId != null) {
            val visitorSubject = SubjectId.of(subject.visitorId)
            val userSubject = SubjectId.of(userId)
            if (!links.isLinked(visitorSubject, userSubject)) {
                val link = IdentityLinked(visitorSubject, userSubject, cause = "login")
                eventStore.append(listOf(link), condition = null)
                links.apply(link)
            }
        }

        val allSubjects = links.linkedSubjects(initialSubjects)
        if (allSubjects != initialSubjects) {
            events = eventStore.query(EventQuery(queryTags(allSubjects, article, period))).events
        }

        val meter = MeterDecision(period).also { it.applyAll(events) }
        val entitlement = cachedEntitlement(subject, events, now)
        val grant = cachedGrant(article.id, allSubjects, events, now)
        // CEP advice arrives as published events (same union query — they are
        // subject-tagged); the access layer acts on them, it never calls the CEP.
        val cepAdvice = CepAdviceDecision().also { it.applyAll(events) }

        // DY-01/DY-06: compute propensity score. Caller may supply an external score
        // (DY-06) from a CDP or ML model; if provided and in range it takes precedence.
        // Otherwise the heuristic is used with variant-specific weights (DY-01).
        val propensityScore = externalScore?.takeIf { it in 0..100 }
            ?: run {
                val scorer = (variant.strategy as? StrategyConfig.Dynamic)
                    ?.let { HeuristicPropensityScorer(it.scorerWeights) }
                    ?: propensityScorer
                scorer.score(events, meter.used, now, subject.registered)
            }

        // AC-13: check for a recent soft-gate dismissal in the subject's event stream.
        // Dismissals are subject-tagged, so they appear in the same event set loaded above.
        val softGateDismissed = events.filterIsInstance<SoftGateDismissed>()
            .any { it.isWithinSession(now) }

        val decision = AccessDecisionEngine.decide(
            AccessRequest(
                article = article,
                subject = subject,
                strategy = variant.strategy,
                entitlement = entitlement,
                grant = grant,
                meter = meter,
                cepAdvice = cepAdvice,
                propensityScore = propensityScore,
                softGateDismissed = softGateDismissed,
                registrationWall = variant.registrationWall, // PW-50
                nowEpochMs = now,
            ),
        )

        var usedAfter = meter.used
        // MT-04/MT-05: bots (non-crawler) and prefetch reads must not consume a
        // meter credit. isVerifiedCrawler already returned early above; this guard
        // covers the remaining bot/prefetch signals before writing MeterIncremented.
        if (decision is AccessDecision.Full && decision.countsTowardMeter && !isBot && !isPrefetch) {
            RecordArticleReadHandler(eventStore)
                .handle(RecordArticleRead(subject.subjectId, article.id, period))
            usedAfter += 1
        }

        // DY-05: counterfactuals — reuse the same decision models (no extra queries).
        // Only computed for real (non-debug) calls so QA runs don't inflate the map.
        val counterfactuals: Map<String, String> = if (forceVariant == null) {
            currentExperiment.variants
                .filter { it.name != variant.name && it.name !in killedVariants }
                .associate { other ->
                    val cfDecision = AccessDecisionEngine.decide(
                        AccessRequest(
                            article = article,
                            subject = subject,
                            strategy = other.strategy,
                            entitlement = entitlement,
                            grant = grant,
                            meter = meter,
                            cepAdvice = cepAdvice,
                            propensityScore = propensityScore,
                            softGateDismissed = softGateDismissed,
                            registrationWall = other.registrationWall,
                            nowEpochMs = now,
                        )
                    )
                    other.name to if (cfDecision is AccessDecision.Full) "full" else "gate"
                }
        } else emptyMap()

        // EX-05: suppress analytics for debug overrides so QA runs don't skew funnel data.
        if (forceVariant == null) {
            logWallEvent(subject, article, variant, channel, decision, grant, now, isBot, isSuspicious, propensityScore, correlationId, counterfactuals)
        }

        val meterLimit = (variant.strategy as? StrategyConfig.Metered)?.let { strategy ->
            if (subject.registered) strategy.registeredLimit ?: strategy.limit else strategy.limit
        }
        return Outcome(decision, variant, usedAfter, meterLimit, counterfactuals)
    }

    /**
     * AC-03: return an EntitlementDecision for the subject, using a per-user
     * in-memory cache (TTL 5 min) for authenticated requests. Unauthenticated
     * (anonymous) requests always build from events — no userId to key on.
     */
    private fun cachedEntitlement(
        subject: Subject,
        events: List<nl.incedo.paywall.core.DomainEvent>,
        now: Long,
    ): EntitlementDecision {
        val userId = subject.userId
        if (userId != null) {
            val cached = entitlementCache[userId.value]
            if (cached != null && cached.expiresAtMs > now) {
                return if (cached.valid) {
                    EntitlementDecision().apply {
                        apply(
                            EntitlementGranted(
                                subjectId = subject.subjectId,
                                planId = PlanId("cached"),
                                subscriptionRef = SubscriptionId("cached-${userId.value}"),
                                validUntilEpochMs = null,
                            ),
                        )
                    }
                } else {
                    EntitlementDecision()
                }
            }
        }
        val decision = EntitlementDecision().also { it.applyAll(events) }
        if (userId != null) {
            entitlementCache[userId.value] = CachedEntitlement(
                valid = decision.hasValidEntitlement(now),
                expiresAtMs = now + ENTITLEMENT_CACHE_TTL_MS,
            )
        }
        return decision
    }

    /**
     * AN-01/02: server-side funnel logging. A gate logs WALL_SHOWN with the
     * decision context (AN-03); a counted metered read or FGA grant read logs
     * ARTICLE_READ (MT-04 / FGA-04). Other full reads (entitled, free) are
     * page views and are not tracked here — the client emits those.
     */
    private suspend fun logWallEvent(
        subject: Subject,
        article: Article,
        variant: Variant,
        channel: String,
        decision: AccessDecision,
        grant: GrantDecision,
        now: Long,
        isBot: Boolean,
        isSuspicious: Boolean,
        propensityScore: Int,
        correlationId: String? = null,
        counterfactuals: Map<String, String> = emptyMap(), // DY-05
    ) {
        val event: WallEventRecorded? = when (decision) {
            is AccessDecision.Gated -> WallEventRecorded(
                eventType = WallEventType.WALL_SHOWN,
                subjectId = subject.subjectId,
                variant = variant.name,
                channel = channel,
                occurredAtEpochMs = now,
                articleId = article.id,
                context = buildMap {
                    put("wallType", variant.name)
                    decision.meterUsed?.let { put("meterUsed", it.toString()) }
                    decision.meterLimit?.let { put("meterLimit", it.toString()) }
                    put("score", propensityScore.toString()) // PW-43
                    // AN-03: log thresholds for dynamic so decisions are reproducible in analysis (DY-03)
                    val dyn = variant.strategy as? nl.incedo.paywall.access.StrategyConfig.Dynamic
                    dyn?.let { put("tSoft", it.tSoft.toString()); put("tHard", it.tHard.toString()) }
                    if (isBot) put("bot", "true")
                    if (isSuspicious) put("suspicious_ip", "true")
                    correlationId?.let { put("request_id", it) } // NFR-14
                    counterfactuals.forEach { (name, outcome) -> put("cf_$name", outcome) } // DY-05
                },
            )
            is AccessDecision.Full -> when {
                // FGA-04/AG-03: grant-based access is distinguishable in analytics by granted_by.
                // "ad_gated" grants log granted_by=ad_gated so their cannibalization of
                // subscriptions is measurable per requirement AG-03.
                decision.reason == nl.incedo.paywall.access.AccessReason.GRANT ->
                    WallEventRecorded(
                        eventType = WallEventType.ARTICLE_READ,
                        subjectId = subject.subjectId,
                        variant = variant.name,
                        channel = channel,
                        occurredAtEpochMs = now,
                        articleId = article.id,
                        context = buildMap {
                            put("reason", "fga_grant")
                            put("granted_by", grant.liveGrantedBy(now) ?: "unknown")
                            put("score", propensityScore.toString())
                            if (isBot) put("bot", "true")
                            if (isSuspicious) put("suspicious_ip", "true")
                            correlationId?.let { put("request_id", it) } // NFR-14
                            counterfactuals.forEach { (name, outcome) -> put("cf_$name", outcome) } // DY-05
                        },
                    )
                decision.countsTowardMeter ->
                    WallEventRecorded(
                        eventType = WallEventType.ARTICLE_READ,
                        subjectId = subject.subjectId,
                        variant = variant.name,
                        channel = channel,
                        occurredAtEpochMs = now,
                        articleId = article.id,
                        context = buildMap {
                            put("reason", decision.reason.name.lowercase())
                            put("score", propensityScore.toString()) // PW-43
                            if (isBot) put("bot", "true")
                            if (isSuspicious) put("suspicious_ip", "true")
                            correlationId?.let { put("request_id", it) } // NFR-14
                            counterfactuals.forEach { (name, outcome) -> put("cf_$name", outcome) } // DY-05
                        },
                    )
                else -> null // entitled/free full reads are page views, not funnel events
            }
        }
        if (event != null) eventStore.append(listOf(event), condition = null)
    }

    /**
     * FGA-05: return the grant decision for the subject+article, using a 60-second
     * per-subject+article cache. Subject-scoped filtering prevents cross-subject
     * grant leakage via the shared article tag in the union event query.
     */
    private fun cachedGrant(
        articleId: nl.incedo.paywall.core.ArticleId,
        subjects: Set<SubjectId>,
        events: List<nl.incedo.paywall.core.DomainEvent>,
        now: Long,
    ): GrantDecision {
        // Use the lexicographically smallest subject ID as the stable cache key.
        val primarySubject = subjects.minByOrNull { it.value } ?: return GrantDecision(articleId, subjects)
        val cacheKey = "${primarySubject.value}#${articleId.value}"
        val cached = grantCache[cacheKey]
        if (cached != null && cached.expiresAtMs > now) {
            return GrantDecision.ofCached(articleId, cached.hasGrant, cached.grantedBy)
        }
        val decision = GrantDecision(articleId, subjects).also { it.applyAll(events) }
        grantCache[cacheKey] = CachedGrantResult(
            hasGrant = decision.hasLiveGrant(now),
            grantedBy = decision.liveGrantedBy(now),
            expiresAtMs = now + GRANT_CACHE_TTL_MS,
        )
        return decision
    }

    /**
     * MT-06: resolve the meter period for the given strategy. Only "calendar_month"
     * is implemented; any other periodType is rejected at config-load (API-03).
     * This error() fires only if a legacy config bypassed that validation.
     */
    internal fun periodFor(strategy: StrategyConfig): MeterPeriod =
        when {
            strategy is StrategyConfig.Metered && strategy.periodType != "calendar_month" ->
                error("unsupported meterPeriodType '${strategy.periodType}' — rejected at config-load (MT-06)")
            else -> currentPeriod()
        }

    private fun queryTags(subjects: Set<SubjectId>, article: Article, period: MeterPeriod): Set<String> =
        buildSet {
            subjects.forEach { id ->
                add(meterTag(id, period))
                add("subject:${id.value}")
            }
            add("article:${article.id.value}")
        }
}
