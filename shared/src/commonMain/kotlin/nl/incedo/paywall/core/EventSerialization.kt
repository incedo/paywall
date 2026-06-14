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
import nl.incedo.paywall.experiments.VariantKilled
import nl.incedo.paywall.experiments.VariantRestored
import nl.incedo.paywall.analytics.SoftGateDismissed
import nl.incedo.paywall.analytics.WallEventRecorded
import nl.incedo.paywall.cep.CepGateAdviceWithdrawn
import nl.incedo.paywall.cep.CepGateAdvised
import nl.incedo.paywall.entitlements.CancellationSurveySubmitted
import nl.incedo.paywall.entitlements.EntitlementGranted
import nl.incedo.paywall.entitlements.EntitlementRevoked
import nl.incedo.paywall.entitlements.SubscriptionPaused
import nl.incedo.paywall.entitlements.SubscriptionResumed
import nl.incedo.paywall.offers.OfferAccepted
import nl.incedo.paywall.offers.OfferDeclined
import nl.incedo.paywall.offers.OfferSuppressed
import nl.incedo.paywall.offers.OfferTriggered
import nl.incedo.paywall.grants.DataGateConsentGiven
import nl.incedo.paywall.grants.GrantIssued
import nl.incedo.paywall.grants.GrantRevoked
import nl.incedo.paywall.grants.ShareTokenIssued
import nl.incedo.paywall.partners.PartnerCreated
import nl.incedo.paywall.partners.PartnerIpRangeConfigured
import nl.incedo.paywall.partners.PartnerMemberAdded
import nl.incedo.paywall.partners.PartnerMemberRemoved
import nl.incedo.paywall.partners.PartnerOffboarded
import nl.incedo.paywall.metering.MeterIncremented
import nl.incedo.paywall.metering.MeterReset
import nl.incedo.paywall.notifications.MailSent
import nl.incedo.paywall.walls.WallConfigChanged
import nl.incedo.paywall.walls.WallCreated
import nl.incedo.paywall.walls.WallPublished
import nl.incedo.paywall.walls.WallTemplateCreated
import nl.incedo.paywall.storybook.StoryRegistered
import nl.incedo.paywall.storybook.StoryArchived
import nl.incedo.paywall.storybook.StoryMetadataUpdated
import nl.incedo.paywall.storybook.ScenarioRegistered
import nl.incedo.paywall.storybook.ScenarioMetadataUpdated
import nl.incedo.paywall.storybook.ScenarioArchived
import nl.incedo.paywall.storybook.DecoratorRegistered
import nl.incedo.paywall.storybook.DecoratorMetadataUpdated
import nl.incedo.paywall.storybook.DecoratorPriorityChanged
import nl.incedo.paywall.storybook.DecoratorLinkedToStory
import nl.incedo.paywall.storybook.DecoratorLinkedToScenario
import nl.incedo.paywall.storybook.DecoratorArchived
import nl.incedo.paywall.storybook.ControlSchemaRegistered
import nl.incedo.paywall.storybook.ControlAdded
import nl.incedo.paywall.storybook.ControlRemoved
import nl.incedo.paywall.storybook.ControlDefaultChanged
import nl.incedo.paywall.storybook.ControlSchemaArchived
import nl.incedo.paywall.storybook.ResponsiveProfileRegistered
import nl.incedo.paywall.storybook.ResponsiveFormFactorSupported
import nl.incedo.paywall.storybook.ResponsiveWidthClassesDefined
import nl.incedo.paywall.storybook.ResponsiveNavigationPatternSet
import nl.incedo.paywall.storybook.ResponsiveDensityProfileSet
import nl.incedo.paywall.storybook.ResponsiveExpectationLinked
import nl.incedo.paywall.storybook.ResponsiveLayoutRuleAdded
import nl.incedo.paywall.storybook.ResponsiveProfileArchived
import nl.incedo.paywall.storybook.PhaseRegistered
import nl.incedo.paywall.storybook.CapabilityAddedToPhase
import nl.incedo.paywall.storybook.PhaseActivated
import nl.incedo.paywall.storybook.PhaseSatisfied
import nl.incedo.paywall.storybook.PhaseSuperseded
import nl.incedo.paywall.storybook.GovernancePolicyRegistered
import nl.incedo.paywall.storybook.QualityGateAttached
import nl.incedo.paywall.storybook.OwnerAssigned
import nl.incedo.paywall.storybook.EvidenceLinked
import nl.incedo.paywall.storybook.GovernanceDecisionRecorded
import nl.incedo.paywall.storybook.LifecycleGoverned

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
        subclass(CancellationSurveySubmitted::class)
        subclass(DataGateConsentGiven::class)
        subclass(GrantIssued::class)
        subclass(GrantRevoked::class)
        subclass(ShareTokenIssued::class)
        subclass(PartnerCreated::class)
        subclass(PartnerMemberAdded::class)
        subclass(PartnerMemberRemoved::class)
        subclass(PartnerOffboarded::class)
        subclass(PartnerIpRangeConfigured::class)
        subclass(BrandCreated::class)
        subclass(BrandThemeUpdated::class)
        subclass(IdentityLinked::class)
        subclass(IdentityUnlinked::class)
        subclass(UserDeleted::class)
        subclass(ExperimentConfigPublished::class)
        subclass(VariantKilled::class)
        subclass(VariantRestored::class)
        subclass(CepGateAdvised::class)
        subclass(CepGateAdviceWithdrawn::class)
        subclass(OfferAccepted::class)
        subclass(OfferTriggered::class)
        subclass(OfferSuppressed::class)
        subclass(OfferDeclined::class)
        subclass(SoftGateDismissed::class)
        subclass(WallEventRecorded::class)
        subclass(WallCreated::class)
        subclass(WallConfigChanged::class)
        subclass(WallPublished::class)
        subclass(WallTemplateCreated::class)
        subclass(MailSent::class)
        subclass(StoryRegistered::class)
        subclass(StoryArchived::class)
        subclass(StoryMetadataUpdated::class)
        subclass(ScenarioRegistered::class)
        subclass(ScenarioMetadataUpdated::class)
        subclass(ScenarioArchived::class)
        subclass(ControlSchemaRegistered::class)
        subclass(ControlAdded::class)
        subclass(ControlRemoved::class)
        subclass(ControlDefaultChanged::class)
        subclass(ControlSchemaArchived::class)
        subclass(DecoratorRegistered::class)
        subclass(DecoratorMetadataUpdated::class)
        subclass(DecoratorPriorityChanged::class)
        subclass(DecoratorLinkedToStory::class)
        subclass(DecoratorLinkedToScenario::class)
        subclass(DecoratorArchived::class)
        subclass(ResponsiveProfileRegistered::class)
        subclass(ResponsiveFormFactorSupported::class)
        subclass(ResponsiveWidthClassesDefined::class)
        subclass(ResponsiveNavigationPatternSet::class)
        subclass(ResponsiveDensityProfileSet::class)
        subclass(ResponsiveExpectationLinked::class)
        subclass(ResponsiveLayoutRuleAdded::class)
        subclass(ResponsiveProfileArchived::class)
        subclass(PhaseRegistered::class)
        subclass(CapabilityAddedToPhase::class)
        subclass(PhaseActivated::class)
        subclass(PhaseSatisfied::class)
        subclass(PhaseSuperseded::class)
        subclass(GovernancePolicyRegistered::class)
        subclass(QualityGateAttached::class)
        subclass(OwnerAssigned::class)
        subclass(EvidenceLinked::class)
        subclass(GovernanceDecisionRecorded::class)
        subclass(LifecycleGoverned::class)
    }
}

/** JSON configuration for event persistence: stable, additive-schema friendly. */
val eventJson = Json {
    serializersModule = paywallSerializersModule
    ignoreUnknownKeys = true
    encodeDefaults = true
    classDiscriminator = "type"
}
