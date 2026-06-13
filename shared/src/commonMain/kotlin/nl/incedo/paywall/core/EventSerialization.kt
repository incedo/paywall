package nl.incedo.paywall.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import nl.incedo.paywall.brands.BrandCreated
import nl.incedo.paywall.brands.BrandThemeUpdated
import nl.incedo.paywall.accounts.IdentityLinked
import nl.incedo.paywall.accounts.IdentityUnlinked
import nl.incedo.paywall.accounts.UserDeleted
import nl.incedo.paywall.experiments.ExperimentConfigPublished
import nl.incedo.paywall.analytics.WallEventRecorded
import nl.incedo.paywall.cep.CepGateAdviceWithdrawn
import nl.incedo.paywall.cep.CepGateAdvised
import nl.incedo.paywall.entitlements.EntitlementGranted
import nl.incedo.paywall.entitlements.EntitlementRevoked
import nl.incedo.paywall.entitlements.SubscriptionPaused
import nl.incedo.paywall.entitlements.SubscriptionResumed
import nl.incedo.paywall.offers.OfferDeclined
import nl.incedo.paywall.offers.OfferSuppressed
import nl.incedo.paywall.offers.OfferTriggered
import nl.incedo.paywall.grants.GrantIssued
import nl.incedo.paywall.grants.GrantRevoked
import nl.incedo.paywall.grants.ShareTokenIssued
import nl.incedo.paywall.partners.PartnerCreated
import nl.incedo.paywall.partners.PartnerIpRangeConfigured
import nl.incedo.paywall.partners.PartnerMemberAdded
import nl.incedo.paywall.partners.PartnerMemberRemoved
import nl.incedo.paywall.metering.MeterIncremented
import nl.incedo.paywall.metering.MeterReset
import nl.incedo.paywall.walls.WallConfigChanged
import nl.incedo.paywall.walls.WallCreated
import nl.incedo.paywall.walls.WallPublished

/**
 * Polymorphic registration of every domain event, shared by all event-store
 * adapters (PostgreSQL JSONB persistence, API payloads, exports per AN-04).
 * New events MUST be registered here — the serialization round-trip test
 * fails otherwise.
 */
val paywallSerializersModule = SerializersModule {
    polymorphic(DomainEvent::class) {
        subclass(MeterIncremented::class)
        subclass(MeterReset::class)
        subclass(EntitlementGranted::class)
        subclass(EntitlementRevoked::class)
        subclass(SubscriptionPaused::class)
        subclass(SubscriptionResumed::class)
        subclass(GrantIssued::class)
        subclass(GrantRevoked::class)
        subclass(ShareTokenIssued::class)
        subclass(PartnerCreated::class)
        subclass(PartnerMemberAdded::class)
        subclass(PartnerMemberRemoved::class)
        subclass(PartnerIpRangeConfigured::class)
        subclass(BrandCreated::class)
        subclass(BrandThemeUpdated::class)
        subclass(IdentityLinked::class)
        subclass(IdentityUnlinked::class)
        subclass(UserDeleted::class)
        subclass(ExperimentConfigPublished::class)
        subclass(CepGateAdvised::class)
        subclass(CepGateAdviceWithdrawn::class)
        subclass(OfferTriggered::class)
        subclass(OfferSuppressed::class)
        subclass(OfferDeclined::class)
        subclass(WallEventRecorded::class)
        subclass(WallCreated::class)
        subclass(WallConfigChanged::class)
        subclass(WallPublished::class)
    }
}

/** JSON configuration for event persistence: stable, additive-schema friendly. */
val eventJson = Json {
    serializersModule = paywallSerializersModule
    ignoreUnknownKeys = true
    encodeDefaults = true
    classDiscriminator = "type"
}
