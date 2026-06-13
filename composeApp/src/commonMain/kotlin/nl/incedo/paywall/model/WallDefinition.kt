package nl.incedo.paywall.model

/**
 * Structured wall definition (ADM-11): the visual wall editor outputs this,
 * the gate component renders it. No arbitrary HTML/JS — content only.
 * The four types mirror the paywall strategies of Doc 1 (PW-*).
 */
enum class WallType(val label: String) {
    Hard("Hard"),
    Metered("Metered"),
    Freemium("Freemium"),
    Dynamic("Dynamic"),
}

enum class Channel(val label: String, val short: String) {
    Web("Web", "W"),
    MobileApp("Mobile app", "M"),
    Chat("Chat", "C"),
    InProduct("In-product", "P"),
    Email("Email", "E"),
    Sms("SMS", "S"),
}

data class WallDefinition(
    val type: WallType = WallType.Metered,
    val title: String = "You've reached this month's free limit",
    val body: String = "Pro includes unlimited documents, exports and payment reminders for €10 per user / month.",
    val primaryCta: String = "Upgrade to Pro",
    val secondaryCta: String = "Compare plans",
    val channels: Set<Channel> = setOf(Channel.Web, Channel.MobileApp, Channel.Email),
    val darkPreview: Boolean = false,
    /** PW-50: show a registration wall for anonymous visitors before the paywall strategy runs. */
    val registrationWall: Boolean = false,
    /** AC-14: show a GDPR consent step before the subscription gate where legally required. */
    val requireConsentStep: Boolean = false,
    /** ADM-11: optional image URL shown in the gate (empty = no image block). */
    val imageUrl: String = "",
    /** ADM-17: alt text for the image block (empty = decorative / aria-hidden). */
    val imageAlt: String = "",
    /** ADM-11: optional legal/disclaimer text rendered below CTAs (empty = no block). */
    val legalText: String = "",
    /**
     * ADM-15: per-locale copy overrides keyed by BCP-47 locale tag (e.g. "nl-NL").
     * Only fields that differ from the defaults need to be set; null = use default.
     */
    val translations: Map<String, nl.incedo.paywall.walls.WallCopy> = emptyMap(),
)

/**
 * PW-11: default copy per wall type — hard-paywall gate emphasises "unlimited
 * access" rather than "you've reached your limit" (which is metered copy).
 * The ConfigPanel swaps to these defaults when the user changes wall type,
 * unless the title has already been customised.
 */
data class WallTypeCopy(val title: String, val body: String)

val defaultCopyFor: Map<WallType, WallTypeCopy> = mapOf(
    WallType.Metered to WallTypeCopy(
        title = "You've reached this month's free limit",
        body = "Pro includes unlimited documents, exports and payment reminders for €10 per user / month.",
    ),
    WallType.Hard to WallTypeCopy(
        title = "Unlock unlimited access",
        body = "Get Pro for unlimited documents, SEPA direct debit, CSV exports, and priority support — from €10 per user / month.",
    ),
    WallType.Freemium to WallTypeCopy(
        title = "This content is for Pro members",
        body = "Upgrade to Pro for unlimited documents and premium features — from €10 per user / month.",
    ),
    WallType.Dynamic to WallTypeCopy(
        title = "Continue reading with Pro",
        body = "Based on your reading habits, you'd benefit from unlimited access — try Pro for €10 per user / month.",
    ),
)

enum class WallStatus(val label: String) {
    Live("Live"),
    Draft("Draft"),
    Paused("Paused"),
}

/** Row in the walls overview (design "Wall Designer", variant B1). */
data class WallSummary(
    val id: String,
    val name: String,
    val type: WallType,
    val channels: Set<Channel>,
    val status: WallStatus,
    val variants: Int,
    val conversion: String,
    val updated: String,
)

val demoWalls = listOf(
    WallSummary(
        id = "demo-1",
        name = "Metered limit — invoices",
        type = WallType.Metered,
        channels = setOf(Channel.Web, Channel.MobileApp, Channel.Chat, Channel.InProduct, Channel.Email),
        status = WallStatus.Draft,
        variants = 2,
        conversion = "—",
        updated = "Today",
    ),
    WallSummary(
        id = "demo-2",
        name = "Hard wall — trial expiry",
        type = WallType.Hard,
        channels = setOf(Channel.Web, Channel.MobileApp, Channel.Email, Channel.Sms),
        status = WallStatus.Live,
        variants = 2,
        conversion = "4.8%",
        updated = "4 Jun",
    ),
    WallSummary(
        id = "demo-3",
        name = "Freemium — premium content",
        type = WallType.Freemium,
        channels = setOf(Channel.Web, Channel.InProduct, Channel.Email),
        status = WallStatus.Live,
        variants = 1,
        conversion = "2.1%",
        updated = "28 May",
    ),
    WallSummary(
        id = "demo-4",
        name = "Dynamic — CEP gate",
        type = WallType.Dynamic,
        channels = setOf(Channel.Web),
        status = WallStatus.Paused,
        variants = 1,
        conversion = "0.9%",
        updated = "14 May",
    ),
)

/** API channel names are the enum names in lowercase (one contract, every target). */
fun Channel.apiName(): String = name.lowercase()

fun channelFromApi(value: String): Channel? =
    Channel.entries.find { it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) }

fun wallTypeFromApi(value: String): WallType =
    WallType.entries.find { it.name.equals(value, ignoreCase = true) } ?: WallType.Metered
