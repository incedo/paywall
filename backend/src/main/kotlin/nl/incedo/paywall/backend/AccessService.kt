package nl.incedo.paywall.backend

import nl.incedo.paywall.access.AccessDecision
import nl.incedo.paywall.access.AccessDecisionEngine
import nl.incedo.paywall.access.AccessRequest
import nl.incedo.paywall.access.Article
import nl.incedo.paywall.access.Subject
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

    suspend fun decide(subject: Subject, article: Article): Outcome {
        val variant = VariantAssigner.assign(subject.visitorId, experiment)
        val period = currentPeriod()
        val now = clock()

        // One union query covers the subject's events: composite meter tags
        // (DM-05 — only the current period) for visitor + linked user (MT-03),
        // subject tags for entitlements/CEP advice, and the article tag for grants.
        val subjectIds = buildList {
            add(SubjectId.of(subject.visitorId))
            subject.userId?.let { add(SubjectId.of(it)) }
        }
        val queryTags = buildSet {
            subjectIds.forEach { id ->
                add(meterTag(id, period))
                add("subject:${id.value}")
            }
            add("article:${article.id.value}")
        }
        val events = eventStore.query(EventQuery(queryTags)).events

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
        return Outcome(decision, variant, usedAfter)
    }
}
