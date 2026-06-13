package nl.incedo.paywall.access

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.UserId
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.cep.CepAdviceDecision
import nl.incedo.paywall.entitlements.EntitlementDecision
import nl.incedo.paywall.grants.GrantDecision
import nl.incedo.paywall.metering.MeterDecision

/**
 * PW-01: content tier. FREE is never gated. PREMIUM requires any active subscription.
 * COMPLETE requires a rank-2 (complete) plan — basic subscribers are tier-locked (UP-12).
 */
enum class ContentTier { FREE, PREMIUM, COMPLETE }

@Serializable
data class Article(val id: ArticleId, val tier: ContentTier)

/** The requesting subject: anonymous visitor or logged-in user (AC-07: invalid token degrades to anonymous). */
data class Subject(
    val visitorId: VisitorId,
    val userId: UserId? = null,
) {
    val registered: Boolean get() = userId != null
    val subjectId: SubjectId get() = userId?.let { SubjectId.of(it) } ?: SubjectId.of(visitorId)
}

/**
 * DY-01: configurable weights for the heuristic propensity scorer.
 * Defaults match the original hard-coded values so existing experiments
 * need no migration.
 */
@Serializable
data class ScorerWeights(
    /** Points per premium read consumed this month (strongest conversion predictor). */
    val meterWeight: Int = 8,
    /** Points per wall/read event in the last 30 days. */
    val activityWeight: Int = 3,
    /** Cap on tenure contribution — each day up to this limit contributes 1 point. */
    val tenureCap: Int = 60,
    /** DY-01: bonus added when the subject is a registered (logged-in) visitor. */
    val registeredBonus: Int = 5,
)

/** Per-variant strategy configuration (PW-05/06): parameters are config, not code. */
@Serializable
sealed interface StrategyConfig {
    @Serializable
    data object Hard : StrategyConfig

    @Serializable
    data class Metered(
        /** PW-20 default 5. */
        val limit: Int = 5,
        /** PW-25 (COULD): higher limit for registered visitors; null = same as anonymous. */
        val registeredLimit: Int? = null,
        /**
         * MT-06: meter period type per variant config. Supported values:
         *   "calendar_month" (default, PW-24) — resets on the first of each month (Europe/Amsterdam).
         *   "rolling_30d"                      — rolling window of the last 30 calendar days.
         * Structure is in place for future period types without a schema change.
         */
        val periodType: String = "calendar_month",
    ) : StrategyConfig

    @Serializable
    data object Freemium : StrategyConfig

    @Serializable
    data class Dynamic(
        /** PW-42: floor rule — gate always appears at or before the Nth premium article per month (default 10). */
        val floorLimit: Int = 10,
        /** DY-02: heuristic score threshold — at or above this score the gate is shown (default 40). */
        val tSoft: Int = 40,
        /** DY-02: upper threshold — at or above this the hard-gate copy is used (default 70). */
        val tHard: Int = 70,
        /** DY-01: per-variant scorer weights; defaults preserve the original heuristic. */
        val scorerWeights: ScorerWeights = ScorerWeights(),
    ) : StrategyConfig
}

/** Everything the engine needs; the caller (command/query handler) builds the decision models from event queries. */
data class AccessRequest(
    val article: Article,
    val subject: Subject,
    val strategy: StrategyConfig,
    val entitlement: EntitlementDecision,
    val grant: GrantDecision,
    val meter: MeterDecision,
    /**
     * PW-40 verdict for the dynamic strategy, rebuilt from CEP-published
     * advice events (the CEP owns propensity scoring and thresholds; the
     * access layer acts on its events and applies the mechanical floor
     * rule PW-42 on top — no synchronous CEP call in the decide path).
     */
    val cepAdvice: CepAdviceDecision = CepAdviceDecision(),
    /**
     * DY-01/02: local propensity score [0, 100] computed by [PropensityScorer]
     * before calling the engine. Null means no local scorer is configured —
     * the engine falls back to CEP advice + floor rule only.
     */
    val propensityScore: Int? = null,
    /** AC-13: true when the visitor dismissed the soft gate within the current session window. */
    val softGateDismissed: Boolean = false,
    /** PW-50: true when the variant requires registration before metering/gating. */
    val registrationWall: Boolean = false,
    val nowEpochMs: Long,
)

enum class AccessReason {
    FREE_CONTENT, ENTITLED, GRANT, METER_CREDIT, DYNAMIC_OPEN,
    /** MT-05/SEO-02: verified search crawler (edge signal, not UA alone). */
    VERIFIED_CRAWLER,
    /** PA-01/IPW-02: user is a member of a partner with an active subscription. */
    PARTNER_ENTITLED,
    /** NFR-15: admin kill switch active for this variant — open access until restored. */
    VARIANT_KILLED,
}

sealed interface AccessDecision {
    /**
     * Serve the full article. [countsTowardMeter] is true only for a freshly
     * counted metered read (MT-04; PW-21: re-reads don't consume credit).
     */
    data class Full(val reason: AccessReason, val countsTowardMeter: Boolean = false) : AccessDecision

    /** Serve teaser + gate (AC-01: the premium body never leaves the server). */
    data class Gated(
        val strategy: StrategyConfig,
        val meterUsed: Int? = null,
        val meterLimit: Int? = null,
        /** UP-12: basic subscriber hitting complete-tier content → client shows tier-upgrade offer. */
        val tierLocked: Boolean = false,
        /** PW-50: visitor needs to register before the normal paywall strategy applies. */
        val registrationRequired: Boolean = false,
    ) : AccessDecision
}

/**
 * The access decision flow of Doc 2 §1 as a pure function: free → full;
 * entitled → full; live grant → full; otherwise the variant's paywall
 * strategy decides. Wall-event logging (AN-*) is the caller's concern.
 */
object AccessDecisionEngine {

    fun decide(request: AccessRequest): AccessDecision {
        val article = request.article

        if (article.tier == ContentTier.FREE) {
            return AccessDecision.Full(AccessReason.FREE_CONTENT)
        }

        // UP-12: complete-tier content requires a rank-2 (complete) plan.
        // Basic subscribers (rank=1) are gated with tierLocked=true so the client
        // can show a tier-upgrade offer. Grants still override tier-lock.
        if (article.tier == ContentTier.COMPLETE) {
            if (request.entitlement.hasValidEntitlementForTier(request.nowEpochMs, minRank = 2)) {
                return AccessDecision.Full(AccessReason.ENTITLED)
            }
            if (request.grant.hasLiveGrant(request.nowEpochMs)) {
                return AccessDecision.Full(AccessReason.GRANT)
            }
            val isBasicSubscriber = request.entitlement.hasValidEntitlement(request.nowEpochMs)
            return AccessDecision.Gated(request.strategy, tierLocked = isBasicSubscriber)
        }

        if (request.entitlement.hasValidEntitlement(request.nowEpochMs)) {
            return AccessDecision.Full(AccessReason.ENTITLED)
        }
        if (request.grant.hasLiveGrant(request.nowEpochMs)) {
            return AccessDecision.Full(AccessReason.GRANT)
        }

        // PW-50: registration wall — interpose before the variant strategy when
        // configured and the visitor is anonymous (not logged in).
        if (request.registrationWall && !request.subject.registered) {
            return AccessDecision.Gated(request.strategy, registrationRequired = true)
        }

        return when (val strategy = request.strategy) {
            is StrategyConfig.Hard -> AccessDecision.Gated(strategy) // PW-10

            is StrategyConfig.Freemium -> AccessDecision.Gated(strategy) // PW-30

            is StrategyConfig.Metered -> {
                val limit = if (request.subject.registered) {
                    strategy.registeredLimit ?: strategy.limit
                } else {
                    strategy.limit
                }
                if (request.meter.hasCreditFor(article.id, limit)) {
                    AccessDecision.Full(
                        reason = AccessReason.METER_CREDIT,
                        countsTowardMeter = !request.meter.isCounted(article.id),
                    )
                } else {
                    AccessDecision.Gated(strategy, meterUsed = request.meter.used, meterLimit = limit)
                }
            }

            is StrategyConfig.Dynamic -> {
                val floorReached = !request.meter.hasCreditFor(article.id, strategy.floorLimit) // PW-42
                val score = request.propensityScore
                // DY-02: heuristic score gates at or above tSoft; floor rule and CEP advice gate unconditionally.
                val isHardGate = request.cepAdvice.gateAdvised(request.nowEpochMs) || floorReached ||
                    (score != null && score >= strategy.tHard)
                val isSoftGate = !isHardGate && score != null && score >= strategy.tSoft
                // AC-13: soft gate is dismissible once per session; hard gate is not.
                if (isHardGate || (isSoftGate && !request.softGateDismissed)) {
                    AccessDecision.Gated(strategy, meterUsed = request.meter.used, meterLimit = strategy.floorLimit)
                } else {
                    AccessDecision.Full(
                        reason = AccessReason.DYNAMIC_OPEN,
                        countsTowardMeter = !request.meter.isCounted(article.id), // feeds the floor rule
                    )
                }
            }
        }
    }
}
