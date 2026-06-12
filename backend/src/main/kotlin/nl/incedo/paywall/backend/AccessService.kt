package nl.incedo.paywall.backend

import nl.incedo.paywall.access.AccessDecision
import nl.incedo.paywall.access.AccessDecisionEngine
import nl.incedo.paywall.access.AccessRequest
import nl.incedo.paywall.access.Article
import nl.incedo.paywall.access.StrategyConfig
import nl.incedo.paywall.access.Subject
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
    /**
     * Outbound port to the CEP: does marketing decisioning advise a gate for
     * this subject/article? Propensity scoring and thresholds live in the CEP
     * (Doc 7 scope boundary). Default: no advice — the floor rule (PW-42)
     * remains the only dynamic gate trigger.
     */
    private val cepGateAdvice: suspend (Subject, Article) -> Boolean = { _, _ -> false },
) {

    data class Outcome(val decision: AccessDecision, val variant: Variant, val meterUsedAfter: Int)

    suspend fun decide(subject: Subject, article: Article): Outcome {
        val variant = VariantAssigner.assign(subject.visitorId, experiment)
        val period = currentPeriod()
        val now = clock()

        // One union query covers visitor + linked user meter state (MT-03)
        // and entitlements; grants are article-tagged, fetched alongside.
        val subjectTags = buildSet {
            add("subject:${SubjectId.of(subject.visitorId).value}")
            subject.userId?.let { add("subject:${SubjectId.of(it).value}") }
            add("article:${article.id.value}")
        }
        val events = eventStore.query(EventQuery(subjectTags)).events

        val meter = MeterDecision(period).also { it.applyAll(events) }
        val entitlement = EntitlementDecision().also { it.applyAll(events) }
        val grant = GrantDecision(article.id).also { it.applyAll(events) }

        // The CEP is consulted only when the variant's strategy is dynamic —
        // every other strategy is purely mechanical (no marketing decisioning).
        val cepGateAdvised = if (variant.strategy is StrategyConfig.Dynamic) {
            cepGateAdvice(subject, article)
        } else {
            false
        }

        val decision = AccessDecisionEngine.decide(
            AccessRequest(
                article = article,
                subject = subject,
                strategy = variant.strategy,
                entitlement = entitlement,
                grant = grant,
                meter = meter,
                cepGateAdvised = cepGateAdvised,
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
