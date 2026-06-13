package nl.incedo.paywall.core

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Serializable @JvmInline value class VisitorId(val value: String)
@Serializable @JvmInline value class UserId(val value: String)
@Serializable @JvmInline value class ArticleId(val value: String)
@Serializable @JvmInline value class WallId(val value: String)
@Serializable @JvmInline value class ExperimentId(val value: String)
@Serializable @JvmInline value class GrantId(val value: String)
@Serializable @JvmInline value class PlanId(val value: String)
@Serializable @JvmInline value class PartnerId(val value: String)

/** Reference to the external subscription administration's id — not owned here. */
@Serializable @JvmInline value class SubscriptionId(val value: String)

/**
 * Abstract subject of metering and entitlements (DM-03): an anonymous
 * visitor or a logged-in user. On login the visitor's state is merged into
 * the user's (MT-03/US-04).
 */
@Serializable @JvmInline value class SubjectId(val value: String) {
    companion object {
        fun of(visitorId: VisitorId) = SubjectId("visitor:${visitorId.value}")
        fun of(userId: UserId) = SubjectId("user:${userId.value}")
    }
}
