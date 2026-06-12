package nl.incedo.paywall.backend

import nl.incedo.paywall.access.AccessDecision
import nl.incedo.paywall.access.AccessDecisionEngine
import nl.incedo.paywall.access.AccessRequest
import nl.incedo.paywall.access.Article
import nl.incedo.paywall.access.Subject
import nl.incedo.paywall.accounts.IdentityLinkDecision
import nl.incedo.paywall.accounts.IdentityLinked
import nl.incedo.paywall.analytics.WallEventRecorded
import nl.incedo.paywall.analytics.WallEventType
import nl.incedo.paywall.cep.CepAdviceDecision
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.core.port.EventStore
import nl.incedo.paywall.entitlements.EntitlementDecision
import nl.incedo.paywall.experiments.ExperimentDefinition
import nl.incedo.paywall.experiments.Variant
import nl.incedo.paywall.experiments.VariantAssigner
import nl.incedo.paywall.grants.GrantDecision
import nl.incedo.paywall.metering.MeterDecision
import nl.incedo.paywall.metering.meterTag
import nl.incedo.paywall.metering.MeterPeriod
import nl.incedo.paywall.metering.RecordArticleRead
import nl.incedo.paywall.metering.RecordArticleReadHandler

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
) {

    data class Outcome(val decision: AccessDecision, val variant: Variant, val meterUsedAfter: Int)

    suspend fun decide(subject: Subject, article: Article, channel: String = "web"): Outcome {
        val variant = VariantAssigner.assign(subject.visitorId, experiment)
        val period = currentPeriod()
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
        val entitlement = EntitlementDecision().also { it.applyAll(events) }
        val grant = GrantDecision(article.id).also { it.applyAll(events) }
        // CEP advice arrives as published events (same union query — they are
        // subject-tagged); the access layer acts on them, it never calls the CEP.
        val cepAdvice = CepAdviceDecision().also { it.applyAll(events) }

        val decision = AccessDecisionEngine.decide(
            AccessRequest(
                article = article,
                subject = subject,
                strategy = variant.strategy,
                entitlement = entitlement,
                grant = grant,
                meter = meter,
                cepAdvice = cepAdvice,
                nowEpochMs = now,
            ),
        )

        var usedAfter = meter.used
        if (decision is AccessDecision.Full && decision.countsTowardMeter) {
            RecordArticleReadHandler(eventStore)
                .handle(RecordArticleRead(subject.subjectId, article.id, period))
            usedAfter += 1
        }

        logWallEvent(subject, article, variant, channel, decision, now)
        return Outcome(decision, variant, usedAfter)
    }

    /**
     * AN-01/02: server-side funnel logging. A gate logs WALL_SHOWN with the
     * decision context (AN-03); a counted read logs ARTICLE_READ (MT-04 —
     * teaser/gate views never count as reads).
     */
    private suspend fun logWallEvent(
        subject: Subject,
        article: Article,
        variant: Variant,
        channel: String,
        decision: AccessDecision,
        now: Long,
    ) {
        val event = when (decision) {
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
                },
            )
            is AccessDecision.Full ->
                if (decision.countsTowardMeter) {
                    WallEventRecorded(
                        eventType = WallEventType.ARTICLE_READ,
                        subjectId = subject.subjectId,
                        variant = variant.name,
                        channel = channel,
                        occurredAtEpochMs = now,
                        articleId = article.id,
                        context = mapOf("reason" to decision.reason.name.lowercase()),
                    )
                } else {
                    null // entitled/grant/free full reads are page views, not funnel reads
                }
        }
        if (event != null) eventStore.append(listOf(event), condition = null)
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
